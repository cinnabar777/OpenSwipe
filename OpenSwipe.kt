// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.gesture

import android.content.Context
import android.util.Log
import helium314.keyboard.keyboard.Keyboard
import helium314.keyboard.latin.SuggestedWords.SuggestedWordInfo
import helium314.keyboard.latin.common.InputPointers
import helium314.keyboard.latin.dictionary.Dictionary
import helium314.keyboard.latin.utils.SuggestionResults

import java.io.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ln
import kotlin.math.PI
import kotlin.math.sqrt

// ─── Constants ──────────────────────────────────────────────────────────────

private const val N_PTS = 16
private const val DTW_BAND = 3
private const val FREQ_WEIGHT = 0.15f
private const val USER_BOOST_MAX = 50
private const val PROXIMITY_RADIUS = 0.05f
private const val PUNCTUATION_RADIUS = 0.06f
private const val LB_SAFETY_MARGIN = 1.15f
private const val EARLY_ABANDON_TOL = 1.05f
private const val SEQ_MATCH_RADIUS_SQ = 0.012f

// [BUG FIX #3 — OOM] Hard cap on entries kept per first-letter bucket.
// After sorting by frequency, only the top MAX_ENTRIES_PER_LETTER words are
// retained.  For English this cap is never reached (~3 k words per letter at
// most); for German/Finnish/Turkish it prevents the index growing to 90+ MB.
private const val MAX_ENTRIES_PER_LETTER = 8_000

// [BUG FIX #4 — unbounded growth] sUserPaths and sUserBoost are trimmed to
// these limits at save time so the on-disk file and cold-start load stay fast.
private const val MAX_USER_PATHS = 1_000
private const val MAX_USER_BOOSTS = 2_000

// ─── Main class ─────────────────────────────────────────────────────────────

class SwipeGestureEngineKotlin {

    // ─── Nested classes ─────────────────────────────────────────────────────

    class IndexEntry(
        val word: String,
        path: FloatArray,
        val frequency: Int
    ) {
        val freqBonus: Float = if (frequency > 0) (ln(frequency + 1.0) * FREQ_WEIGHT).toFloat() else 0f

        // [BUG FIX #1 — race condition] pathLen is read by rankByIndex() and written
        // by recordAccepted() on potentially different threads.  @Volatile ensures
        // every read sees the latest write without synchronisation overhead.
        // A slightly stale pathLen only nudges the DTW-normalisation denominator
        // by a small amount — it does not corrupt the path itself.
        @Volatile var pathLen: Float = 0f
            private set

        val canonicalSeq: String

        // [BUG FIX #1 — race condition] The original design stored the packed path as
        // four separate Long fields (path0…path3).  updatePath() wrote them one at a
        // time; unpackPath() read them one at a time.  If recordAccepted() called
        // updatePath() while rankByIndex() called unpackPath() on another thread, the
        // reader could see e.g. path0=NEW + path1=OLD + path2=OLD + path3=OLD —
        // a torn read that silently corrupts the candidate path and causes certain
        // words to score very badly regardless of how accurately the user gestures them.
        //
        // Fix: store all four longs in a single LongArray behind an AtomicReference.
        // set() is one atomic pointer swap — the reader either sees the complete old
        // array or the complete new one, never a mix.
        private val packedPath = AtomicReference(LongArray(4))

        init {
            updatePath(path)
            canonicalSeq = canonicalNodeSeq(word)
        }

        fun updatePath(newPath: FloatArray) {
            val arr = LongArray(4)
            arr[0] = pack8Bytes(newPath, 0)
            arr[1] = pack8Bytes(newPath, 8)
            arr[2] = pack8Bytes(newPath, 16)
            arr[3] = pack8Bytes(newPath, 24)
            packedPath.set(arr)            // single atomic publish
            pathLen = pathLength(newPath)  // @Volatile write; visible to other threads
        }

        fun unpackPath(out: FloatArray) {
            val arr = packedPath.get()     // single atomic snapshot — no torn read
            unpack8Bytes(arr[0], out, 0)
            unpack8Bytes(arr[1], out, 8)
            unpack8Bytes(arr[2], out, 16)
            unpack8Bytes(arr[3], out, 24)
        }
    }

    class GestureIndex(
        val byFirst: Map<Char, List<IndexEntry>>,
        val charToPos: Array<FloatArray>,
        val punctuationPos: Map<Char, FloatArray>
    )

    // Carries a detected snake/skip letter together with its normalised
    // position along the gesture (0.0 = start, 1.0 = end).  Used to check that
    // the letter appears at roughly the right position in a candidate word.
    data class LetterHint(val char: Char, val gesturePosition: Float)

    // ─── Companion object ────────────────────────────────────────────────────

