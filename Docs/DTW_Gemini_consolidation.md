

>[!INFO] Source Links
>**Source document:** [OpenSwipe](https://github.com/cinnabar777/OpenSwipe)
>**Target Projects:**
> - [WMKeyboard](https://github.com/wasi-master/wmkeyboard) (primary focus due to it having a built-in gesture engine)
> - AOSP keyboards: [LeanType](https://github.com/LeanBitLab/LeanType), [HeliBoard](https://github.com/HeliBorg/HeliBoard), [SonderKey](https://github.com/Verisonder/SonderKey)

>[!NOTE] Terminology
>I use several terms and phrases throughout that are all intended to mean the same thing; they concern the words the keyboard learns from the user's typing. WMKeyboard uses "personal dictionary". AOSP keyboard forks use "user history" and automatically import words from the Android user dictionary. I attempt to be redundant to clarify that I'm talking about the learned words based on the user typing, stored internally, which are not the main dictionary words or an imported word list.

---

# 1. INTRODUCTION

I am NOT a software engineer; I worked with multiple LLMs on this, mainly Deepseek, Gemini, and Replit. I did this on a Pixel 6a, using GitHub mobile, which is horrible—it was painful!

[LeanType](https://github.com/LeanBitLab/LeanType) added a Java gesture engine from this [pull request](https://github.com/HeliBorg/HeliBoard/pull/2351), which gave me a playground to test some ideas. I set out with Deepseek to design a complicated dual-engine gesture typing engine. It worked well, but the chat broke down. I removed the DTW engine from the original L2 Java, converted it into Kotlin, and it performed much better. Replit helped optimize it into the current "modified DTW" gesture engine found in the [OpenSwipe.kt](https://github.com/cinnabar777/OpenSwipe/blob/main/OpenSwipe.kt) file.

The basic sandbox idea comes from the simple reality that the top 1,000 words in English account for 89% of communication. The average person only uses a few thousand words in their written/typing communication. Why is every gesture compared to 150,000+ words? The sandbox hypothesis tested out in the [Java build](https://github.com/cinnabar777/OpenSwipe/blob/main/Java%20dual%20engine.zip) proved how dramatically word clarity can be increased by forcing the engine to focus on learned words.

This document is a blueprint that a developer could use with AI to quickly build out a gesture engine.

## TAP VS. SWIPE TYPING

A user tap typing is in full control of the output; every means of prediction and spell checking can be used immediately. When swipe typing, the user is totally dependent on the gesture engine. Anyone who gestures over 20 WPM is going faster than the preview is being displayed.

Swype keyboard told users to forget about errors and just gesture as fast as they can, then during proofreading, it would offer up the original gesture with original suggestions for error correction. Implementing a swipe-first design will actually improve tap typing, whereas the opposite is true when tap typing is the primary design.

## DICTIONARY SIZE MATTERS

For thumb typers, the size of the dictionary is irrelevant. For swipe typing, more words in the dictionary mean lower word clarity and more word noise competing for every gesture. Most English keyboards ship with dictionaries over 150,000 words.

Is there a way to have both spell checking and swipe clarity? Yes! A simple setting for gesture typing to limit the words used in a dictionary by frequency. The top 1,000 words account for 89% of writing.

---

# 2. DATA ARCHITECTURE

## TYPING DICTIONARY & WORD FREQUENCY

A useful design separates the everyday typing vocabulary from the full spelling vocabulary.

- **Everyday Typing Vocabulary:** Contains common, useful words (~50,000) that should compete during ordinary gesture recognition.
- **Full Spelling Vocabulary:** Contains rare, technical, or unusual words that remain available for spelling support or an explicit search.

A frequency limit can be used to control which words are active. Word frequencies derived from the internet have little direct relation to the average person typing on a phone. A developer can rearrange a dictionary, moving nouns and acronyms to a lower frequency and out of the typing dictionary.

## WORD CACHE (THE HOLDING PEN)

Before words enter internal long-term storage, they must be cached until the user closes the keyboard. A safer design places recent typing events into a temporary "holding pen" to give the user time to correct mistakes before permanent learning occurs.

A holding-pen record should remember:

- The word that was inserted.
- Whether it came from a gesture or individual taps.
- The original gesture path or tap-letter sequence.
- The candidate words that were shown.
- Whether the user selected a different suggestion or edited the word.
- The language, keyboard layout, and time/session.

**Proofreading support:** If the original gesture or tap sequence is still available in the cache, the engine can use it to show the suggestions that belonged to that original input. The cache should have a clear expiration rule (e.g., when the keyboard closes or a size limit is reached).

### WHAT SHOULD GRADUATE?

A word can graduate to long-term storage when it is a real word, accepted by the user, and not immediately corrected.

- **Do not automatically store:** Random strings, temporary verification codes, password-like text, words typed in secure fields, or words that were immediately deleted.
- **AI Autocorrect:** If AI changes a word, ensure the gesture string attached to it is actually what the user intended.

## INTERNAL LEARNED WORDS (PERSONAL DICTIONARY)

This is the internal record of what this particular user actually types, how they gesture it, how often they use it, and which words they choose. It must contain word relations, gesture data, and word weight.

### LEARNING FROM CORRECTIONS & CONFLICTING WORDS

Common conflicts include words like `not` and `nor`, or `can` and `van`. The mature learning record must track the relationship between a gesture and a word. When a user corrects a word via the suggestion strip, the chosen word should gain evidence for that gesture, and the rejected word should lose evidence gradually.

### FAST FUZZY LOGIC & MULTIPLE GESTURE EXAMPLES

Users do not draw the same word exactly the same way every time. The system should retain several representative gesture examples for a word (e.g., 5-10 paths). To increase learning speed, one confirmed gesture can be used to generate small variations (shifted left, right, high, low), effectively giving the engine 3-5 gestures to learn from quickly. Over time, these generated variations should be replaced by real user paths.

### CAPITALIZATION AND WORD FORM

Learned words should preserve useful capitalization. Storing everything in lowercase loses names, while storing auto-capitalized forms creates errors. The system should distinguish between normal lowercase, sentence-start capitalization, and proper names.

### CONTEXT AND WORD RELATIONSHIPS

The engine can use words immediately before the current word as supporting evidence. An individualized language model (trigram + unigram structure) tracks short phrases the user repeatedly writes. Context acts as a booster or tie-breaker, not an absolute override.

## LEARNING MODE

A separate learning mode can quickly build a personal vocabulary from text the user has already written. The user places the cursor in a document, activates this mode, and the keyboard examines words one at a time. Unknown words are offered to the user to add, ignore, or correct. This must be designed with strong privacy controls and never silently read secure content.

## PRIVACY AND INCOGNITO BEHAVIOR

The engine should have a private/incognito mode that prevents new learning.

- New words are not added to the holding pen.
- New gesture paths and usage counts are not saved.
- Existing learned words may still be used for suggestions if permitted.

## STORAGE, BACKUP, AND RECOVERY

Learned gesture data should be stored in application data storage, not a temporary cache that the OS might clear.

- Saving should write a complete temporary copy first, verify it, and then replace the previous valid copy.
- The storage format should be versioned so future updates can migrate data safely.

---

# 3. DATA STRUCTURE OUTLINE

1. **Dictionary**
  - **Typing dictionary:** ~50,000 words, minimize capitalized words, stick to real words.
  - **Spell checking dictionary:** Large, blocks per frequency, watch for memory issues.
2. **Word Cache (Holding Pen)**
  - Keep words until keyboard closes.
  - Add unique words via user authorization (ensure proper capitalization).
  - Retain gesture data and original input sequence.
3. **Learned Words (Internal Personal Dictionary)**
  - **Structure:** Minimum trigram+unigram (word relations).
  - **Gesture Strings:** Store ~5-10 real and generated paths. Fast fuzzy learning (3x→5x). Penalize wrong words and boost correct words. Add usage count to gesture strings.
  - **Metadata:** Store word weight, correction evidence, layout association, and source (user-added vs gesture-learned).
4. **Settings & Management**
  - **Recognition:** Enable/disable, choose active vocabulary, set conservative thresholds.
  - **Learning:** Enable/disable, set gesture example limits, generated path toggles.
  - **Management Screen:** Search words, delete word+data, edit relationships, reset rank.
  - **Sorting Options:** Age (new/old), Weight (high/low), Times used, Alphabetical.
  - **Sync/Import:** Push to or pull from Android User Dictionary, handling duplicates.

---

# 4. GESTURE ENGINE

There are three main types of gesture typing engines: Neural, Modified DTW, and L2. The modified DTW is a noticeable step up from L2 and is the clear winner for projects that allow custom layouts. This document focuses on building the best "modified DTW" gesture engine.

## USER INTENTS (GESTURE HINTS)

Small intentional motions can be treated as optional hints to express intent.

- **Dwell time:** A brief pause over a key (~150ms) to indicate emphasis or a repeated letter.
- **Circle:** A loop around a key for emphasis. The engine should focus on the center and exclude letters on the outside.
- **Wiggle:** A short back-and-forth motion (180° or 90°) to communicate emphasis.
- **Snake:** A winding route between close keys to distinguish letters on crowded layouts.
- **Under or over:** Passing below or above a key to include/avoid it, helping differentiate words like `write`, `wrote`, and `wire`.

## GESTURE OPTIMIZATIONS & GEOMETRY

- **Direction-change points:** Meaningful changes in direction (nodes) indicate intended keys.
- **Tolerances / Error Radius:** The beginning, middle, and end of a gesture have different meanings. There should be separate user settings for Start-key tolerance, End-key tolerance, and Middle-path tolerance.
- **Fly-over letters:** Distinguish a meaningful visit from a quick pass using path direction and time.
- **Edge key optimization:** When a gesture starts/ends near the edge, the error radius should be limited to adjacent keys, reducing ambiguity.

## USABILITY ENHANCEMENTS

- **The apostrophe key:** Allow the user to define a single key (like the spacebar) to act as a punctuation gesture anchor.
- **Appending 's:** Allow a small punctuation action to append `'s` to the last completed word, removing temporary spaces automatically.
- **Gesture punctuation & symbols:** Gesture from a defined key to the spacebar.
- **Suggestion Strip Controls:** Long-press on a word to show actions: Add Word, Delete Word, Increase/Decrease Rank, Delete & Blacklist, or Remove gesture string association.
- **Space cleanup:** Ensure proper flow when combining taps, gestures, and punctuation without awkward spaces.

## INDEXING

- **Match to engine:** The index must match the gesture engine's search style (first/last letter groups, direction-change patterns, etc.).
- **Cache Safety:** Defend against huge dictionaries causing RAM issues or indexing storms. Ensure stale indexes are safely rebuilt.

## MODES (USER-FACING POLICIES)

Modes are user-facing policies for how broad the word search should be.

- **Normal Mode:** Searches the everyday typing vocabulary (frequency limited) and internal words.
- **Sandbox Mode:** A "look first" policy where the engine strictly prefers the learned personal words, dropping to the broader dictionary only if confidence is weak.
- **Sandbox Only Mode:** Searches *only* the internal user learned words for exceptional clarity.
- **Manual Mode (Recovery):** Triggered by a toolbar key or magic gesture (e.g., `q` to spacebar). It uses the cached gesture string to search the *entire* unlimited dictionary for proofreading.
- **Automatic Mode:** Gradually moves the user toward Sandbox mode as their personal dictionary matures.

*(Note: Early prediction mode—predicting a word before the gesture is complete—requires a strict alphabetical index and linear prediction, which conflicts with dual-direction prediction forgiveness, and is thus reserved for advanced Sandbox sub-modes.)*

---

# 5. LAYOUTS

The DTW engine is layout agnostic, but specific optimizations can increase clarity for unique layouts like [ClearFlow](https://clearflowkeyboard.github.io/), [KASROZ](https://futo.tech/blog/swipe-keyboard), and [Vowel Vortex Clarity](https://github.com/cinnabar777/Vowel-Vortex-Keyboard-Layouts).

**Vowel Vortex Clarity Layout:**

- Row 1: `q w r t y p l`
- Row 2: `a , e u i . o`
- Row 3: `s d f g h j k`
- Row 4: `z x c v b n m`

Because `a` and `o` are on edges and separated by punctuation, the engine must treat punctuation keys as gaps/walls. The gesture must fully enter the `a` or `o` key. Since `e` and `i` are larger, the gesture should be more central to trigger them, giving `u` its full area. The layout requires frequent direction changes to enter/leave the vowel row, which should be heavily leveraged as node points.

---

# 6. TESTING THE COMPLETE USER EXPERIENCE

The engine must be tested as a learning product, not just a mathematical matcher.

- Gesture a common word, close the keyboard, and verify learning remains available.
- Correct a selected word from the strip and verify that ranking changes gradually.
- Use conflicting words repeatedly (e.g., `not` / `nor`) and verify both remain usable.
- Enable private mode and verify no new counts or paths are saved.
- Test punctuation immediately after a gesture to verify no unwanted space remains.
- Clear the app cache and verify durable learned data is not erased.
- Measure how often a learned correction is remembered and how often the user has to immediately correct a result.
