
**THIS IS A WORK IN PROGRESS AND IS NOT COMPREHENSIVE!** 

___

This version was produced by Deepseek based on [Part 1](https://github.com/cinnabar777/OpenSwipe/blob/main/Docs/DTW_PART_1.md), my original writing, and [Part 2](https://github.com/cinnabar777/OpenSwipe/blob/main/Docs/DTW_PART_2.md), Replit compilation of old documentation I had from originally working on the engines. These documents are in the [Docs folder](https://github.com/cinnabar777/OpenSwipe/tree/main/Docs). 

I've added a chat at the end with 4 different AI starting from a discussion on Swype keyboard. It really is about L2 vs DTW. While I was under the impression that DTW would be more accurate and better for custom layouts I've been able to test some keyboards running both, and the reality is, it comes down to tuning and learning. 

And there is a chat on [early prediction](https://github.com/cinnabar777/OpenSwipe/blob/main/Docs/early_prediction.md). 
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
___
___

# AI CHATS SWYPE+ (L2 & DTW)
___
___
___
___

Yes, the architecture behind the original **Swype keyboard** is well-documented through its core patents—most notably **US Patent 7,098,896** ("System and method for continuous stroke word-based text input") filed by Swype's co-inventor, Cliff Kushler. [[1](https://www.cnn.com/2011/TECH/innovation/01/05/swype.kushler/), [2](https://patents.google.com/patent/US7098896B2/en), [3](https://arstechnica.com/gadgets/2018/02/swype-keyboard-it-lived-everyone-copied-it-and-now-its-dead/)]

Swype did not use neural networks. It was a deterministic, **algorithmic geometric shape-matching engine** combined with a **probabilistic language model**. [[1](https://en.wikipedia.org/wiki/Swype), [2](https://www.quora.com/Which-data-structure-is-used-by-samsung-swype-keypad)]

The exact dual-engine system Swype pioneered to decode gestures consists of the following components:

1. The Spatial (Geometric) Engine

Instead of trying to figure out which exact letters you were hitting, Swype treated the entire keyboard as a 2D graph and your finger trace as a single geometric curve. [[1](https://scholarworks.calstate.edu/downloads/1831cq73s), [2](https://arstechnica.com/gadgets/2018/02/swype-keyboard-it-lived-everyone-copied-it-and-now-its-dead/)]

- **Bounding Box Trajectory:** The engine looked at the exact coordinate vector of your finger. It paid special attention to **inflection points** (where your finger sharply changed direction) and **pauses** (where the finger slowed down over a letter). [[1](https://www.quora.com/Whats-the-algorithm-behind-the-Swype-keyboard), [2](https://www.nytimes.com/2010/06/21/technology/21swype.html)]
- **Distance/Error Scoring:** It compared the user's drawn shape against the "ideal paths" of words in its dictionary. It calculated an error score based on how far the drawn path strayed from the ideal path. [[1](https://www.quora.com/Which-data-structure-is-used-by-samsung-swype-keypad), [2](https://www.extremetech.com/extreme/97837-how-does-swype-really-work)]
- **Wildcard Channeling:** To account for human sloppiness, it constructed a "channel" or buffer zone around the swipe trajectory. Any word whose ideal path fell completely inside that fuzzy channel was added to a list of potential candidates. [[1](https://patents.google.com/patent/US7098896B2/en), [2](https://www.extremetech.com/extreme/97837-how-does-swype-really-work), [3](https://arstechnica.com/gadgets/2014/09/a-look-at-third-party-keyboards-on-ios-8-type-any-way-you-want/)]

1. The Lexical (Language) Engine

A purely geometric engine breaks down when words have identical shapes (for example, swiping "our" vs. "one" on QWERTY results in almost the exact same straight line). Swype solved this using structural data models to rank the candidates: [[1](https://en.wikipedia.org/wiki/Swype), [2](https://arstechnica.com/gadgets/2018/02/swype-keyboard-it-lived-everyone-copied-it-and-now-its-dead/)]

- **Directed Acyclic Word Graphs (DAWG) / Tries:** The dictionary wasn't a flat list. It was stored in a highly compressed prefix tree (Trie/DAWG). As your finger moved, the algorithm simultaneously walked down the branches of this tree, immediately discarding paths that didn't form real words. [[1](https://www.quora.com/Which-data-structure-is-used-by-samsung-swype-keypad)]
- **N-Gram Frequency Modeling:** If multiple words perfectly matched your gesture's shape, Swype used context. It analyzed the *previous* words you typed (bigrams and trigrams) to predict which candidate made the most sense grammatically. [[1](https://www.quora.com/Which-data-structure-is-used-by-samsung-swype-keypad)]
- **The "Loop" Rule for Double Letters:** Because you cannot natively swipe a double letter without lifting your finger, Kushler explicitly hardcoded a rule into the gesture engine: if a user makes a tiny circular loop on top of a key, the geometric decoder flags it as a double character (e.g., looping on 'O' to type "school"). [[1](https://www.quora.com/Whats-the-algorithm-behind-the-Swype-keyboard), [2](https://allaboutsymbian.com/news/item/13280_Swype_for_Symbian_updated_to_v.php), [3](https://www.theregister.com/on-prem/2022/06/06/microsofts-swype-still-the-best-software-keyboard-there-is/328522)]

Why this matters for your AI build

Swype's classic architecture proves that a highly accurate keyboard can be built using pure math and data structures rather than heavy AI models. Because Swype relies on clean concepts like **coordinate mapping, inflection point detection, and prefix trees**, an LLM will be excellent at helping you write and optimize the code for it. [[1](https://en.wikipedia.org/wiki/Swype), [2](https://www.quora.com/Which-data-structure-is-used-by-samsung-swype-keypad), [3](https://www.cnet.com/pictures/best-third-party-keyboards-for-iphone/), [4](https://www.vox.com/2014/6/2/11627562/swiftkey-swype-already-working-on-ios-keyboard-apps)]

Referring back to the original three options, the original **Swype keyboard** used the **SHARK / Sequence Alignment** architecture. [[1](http://pokristensson.com/gesturekeyboard.html)]

In fact, Swype is a direct commercial evolution of the **SHARK** (Shorthand Aided Rapid Keyboarding) engine. SHARK was originally developed in an academic setting by researchers Shumin Zhai and Per Ola Kristensson. Cliff Kushler (the co-inventor of T9 text input) took those core geometric sequence-matching concepts, refined them into a highly optimized commercial package, and co-founded Swype. [[1](https://www.researchgate.net/figure/Shorthand-aided-rapid-keyboarding-SHARK-The-first-publicly-released-fully-functioning_fig5_254463377), [2](https://en.wikipedia.org/wiki/Swype), [3](http://pokristensson.com/gesturekeyboard.html)]

How Swype Map Alignment Works

Rather than checking basic templates like the **$1 Recognizer**, Swype aligned shapes using **sequence matching and bounding-box nodes**:

- **Sequence Node Alignment:** The keyboard breaks your swipe path into a series of key boxes. It matches the sequence of characters you passed over using a modified version of the **Smith-Waterman or Longest Common Subsequence (LCS)** algorithm. It then flags the chronological order of letters that match words in the dictionary prefix tree. [[1](http://pokristensson.com/gesturekeyboard.html), [2](https://www.linkedin.com/pulse/swype-style-keyboard-decoding-using-linq-sudipta-mukherjee-greqc)]
- **Inflection Point Emphasis:** Swype gives higher mathematical weights to "vertexes"—points on the screen where your finger sharply changed direction or slowed down. If you made a sharp turn on a letter, the engine prioritized that letter heavily over keys you just flew over in a straight line.

Why SHARK/Swype Is Ideal For ClearFlow

Because ClearFlow is a layout specifically optimized for fluid motion, the **SHARK sequence alignment engine** is exactly what you want. Your AI assistant will find this architecture easier to construct than a massive template catalog because it relies on standard algorithmic loops and discrete letter coordinate boundaries.

If you want to focus on this architecture, I can show you how to write the logic for an **inflection point detector** in Kotlin. This will strip out unnecessary straight-line data from a gesture and keep only the critical corner coordinates where the user turned.

**Yes, combining Dynamic Time Warping (DTW) with the Swype/SHARK framework is a proven approach that directly addresses the weaknesses of the original system.** [[1](https://dl.acm.org/doi/fullHtml/10.1145/3290605.3300395)]

Human-computer interaction (HCI) researchers have successfully melded these two methodologies—most notably in projects like **HotStrokes (word-gesture shortcuts on trackpads)**—specifically because SHARK's classic layout engine struggles with mid-word letter ambiguity, which DTW effectively solves. [[1](https://dl.acm.org/doi/fullHtml/10.1145/3290605.3300395)]

---

The Flaw in Swype/SHARK (Why it needs DTW)

The original SHARK/Swype engine relies heavily on **inflection points** (vertices where your finger changes direction) and **word-bounding boxes** to determine context. If you swipe a straight line through multiple letters, it relies on static sequence math (like the Smith-Waterman sequence alignment) to guess what you meant.

This introduces two distinct failure points:

1. **The "Straight-Line Mud" Problem:** If your layout requires you to fly straight through a letter without turning on it, the classic SHARK algorithm often skips it or penalizes the word because there was no geometric "vertex" there.
2. **Speed & Sampling Artifacts:** Human swiping varies wildly in speed. A user might accelerate through the beginning of a word and slow down dramatically toward the end. Classic shape-matching algorithms get confused by this uneven time-series scaling.

---

How DTW and SHARK Meld Together

When you fuse them, you create a **Hybrid Spatiotemporal Decoder**. Instead of replacing SHARK with DTW, you use them sequentially to maximize the benefits of both systems: [[1](https://dl.acm.org/doi/fullHtml/10.1145/3290605.3300395), [2](https://www.researchgate.net/figure/llustration-word-shorthand-gesture-keyboarding-is-expected-to-shift-from-primarily_fig3_254463377)]

```
[User Swipe Path] ──> [SHARK Filter] ──> Extracts Top Candidate Words
       │
       ▼
[Output Word] <── [Selects Lowest Cost] <── [DTW Engine] (Calculates time-warped path error)
```

1. **Step 1: SHARK acts as the structural filter.** The raw gesture passes through the SHARK logic. It discards 99% of the dictionary by throwing away words whose layout boundaries do not match the geometric bounds of the trace.
2. **Step 2: DTW acts as the micro-decoder.** For the remaining handful of highly ambiguous candidate words, DTW maps the user’s continuous stroke directly onto the "ideal template paths" of those words. [[1](https://dl.acm.org/doi/fullHtml/10.1145/3290605.3300395)]

Because DTW allows for non-linear temporal alignment, it can dynamically stretch or compress parts of the user's swipe path to find the structural fit against a dictionary word, ignoring whether the user sped up or slowed down over a particular letter. [[1](https://www.mdpi.com/2313-433X/10/5/123), [2](https://medium.com/@mail2rajivgopinath/dynamic-time-warping-bridging-the-gaps-in-time-series-analysis-e92b16049b10), [3](https://medium.com/@markstent/dynamic-time-warping-a8c5027defb6)]

3 Major Upgrades DTW Brings to ClearFlow

Melding these two approaches provides unique advantages when developing a custom layout engine:

- **Perfect Mid-Gesture Matching:** If you swipe the word *"Water"*, your finger passes straight over the `A` and the `E` keys without making a hard turn. DTW excels at matching these intermediate points to the ideal template letters even without structural inflection corners. [[1](https://dl.acm.org/doi/fullHtml/10.1145/3290605.3300395)]
- **Fuzzy Bounding Tolerance:** If the layout is small or the user is swiping carelessly, DTW's matrix calculation inherently allows for structural deviations while still maintaining strict sequential alignment order.
- **Deterministic Execution:** The combined system operates as a purely mathematical matrix calculation. It requires no AI weight tuning or training epochs, making it simple to code using basic array logic.

Implementation Strategy for an AI-Assisted Build

If you instruct an AI assistant to write the code for this combined approach, avoid combining them immediately. Build the system in two distinct steps:

1. **Ask the AI for a "Bounding Box Lexicon Pruner":** Have it create code that filters the dictionary down to words that match the first letter, last letter, and general structural bounding box of the swipe.
2. **Ask the AI for a "2D Dynamic Time Warping (DTW) Matrix Calculator":** Give it a matrix framework where `X` and `Y` coordinates represent the two sequences being aligned. The AI can then write the dynamic programming loop that fills the cost matrix and outputs the minimum alignment cost for the pruned words.

___

___

As an AI analyzing this from a computational efficiency and data-structuring perspective, the SHARK + DTW hybrid is a fantastic deterministic architecture. Using SHARK as the structural filter to discard the bulk of the dictionary and DTW as the micro-decoder to calculate the time-warped path error directly addresses the classic "straight-line mud" problem.

However, a purely spatiotemporal decoder is only half the battle. To turn this mathematical framework into a highly accurate, production-ready keyboard, I would add three specific components to the pipeline:

# 1. N-GRAM INTEGRATION AND FREQUENCY WEIGHTING

Even with DTW providing perfect mid-gesture matching, the engine will still encounter collisions where two words share nearly identical geometric paths (e.g., swiping "pit" vs. "pot"). To resolve this, the spatiotemporal cost from the DTW matrix must be merged with a lexical cost.

You can accomplish this by processing custom wordlists to generate static word frequencies and bigram data, and then compiling them into a highly compressed binary dictionary format (similar to how AOSP dictionaries operate). By integrating these compiled word frequencies and bigram probabilities into the final scoring loop, the engine can heavily penalize obscure words or grammatically unlikely sequences, allowing contextual logic to break geometric ties.

# 2. CONSTRAINING THE DTW MATRIX

Standard Dynamic Time Warping is computationally heavy, operating with a time complexity of $O(N \times M)$ where $N$ and $M$ are the coordinate sequence lengths. Running an unconstrained matrix calculation for multiple candidate words simultaneously can introduce input lag on mobile devices.

To optimize this, you should implement a global constraint like a **Sakoe-Chiba Band** or an **Itakura Parallelogram** within the DTW loop. This restricts the allowable warping path to a specific mathematical window around the diagonal of the matrix. It prevents the algorithm from wasting CPU cycles calculating wildly distorted temporal alignments that a human wouldn't naturally make.

# 3. L2 DISTANCE ANCHORS FOR ENDPOINTS

While DTW is ideal for fuzzy bounding tolerance and non-linear stretching in the middle of a word, the initial touch-down and final lift-off of a swipe are typically highly deliberate.

Before passing the swipe path to the SHARK filter, I recommend adding a strict Euclidean distance check (using the $L_2$ norm) specifically for the first and last coordinates against the target keys. If the $L_2$ distance of the start/end points exceeds a specific geometric radius, the candidate word is instantly rejected. This acts as a highly aggressive pre-filter that saves the SHARK and DTW engines from processing unnecessary data.

This three-pronged addition ensures that your engine is not only geometrically accurate but also contextually aware and computationally optimized.

___

___

This is an exceptionally well-reasoned foundation. The SHARK + DTW hybrid with N-gram weighting, constrained matrices, and L2 anchors already addresses the core failure modes of classic gesture keyboards. However, to push this from a *theoretically sound* system to a *production-robust* one, I would add five critical layers to the pipeline.

# 1. PRE-PROCESSING: PATH SMOOTHING AND VERTEX COMPRESSION

Raw touch events are noisy (jittery pixels). If you calculate inflection points directly on raw coordinates, you will flag hundreds of false vertices due to micro-tremors.

- **Add a Douglas-Peucker or Ramer-Douglas-Peucker (RDP) algorithm** before the SHARK filter. This simplifies the gesture path into a polyline with a tolerance parameter (e.g., 3-5 pixels).
- **Why it matters:** It drastically reduces the DTW sequence length (NN and MM), which directly reduces the computational complexity of the matrix from O(N×M)O(N×M) to O(N′×M′)O(N′×M′). It also ensures that the "Inflection Point Emphasis" only triggers on *deliberate* directional changes, not screen noise.

# 2. KEY-AFFINITY HEATMAPS (GAUSSIAN BOUNDING, NOT BINARY)

The text mentions "wildcard channeling" (fuzzy buffer zones) and bounding boxes. This is a binary check (inside/outside). I would upgrade this to a **Gaussian Probability Density** mapping.

- Instead of checking if a path point falls *inside* a key's rectangle, calculate a 2D Gaussian weight centered on each key's centroid, where the standard deviation (σσ) correlates to the distance between adjacent keys.
- **Why it matters:** When a path cuts *just barely* outside the corner of a key, a binary bounding box rejects it. A Gaussian heatmap assigns a slight penalty but doesn't kill the word. This is crucial for the SHARK filter; it prevents valid words from being discarded prematurely, allowing the DTW engine to work its magic later.

# 3. HANDLING "SKIPS" VIA AFFINE GAP PENALTIES (SMITH-WATERMAN VARIANT)

DTW *stretches* paths, but it doesn't natively handle *skipping* (gaps) unless heavily distorted. In fast swiping, users often completely miss a letter (e.g., brushing the keypad edge).

I would modify the initial SHARK sequence alignment step to use a **Needleman-Wunsch (Global) or Smith-Waterman (Local)** algorithm with **affine gap penalties** (a high penalty for opening a gap, but a low penalty for extending it).

- **Why it matters:** This explicitly allows the algorithm to "skip" a single letter if the spatial distance is extreme, without having to warp the entire rest of the path to accommodate it. It works beautifully with the DAWG/Trie: if the path matches "P-A-T" but the user completely overshot the 'A', the gap algorithm can insert a deletion penalty for 'A' and still rank "PAT" highly.

# 4. MERGE N-GRAM AND SPATIAL COSTS **BEFORE** THE CANDIDATE FINALIZATION

The text suggests using N-gram to break ties *after* DTW. I recommend **fusing the costs into a single objective function** earlier in the pipeline:

TotalCost=α⋅DTW_Geometric_Cost+β⋅Ngram_Log_Prob+γ⋅Freq_Log_ProbTotalCost=α⋅DTW_Geometric_Cost+β⋅Ngram_Log_Prob+γ⋅Freq_Log_Prob

- **Why it matters:** If you run N-gram *after* DTW, you are spending CPU cycles running DTW on geometrically terrible words just to use N-gram to throw them out later. By applying weighted lexico-spatial scoring *during* the traversal of the Trie (passing the accumulated cost down the tree), you can implement **branch-and-bound pruning**—aborting the DTW calculation for a branch early if its combined cost exceeds the current best-word cost by a wide margin. This dramatically reduces the dictionary search space.

# 5. DYNAMIC CALIBRATION OF THE SAKOE-CHIBA BAND (SPEED-ADAPTIVE WINDOW)

You mentioned a fixed Sakoe-Chiba band for DTW. I would make the window width **dynamically proportional** to the ratio of the *gesture path length* to the *ideal path length*, and adjust it based on the user's *instantaneous speed* at the start of the swipe.

- **Why it matters:** A user swiping "the" slowly will have a high point density, while a user swiping the same word quickly will have sparse points. If the band is too wide, you waste CPU; if it's too narrow, you reject valid swipes. By calculating the average Euclidean step size between touch events and setting the band width to, say, 15% of the total path length, you maintain sub-linear complexity while preserving flexibility.

# A NOTE ON THE "LOOP RULE" (DOUBLE LETTERS)

The text mentions the hardcoded loop rule. I would **not** hardcode this as a special case in the geometric engine. Instead, I would treat it as a **sub-path feature**:

- Detect circularity in the RDP-smoothed path using the area-to-perimeter ratio (4πA/P24πA/P2).
- If a localized loop is detected over a key with an area > a small threshold, inject an *additional* virtual coordinate into the sequence representing a "double-press" of that key, and let the DTW alignment naturally warp to that extra point.
- This allows the system to handle double letters without requiring a static rule that might break in custom layouts.

# FINAL RECOMMENDATION FOR YOUR AI BUILD ORDER

If you are tasking an AI to implement this, break the build into **four distinct modules**:

1. **Path Simplifier** (RDP + Gaussian Weighting).
2. **Trie Walker with Branch Pruning** (using the fused cost function).
3. **Constrained DTW Core** (with dynamic banding).
4. **Gap-Alignment Pre-Filter** (for OOV words or miss strokes).

This preserves the deterministic, non-neural philosophy while eliminating almost all edge-case failures of the original Swype design.

___

___

Yes. The combined approach is strong, but I would change the **role and order of several components**. The biggest risk is building an unnecessarily expensive DTW-heavy system that is elegant mathematically but less accurate than a simpler probabilistic decoder.

# WHAT I WOULD CHANGE

## 1. TREAT THE SWIPE AS EVIDENCE OVER KEY SEQUENCES—NOT PRIMARILY AS A TEMPLATE SHAPE

The document frames DTW as matching the entire gesture against an “ideal path” for every candidate word. That can work, but it has two weaknesses:

- There is no single ideal path: users vary in hand position, speed, curvature, device, and keyboard layout.
- DTW can over-align noisy or accidental loops and produce a plausible-looking but wrong match.

I would make the core representation a sequence of **per-sample key likelihoods**:

P(kt∣xt)P(kt​∣xt​)

where each touch sample receives a soft probability distribution over nearby keys. Then decode word candidates with a weighted finite-state search:

gesture score+language score+transition scoregesture score+language score+transition score

This is closer to a Hidden Markov Model or weighted finite-state transducer than pure template matching. DTW can remain as an optional refinement, not the central decoder.

## 2. REPLACE “HARD SHARK FILTER, THEN DTW” WITH BEAM SEARCH

The proposed pipeline is:

`   SHARK filter → candidate words → DTW reranking   `

The danger is false negatives: if SHARK prunes a correct word early, DTW never gets a chance to recover it. Instead, use a **beam search over trie states**:

- Maintain the best 20–100 partial word hypotheses.
- Advance, skip, or repeat a character with explicit costs.
- Prune only when a candidate’s lower-bound score is clearly worse.
- Use the language model while expanding the beam.

This gives you the useful performance properties of a trie without making the first stage a brittle gate.

## 3. DO NOT FUSE LANGUAGE PROBABILITY TOO EARLY WITHOUT CALIBRATION

The recommendation to merge DTW, n-gram, and frequency costs during traversal is directionally right, but the weights are not naturally comparable.

A geometric score, a log frequency, and an n-gram log probability have different scales and distributions. I would:

1. Normalize each component on held-out swipe data.
2. Start with a linear score.
3. Tune weights against **top-1 accuracy, top-3 recall, correction rate, and latency**.
4. Add a confidence margin between the firstand second-ranked candidates.

___

### WHAT “TUNING WEIGHTS” MEANS

The decoder might score a candidate like this:

S=wgG+wkK+wsSq+wlL+wpPS=wg​G+wk​K+ws​Sq​+wl​L+wp​P

Where:

- GG: geometric/path mismatch
- KK: key-affinity mismatch
- SqSq​: sequence alignment cost, including skips and repeats
- LL: language-model cost from preceding words
- PP: personal-word/frequency adjustment

The weights determine how much the engine trusts each signal. They are just numbers—not neural-network parameters.

You can start with manually chosen weights, for example:

`   geometry: 0.40  key sequence: 0.30  language model: 0.20  word frequency: 0.07  personal vocab: 0.03   `

Those values would not be assumed correct; they would be measured and adjusted.

### HOW MUCH TRAINING IS NEEDED?

There are three separate levels:

#### 1. NO TRAINING: FUNCTIONAL PROTOTYPE

You can build a useful first version with:

- A dictionary/trie
- General word frequencies
- Handwritten geometric rules
- Fixed insertion, deletion, and repeat costs
- A small bigram language model

No swipe recordings are required.

#### 2. LIGHTWEIGHT TUNING: RECOMMENDED

For good accuracy, collect labeled examples:

`   gesture trace → intended word   `

You do not need millions of examples. A practical first tuning set might be:

- **1,000–5,000 swipe examples** for initial weight fitting
- Several users, not just one
- Common words plus deliberately messy swipes
- Separate validation data that is never used to tune weights

A simple optimizer—grid search, coordinate descent, logistic regression, or ranking optimization—can find better weights. This is statistical calibration, not neural training.

#### 3. PERSONALIZATION: CONTINUOUS AND SMALL

The keyboard can improve for an individual by tracking:

- Which candidate they select
- Which words they correct
- Their common vocabulary
- Their typical swipe offset and speed

That can be implemented as frequency updates and small calibration adjustments. It can happen entirely on-device and does not require sending gestures to a server.

### WHERE THE LANGUAGE MODEL COMES FROM

The language component can also be non-neural:

- Word unigram frequencies
- Bigram or trigram counts
- A compact interpolated n-gram model
- A personal vocabulary overlay

For example, after “ I am going,” the language model can rank “home” above a geometrically similar but rare word. A neural language model could improve this later, but it is not necessary for the gesture engine.

### MY RECOMMENDATION

Build in this order:

1. **Fixed deterministic decoder** with hand-tuned weights.
2. Add logging of gesture, intended/selected word, candidate scores, and latency.
3. Tune the weights against real labeled swipes.
4. Add personal vocabulary adaptation.
5. Only consider a neural language model if the remaining errors are mostly contextual—not geometric.

The expensive part is not “training an AI.” It is collecting representative swipe data and defining the scoring functions correctly. The core engine can remain small, fast, explainable, and fully offline.

___

Also, frequency should not simply “heavily penalize obscure words.” That can make names, technical terms, slang, and recently learned vocabulary nearly impossible to enter. Use a fallback personal dictionary and adaptive user vocabulary.

## 4. USE DTW SELECTIVELY, NOT FOR EVERY CANDIDATE

Constrained DTW still costs roughly O(NM)O(NM) per candidate. On a phone, I would use a three-stage scorer:

1. **Cheap candidate generation:** endpoint anchors, path length, key likelihoods, monotonicity, rough letter progression.
2. **Cheap sequence score:** beam/trie alignment with insertion, deletion, and repetition costs.
3. **DTW refinement:** only for the top perhaps 8–32 ambiguous candidates.

For the refinement stage, consider **derivative DTW** or a normalized polyline distance rather than raw coordinate DTW. Raw coordinates overvalue sampling density and screen-size differences.

## 5. BE CAREFUL WITH THE GAUSSIAN KEY-AFFINITY MODEL

A Gaussian centered on each key is a good starting point, but a circular isotropic Gaussian is not quite right for a keyboard:

- Keys are rectangular.
- Touch error is often anisotropic.
- Nearby keys can have different visual and behavioral separation.
- The finger may pass between keys intentionally.

I would use a learned or calibrated **rectangular/anisotropic affinity function**, perhaps based on distance to the key’s expanded rectangle, with separate horizontal and vertical variances. Keep the continuous score, but do not interpret it as a true probability unless calibrated.

## 6. ADD EXPLICIT STROKE-STATE MODELING

The current plan focuses mainly on coordinates. I would preserve these touch features too:

- Touch-down and lift-off confidence.
- Velocity and acceleration.
- Dwell time near a key.
- Direction changes.
- Whether the path crosses a key or merely passes near it.
- Device orientation, keyboard scale, and keyboard translation.

A small state machine should also distinguish:

- Tap versus swipe.
- Swipe versus accidental drag.
- Intentional pause.
- Word boundary or space gesture.
- Backspace/delete gesture.
- Gesture cancellation.

This will improve real-world behavior more than another refinement to the DTW metric.

## 7. CHANGE THE DOUBLE-LETTER LOOP RULE

The proposed loop detection is a good abstraction, but injecting a “virtual coordinate” into the DTW sequence may create odd behavior.

I would model repeated letters as an explicit **repeat transition** in the decoder:

`   same key → repeat character   `

Then a loop, dwell, or local reversal increases the repeat-transition probability. This lets words such as “school,” “letter,” and “coffee” compete naturally with their single-letter alternatives without forcing geometry to represent a synthetic point.

## 8. ADD A PERSONALIZED ADAPTATION LAYER FROM THE BEGINNING

A production gesture keyboard needs to learn:

- Words the user enters repeatedly.
- Their preferred spelling and names.
- Their personal word frequencies.
- Their typical swipe offset and speed.
- Corrections made after selection.

This does not require a neural model. A small on-device adaptive lexicon plus per-user calibration parameters would likely deliver a substantial accuracy gain while preserving privacy and deterministic behavior.

# RECOMMENDED ARCHITECTURE

I would build it as:

Touch events

  ↓

Normalize + resample + smooth

  ↓

Extract key-affinity sequence and motion features

  ↓

Endpoint/length/validity prefilter

  ↓

Trie beam search with insert/delete/repeat transitions

  ↓

Language-model and personal-dictionary scoring

  ↓

Optional constrained DTW refinement for top candidates

  ↓

Confidence threshold + ranked alternatives

  ↓

Online personalization

# THE MAIN CORRECTION

I would not describe the result as “SHARK plus DTW.” I would describe it as a:

>**probabilistic key-sequence decoder with geometric features, trie-constrained beam search, and optional DTW reranking**

That design retains the deterministic, efficient spirit of Swype while avoiding the two biggest problems in the current proposal: **hard early pruning** and **using DTW as the primary source of linguistic structure**. The document’s RDP smoothing, endpoint anchors, gap handling, constrained warping, and adaptive vocabulary are all worth keeping, with those changes.

___

___

Short answer / recommendation

- Build the system as a probabilistic key-sequence decoder (per-sample key likelihoods) + trie-constrained beam search, with an optional constrained DTW reranker for the top-N hypotheses. That gives robust accuracy, low latency, and avoids brittle hard pruning. Below I summarize concrete design choices, prioritized implementation plan, algorithmic details, optimizations, evaluation recipe, UX & privacy considerations, and a compact Kotlin example (RDP + inflection extraction) you can drop into an MVP.

What I reviewed and what I’m delivering here

- I read your detailed SHARK+DTW proposal and agree with its main strengths (deterministic, explainable, good geometric modeling). I recommend shifting the pipeline so geometry supplies strong evidence to a beam/trie decoder rather than being a brittle gate. Below are actionable ideas, trade-offs, and a short Kotlin snippet for the path simplifier / vertex extractor.

High-level architecture (recommended)

1. Input preprocessing

  - Normalize, translate & scale to keyboard coordinates, low-pass smooth, resample to fixed spacing, Ramer–Douglas–Peucker (RDP) simplify.
  - Extract per-sample features: (x,y), velocity, acceleration, dwell time, +/- pressure if available.

2. Per-sample key-likelihood estimation

  - For each touch sample produce soft probabilities over a small neighborhood of keys (anisotropic Gaussian / rectified distance). This turns coordinates → sequence of key-likelihood vectors P(k_t | x_t).
3. Trie-constrained beam search (core decoder)

  - Beam of partial trie states (width 20–200 depending on device).
  - Transition types: advance (next key), repeat (same key), insertion/deletion (gap) with affine penalties.
  - Score components accumulated as you expand: geometry (sum log P(k_t|x_t) along matched indices), sequence alignment cost, LM cost (n-gram), word frequency / personal weight.
  - Use branch-and-bound: compute a lower-bound future cost for a trie branch; prune if worse than best-so-far + margin.

4. Optional DTW reranker for top-N

  - Only run DTW on top K candidates (K ~ 8–32).
  - Use constrained DTW (Sakoe–Chiba or Itakura) with dynamic band width proportional to path ratio & sampling density.
  - Use LB_Keogh or other lower-bound tests + early abandoning to prune before computing full matrix.
5. Post-processing & personalization

  - Merge geometric score and LM/frequency into a normalized objective. Calibrate weights on held-out gestures.
  - Update on-device personal lexicon and small per-user calibration (offset & sampling scale).
  - Provide top-3 alternatives for corrections and fast accept/correct UI.

Prioritized implementation plan (milestones)

- MVP (fast, small, usable)
  - Preproc: normalize + RDP + resample
  - Per-sample key affinity (anisotropic distance)
  - Compact trie/DAWG with unigram frequencies
  - Beam search decoder (no DTW), simple bigram model
  - Metrics logging (latency, top-1/top-3, selection/correction)
- Stage 2 (accuracy & robustness)
  - Add affine-gap smith-waterman/needleman-style costs in beam transitions
  - Add constrained DTW reranker with LB_Keogh & Sakoe–Chiba window
  - Better key-affinity (rectangular/aniso Gaussian, calibrated)
  - Loop/double-letter handling as repeat transition
- Stage 3 (optimization & personalization)
  - Dynamic band width (speed-adaptive)
  - On-device personalization, frequency updates, per-user offset
  - Native optimizations: SIMD, C++ core for DTW, memorypooling
  - A/B testing & weight optimization with small labeled set

Concrete algorithm and data-structure recommendations

- Dictionary: represent as DAWG or compressed Trie for memory + fast prefix traversal. Store frequency, bigram pointers, and precomputed bounding boxes and templates for frequent words.
- Per-word geometric template: store a normalized ideal polyline (multi-resolution). Normalization = translate so first point and last point map to key centers, scale to keyboard extents.
- Pre-pruning: use cheap tests before heavy compute:
  - bounding-box overlap,
  - start/end L2 anchor test against first/last key
  - path-length ratio tolerance (reject words whose ideal path length differs > threshold)
- DTW pruning: LB_Keogh lower bound against precomputed template envelope; early-abandon DTW if running cost exceeds best candidate.
- Gap handling: implement Smith-Waterman/Needleman-Wunsch variant with affine gap penalties when aligning discrete key sequences; it handles missing letters better than simple DTW.
- Scoring: compute normalized components (z-score or min-max from held-out data) before linear combination: Total = α·Geom + β·Seq + γ·LM + δ·Freq + ε·Personal. Start with manual weights, then fit via grid/search on validation set.
- Beam search specifics: each beam entry contains (trie node, t_index, score, backpointer). Allow multiple alignments per node with different t_index (time index) to represent skipping or repeating.

Performance & implementation optimizations

- Resample to fixed N (e.g., 32–128 points) to bound DTW cost.
- Multi-resolution templates: run cheap coarse DTW on downsampled traces, then refine on full trace for winners.
- LB_Keogh for DTW lower bounds (very effective).
- Use early-abandon in DTW with running cost > best-so-far.
- Precompute word templates as integer coordinates (fixed-point) to avoid float cost and increase cache friendliness.
- Use native library (C/C++) for heavy inner loops (DTW), with JNI for Kotlin/Java front-end if targeting Android.
- Memory: store dictionary in memory-mapped binary for fast loading and low RAM.
- Use incremental computation to reuse partial results when user corrects or types next word.

Evaluation, metrics, and data

- Metrics: top-1 accuracy, top-3 recall, average latency (ms), correction rate (how often user picks alternate), time-to-accept.
- Latency target: aim < 30–50 ms for decode on mid-tier phone (interactive feel); DTW reranker should only add a few ms because it runs on a tiny candidate set.
- Data: gather 1k–10k labeled swipes across users for tuning weights. Synthetic generation: perturb ideal templates with jitter/scale/anisotropy to augment.
- Logging: record raw trace, selected word, candidate list + scores, device parameters; keep on-device or opt-in telemetry.

UX considerations

- Show alternatives (top-3) and allow quick correction via long-press or small candidate strip.
- Provide explicit…

Continuing from "Provide explicit…" and finishing the interrupted section.

Provide explicit affordances for correction and confidence

- Show top-3 alternatives inline (candidate strip) and allow one-tap selection.
- Use size/opacity to indicate confidence: bold/center the highest-scoring candidate, dim lower-ranked ones.
- Offer an unobtrusive confidence indicator (tiny chevron or color) so power users can tell when the engine is unsure.
- When confidence is low, switch the accept behavior: show candidate strip automatically and make the initial selection non-committal (e.g., require a brief tap to confirm) to reduce wrong-autocompletes.

Privacy, telemetry & on-device personalization

- Keep personalization fully on-device by default: increment local unigram/frequency counts and per-user geometry calibration only after explicit opt-in.
- If remote telemetry is used (opt-in), transmit only aggregated or anonymized stats and never raw touch traces unless the user consents. Store labels separately and securely.
- Allow users to clear personal vocab and reset calibration parameters.

Testing, metrics & A/B strategy

- Unit tests: RDP simplifier, key-affinity scoring, trie traversal, beam pruning, DTW lower-bound & full DTW.
- Integration tests: synthetic swipe generator that perturbs templates (jitter, scale, anisotropy, missing letters, loops).
- Lab evaluation: gather 1k–5k labeled swipes; evaluate top-1 accuracy, top-3 recall, and latency distribution.
- A/B tests: different beam widths, different α/β/γ weights, and DTW-on vs DTW-off to measure latency vs accuracy tradeoffs. Use correction rate as the primary UX metric.

Production deployment notes

- Target latency: decode < 30–50 ms on mid-range devices (beam-only); DTW reranker should add < ~5–15 ms because it runs on just top-K candidates.
- Use native C/C++ for DTW inner loop (SIMD-friendly) and JNI wrapper for Kotlin/Java front-end on Android. Keep beam/trie in managed code if memory is fine.
- Memory map the binary dictionary to achieve fast cold start and low RSS. Precompute/downsample templates and LB_Keogh envelopes at build time.

Implementation checklist (practical defaults to start with)

- Preprocessing: RDP tolerance = 3–4 px, resample to 64 points, low-pass filter with small alpha.
- Key affinity: anisotropic rectangle distance, horizontal σx = key_width/2, vertical σy = key_height/2.
- Beam: width = 64 (adjustable), keep top 3 backpointers per trie node.
- Gap penalties: open = 1.5, extend = 0.5 (tunable).
- DTW: Sakoe–Chiba band = 15% of path length (dynamic), LB_Keogh early bound, early abandon if running cost > best-so-far.
- Scoring weights (starting): geometry α=0.45, sequence β=0.25, LM γ=0.20, freq δ=0.07, personal ε=0.03. Calibrate on held-out data.

Minimal on-device data collection schema (opt-in)

- For each accepted swipe: store (hashed user id if remote, timestamp, keyboard layout, resampled trace summary, selected word, top-3 candidates + scores, latency). Avoid storing raw high-resolution touch traces unless explicitly consented.

Small Kotlin example: RDP path simplifier + inflection (vertex) extractor

- This snippet implements Ramer–Douglas–Peucker to compress a polyline, then finds vertices (angle & dwell-based) useful for SHARK-like inflection emphasis. Drop into an Android/Kotlin module and adapt thresholds to device DPI.

```
// Compact RDP + inflection point extractor for gesture preproc.
// Tweak tolerances (epsilonPx, angleThresholdDeg, dwellThresholdMs) per layout/DPI.

data class Point(val x: Float, val y: Float, val t: Long) // t = timestamp ms

fun rdp(points: List<Point>, epsilon: Float): List<Point> {
  if (points.size < 3) return points
  var maxDist = 0f
  var index = -1
  val a = points.first()
  val b = points.last()
  fun sqr(v: Float) = v * v
  fun perpDistance(p: Point): Float {
  val dx = b.x - a.x
  val dy = b.y - a.y
  if (dx == 0f && dy == 0f) return kotlin.math.hypot(p.x - a.x, p.y - a.y)
  val num = kotlin.math.abs(dy * p.x - dx * p.y + b.x * a.y - b.y * a.x)
  val den = kotlin.math.hypot(dx, dy)
  return num / den
  }
  for (i in 1 until points.lastIndex) {
  val d = perpDistance(points[i])
  if (d > maxDist) {
    index = i
    maxDist = d
  }
  }
  return if (maxDist > epsilon) {
  val left = rdp(points.subList(0, index + 1), epsilon)
  val right = rdp(points.subList(index, points.size), epsilon)
  left.dropLast(1) + right
  } else {
  listOf(a, b)
  }
}

fun extractInflectionPoints(simplified: List<Point>, angleThresholdDeg: Float = 30f, dwellThresholdMs: Long = 40): List<Point> {
  if (simplified.size < 3) return simplified
  val verts = mutableListOf<Point>()
  verts.add(simplified.first())
  for (i in 1 until simplified.lastIndex) {
  val p0 = simplified[i - 1]
  val p1 = simplified[i]
  val p2 = simplified[i + 1]
  val v1x = p1.x - p0.x
  val v1y = p1.y - p0.y
  val v2x = p2.x - p1.x
  val v2y = p2.y - p1.y
  val dot = v1x * v2x + v1y * v2y
  val mag1 = kotlin.math.hypot(v1x, v1y)
  val mag2 = kotlin.math.hypot(v2x, v2y)
  val angle = if (mag1 == 0f || mag2 == 0f) 180f else {
    val cos = (dot / (mag1 * mag2)).coerceIn(-1f, 1f)
    kotlin.math.acos(cos) * (180f / Math.PI.toFloat())
  }
  val dwell = (p2.t - p0.t) // coarse dwell around p1
  if (angle >= angleThresholdDeg || dwell >= dwellThresholdMs) {
    verts.add(p1)
  }
  }
  verts.add(simplified.last())
  return verts
}

// Usage example:
// val raw: List<Point> = ... // collected touch points (x,y,t)
// val epsilonPx = 3f * density // e.g., scale by device density
// val simplified = rdp(raw, epsilonPx)
// val vertices = extractInflectionPoints(simplified)
```

What I’ve done in this message

- Resumed and finished the interrupted UX, privacy, testing, deployment sections.
- Added concrete defaults, implementation checklist, evaluation guidance, and a drop-in Kotlin utility for path simplification + inflection extraction.

What's next (recommended immediate next engineering steps)

1. Implement the preprocessing + per-sample key-likelihood module and the compact trie.
2. Implement beam search decoder (no DTW) and run the first lab evaluation with 1k synthetic + 200 real swipes.
3. Add DTW reranker with LB_Keogh and native optimization only if beam-only accuracy is insufficient.
4. Start lightweight weight tuning (grid search) and a small opt-in personalization loop.

___
___
___
___

# MISSED ITEMS

- Shortcuts: Dictionary and android user dictionary shortcuts must be handled differently. Dictionary shortcuts are usually set to f=0, therefore they have to be separated out because the actual output is whitelisted. Furthermore, the comma, period, apostrophe, and space should be recognized and utilized in shortcuts. All shortcuts should be available to the gesture engine and gestureable.
- gesture typing in different fields: url etc
- [Early prediction mode](https://github.com/cinnabar777/OpenSwipe/blob/main/Docs/DTW_PART_1.md#early-prediction-mode-theoretical)
- [Vowel Vortex Clarity Layout Optimization](https://github.com/cinnabar777/OpenSwipe/blob/main/Docs/DTW_PART_1.md#vowel-vortex-clarity) 