    companion object {

        private val sUserBoost = ConcurrentHashMap<String, Int>()
        private val sUserPaths = ConcurrentHashMap<String, FloatArray>()
        private val sUserBoostCache = FloatArray(USER_BOOST_MAX + 1).apply {
            for (i in indices) {
                this[i] = (ln(i + 1.0) * 0.08f).toFloat()
            }
        }
        private var sUserDataFile: File? = null

        private fun loadUserData() {
            if (sUserDataFile == null || !sUserDataFile!!.exists()) return
            synchronized(this) {
                try {
                    DataInputStream(BufferedInputStream(FileInputStream(sUserDataFile!!))).use { inp ->
                        val version = inp.readInt()
                        if (version != 1) return
                        sUserBoost.clear()
                        val numBoosts = inp.readInt()
                        for (i in 0 until numBoosts) {
                            val key = inp.readUTF()
                            val count = inp.readInt()
                            sUserBoost[key] = count
                        }
                        sUserPaths.clear()
                        val numPaths = inp.readInt()
                        for (i in 0 until numPaths) {
                            val key = inp.readUTF()
                            val path = FloatArray(N_PTS * 2)
                            for (j in path.indices) path[j] = inp.readFloat()
                            sUserPaths[key] = path
                        }
                    }
                } catch (e: IOException) {
                    Log.e("GestureEngineKotlin", "Error loading user data", e)
                }
            }
        }

        private fun saveUserData() {
            if (sUserDataFile == null) return
            try {
                // [BUG FIX #4 — unbounded growth] Trim both maps before persisting so
                // the binary file and the in-memory maps never grow without bound.
                // We keep the highest-boosted words (most likely to be useful again) and
                // drop the tail.  Paths are trimmed to the same ceiling; any word whose
                // path is dropped will be regenerated from the canonical dictionary path
                // the next time buildIndex() runs, which is acceptable.
                if (sUserBoost.size > MAX_USER_BOOSTS) {
                    sUserBoost.entries
                        .sortedBy { it.value }
                        .take(sUserBoost.size - MAX_USER_BOOSTS)
                        .forEach { sUserBoost.remove(it.key) }
                }
                if (sUserPaths.size > MAX_USER_PATHS) {
                    // Drop paths for words with the lowest boost counts first.
                    sUserPaths.keys
                        .sortedBy { sUserBoost[it] ?: 0 }
                        .take(sUserPaths.size - MAX_USER_PATHS)
                        .forEach { sUserPaths.remove(it) }
                }

                DataOutputStream(BufferedOutputStream(FileOutputStream(sUserDataFile!!))).use { out ->
                    out.writeInt(1) // version
                    out.writeInt(sUserBoost.size)
                    for ((key, count) in sUserBoost) {
                        out.writeUTF(key)
                        out.writeInt(count)
                    }
                    out.writeInt(sUserPaths.size)
                    for ((key, path) in sUserPaths) {
                        out.writeUTF(key)
                        for (v in path) out.writeFloat(v)
                    }
                }
            } catch (e: IOException) {
                Log.e("GestureEngineKotlin", "Error saving user data", e)
            }
        }

        private fun saveUserDataAsync() {
            Thread {
                synchronized(this) { saveUserData() }
            }.start()
        }

        // ─── Public static API ────────────────────────────────────────────────

        @JvmStatic
        fun initialize(context: Context) {
            if (sUserDataFile != null) return
            sUserDataFile = File(context.filesDir, "gesture_user_data_kotlin.bin")
            loadUserData()
            Log.i("GestureEngineKotlin", "initialize: loaded ${sUserBoost.size} boosted words, ${sUserPaths.size} user paths")
        }

        @JvmStatic
        fun buildIndex(
            facilitator: helium314.keyboard.latin.DictionaryFacilitator,
            keyboard: Keyboard
        ): GestureIndex {
            val charToPos = buildCharToPos(keyboard)

            val punctuationPos = mutableMapOf<Char, FloatArray>()
            val kw = keyboard.mOccupiedWidth.toFloat()
            val kh = keyboard.mOccupiedHeight.toFloat()
            for (key in keyboard.sortedKeys) {
                val code = key.code
                val hitBox = key.hitBox
                when (code.toChar()) {
                    ',' -> punctuationPos[','] = floatArrayOf(hitBox.exactCenterX() / kw, hitBox.exactCenterY() / kh)
                    '.' -> punctuationPos['.'] = floatArrayOf(hitBox.exactCenterX() / kw, hitBox.exactCenterY() / kh)
                    '\'' -> punctuationPos['\''] = floatArrayOf(hitBox.exactCenterX() / kw, hitBox.exactCenterY() / kh)
                }
            }

            val byFirst = mutableMapOf<Char, MutableList<IndexEntry>>()

            facilitator.forEachMainDictionaryWord { raw, freqVal ->
                if (raw == null) return@forEachMainDictionaryWord
                if (facilitator.isBlacklisted(raw)) return@forEachMainDictionaryWord

                var freq = freqVal ?: 0
                val lower = raw.lowercase(Locale.ROOT)
                val boost = sUserBoost[lower]
                if (boost != null) freq = (freq + boost * 5).coerceAtMost(255)

                if (freq < 3) return@forEachMainDictionaryWord
                if (lower.isEmpty()) return@forEachMainDictionaryWord

                val first = lower[0]
                if (first !in 'a'..'z') return@forEachMainDictionaryWord

                val userPath = sUserPaths[lower]
                val path = if (userPath != null && userPath.size == N_PTS * 2) {
                    val blended = FloatArray(N_PTS * 2)
                    val dictPath = wordPath(lower, charToPos)
                    for (i in blended.indices) blended[i] = dictPath[i] * 0.3f + userPath[i] * 0.7f
                    blended
                } else {
                    wordPath(lower, charToPos)
                }

                byFirst.getOrPut(first) { mutableListOf() }.add(IndexEntry(raw, path, freq))
            }

            // Sort each bucket by frequency descending, then apply the bucket cap.
            // [BUG FIX #3 — OOM] Without the cap, languages like German/Finnish/Turkish
            // produce 400 k+ IndexEntry objects holding ~90 MB of heap, triggering OOM
            // on devices with 192 MB limits.  Capping at MAX_ENTRIES_PER_LETTER keeps
            // the top-frequency words (the ones users actually gesture) and discards the
            // long tail of rare words that would never rank in the top results anyway.
            var totalEntries = 0
            for (list in byFirst.values) {
                list.sortByDescending { it.frequency }
                if (list.size > MAX_ENTRIES_PER_LETTER) {
                    list.subList(MAX_ENTRIES_PER_LETTER, list.size).clear()
                }
                totalEntries += list.size
            }

            Log.i("GestureEngineKotlin", "buildIndex: $totalEntries entries across ${byFirst.size} first-letter buckets")

            return GestureIndex(byFirst, charToPos, punctuationPos)
        }

        @JvmStatic
        fun layoutFingerprint(keyboard: Keyboard): Int =
            Arrays.deepHashCode(buildCharToPos(keyboard))

        @JvmStatic
        fun rankByIndex(
            index: GestureIndex?,
            pointers: InputPointers,
            keyboard: Keyboard,
            maxResults: Int,
            predictionSet: Set<String>?
        ): SuggestionResults {
            val empty = SuggestionResults(1, false, false)
            if (index == null) return empty

            val n = pointers.pointerSize
            if (n < 2) return empty

            val xs = pointers.xCoordinates
            val ys = pointers.yCoordinates
            val kw = keyboard.mOccupiedWidth.toFloat()
            val kh = keyboard.mOccupiedHeight.toFloat()

            val rawFlat = FloatArray(n * 2)
            for (i in 0 until n) {
                rawFlat[2 * i] = xs[i] / kw
                rawFlat[2 * i + 1] = ys[i] / kh
            }

            val inputVec = resampleFlat(rawFlat, n, N_PTS)
            val inputLen = pathLength(inputVec)
            val charToPos = index.charToPos

            // ─── DETECT PAUSES & LOOPS (Hard constraints) ────────────────────
            val times = pointers.times
            val constraints = extractHardConstraints(rawFlat, n, times, charToPos)

            // ─── DETECT SNAKES & ARCS (Soft constraints) ─────────────────────
            val (snakeHints, skipHints) = detectKeyInteractions(inputVec, charToPos)

            // ─── PREPARE CANDIDATES ──────────────────────────────────────────
            val startLetters = nearestLettersFromMap(rawFlat[0], rawFlat[1], charToPos)
            val endLetters = nearestLettersFromMap(rawFlat[2 * (n - 1)], rawFlat[2 * (n - 1) + 1], charToPos)

            val candidates = mutableListOf<IndexEntry>()
            for (first in startLetters) {
                index.byFirst[first]?.let { candidates.addAll(it) }
            }
            if (candidates.isEmpty()) return empty

            // Apply hard constraints (pauses/loops)
            var finalCandidates = mutableListOf<IndexEntry>()
            if (constraints.isNotEmpty()) {
                for (e in candidates) {
                    val wordLower = e.word.lowercase(Locale.ROOT)
                    var matches = true
                    var lastFoundIdx = -1
                    for (c in constraints) {
                        val idx = wordLower.indexOf(c, startIndex = lastFoundIdx + 1)
                        if (idx == -1) { matches = false; break }
                        lastFoundIdx = idx
                    }
                    if (matches) finalCandidates.add(e)
                }
                if (finalCandidates.isEmpty()) finalCandidates.addAll(candidates)
            } else {
                finalCandidates.addAll(candidates)
            }

            // Filter by end letter
            val filtered = mutableListOf<IndexEntry>()
            for (e in finalCandidates) {
                val lower = e.word.lowercase(Locale.ROOT)
                if (lower.isNotEmpty() && endLetters.contains(lower.last())) filtered.add(e)
            }
            if (filtered.isEmpty()) filtered.addAll(finalCandidates)

            // LCS filter
            val nodeSignature = extractNodeSignature(rawFlat, n, charToPos)
            if (nodeSignature.isNotEmpty()) {
                val lcsFiltered = lcsFilter(filtered, nodeSignature)
                if (lcsFiltered.isNotEmpty()) {
                    filtered.clear()
                    filtered.addAll(lcsFiltered)
                }
            }

            if (filtered.isEmpty()) return empty

            // Re-sort by freqBonus so high-frequency candidates are evaluated first,
            // maximising early-abandon pruning effectiveness.
            filtered.sortByDescending { it.freqBonus }

            val detectedPunctuation = detectPunctuation(inputVec, index.punctuationPos)

            // ─── PREPARE LB_Keogh envelope ────────────────────────────────────
            val envMinX = FloatArray(N_PTS) { Float.MAX_VALUE }
            val envMaxX = FloatArray(N_PTS) { -Float.MAX_VALUE }
            val envMinY = FloatArray(N_PTS) { Float.MAX_VALUE }
            val envMaxY = FloatArray(N_PTS) { -Float.MAX_VALUE }
            for (i in 0 until N_PTS) {
                val start = (i - DTW_BAND).coerceAtLeast(0)
                val end = (i + DTW_BAND).coerceAtMost(N_PTS - 1)
                for (k in start..end) {
                    val x = inputVec[2 * k]; val y = inputVec[2 * k + 1]
                    if (x < envMinX[i]) envMinX[i] = x
                    if (x > envMaxX[i]) envMaxX[i] = x
                    if (y < envMinY[i]) envMinY[i] = y
                    if (y > envMaxY[i]) envMaxY[i] = y
                }
            }

            // Shared DTW matrix pre-filled with MAX_VALUE so band-edge cells left
            // unwritten for one candidate cannot bleed stale values into the next.
            val dtwMatrix = Array(N_PTS) { FloatArray(N_PTS) { Float.MAX_VALUE } }

            val m = filtered.size
            val scores = FloatArray(m)
            val order = IntArray(m)
            val candidatePath = FloatArray(N_PTS * 2)

            // Track best normalised DTW (dtwDist / avgLen) for LB_Keogh and
            // early-abandon budgets — keeps thresholds consistent across gesture lengths.
            var bestDtwNorm = Float.MAX_VALUE

            for (i in 0 until m) {
                val e = filtered[i]
                e.unpackPath(candidatePath)  // atomic snapshot — safe even if recordAccepted() fires concurrently

                val avgLen = (inputLen + e.pathLen) * 0.5f + 1e-6f

                // LB_Keogh pre-filter in raw-squared space
                val rawBudgetLB = bestDtwNorm * avgLen * LB_SAFETY_MARGIN
                val lb = computeLBKeogh(inputVec, candidatePath, envMinX, envMaxX, envMinY, envMaxY)
                if (lb > rawBudgetLB * rawBudgetLB) {
                    scores[i] = Float.NEGATIVE_INFINITY
                    order[i] = i
                    continue
                }

                // Safe DTW with shared matrix and early abandon
                val rawBudgetDtw = bestDtwNorm * avgLen * EARLY_ABANDON_TOL
                val dtwSq = dtwDistanceSqSafe(inputVec, candidatePath, dtwMatrix, rawBudgetDtw * rawBudgetDtw)
                if (dtwSq.isInfinite()) {
                    scores[i] = Float.NEGATIVE_INFINITY
                    order[i] = i
                    continue
                }

                val dtwNorm = sqrt(dtwSq) / avgLen
                if (dtwNorm < bestDtwNorm) bestDtwNorm = dtwNorm

                val lower = e.word.lowercase(Locale.ROOT)

                val userBoostVal = sUserBoost[lower]?.let { sUserBoostCache[it] } ?: 0f
                val predBonus = if (predictionSet != null && predictionSet.contains(lower)) 0.15f else 0f
                val lenPenalty = -abs(inputLen - e.pathLen) * 0.2f
                val seqMatch = isSequenceMatch(lower, inputVec, charToPos)
                val seqPenalty = if (seqMatch) 0f else -0.35f
                val punctBonus = if (detectedPunctuation != null && lower.contains(detectedPunctuation)) 0.3f else 0f
                val snakeBonus = checkPositionAware(lower, snakeHints, 0.25f)
                val skipPenalty = checkPositionAware(lower, skipHints, -0.25f)

                val constraintBonus = if (constraints.isNotEmpty()) {
                    var matchCount = 0; var lastIdx = -1
                    for (c in constraints) {
                        val idx = lower.indexOf(c, startIndex = lastIdx + 1)
                        if (idx != -1) { matchCount++; lastIdx = idx }
                    }
                    matchCount * 0.5f
                } else 0f

                val nodeLenPenalty = run {
                    val ratio = nodeSignature.length.coerceAtLeast(1).toFloat() /
                                e.canonicalSeq.length.coerceAtLeast(1).toFloat()
                    if (ratio in 0.5f..2.0f) 0f else -0.25f * abs(1f - ratio)
                }

                scores[i] = -dtwNorm + e.freqBonus + userBoostVal + predBonus +
                             lenPenalty + seqPenalty + punctBonus +
                             snakeBonus + skipPenalty + constraintBonus + nodeLenPenalty
                order[i] = i
            }

            // Insertion sort for top-K
            val take = minOf(maxResults, m)
            for (i in 1 until m) {
                val key = order[i]; val ks = scores[key]; var j = i - 1
                while (j >= 0 && scores[order[j]] < ks) { order[j + 1] = order[j]; j-- }
                order[j + 1] = key
            }

            val result = SuggestionResults(take, false, false)
            val baseScore = 1_000_000
            for (rank in 0 until take) {
                val e = filtered[order[rank]]
                result.add(SuggestedWordInfo(
                    e.word, "", baseScore - rank * 1000,
                    SuggestedWordInfo.KIND_CORRECTION,
                    Dictionary.DICTIONARY_USER_TYPED,
                    SuggestedWordInfo.NOT_AN_INDEX,
                    SuggestedWordInfo.NOT_A_CONFIDENCE
                ))
            }
            return result
        }

        // ─── REMAINDER OF PRIVATE HELPERS ────────────────────────────────────

        @JvmStatic
        fun recordAccepted(
            word: String,
            pointers: InputPointers?,
            keyboard: Keyboard?,
            activeIndex: GestureIndex?
        ) {
            if (word.isEmpty()) return
            val lower = word.lowercase(Locale.ROOT)

            val currentBoost = sUserBoost[lower] ?: 0
            sUserBoost[lower] = (currentBoost + 1).coerceAtMost(USER_BOOST_MAX)

            if (pointers != null && pointers.pointerSize >= 2 && keyboard != null) {
                val n = pointers.pointerSize
                val xs = pointers.xCoordinates
                val ys = pointers.yCoordinates
                val kw = keyboard.mOccupiedWidth.toFloat()
                val kh = keyboard.mOccupiedHeight.toFloat()

                val rawFlat = FloatArray(n * 2)
                for (i in 0 until n) {
                    rawFlat[2 * i] = xs[i] / kw
                    rawFlat[2 * i + 1] = ys[i] / kh
                }
                val path = resampleFlat(rawFlat, n, N_PTS)
                sUserPaths[lower] = path

                if (activeIndex != null && lower.isNotEmpty()) {
                    val first = lower[0]
                    val list = activeIndex.byFirst[first]
                    if (list != null) {
                        for (entry in list) {
                            if (entry.word.lowercase(Locale.ROOT) == lower) {
                                val current = FloatArray(N_PTS * 2)
                                entry.unpackPath(current)  // atomic read — safe
                                val blended = FloatArray(N_PTS * 2)
                                for (i in blended.indices) blended[i] = current[i] * 0.3f + path[i] * 0.7f
                                entry.updatePath(blended)  // atomic publish — safe
                                break
                            }
                        }
                    }
                }
            }

            saveUserDataAsync()
        }

        @JvmStatic
        fun nearestLetters(x: Int, y: Int, keyboard: Keyboard): List<Char> {
            val kw = keyboard.mOccupiedWidth.toFloat()
            val kh = keyboard.mOccupiedHeight.toFloat()
            return nearestLettersFromMap(x / kw, y / kh, buildCharToPos(keyboard))
        }

        @JvmStatic
        fun hasLoopAtEnd(pointers: InputPointers, keyboard: Keyboard): Boolean {
            val n = pointers.pointerSize
            if (n < 6) return false
            val xs = pointers.xCoordinates
            val ys = pointers.yCoordinates

            val pointsToCheck = (n / 2).coerceAtMost(10).coerceAtLeast(4)
            val startIdx = n - pointsToCheck

            var pathLen = 0f
            for (i in startIdx until n - 1) {
                val dx = (xs[i + 1] - xs[i]).toFloat()
                val dy = (ys[i + 1] - ys[i]).toFloat()
                pathLen += sqrt(dx * dx + dy * dy)
            }

            val startEndX = (xs[n - 1] - xs[startIdx]).toFloat()
            val startEndY = (ys[n - 1] - ys[startIdx]).toFloat()
            val displacement = sqrt(startEndX * startEndX + startEndY * startEndY)

            val kw = keyboard.mOccupiedWidth.toFloat()
            if (pathLen < kw * 0.02f) return false
            return pathLen > 2.0f * displacement
        }

        @JvmStatic
        fun isSequenceMatch(word: String, path: FloatArray, charToPos: Array<FloatArray>): Boolean {
            val n = path.size / 2
            var segmentIdx = 0
            var prevT = -0.01f
            var lastChar = 0.toChar()
            val outT = FloatArray(1)

            for (i in word.indices) {
                val c = word[i]
                if (c !in 'a'..'z') continue
                if (c == lastChar) continue

                val target = charToPos[c - 'a']
                if (target[0] == 0f && target[1] == 0f) continue

                var found = false
                while (segmentIdx < n - 1) {
                    val distSq = sqDistanceToSegment(
                        target[0], target[1],
                        path[2 * segmentIdx], path[2 * segmentIdx + 1],
                        path[2 * (segmentIdx + 1)], path[2 * (segmentIdx + 1) + 1],
                        outT
                    )
                    if (distSq <= SEQ_MATCH_RADIUS_SQ) {
                        val t = outT[0]
                        if (t > prevT) { prevT = t; found = true; break }
                    }
                    segmentIdx++
                    prevT = -0.01f
                }
                if (!found) return false
                lastChar = c
            }
            return true
        }

        // ─── Stubs ────────────────────────────────────────────────────────────

        @JvmStatic fun reloadSettings() {}
        @JvmStatic fun syncUserHistory(facilitator: helium314.keyboard.latin.DictionaryFacilitator) {}
        @JvmStatic fun onWordTapped(word: String, facilitator: helium314.keyboard.latin.DictionaryFacilitator) {}
        @JvmStatic fun setPredictionContext(prevWord1: String?, prevWord2: String?) {}
        @JvmStatic fun markLastGestureBackspaced() {}
        @JvmStatic fun getGestureCandidates(word: String): List<String> = emptyList()
        @JvmStatic fun onSuggestionPicked(originalWord: String, newWord: String) {}
        @JvmStatic fun retryWithFallback(maxResults: Int, predictionSet: Set<String>?): SuggestionResults =
            SuggestionResults(1, false, false)

        // ─── CORE HELPERS ─────────────────────────────────────────────────────

        private fun pathLength(path: FloatArray): Float {
            var len = 0f
            val n = path.size / 2
            for (i in 0 until n - 1) {
                val dx = path[2 * (i + 1)] - path[2 * i]
                val dy = path[2 * (i + 1) + 1] - path[2 * i + 1]
                len += sqrt(dx * dx + dy * dy)
            }
            return len
        }

        private fun resampleFlat(pts: FloatArray, numPts: Int, n: Int): FloatArray {
            if (numPts == 0) return FloatArray(n * 2)
            if (numPts == 1) {
                val r = FloatArray(n * 2)
                val x = pts[0]; val y = pts[1]
                for (i in 0 until n) { r[2 * i] = x; r[2 * i + 1] = y }
                return r
            }

            val cum = FloatArray(numPts)
            for (i in 1 until numPts) {
                val dx = pts[2 * i] - pts[2 * (i - 1)]
                val dy = pts[2 * i + 1] - pts[2 * (i - 1) + 1]
                cum[i] = cum[i - 1] + sqrt(dx * dx + dy * dy)
            }

            val total = cum[numPts - 1]
            if (total < 1e-9f) {
                val r = FloatArray(n * 2)
                val x = pts[0]; val y = pts[1]
                for (i in 0 until n) { r[2 * i] = x; r[2 * i + 1] = y }
                return r
            }

            val result = FloatArray(n * 2)
            var seg = 0
            for (i in 0 until n) {
                val t = total * i / (n - 1)
                while (seg < numPts - 2 && cum[seg + 1] < t) seg++
                val segLen = cum[seg + 1] - cum[seg]
                val alpha = if (segLen > 1e-9f) (t - cum[seg]) / segLen else 0f
                result[2 * i] = pts[2 * seg] + alpha * (pts[2 * (seg + 1)] - pts[2 * seg])
                result[2 * i + 1] = pts[2 * seg + 1] + alpha * (pts[2 * (seg + 1) + 1] - pts[2 * seg + 1])
            }
            return result
        }

        private fun wordPath(word: String, charToPos: Array<FloatArray>): FloatArray {
            val pts = mutableListOf<FloatArray>()
            var lastX = -1f
            var lastY = -1f

            for (c in word) {
                val idx = c - 'a'
                if (idx !in 0..25) continue
                val p = charToPos[idx]
                // [BUG FIX #2 — unmapped keys] charToPos entries default to [0f, 0f]
                // for any letter absent from the current keyboard layout (e.g. a letter
                // that lives on a long-press popup, or a layout that omits certain keys).
                // Without this guard, the origin [0, 0] is added as a waypoint, creating
                // a false inflection at the top-left corner.  The resulting path diverges
                // from any real gesture so the word can never rank well — it appears
                // "blocked" even when the user gestures it correctly.
                // isSequenceMatch already has this guard; wordPath now matches it.
                if (p[0] == 0f && p[1] == 0f) continue
                if (pts.isEmpty() || p[0] != lastX || p[1] != lastY) {
                    pts.add(floatArrayOf(p[0], p[1]))
                    lastX = p[0]; lastY = p[1]
                }
            }

            // All letters were non-ASCII or unmapped — return a zero path so the
            // entry is built without crashing; it will rank last in every recognition.
            if (pts.isEmpty()) return FloatArray(N_PTS * 2)

            if (pts.size < 2) {
                val r = FloatArray(N_PTS * 2)
                val x = pts[0][0]; val y = pts[0][1]
                for (i in 0 until N_PTS) { r[2 * i] = x; r[2 * i + 1] = y }
                return r
            }

            val flat = FloatArray(pts.size * 2)
            for (i in pts.indices) { flat[2 * i] = pts[i][0]; flat[2 * i + 1] = pts[i][1] }
            return resampleFlat(flat, pts.size, N_PTS)
        }

        // ─── SAFE DTW (shared matrix, early abandon with tolerance) ──────────

        private fun dtwDistanceSqSafe(
            path1: FloatArray, path2: FloatArray,
            cost: Array<FloatArray>, maxAllowed: Float
        ): Float {
            val len = N_PTS; val band = DTW_BAND

            for (i in 0 until len) {
                val jMin = (i - band).coerceAtLeast(0)
                val jMax = (i + band).coerceAtMost(len - 1)
                var rowMin = Float.MAX_VALUE

                for (j in jMin..jMax) {
                    val dx = path1[2 * i] - path2[2 * j]
                    val dy = path1[2 * i + 1] - path2[2 * j + 1]
                    val dist = dx * dx + dy * dy

                    val minPrev = when {
                        i == 0 && j == 0 -> 0f
                        else -> {
                            var m = Float.MAX_VALUE
                            if (i > 0 && j >= (i - 1) - band && j <= (i - 1) + band) m = minOf(m, cost[i - 1][j])
                            if (j > 0 && i >= (j - 1) - band && i <= (j - 1) + band) m = minOf(m, cost[i][j - 1])
                            if (i > 0 && j > 0) m = minOf(m, cost[i - 1][j - 1])
                            m
                        }
                    }
                    if (minPrev == Float.MAX_VALUE) continue
                    cost[i][j] = dist + minPrev
                    if (cost[i][j] < rowMin) rowMin = cost[i][j]
                }

                if (i > 0 && rowMin > maxAllowed) return Float.POSITIVE_INFINITY
            }
            return cost[len - 1][len - 1]
        }

        // ─── LB_Keogh ─────────────────────────────────────────────────────────

        private fun computeLBKeogh(
            input: FloatArray, candidate: FloatArray,
            envMinX: FloatArray, envMaxX: FloatArray,
            envMinY: FloatArray, envMaxY: FloatArray
        ): Float {
            var dist = 0f
            for (i in 0 until N_PTS) {
                val cx = candidate[2 * i]; val cy = candidate[2 * i + 1]
                if (cx < envMinX[i]) dist += (envMinX[i] - cx) * (envMinX[i] - cx)
                else if (cx > envMaxX[i]) dist += (cx - envMaxX[i]) * (cx - envMaxX[i])
                if (cy < envMinY[i]) dist += (envMinY[i] - cy) * (envMinY[i] - cy)
                else if (cy > envMaxY[i]) dist += (cy - envMaxY[i]) * (cy - envMaxY[i])
                if (dist > Float.MAX_VALUE / 2) return Float.POSITIVE_INFINITY
            }
            return dist
        }

        // ─── PUNCTUATION, PAUSES, LOOPS, SNAKES, ARCS ───────────────────────

        private fun detectPunctuation(path: FloatArray, punctuationPos: Map<Char, FloatArray>): Char? {
            if (punctuationPos.isEmpty()) return null
            val threshold = PUNCTUATION_RADIUS * PUNCTUATION_RADIUS
            var bestChar: Char? = null; var bestDist = Float.MAX_VALUE
            for ((key, pos) in punctuationPos) {
                var minDistSq = Float.MAX_VALUE
                val n = path.size / 2
                for (i in 0 until n) {
                    val dx = path[2 * i] - pos[0]; val dy = path[2 * i + 1] - pos[1]
                    val d = dx * dx + dy * dy
                    if (d < minDistSq) minDistSq = d
                }
                if (minDistSq < threshold && minDistSq < bestDist) { bestDist = minDistSq; bestChar = key }
            }
            return bestChar
        }

        private fun extractHardConstraints(
            rawFlat: FloatArray, numPts: Int,
            times: IntArray?, charToPos: Array<FloatArray>
        ): List<Char> {
            val constraints = mutableListOf<Char>()

            // 1. PAUSE DETECTION
            if (times != null && times.size == numPts) {
                val pauseThresholdDist = 0.015f
                val pauseThresholdTimeMs = 80L
                var i = 1
                while (i < numPts) {
                    val dx = rawFlat[2 * i] - rawFlat[2 * (i - 1)]
                    val dy = rawFlat[2 * i + 1] - rawFlat[2 * (i - 1) + 1]
                    val dt = (times[i] - times[i - 1]).toFloat()
                    val speed = if (dt > 0f) sqrt(dx * dx + dy * dy) / dt else Float.MAX_VALUE
                    if (speed < pauseThresholdDist && (times[i] - times[i - 1]) > pauseThresholdTimeMs) {
                        val key = getNearestKey(rawFlat[2 * i], rawFlat[2 * i + 1], charToPos)
                        if (key in 'a'..'z') {
                            constraints.add(key)
                            while (i < numPts - 1 && sqrt(
                                    (rawFlat[2 * (i + 1)] - rawFlat[2 * i]) * (rawFlat[2 * (i + 1)] - rawFlat[2 * i]) +
                                    (rawFlat[2 * (i + 1) + 1] - rawFlat[2 * i + 1]) * (rawFlat[2 * (i + 1) + 1] - rawFlat[2 * i + 1])
                                ) < pauseThresholdDist) { i++ }
                        }
                    }
                    i++
                }
            }

            // 2. LOOP DETECTION
            if (numPts > 6) {
                val minLoopRadius = 0.06f
                for (centerIdx in 1 until numPts - 1) {
                    var totalAngle = 0f; var insideCount = 0
                    val cx = rawFlat[2 * centerIdx]; val cy = rawFlat[2 * centerIdx + 1]
                    for (k in -3..3) {
                        val idx = centerIdx + k
                        if (idx < 0 || idx >= numPts) continue
                        val dx = rawFlat[2 * idx] - cx; val dy = rawFlat[2 * idx + 1] - cy
                        if (sqrt(dx * dx + dy * dy) < minLoopRadius) {
                            insideCount++
                            if (k > -3) {
                                val prevIdx = centerIdx + k - 1
                                if (prevIdx >= 0) {
                                    val prevDx = rawFlat[2 * prevIdx] - cx
                                    val prevDy = rawFlat[2 * prevIdx + 1] - cy
                                    totalAngle += atan2(dy, dx) - atan2(prevDy, prevDx)
                                }
                            }
                        }
                    }
                    if (insideCount >= 5 && abs(totalAngle) > 5.0f) {
                        val key = getNearestKey(cx, cy, charToPos)
                        if (key in 'a'..'z') constraints.add(key)
                        break
                    }
                }
            }

            return constraints.distinct()
        }

        private fun detectKeyInteractions(
            path: FloatArray, charToPos: Array<FloatArray>
        ): Pair<Set<LetterHint>, Set<LetterHint>> {
            val selected = mutableSetOf<LetterHint>()
            val skipped = mutableSetOf<LetterHint>()
            val n = path.size / 2
            if (n < 3) return Pair(selected, skipped)

            val curvature = FloatArray(n)
            for (i in 1 until n - 1) {
                val ax = path[2 * (i - 1)]; val ay = path[2 * (i - 1) + 1]
                val bx = path[2 * i];       val by = path[2 * i + 1]
                val cx = path[2 * (i + 1)]; val cy = path[2 * (i + 1) + 1]
                val v1x = bx - ax; val v1y = by - ay
                val v2x = cx - bx; val v2y = cy - by
                val len1 = sqrt(v1x * v1x + v1y * v1y)
                val len2 = sqrt(v2x * v2x + v2y * v2y)
                if (len1 > 1e-6f && len2 > 1e-6f) {
                    val dot = v1x * v2x + v1y * v2y
                    val cross = v1x * v2y - v1y * v2x
                    curvature[i] = (atan2(abs(cross), dot) * 180.0 / PI).toFloat()
                }
            }

            val proximityThreshold = 0.04f
            for (i in 0..25) {
                val px = charToPos[i][0]; val py = charToPos[i][1]
                if (px == 0f && py == 0f) continue
                var minDist = Float.MAX_VALUE; var minIdx = -1
                for (j in 0 until n) {
                    val dx = path[2 * j] - px; val dy = path[2 * j + 1] - py
                    val d = dx * dx + dy * dy
                    if (d < minDist) { minDist = d; minIdx = j }
                }
                if (minDist < proximityThreshold * proximityThreshold && minIdx > 0 && minIdx < n - 1) {
                    val angle = curvature[minIdx]
                    val gesturePos = minIdx.toFloat() / (n - 1).toFloat()
                    val letter = ('a'.code + i).toChar()
                    when {
                        angle > 35f -> selected.add(LetterHint(letter, gesturePos))
                        angle < 15f -> skipped.add(LetterHint(letter, gesturePos))
                    }
                }
            }
            return Pair(selected, skipped)
        }

        private fun checkPositionAware(word: String, hints: Set<LetterHint>, perHit: Float): Float {
            if (hints.isEmpty() || word.isEmpty()) return 0f
            var total = 0f
            val wordLen = (word.length - 1).coerceAtLeast(1)
            for (hint in hints) {
                val idx = word.indexOf(hint.char)
                if (idx < 0) continue
                val wordPos = idx.toFloat() / wordLen
                if (abs(wordPos - hint.gesturePosition) < 0.35f) total += perHit
            }
            return total
        }

        // ─── PACKING / UNPACKING ──────────────────────────────────────────────

        private fun pack8Bytes(pts: FloatArray, startIndex: Int): Long {
            var value = 0L
            for (i in 0 until 8) {
                var f = pts[startIndex + i]
                if (f < 0f) f = 0f else if (f > 1f) f = 1f
                val b = (f * 255f).toInt() and 0xFF
                value = value or (b.toLong() shl (i * 8))
            }
            return value
        }

        private fun unpack8Bytes(value: Long, out: FloatArray, startIndex: Int) {
            for (i in 0 until 8) {
                val b = (value ushr (i * 8)).toInt() and 0xFF
                out[startIndex + i] = b / 255f
            }
        }

        // ─── KEYBOARD HELPERS ─────────────────────────────────────────────────

        private fun isAsciiLetter(code: Int): Boolean =
            code in 'a'.code..'z'.code || code in 'A'.code..'Z'.code

        private fun buildCharToPos(keyboard: Keyboard): Array<FloatArray> {
            val map = Array(26) { FloatArray(2) }
            val kw = keyboard.mOccupiedWidth.toFloat()
            val kh = keyboard.mOccupiedHeight.toFloat()
            for (key in keyboard.sortedKeys) {
                val code = key.code
                if (!isAsciiLetter(code)) continue
                val idx = code.toChar().lowercaseChar() - 'a'
                val hitBox = key.hitBox
                map[idx][0] = hitBox.exactCenterX() / kw
                map[idx][1] = hitBox.exactCenterY() / kh
            }
            return map
        }

        private fun nearestLettersFromMap(nx: Float, ny: Float, charToPos: Array<FloatArray>): List<Char> {
            var minDist = Float.MAX_VALUE
            val dists = FloatArray(26)
            for (i in 0..25) {
                val cx = charToPos[i][0]; val cy = charToPos[i][1]
                if (cx == 0f && cy == 0f) { dists[i] = Float.MAX_VALUE; continue }
                val d = (nx - cx) * (nx - cx) + (ny - cy) * (ny - cy)
                dists[i] = d
                if (d < minDist) minDist = d
            }
            val results = mutableListOf<Char>()
            val threshold = minDist + PROXIMITY_RADIUS
            for (i in 0..25) {
                if (dists[i] <= threshold) results.add(('a'.code + i).toChar())
            }
            return results
        }

        private fun lcsFilter(candidates: List<IndexEntry>, nodeSignature: String): List<IndexEntry> {
            if (nodeSignature.isEmpty()) return candidates
            val filtered = mutableListOf<IndexEntry>()
            val minMatch = maxOf(1, (0.75 * nodeSignature.length).toInt())
            for (e in candidates) {
                if (longestCommonSubsequence(nodeSignature, e.canonicalSeq) >= minMatch) filtered.add(e)
            }
            return if (filtered.isNotEmpty()) filtered else candidates
        }

        private fun longestCommonSubsequence(a: String, b: String): Int {
            val m = a.length; val n = b.length
            val dp = IntArray(n + 1)
            for (i in 1..m) {
                var prev = 0
                for (j in 1..n) {
                    val temp = dp[j]
                    dp[j] = if (a[i - 1] == b[j - 1]) prev + 1 else maxOf(dp[j], dp[j - 1])
                    prev = temp
                }
            }
            return dp[n]
        }

        private fun canonicalNodeSeq(word: String): String {
            val sb = StringBuilder(); var prev = 0.toChar()
            for (c in word) {
                if (c in 'a'..'z' && c != prev) { sb.append(c); prev = c }
            }
            return sb.toString()
        }

        private fun extractNodeSignature(rawFlat: FloatArray, numPoints: Int, charToPos: Array<FloatArray>): String {
            if (numPoints < 2) return ""
            val sb = StringBuilder()
            val vecX = FloatArray(numPoints - 1)
            val vecY = FloatArray(numPoints - 1)
            for (i in 0 until numPoints - 1) {
                vecX[i] = rawFlat[2 * (i + 1)] - rawFlat[2 * i]
                vecY[i] = rawFlat[2 * (i + 1) + 1] - rawFlat[2 * i + 1]
            }

            var lastKey = 0.toChar()
            val startKey = getNearestKey(rawFlat[0], rawFlat[1], charToPos)
            if (startKey in 'a'..'z') { sb.append(startKey); lastKey = startKey }

            for (i in 1 until numPoints - 2) {
                val v1x = vecX[i - 1]; val v1y = vecY[i - 1]
                val v2x = vecX[i];     val v2y = vecY[i]
                val len1Sq = v1x * v1x + v1y * v1y
                val len2Sq = v2x * v2x + v2y * v2y
                if (len1Sq == 0f || len2Sq == 0f) continue
                val dot = v1x * v2x + v1y * v2y
                val cross = v1x * v2y - v1y * v2x
                val angle = (atan2(abs(cross), dot) * 180.0 / PI).toFloat()
                if (angle >= 45f) {
                    val key = getNearestKey(rawFlat[2 * i], rawFlat[2 * i + 1], charToPos)
                    if (key in 'a'..'z' && key != lastKey) { sb.append(key); lastKey = key }
                }
            }

            val endKey = getNearestKey(rawFlat[2 * (numPoints - 1)], rawFlat[2 * (numPoints - 1) + 1], charToPos)
            if (endKey in 'a'..'z' && endKey != lastKey) sb.append(endKey)
            return sb.toString()
        }

        private fun getNearestKey(x: Float, y: Float, charToPos: Array<FloatArray>): Char {
            var minDist = Float.MAX_VALUE; var best = 0.toChar()
            for (i in 0..25) {
                val cx = charToPos[i][0]; val cy = charToPos[i][1]
                if (cx == 0f && cy == 0f) continue
                val d = (x - cx) * (x - cx) + (y - cy) * (y - cy)
                if (d < minDist) { minDist = d; best = ('a'.code + i).toChar() }
            }
            return best
        }

        private fun sqDistanceToSegment(
            px: Float, py: Float, ax: Float, ay: Float, bx: Float, by: Float, outT: FloatArray
        ): Float {
            val dx = bx - ax; val dy = by - ay
            val segmentLenSq = dx * dx + dy * dy
            if (segmentLenSq < 1e-9f) { outT[0] = 0f; return (px - ax) * (px - ax) + (py - ay) * (py - ay) }
            var t = ((px - ax) * dx + (py - ay) * dy) / segmentLenSq
            if (t < 0f) t = 0f else if (t > 1f) t = 1f
            outT[0] = t
            val closestX = ax + t * dx; val closestY = ay + t * dy
            return (px - closestX) * (px - closestX) + (py - closestY) * (py - closestY)
        }
    }
}
