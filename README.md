
**THIS IS A WORK IN PROGRESS AND IS NOT COMPREHENSIVE!** 

___

This version was produced by Deepseek based on [Part 1](https://github.com/cinnabar777/OpenSwipe/blob/main/Docs/DTW_PART_1.md), my original writing, and [Part 2](https://github.com/cinnabar777/OpenSwipe/blob/main/Docs/DTW_PART_2.md), Replit compilation of old documentation I had from originally working on the engines. These documents are in the [Docs folder](https://github.com/cinnabar777/OpenSwipe/tree/main/Docs). 

___

**How to use these documents:** drop them into an LLM, at least Part 1 & 2 and this one, with reference code like the [OpenSwipe.kt](https://github.com/cinnabar777/OpenSwipe/blob/main/OpenSwipe.kt) and the old [java files](https://github.com/cinnabar777/OpenSwipe/blob/main/Java%20dual%20engine.zip), and/or the gesture engine from WMKeyboard and decide what parts you want to work on. The goal is to minimize your time having to research the subject and explain it to an LLM. This Deepseek version has most of the info, things are more explained in part 1, and is easier to read, shorter, and I didn't proofread part 1 😂. **When I find something that is not covered I'll append it at the bottom of this front document.** 

___

Source documents: [OpenSwipe](https://github.com/cinnabar777/OpenSwipe)

Target Projects:

- [WMKeyboard](https://github.com/wasi-master/wmkeyboard) (primary focus due to it hanging a built-in gesture engine.)
- AOSP keyboards:
    - [LeanType](https://github.com/LeanBitLab/LeanType) 
    - [HeliBoard](https://github.com/HeliBorg/HeliBoard) 
    - [SonderKey](https://github.com/Verisonder/SonderKey)

___
___
___

# BLUEPRINT FOR A MODERN DTW GESTURE TYPING ENGINE (SWIPE-FIRST ARCHITECTURE)

**Intended Audience**: Developers, AI assistants, and enthusiasts who want to build a gesture engine, or deeply understand why current keyboards frustrate swipe users.

**Goal**: To create a "Swype 2.0" — a keyboard that is engineered for swiping first, learns the user's unique vocabulary, and dramatically improves tap typing as a side effect.

---

## 1. INTRODUCTION: WHY THIS GUIDE EXISTS

I am not a professional software engineer. I am a user and enthusiast who has spent countless hours collaborating with Large Language Models (DeepSeek, Gemini, and Replit) to build, test, and refine gesture-typing engines. I did this primarily on a Pixel 6a using GitHub Mobile — a painful process, but one that gave me deep, practical insights into what makes a gesture engine work *well* for a human user.

The genesis of this project came when [LeanType](https://github.com/LeanBitLab/LeanType) incorporated a Java-based gesture engine from a [pull request](https://github.com/HeliBorg/HeliBoard/pull/2351). This gave me, a non-developer, a sandbox to test my core hypothesis: **that modern gesture typing is fundamentally broken because it is treated as an afterthought to tap typing.**

I tested my "sandbox idea" (forcing the engine to focus on the user's personal lexicon rather than the massive main dictionary) and it worked surprisingly well. I ported the engine from Java to Kotlin, which yielded massive performance improvements. I re-integrated features, optimized the code with Replit, and produced the current `OpenSwipe.kt` file.

However, the journey revealed a massive gap: **Developers build keyboards for tap typing, and then tack on gesture typing as an afterthought.** This leads to fundamental UX flaws, heavy dictionaries, and frustrating autocorrect errors.

This document is the result of merging hundreds of hours of testing with a deep analysis of what makes a gesture engine feel "intelligent." It is a blueprint — designed to be given to an LLM along with a basic codebase — so that a developer can quickly build a **swipe-first** gesture engine that also enhances tap typing.

---

## 2. THE CORE PROBLEM: TAP VS. SWIPE TYPING

There is a fundamental engineering difference between designing for tap typing and designing for swipe typing. If you fail to understand this distinction, your gesture engine will always feel like a frustrating gimmick.

### 2.1 THE MINDSET OF A TAP TYPER

A tap typer is in total control. Every keypress is an explicit, deliberate command.

- If the prediction engine suggests the wrong word, the tap typer barely notices — they just backspace and tap the correct letters.
- The dictionary can contain 300,000 words because the user narrows it down letter by letter.
- The user looks at the suggestion strip frequently to get that long word within a few taps. 

### 2.2 THE MINDSET OF A SWIPE TYPER

A swipe typer is totally dependent on the gesture engine. They are moving their finger continuously at high speed.

- A user swiping at 20+ Words Per Minute (WPM) does not look at the preview strip; watching suggestions breaks their focus and slows them down.
- They do not tap the spacebar to cycle through alternatives — by the time they do, they could have already gestured the next word.
- They rely on muscle memory. They want the keyboard to *read their mind*, not just guess based on a massive, noisy dictionary.

### 2.3 THE SWYPE LEGACY: A LESSON IN SWIPE-FIRST DESIGN

Swype (the original gesture keyboard) understood this perfectly. They told users to gesture as fast as they could, ignore errors, and only fix things during proofreading.

Crucially, Swype retained the **original gesture path** and the **original suggestion list** for error correction — something no other keyboard has replicated since. They also required the user to explicitly tap a new word to add it to the dictionary, rather than silently learning it after 3 uses (which is standard in modern tap-first keyboards). This proves Swype was engineered *for* swiping, while modern keyboards are tap-first with gesture features bolted on.

**The Goal**: We want a *swipe-first* architecture. If we engineer for swiping, tap typing will naturally improve. The reverse is not true.

---

## 3. THE SINGLE BIGGEST ISSUE: DICTIONARY SIZE MATTERS

For tap typers, dictionary size is irrelevant. For swipe typers, **dictionary size is the #1 factor affecting word clarity**.

Here is why: A gesture engine compares a user's continuous finger path against a mathematical representation of every word in the dictionary. The more words you have, the higher the likelihood of "gesture collisions" — where two completely different words produce nearly identical swipe paths.

For example, on a QWERTY layout, the words *"write"*, *"wrote"*, and *"wire"* have almost identical paths. If your dictionary has 300,000 words, there will be dozens of paths competing for the same gesture, creating massive noise.

- Swype used a reasonably small dictionary (~50,000 words).
- Most modern keyboards ship with dictionaries exceeding 150,000 words, sometimes over 300,000.

**Why are modern keyboards doing this?** Because they are designed for tap typing, where a larger dictionary means better spell-checking. They fail to realize that these extra 100,000+ words create noise for the gesture engine.

### 3.1 THE SOLUTION: SPLIT DICTIONARIES (TYPING VS. SPELL-CHECK)

We do not need to delete the large dictionary; we just need to isolate it. Think of it like a library: you have a "Quick Reference" shelf (Typing Dictionary) and a "Archival Vault" (Spell-Check Dictionary).

1. **Typing Dictionary (Primary)**: A tightly curated list of ~50,000 real, commonly used English words. This is the dictionary the gesture engine queries *by default*. It contains no garbage acronyms, no proper nouns (unless heavily used), and no obscure technical jargon. The gesture engine never sees the other 250,000 words, except for a manual mode search.
2. **Spell-Check / Fallback Dictionary (Secondary)**: A massive dictionary (unlimited size) used *only* for spell-checking, tap-typing corrections, and manual searches. The gesture engine ignores this dictionary unless the user explicitly demands it (via a Manual Mode search).

### 3.2 WORD FREQUENCY ADJUSTMENTS

Most word-frequency lists are scraped from the internet and are heavily skewed by news media, corporate reports, and government documents. A developer should run a few scripts to demote nouns, acronyms, and proper nouns to the bottom of the frequency list, effectively pushing them out of the Typing Dictionary. Never force a user to blacklist thousands of words just to get decent gesture results.

---

## 4. THE BRAIN OF THE ENGINE: DATA STRUCTURES

A great gesture engine is useless without a well-designed data ecosystem. The "brain" is not the algorithm; it is the **Learned Words dataset** (often called the User History or Personal Dictionary). A weak DTW engine paired with a brilliant personal dictionary will outperform a complex Neural engine with a static dictionary.

>[!note] Important Terminology
>Different keyboard projects use different names: "personal dictionary," "user history," "learned words," and "user dictionary." For clarity, this document always distinguishes:
> 1. The **built-in main dictionary** (supplied by the keyboard).
> 2. The **spell-check dictionary** (for rare words).
> 3. Words **imported** by the user from a file or contacts.
> 4. Words supplied by the **Android OS user dictionary**.
> 5. **Learned Words**: The internal dataset of words the engine observed the user typing, correcting, and explicitly accepting.
> 6. **Gesture Records**: The specific finger paths associated with those learned words.

### 4.1 THE TYPING DICTIONARY (PRIMARY DATA)

- **Size**: ~50,000 words.
- **Content**: Real words, minimal capitalization. All possessive forms (e.g., `apple's`) should be removed. The gesture engine should handle `'s` via a dedicated usability shortcut (see Section 8.2).
- **Storage**: A compressed binary format or a highly optimized `SortedSet`. It must be loaded into memory efficiently to avoid typing latency.

### 4.2 THE WORD CACHE (SHORT-TERM MEMORY / "HOLDING PEN")

Words do not enter long-term storage immediately. They are held in a **Word Cache** (often called a "Holding Pen") until the keyboard is dismissed. This is critical for preventing typos from entering the permanent learning database.

**Why delay learning?** Imagine you accidentally type "teh" and then backspace and correct it to "the". If the engine instantly learned "teh", it would forever associate that incorrect path with the word "the", causing future frustration. The holding pen gives the user a grace period to fix mistakes before they permanently change the keyboard's behavior.

**What to cache**:

- The exact word output.
- The raw gesture path (the sequence of X/Y coordinates).
- Whether it came from a gesture or individual taps.
- The candidate words that were originally shown (if the product wants to restore original suggestions later).
- The language and keyboard layout in use.

**When to commit (Graduation)**:

The cache is normally processed when the keyboard session closes, when the user explicitly confirms a word, or when a clearly defined timer expires.

**What should graduate (enter long-term storage)**:

- A word that is a real dictionary word.
- A word that was accepted or confirmed by the user (e.g., selected from the suggestion strip).
- A word that was not immediately corrected or deleted.
- A unknown word the user accepted to add. 

**What should NOT graduate automatically**:

- Random strings, accidental key sequences, temporary verification codes, or password-like text.
- Words typed in secure or private fields.
- Words that were immediately deleted as part of correcting a sentence.
- Punctuation-only entries.
- URLs, email addresses, or file paths (unless explicitly intended as a product feature).
- Words that are present only because an automatic correction changed the user’s original input.

*(If an AI corrects "thier" to "their", the engine should not save "their" with the user's original gesture path, because the user didn't actually gesture "their".)*

### 4.3 THE LEARNED WORDS (LONG-TERM PERSONAL DICTIONARY)

This is the most critical data structure. It must be far more complex than a simple bigram table used for tap prediction. This database records *not only* what words the user types, but *how* they type them.

**Required Structure (Minimum)**:

1. **Word (String)**: The exact text (e.g., "the", "running").
2. **Unigram Frequency (Int)**: How many times this word has been used by the user.
3. **Trigram Relationships (Map of Word to Int)**: A map of the previous word and the next word, forming a personal language model. Example: `"the" -> { "cat": 12, "dog": 5 }` and `"cat" -> { "ran": 2 }`. This stores short phrases the user repeatedly writes.
4. **Gesture Path Storage (FloatArray or ByteArray)**: A normalized representation of the swipe path (e.g., downsampled to 20 points, normalized to a 0-1 coordinate scale). Store ~5 to 10 of the most recent unique gesture paths for this word.
5. **Gesture Acceptance/Rejection Counts (Int)**: For each stored gesture path, track how many times the user successfully used that exact path to yield this word, AND how many times it was rejected (corrected to another word). This helps resolve conflicts dynamically (see Section 4.5).
6. **Last Used Timestamp**: When the word was last typed.
7. **Source Flag**: Whether the word was user-added, imported, tapped, or gesture-learned.
8. **Capitalization Status**: Whether the word is a proper noun, acronym, or standard lowercase word (see Section 11).

>[!note] Implementation Advice
>Do not store this as a simple JSON array. For performance on Android, store this in an SQLite database or a lightweight, memory-mapped key-value store. The database must survive app updates.

### 4.4 MULTIPLE GESTURE EXAMPLES PER WORD

Users do not draw the same word exactly the same way every time. A user might be rushed and draw a sloppy "the", or be careful and draw a crisp "the". Therefore, a good engine retains several representative gesture examples for a word instead of replacing the old path every time.

The stored examples can represent:

- The user’s most common route.
- A slow, careful route.
- A fast route with fewer visible turns.
- A route used on a different keyboard size or orientation.
- A route that was confirmed after correcting a competing word.

**Eviction Policy**: Keep approximately 5 to 10 representative paths per word. Long words and frequently confused words may justify more examples, while rarely used words may need only one.

### 4.5 FAST FUZZY LOGIC LEARNING (THE 3X MULTIPLIER / SYNTHETIC VARIATIONS)

To help the engine learn a new word *fast*, we apply a "synthetic gesture expansion" technique.

When a user types a word for the first time and it is added to the Learned Words, we do not store just *one* gesture string. We store **three**:

1. The **actual gesture** the user performed.
2. A **synthetic left-offset gesture** (shifting the entire path 15% to the left).
3. A **synthetic right-offset gesture** (shifting the entire path 15% to the right).

**Why this works**: If the user gestures "dictionary" slightly to the left of the ideal path, the left-shifted synthetic version will match their actual gesture closely, allowing the engine to recognize it immediately.

Over time, as the user performs the gesture more accurately, these synthetic entries are replaced by real, high-confidence gesture paths. This drastically reduces the "cold start" problem.

>[!warning] Important
>Synthetic variations should be treated as temporary approximations. They should be replaced or outweighed by real paths over time. They should never be allowed to multiply without a limit, and they should not be used to hide poor matching quality.

### 4.6 CONTEXT AND WORD RELATIONSHIPS (TRIGGERS)

A gesture engine can use the words immediately before the current word as supporting evidence. This is called **context**.

For example, if the user typed "The quick brown" and then gestures a word that matches both "fox" and "fix", the context will heavily favor "fox" because the phrase "brown fox" is far more common than "brown fix".

- Context should be used as a **tie-breaker** or **booster**, but it should never override a strong gesture match.
- If a gesture perfectly matches "fix", the engine should output "fix", even if "fox" would make more sense contextually. The user's gesture is the primary signal.

Context storage should include:

- The previous word.
- The word before the previous word.
- The current candidate.
- How many times that phrase pattern was used.
- How recently it was used.

>[!warning] Privacy Caution
>Context information can reveal more about a user's writing style than an isolated word. It should be bounded, locally controlled, and excluded from secure input.

---

## 5. THE GESTURE ENGINE: MODIFIED DYNAMIC TIME WARPING (DTW)

There are three main approaches to gesture typing algorithms. I have tested them extensively:

1. **Neural Networks (e.g., FUTO Keyboard)**: Excellent, but requires large training datasets, extensive mathematical expertise, and significant processing power. Not ideal for a rapid LLM-assisted build.
2. **L2 Distance (Euclidean)**: Fast and cheap computationally, but strictly layout-dependent. It struggles with arbitrary custom layouts and is highly sensitive to speed variations.
3. **Modified DTW**: The sweet spot. It allows for "spelling forgiveness" (matching paths even if the user skips a key) and is layout-agnostic. The computational cost is higher than L2, but with optimizations like LB_Keogh pruning and Sakoe-Chiba bands, it runs smoothly on modern Android devices.

**Why DTW wins for custom layouts**: DTW warps the time axis to align two sequences. If a user gestures on a custom layout (like Vowel Vortex or KASROZ), DTW can handle the geometric differences because it focuses on the *shape* of the path, not just the absolute coordinates. L2 fails on custom layouts because it requires exact coordinate matching.

### 5.1 CORE DTW OPTIMIZATIONS (BASED ON OPENSWIPE.KT)

The `OpenSwipe.kt` implementation includes several non-negotiable optimizations that must be retained:

- **Atomic Path Storage**: Gesture paths are stored as `LongArray` within an `AtomicReference` to ensure thread-safe updates. This prevents torn reads and crashes during concurrent processing.
- **LB_Keogh Pruning**: This is a lower-bound filter. Before running the full DTW calculation, the engine checks if the candidate path falls *outside* the "envelope" of the input path. If it does, we skip it immediately. This cuts down 70% of unnecessary DTW calculations.
- **Early Abandon**: During the DTW matrix calculation, if the cumulative cost exceeds a predefined threshold (e.g., `bestNorm * avgLen * 1.05`), the calculation aborts early. This is critical for preventing lag on long words.
- **Sakoe-Chiba Band (Band Width = 3)**: DTW usually compares every point to every other point. We restrict the warping path to a band of 3 points around the diagonal. This reduces complexity from O(N*M) to O(N) and forces the warping to stay physically realistic.
- **Unmapped-Key Guard**: If the gesture path passes over a key that has no mapped coordinates (e.g., a punctuation key in a custom layout), the engine must skip that point rather than treating it as `[0,0]`, which would corrupt the reference path.
- **Bucket Capping**: The index is grouped by the first letter. To prevent Out-Of-Memory (OOM) errors, we limit each bucket to 8,000 entries. Additionally, we trim the user-boost maps to 2,000 entries and path maps to 1,000 entries during save operations.

### 5.2 THE INDEXING STRATEGY (A CRUCIAL ELEMENT)

A bad index will ruin a good DTW engine. The index must be structured to match the gesture engine's search strategy.

- **First-Letter and Last-Letter Index**: Create a multi-map where keys are `FirstLetter_LastLetter` (e.g., `d_y` for "dictionary"). The DTW engine compares the starting key of the gesture to the start of the word, and the ending key to the end of the word. This narrows the search space massively.
- **Dynamic Indexing for "Sandbox" modes**: When the user enters Sandbox Only Mode, the index must dynamically switch to *only* the Learned Words database.
- **Avoid "Index Storms"**: When the user types a new word and adds it to the personal dictionary, do not re-index the entire database. Use an incremental insertion approach. On Android, SQLite with proper indexing (B-Tree) handles this natively.
- **Fingerprinting**: The index should have a "fingerprint" (a unique hash) that identifies the dictionary version, keyboard layout, and relevant settings used to build it. If any of those inputs change, the old index is considered stale and must be rebuilt safely. This prevents the engine from using a corrupt or outdated index.

### 5.3 GESTURE GEOMETRY IMPROVEMENTS (BEYOND PATH SHAPE)

The engine can use more than just the total shape of the path. Meaningful changes in direction, the start, the end, and the keys along the edges can all improve word clarity.

- **Direction-Change Points (Node Points)**: A meaningful change in direction can indicate that the finger intended a particular key. The engine should ignore tiny changes caused by touch noise. The developer must decide how many node points must agree before a candidate is considered strong.
- **Separate Start, End, and Pathway Tolerance**: The beginning, middle, and end of a gesture do not have identical meaning. The first key and final key are exceptionally valuable because they constrain the possible word. The middle path needs enough tolerance for natural movement but should not accept every nearby key.
- **Fly-Over Letters**: This is a letter that the finger passes near or across without intending to select it. This is a common source of wrong words. The engine must distinguish a meaningful visit from a quick pass by checking path direction, time near the key, and surrounding route. A word should never win merely because it contains every letter that the path happened to cross. The path must support the order and role of those letters.
- **Edge Keys**: When a gesture starts or ends near the edge of the keyboard, the possible neighboring keys are naturally limited. The engine can use that fact to reduce ambiguity instead of applying the same large circle of tolerance in every direction. This is particularly useful on layouts with several rows or punctuation keys separating letter keys.

---

## 6. USER INTENTS (MICRO-GESTURES): THE "HUMAN" LAYER

The DTW engine calculates mathematical distances. It does not know the user *intended* a specific letter. We can give the user "micro-gestures" to tell the engine exactly what they want. Swype was the king of this; modern keyboards are lacking.

These are optional hints. They should improve recognition when used, but ordinary gesture typing must remain possible without them.

- **Dwell Time**: A brief pause (<150ms) over a specific key. This implies the user is thinking about the spelling, or emphasizing a repeated letter. A dwell should be measured relative to the user’s normal movement and should not require an exact universal millisecond value.
- **The Circle**: Drawing a small circle or loop around a key. This explicitly tells the engine: *This letter belongs in the word, and do not include letters outside this circle.* This is vital for words like "good" vs "god".
- **The Wiggle**: A rapid back-and-forth micro-gesture over a key (180° or 90°). This implies the user is correcting a specific letter or emphasizing it. Wiggle detection should consider dwell and overall path direction so that ordinary hand tremor is not treated as a command.
- **The Snake**: Zig-zagging through very close keys (e.g., QWERTY's). This helps with tight clusters where a linear path is ambiguous. This behavior should be considered experimental until it is tested with real users.
- **The Under/Over**: Passing slightly below or above a key to explicitly exclude or include it. This is especially useful for differentiating "write", "wrote", and "wire" — which are almost identical on QWERTY. The engine should document which direction means what, and should not require the user to learn a complicated secret gesture language.

>[!note] Implementation Note
>Implementing these requires a secondary, lightweight algorithm running alongside the main DTW. Instead of feeding the *entire* path into the DTW, we first run a path-analysis pass to detect these micro-gestures. If detected, we segment the path and assign higher weights to the affected keys during the DTW cost calculation.

---

## 7. USABILITY ENHANCEMENTS: THE "HUMAN" LAYER (PART 2)

These features are what make a keyboard usable at high speeds without looking at the screen.

### 7.1 THE APOSTROPHE KEY

The gesture engine must recognize a user-defined key (spacebar, comma, period, or apostrophe) as the trigger for an apostrophe. I recommend the **spacebar**. When a user gestures a word and travels to the spacebar, the engine should insert an apostrophe into the word (e.g., "dont" -> "don't"). Keep it to one user-definable key; multiple keys will confuse the path analysis.

### 7.2 THE `'s` APPEND TRICK

Allow the user to define a key (period, comma, or apostrophe) that, when gesture from and to the s key, immediately after a gestured word, appends `'s` to that word (removing any trailing space). The design could also make this an addition to a word instead of a separate gesture, though I've never seen that done. 

**Why this matters**: This means we can remove all possessive forms from the main dictionary (e.g., remove "cat's", "dog's"), shrinking the dictionary size significantly. Once the user does this once, "cat's" should be added to their Learned Words with the full gesture path, so they never have to do it again.

### 7.3 GESTURE PUNCTUATION AND SYMBOLS

Allow the user to define gestures for common punctuation. The best implementation is: **Gesture from a specific key to the spacebar**.

For example: Gesture `m -> spacebar` outputs a question mark. Gesture `v -> spacebar` outputs a colon. This is layout-agnostic and user-definable.

Symbols can eventually be added using the same concept, but a long list of hidden symbol gestures can be difficult to learn and should be avoided without a clear UI.

### 7.4 SPACE AND PUNCTUATION CLEANUP

Gesture typing inserts a space after a completed word. Punctuation usually belongs directly beside the word, so the engine needs a consistent rule for removing or preventing an unwanted space before punctuation.

The engine must test for:

- A gestured word followed by a period (no space).
- A gestured word followed by a comma (no space).
- A gestured word followed by an apostrophe action.
- A tapped word placed between two gestured words (avoiding weird spacing).
- Backspace and undo actions after punctuation cleanup.

### 7.5 THE SUGGESTION STRIP LONG-PRESS MENU

When the user long-presses a word in the suggestion strip, a context menu must appear with clear, distinct actions:

- **Add Word**: Manually add to Learned Words.
- **Delete Word**: Remove from Learned Words.
- **Increase Rank**: Boost the weight of this word in the personal language model.
- **Decrease Rank**: Lower the weight.
- **Delete and Blacklist**: Remove the word and add it to a global blacklist (so it never appears again, even in the dictionary).
- **Remove Gesture Association**: If the engine is incorrectly mapping a gesture to this word, remove that specific path while keeping the word available for normal tapping.

>[!warning] Critical Distinction
>"Delete word" should not unexpectedly delete a legitimate word from the main dictionary. "Remove gesture association" should not remove the word from ordinary tap suggestions. These actions must affect only the personal learning dataset.

---

## 8. THE MODES: THE SANDBOX REVOLUTION (REFRAMED AS POLICIES)

This is the core of the "swipe-first" philosophy. We use the user's *actual* typing history to build a "sandbox" of words, and we slowly shift the engine's focus into that sandbox, ignoring the massive, noisy main dictionary.

The older project documents used the word "modes." I want to reframe these as **User-Facing Policies** for how broad the word search should be. This makes them easier for a non-technical user to understand.

### 8.1 BROAD VOCABULARY POLICY (NORMAL MODE - DEFAULT)

The engine searches the Typing Dictionary (~50k words) and the Learned Words. A frequency limiter is applied to the Typing Dictionary (e.g., only search words with a frequency > 50). This is the "safe" default.

### 8.2 PERSONAL VOCABULARY FIRST POLICY (SANDBOX MODE)

The engine searches *only* the Learned Words. If the DTW confidence score is above a high threshold (e.g., 0.85), it outputs the word. If the confidence drops below that threshold (meaning the word is likely not in the Learned Words), it falls back to Broad Vocabulary Policy. This creates a seamless "best of both worlds" experience.

### 8.3 PERSONAL VOCABULARY ONLY POLICY (SANDBOX ONLY MODE)

The engine searches *only* the Learned Words. No fallback to the main dictionary. This mode offers exceptional word clarity because the dictionary is limited to the 1,000–5,000 words the user actually uses. However, when the user needs a rare word, the engine will fail. This is where **Manual Recovery** saves the day.

### 8.4 MANUAL RECOVERY (THE SPELL CHECKER)

When the user places the cursor on a word that was gestured incorrectly (and that word is still in the Word Cache), they can trigger a "Manual Search" using a toolbar button or a **Magic Gesture** (e.g., gesture from `q` to the center of the spacebar).

This immediately re-runs the gesture engine against **all available dictionaries** (Typing, Spell-check, and Learned) with *no* frequency limits. The engine displays all suggestions. Once the user picks the correct word, it is instantly added to the Learned Words with the cached gesture path. This ensures the user never has to re-gesture a word they just typed.

### 8.5 AUTOMATIC ADAPTATION (THE ALGORITHMIC TRANSITION)

This should be the default policy for most users. An algorithm monitors the growth of the Learned Words.

- **Stage 1**: User has < 500 words in Learned. -> Broad Vocabulary.
- **Stage 2**: User has 500-2000 words. -> Personal Vocabulary First.
- **Stage 3**: User has > 2000 words and adds fewer than 5 new words per day. -> Suggest switching to Personal Vocabulary Only via a popup notification.

The algorithm should seed the Learned Words with the top 1,000 English words to give the user a better out-of-the-box experience. Automatic adaptation should be gradual, measurable, and reversible. The user should be notified before the keyboard changes to a more restrictive policy.

---

## 9. ADVANCED LEARNING AND CONFLICT RESOLUTION

Let's revisit the conflict problem: `not` vs `nor`. A user may use both. The engine must not permanently transfer a gesture from one to the other after a single correction.

When the user changes an automatically selected word to another suggestion, that action is valuable feedback:

- The **chosen word** should gain evidence for that gesture.
- The **rejected word** should lose some evidence for that particular association.

**The adjustment should be gradual, not all-or-nothing.**

A mature learning record tracks the relationship between a gesture and a word, not only the word itself. It should be possible for one gesture pattern to be associated with more than one word, with each association having its own strength.

Useful information to track per gesture-word relationship:

- How many times the path was **accepted** for that word.
- How many times the path was **rejected** for that word.
- How many times the user selected another word instead.
- How recently the relationship was used.

This is far more useful than a simple "word count" because it lets the engine learn that the user’s version of a path may differ from the population’s average path.

---

## 10. PRIVACY AND INCOGNITO MODE

The engine must have a private or incognito mode that prevents new learning while continuing to use already stored data (if the user wants suggestions).

When private mode is active:

- New words are NOT added to the holding pen.
- Existing learned words may still be used for recognition if the user permits it.
- New gesture paths are NOT saved.
- Usage counts and context relationships are NOT increased.
- Graduation and background learning scans are skipped.
- Temporary cache information is cleared according to the private-session policy.

The user interface should clearly explain whether private mode blocks only learning, or also prevents the engine from using previously learned words. Different users may want those two behaviors separately.

---

## 11. CAPITALIZATION AND WORD FORM

Learned words should preserve useful capitalization information. Storing every word only in lowercase loses names and intentional capitalization, while storing the auto-capitalized form as though it were the user’s preferred spelling creates a different problem.

The system must distinguish:

- Normal lowercase spelling (e.g., "apple").
- Sentence-start capitalization (e.g., "The" at the start of a sentence).
- A user intentionally entering an uppercase word (e.g., "OK").
- A proper name or acronym (e.g., "Google", "NASA").
- A word capitalized only because the keyboard auto-capitalized it.

When a word is offered for permanent learning, the engine must decide whether its capitalization is part of the word or merely a temporary text-position effect.

---

## 12. LEARNING MODE (DOCUMENT SCANNING)

A separate learning mode can help build a personal vocabulary from text the user has already written.

The user selects a document (e.g., a note, an email draft, or a long essay). They activate "Learning Mode." The keyboard scans the document word by word.

- Words already in the Learned Words are skipped.
- Words that are matched to a word in one of the dictionaries are automatically added to Learned Words. 
- Unknown words are offered one by one for the user to **Add**, **Ignore**, or **Edit**.
    - If a word is unknown to all dictionaries (a misspelling or a unique name), the user is presented with a pop-up to correct it before adding.

**This quickly builds a personal vocabulary, but it must be designed with strong privacy controls**:

- It should never silently read arbitrary documents.
- It must require an explicit user action.
- It must make it clear what text will be examined and what will be saved.
- Secure content must not be read.

---

## 13. DICTIONARY SYNCHRONIZATION (OS INTEGRATION)

The engine must decide how it relates to the operating system’s user dictionary and the keyboard’s own user history. Possible policies include:

- Read existing OS user-dictionary words when the engine starts.
- Watch for newly learned tap-typed words.
- Import selected words rather than importing everything automatically.
- Allow the user to push selected learned words to the OS dictionary.
- Keep gesture-learned data internal unless the user explicitly asks for synchronization.
- Handle duplicate words with different capitalization (e.g., "apple" vs "Apple").
- Detect when a word was removed from the external dictionary and respect that removal.

Words explicitly confirmed by the user may optionally be written to the OS dictionary in the background. This should be an **opt-in** behavior because external dictionary storage may be shared with other keyboard features.

---

## 14. INTERNAL PERSONAL DICTIONARY MANAGEMENT SCREEN

If the engine exposes a screen for managing learned words, it should provide more than a simple list. Useful controls include:

- **Search** for a word.
- View the word’s usage strength and acceptance count.
- View when it was last used.
- View how many gesture examples it has.
- **Delete** the word and all associated gesture data.
- **Edit** the word and preserve or remove its gesture associations.
- **Remove only the gesture associations** (keep the word for tapping).
- **Reset** the word’s learned rank.
- **Sort** by oldest/newest, most/least used, highest/lowest weight, or alphabetically (A-Z or Z-A).
- **Import/Export** words to a file.
- **Synchronize** selected words with the OS dictionary.

A scroll handle or another fast navigation method may be useful when the learned list becomes large. Any editing screen should make it clear whether an action affects only the personal dataset or also the system dictionary.

---

## 15. STORAGE, BACKUP, AND RECOVERY

Learned gesture data should be stored in durable application data storage, **not** a temporary cache directory that the OS may clear. The user should not lose their learned words simply because they cleared the app cache.

A durable storage plan must answer:

- Where the file or database lives.
- Whether it is included in the keyboard’s normal backup system.
- Whether the user can export it manually.
- Whether the user can import it on another installation.
- Whether the file is versioned (crucial for future updates).
- How incomplete writes are detected.
- How a corrupt file is recovered.
- How the user clears all learned data.
- How layout and language changes affect stored paths.

**Safe Save Protocol**: Write a complete temporary copy first, verify it (e.g., checksum), and then replace the previous valid copy. Saving should happen in the background so it cannot make typing lag. The previous valid data should be preserved until the new copy has been successfully written.

The storage format must be versioned from the beginning. Future versions may add capitalization, multiple paths, word relationships, blacklists, or context information. Older data should be migrated deliberately or safely ignored; it should never be interpreted under a new format by accident.

---

## 16. SETTINGS CATEGORIES FOR A COMPLETE PRODUCT

Settings should be grouped by category, not exposed as a long list of unexplained numbers.

**Recognition Behavior**

- Enable/disable gesture typing.
- Choose the active word vocabulary policy (Broad, Personal First, Personal Only).
- Set how conservative recognition should be (e.g., require high path similarity).
- Set whether a manual broad search is available.

**Learning Behavior**

- Enable/disable learning entirely.
- Learn tapped words that are already known.
- Learn tapped words that are new and explicitly confirmed.
- Choose how many gesture examples to keep per word.
- Choose how quickly a word gains personal strength.
- Choose whether generated path variations are allowed.
- Sync selected words with the OS dictionary.

**Gesture-Hint Behavior**

- Dwell sensitivity.
- Wiggle sensitivity.
- Circle/loop sensitivity.
- Whether hints are enabled at all.

**Layout and Tolerance Behavior**

- Start-key tolerance.
- End-key tolerance.
- Middle-path tolerance.
- Direction-change sensitivity.
- Treatment of edge keys.
- Treatment of fly-over letters.

Advanced controls can be hidden behind an "Expert" section rather than presented to every user.

---

## 17. TESTING THE COMPLETE USER EXPERIENCE (CRUCIAL)

The engine must be tested as a learning product, not only as a mathematical matcher. A useful test plan includes:

1. Gesture a common word, close the keyboard, and verify that its learned information remains available later.
2. Correct a selected word from the suggestion strip and verify that future ranking change.
3. Use two conflicting words (e.g., `not` and `nor`) repeatedly and verify that both remain usable.
4. Add a new word explicitly and verify that capitalization is preserved correctly.
5. Remove a learned word and verify that its gesture data and relationships are also removed.
6. Enable private mode and verify that no new counts or paths are saved.
7. Add a word to the OS dictionary and verify the chosen synchronization behavior (opt-in).
8. Clear the app cache and verify that durable learned data is NOT erased.
9. Interrupt a save (e.g., force close the app mid-write) and verify that the previous valid data can still load.
10. Use a large dictionary and verify that index building does not crash or freeze input.
11. Use a custom layout and verify that start, end, edge, and punctuation behavior follows the actual layout geometry.
12. Test punctuation immediately after a gesture and verify that no unwanted space is left behind.
13. Long-press a suggestion and verify that each learning action affects only the intended data (e.g., delete vs remove gesture).

The most important user-facing measurements are not only top-ranked accuracy. Also measure:

- How often the user **immediately corrects** a result.
- How often a short word loses to a longer word.
- How often a learned correction is remembered.
- How often the engine fails to recover without a second gesture.

---

## 18. WHAT REMAINS OUT OF SCOPE FOR THIS DRAFT

The following topics are deliberately excluded because they are either defunct, theoretical, or too technical for this first documentation pass:

- A dual-engine pipeline (running L2 and DTW side-by-side).
- Early or live word prediction *before* the gesture is complete.
- Engine-specific parallel execution details (e.g., threading models).
- Exact binary field layouts or byte-level serialization.
- Exact numerical scoring formulas (these require tuning based on the actual data).
- Platform-specific method names and source-file integration steps.
- Build instructions and settings-code migration history.

Those topics can be revisited later if the single-engine design requires them, but they are not established behavior in this non-technical guide.

---

## 19. ACTIONABLE CHECKLIST FOR THE DEVELOPER / LLM

1. **Parse** and **compress** a 50,000-word Typing Dictionary (curated for real words).
2. **Build** the Word Cache (in-memory `HashMap` or `LruCache`) with a clear graduation timer.
3. **Design** the Learned Words SQLite schema (Word, Unigram, Trigram, Gesture Paths, Acceptance/Rejection Counts, Last Used, Source, Capitalization).
4. **Optimize** the DTW engine with LB_Keogh, Sakoe-Chiba band, and Early Abandon.
5. **Implement** the "Synthetic Gesture" multiplication logic (3x expansion for cold-start).
6. **Add** User Intents detection (Circle, Dwell, Wiggle, Snake, Under/Over).
7. **Code** the Automatic Mode transition algorithm (seeding with top 1000 words).
8. **Implement** the Manual Recovery feature (Magic Gesture + toolbar button).
9. **Build** the Settings screen with categorized, plain-language options.
10. **Implement** the Personal Dictionary Management screen (Search, Sort, Edit, Delete).
11. **Write** the Safe Save Protocol (write-verify-rename) and versioning system.
12. **Design** the Private/Incognito Mode to block all learning.
13. **Create** the Learning Mode document scanner with strict privacy guards.
14. **Test** all 13 items in the Testing section above on a real device with custom layouts.

---

This blueprint covers the philosophy, data structures, algorithmic optimizations, user experience, storage safety, privacy, and testing required to build a superior gesture keyboard. By following this, you move beyond "tap typing with a swipe option" and into genuine, high-speed, adaptive gesture input.

___
___

# MISSED ITEMS

- Shortcuts: Dictionary and android user dictionary shortcuts must be handled differently. Dictionary shortcuts are usually set to f=0, therefore they have to be separated out because the actual output is whitelisted. Furthermore, the comma, period, apostrophe, and space should be recognized and utilized in shortcuts. All shortcuts should be available to the gesture engine and gestureable.
- gesture typing in different fields: url etc
- [Early prediction mode](https://github.com/cinnabar777/OpenSwipe/blob/main/Docs/DTW_PART_1.md#early-prediction-mode-theoretical)
- [Vowel Vortex Clarity Layout Optimization](https://github.com/cinnabar777/OpenSwipe/blob/main/Docs/DTW_PART_1.md#vowel-vortex-clarity) 
