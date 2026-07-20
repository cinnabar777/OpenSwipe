# OPENSWIPE



## INTRODUCTION

I am NOT a software engineer, I worked with multiple LLM on this, mainly Deepseek, Gemini, and Replit. I did this on a pixel 6a! Using GitHub mobile which is horrible! It was painful!!! 

I've used some form of Swype typing ever since Swype was in beta. I actually never really learned to be a good thumb typer on mobile, life would be easier if I did because, even to this day no keyboard is better at swipe typing than Swype. That is a sad reality! 

[LeanType](https://github.com/LeanBitLab/LeanType) added a Java gesture engine from this [pull request](https://github.com/HeliBorg/HeliBoard/pull/2351)  which gave me, a non-developer, a playground to test some ideas. Never really using any AI for that much before the initial learning curve was painful and frustrating, however, I was able to test out my sandbox idea, and it worked well. Then I set out with Deepseek to design and build out a complicated dual engine gesture typing engine. It actually worked rather well but Deepseek went crazy like LLMs do when the chat is very long and complex which caused that Java project to break down while integrating options into the settings. Now I know how to fix that via AI easily but it was actually a blessing. 

I removed the DTW engine from the original L2 Java, converted it into kotlin, and it performed much better. I reintegrated several of the features from the Java build, gave Deepseek a shot at optimizing it then Replit, which did really well. This is the current "modified DTW" gesture engine in the OpenSwipe.kt file. LeanType integrated it for a while but had a major bug with the indexing. The performance is solid though it still needs optimizing and many of the ideas in the Java build reintegrated to really be a great gesture engine, however, this one engine can work in different modes therefore no second engine is needed. 

I however have decided that the ROI is not there for me to continue this on my phone 😂. I would have to maintain a fork to ensure it is implemented the way I want, and maintaining a fork is not a current life need or desire. 

No doubt a developer with the full game plan could carry this out in days, but there is the rub. It seems developers creating gesture engines are either thumb typers or have no real game plan other than a simple basic gesture engine. 

This design is not a simple basic gesture engine, it is an extremely fast learning, adapting, driven by the user not the software to always adapt to the user, gesture engine. 

The basic idea comes from the simple reality, the top 1,000 words in English account for 89% of communication, therefore why is every gesture I do compare to 150,000 words? Secondly, the average person only uses a few thousand words in their written/typing communication. Again then, why is every gesture compared to 150,000 words? 

The sandbox hypothesis tested out in the Java build proved how dramatic word clarity can be increased with a little innovation. 

Many of the ideas can be pulled from the Java build. It has about 50-70% of the ideas in it. 

From 2007-2014 I worked with a developer on an open source CMS, it was a great collaboration that created something far better than either of us dreamed of. I've found that developers by and large do not work well with non-developers, and I'm just to old to deal with the current self important know it all developer mindset. It would be great to see this come to life, but it would take a unique developer who wants to dig in an get it done asap and can accept direction from a non-developer. 🤣

Therefore as it stands, this is a good modified DTW gesture engine that can easily be developed further. 

___
___

The original kotlin engine name:  `SwipeGestureEngineKotlin.kt` changed to `OpenSwipe.kt`

___
___

Deepseek summary of DTW engine:

# DTW GESTURE ENGINE

## OVERVIEW

`SwipeGestureEngineKotlin` is a pure‑Kotlin dynamic time warping (DTW) gesture recognition engine designed for on‑device keyboard input. It matches an input swipe path against a pre‑computed index of dictionary word paths, using a variety of heuristics to rank suggestions. The engine is self‑contained, learns from user behaviour, and includes several robustness fixes for production use.

---

## KEY FEATURES

- **DTW with Sakoe‑Chiba band** – limits computation to a diagonal band for speed.
- **LB_Keogh lower‑bound pre‑filter** – cheaply rejects unlikely candidates.
- **Early abandon** – stops DTW computation once a partial path exceeds the current best.
- **Atomic path updates** – prevents torn reads during concurrent access.
- **Position‑aware snake/arc detection** – rewards or penalises letters appearing near where the user curved the gesture.
- **Hard constraints** – pauses and loops force the presence of specific letters.
- **User learning** – boosts frequently accepted words and blends user‑specific paths into the index.
- **Memory bounded** – caps index size and user data to prevent OOM and unbounded growth.

---

## ARCHITECTURE

### 1. DATA STRUCTURES

**`IndexEntry`** – represents a single dictionary word with its canonical gesture path.

- `word` – the word itself.
- `frequency` – dictionary frequency (0–255).
- `freqBonus` – logarithmic frequency bonus.
- `pathLen` – total Euclidean length of the gesture path (volatile for thread‑safe reads).
- `canonicalSeq` – sequence of unique letters (used for LCS filtering).
- `packedPath` – the gesture path stored as four longs (each holding 8 bytes) behind an `AtomicReference<LongArray>` to ensure **atomic snapshot updates**.

**`GestureIndex`** – the entire gesture index.

- `byFirst` – a map from first letter (a–z) to a list of `IndexEntry` objects, sorted by frequency.
- `charToPos` – normalised (0..1) coordinates of each letter on the keyboard.
- `punctuationPos` – positions of punctuation keys (`,`, `.`, `'`).

**`LetterHint`** – used for snake/arc detection, contains a character and its normalised position along the gesture (0.0 = start, 1.0 = end).

---

### 2. CONSTANTS

| Constant | Value | Purpose |
|----------|-------|---------|
| `N_PTS` | 16 | Number of resampled points for path comparison. |
| `DTW_BAND` | 3 | Sakoe‑Chiba band width. |
| `FREQ_WEIGHT` | 0.15f | Weight for frequency bonus. |
| `USER_BOOST_MAX` | 50 | Maximum boost count per word. |
| `PROXIMITY_RADIUS` | 0.05f | Radius for matching start/end letters. |
| `PUNCTUATION_RADIUS` | 0.06f | Radius for punctuation detection. |
| `LB_SAFETY_MARGIN` | 1.15f | Slack for LB_Keogh filtering. |
| `EARLY_ABANDON_TOL` | 1.05f | Slack for early abandon threshold. |
| `SEQ_MATCH_RADIUS_SQ` | 0.012f | Squared radius for sequence‑matching distance. |
| `MAX_ENTRIES_PER_LETTER` | 8,000 | Cap on index entries per first‑letter bucket. |
| `MAX_USER_PATHS` | 1,000 | Cap on stored user‑specific paths. |
| `MAX_USER_BOOSTS` | 2,000 | Cap on stored user boost counts. |

---

### 3. INDEX BUILDING (`buildIndex`)

The index is built asynchronously (via `buildGestureIndexAsync` in the keyboard integration) and follows these steps:

1. **Build keyboard layout** – `buildCharToPos()` maps each ASCII letter to its normalised centre position.
2. **Dictionary iteration** – `forEachMainDictionaryWord` traverses all words in the main dictionary.
 - Words with frequency < 3 or non‑ASCII first letters are skipped.
 - User boosts are applied (`boost * 5`), capped at 255.
 - If a user‑specific path exists, it is blended with the dictionary path (`30% dict + 70% user`).
 - Each word is converted to a resampled path using `wordPath()`.
1. **Bucket sorting & capping** – Entries are grouped by first letter, sorted by descending frequency, and each group is capped at `MAX_ENTRIES_PER_LETTER` to prevent OOM.
2. **Punctuation positions** – The coordinates of `,`, `.`, and `'` are stored separately.

---

### 4. RECOGNITION FLOW (`rankByIndex`)

The main recognition pipeline:

1. **Input normalisation** – Raw pointer coordinates are normalised to the keyboard bounds and resampled to `N_PTS` points using `resampleFlat()`.
2. **Hard constraints** – `extractHardConstraints()` detects pauses (speed < threshold for >80ms) and loops (a tight circular motion). Letters detected at these points become mandatory (they must appear in the candidate word).
3. **Soft constraints** – `detectKeyInteractions()` analyses curvature to identify snakes (sharp turns, rewarded) and arcs (gentle curves, penalised). Each detected letter is assigned a normalised position (`LetterHint`).
4. **Candidate filtering**:
 - Start and end letters are matched using nearest‑neighbour with `PROXIMITY_RADIUS`.
 - Candidates must start with at least one of the start letters.
 - If hard constraints exist, candidates must contain all constrained letters in order.
 - Candidates are filtered by end letter.
 - An LCS filter (`lcsFilter`) keeps only words whose `canonicalSeq` has a 75% match with the gesture’s node signature.
1. **DTW scoring**:
 - Candidates are sorted by `freqBonus` to maximise early abandonment.
 - For each candidate, the path is unpacked atomically.
 - LB_Keogh lower bound is computed; if it exceeds the current best (with safety margin), the candidate is skipped.
 - DTW is performed using a shared matrix (`dtwMatrix`) pre‑filled with `Float.MAX_VALUE` to avoid stale data. Early abandon exits if the row minimum exceeds `bestDtwNorm * avgLen * EARLY_ABANDON_TOL`.
 - The normalised DTW distance (`dtwNorm = sqrt(dtwSq) / avgLen`) is computed and used to update the best threshold.
1. **Score calculation** – The final score is a weighted sum:

score = -dtwNorm

- freqBonus
- userBoostVal
- predBonus (if word is in prediction set)
- lenPenalty (-0.2 * abs(inputLen - pathLen))
- seqPenalty (-0.35 if sequence doesn't match)
- punctBonus (0.3 if detected punctuation is in word)
- snakeBonus (position‑aware)
- skipPenalty (position‑aware)
- constraintBonus (0.5 per matched hard constraint)
- nodeLenPenalty (penalises mismatched signature length)

1. **Sorting** – The top `maxResults` candidates are selected via insertion sort and returned as `SuggestionResults`.

---

### 5. USER LEARNING (`recordAccepted`)

When a gesture word is accepted:

- The word’s boost counter is incremented (capped at `USER_BOOST_MAX`).
- If pointers are available, the input path is resampled and stored in `sUserPaths`.
- If the word exists in the active index, its path is blended (`30% old + 70% new`) and atomically updated via `updatePath()`.
- The user data is saved asynchronously.

### 6. PERSISTENCE

User‑specific data (`sUserBoost` and `sUserPaths`) are stored in a binary file (`gesture_user_data_kotlin.bin`) inside the app’s files directory. On load, the data is trimmed to the caps (`MAX_USER_BOOSTS`, `MAX_USER_PATHS`) to keep the file small and load times fast.

---

## BUG FIXES & ROBUSTNESS

The engine includes explicit fixes for common real‑world issues:

| Bug | Fix |
|-----|-----|
| **#1 – Torn reads on path update** | Used `AtomicReference<LongArray>` to publish path updates atomically. |
| **#2 – Unmapped keys creating false inflections** | In `wordPath()`, skip letters whose `charToPos` is `[0f, 0f]` (letters not on the current layout). |
| **#3 – OOM due to huge indices** | Capped each first‑letter bucket to `MAX_ENTRIES_PER_LETTER`; languages like German/Finnish no longer exceed 90 MB. |
| **#4 – Unbounded growth of user data** | Trimmed `sUserBoost` and `sUserPaths` at save time to `MAX_USER_BOOSTS` and `MAX_USER_PATHS`. |

---

## DEPENDENCIES & INTEGRATION

- **Package**: `helium314.keyboard.latin.gesture`
- **Requires**: Android `Context`, `Keyboard`, `InputPointers`, `DictionaryFacilitator`, `SuggestionResults`.
- **Entry points**:
- `initialize(context: Context)` – loads user data.
- `buildIndex(facilitator, keyboard) -> GestureIndex`
- `rankByIndex(index, pointers, keyboard, maxResults, predictionSet) -> SuggestionResults`
- `recordAccepted(word, pointers, keyboard, activeIndex)`

The engine is designed to be called from the keyboard’s `Suggest.kt` and `LatinIME.java` integration points.

---

## PERFORMANCE OPTIMISATIONS

- **Band‑constrained DTW** – O(N * band) instead of O(N²).
- **LB_Keogh lower bound** – rejects up to 70% of candidates without full DTW.
- **Early abandon** – stops DTW as soon as the row minimum exceeds the best distance.
- **Sorted candidate evaluation** – high‑frequency words evaluated first, improving early abandon.
- **Shared DTW matrix** – reused across candidates to avoid allocation overhead.
- **Atomic path packing** – 8‑bit quantisation reduces memory footprint and cache misses.
- **Position‑aware snake/arc bonuses** – lightweight heuristic that does not require extra DTW passes.

---

## LIMITATIONS & FUTURE WORK

- The engine is limited to ASCII letters (a–z) and a fixed layout of 26 keys. Extended layouts (e.g., QWERTZ, AZERTY) are handled via `charToPos` mapping, but non‑ASCII characters are ignored.
- The LCS filter may reject some valid gestures when the node signature is too long or too short; the `nodeLenPenalty` mitigates this but could be further tuned.
- User‑path blending (70% user / 30% dict) is static; a dynamic weighting based on path similarity could improve adaptation.
- The engine currently does not support multi‑word gestures; it works only for single words.

---

## SOURCE FILE

The complete implementation is contained in a single Kotlin file. 
