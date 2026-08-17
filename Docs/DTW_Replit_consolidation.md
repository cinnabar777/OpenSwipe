
Source document: [OpenSwipe](https://github.com/cinnabar777/OpenSwipe)

Target projects:

- [WMKeyboard](https://github.com/wasi-master/wmkeyboard) (primary focus because it already includes a built-in gesture engine)
- AOSP keyboards:
  - [LeanType](https://github.com/LeanBitLab/LeanType)
  - [HeliBoard](https://github.com/HeliBorg/HeliBoard)
  - [SonderKey](https://github.com/Verisonder/SonderKey)

Editorial note: this is a consolidation of the uploaded source documents. It keeps the base organization of document 1 where possible, folds in missing detail from document 2 and document 3 Part 2, removes duplication, and preserves the original intent and voice where that clarifies the guidance. Statements are labeled as current/tested, recommended design, theoretical, or out of scope when the sources distinguish them.

---

# PART 1

## INTRODUCTION

Current/tested experience: I am not a software engineer. I worked with multiple LLMs on this, mainly Deepseek, Gemini, and Replit. I did this on a Pixel 6a using GitHub mobile, which was miserable, but it still produced useful results.

LeanType added a Java gesture engine from a HeliBoard pull request, which gave me a playground to test ideas. That let me try out the sandbox concept and validate that a narrow, user-centered gesture vocabulary can work well in practice.

From there I worked with Deepseek to design and build a more complicated dual-engine gesture typing system. It worked rather well for a while, but the long chat and the complexity eventually caused the Java project to break down while integrating settings.

I then removed the DTW engine from the original L2 Java version, converted it into Kotlin, and it performed much better. I reintegrated several of the Java features, asked Deepseek to optimize it, and then Replit helped further. That became the current modified DTW gesture engine in the [OpenSwipe.kt](https://github.com/cinnabar777/OpenSwipe/blob/main/OpenSwipe.kt) file.

LeanType integrated it for a time, but a major indexing bug later appeared. Even so, the performance was solid. The engine still needs optimization and some of the older Java-build ideas should be reintegrated to make it stronger, but this single engine can operate in different modes, so a second engine is not strictly necessary.

The basic sandbox idea comes from a simple reality: the top 1,000 words in English account for a huge portion of communication, so why compare every gesture against 150,000+ words all the time? The average person also only uses a few thousand words in regular typed communication. That means the engine should not act as if every session begins with a giant open-world vocabulary search.

This document is therefore about building a gesture typing engine that is practical first: fast enough, adjustable, layout-aware, and able to learn from the user without overcommitting to bad data.

## GENERAL ENGINE GOALS

The engine should do more than mathematically match a swipe trace to a dictionary entry. It should:

- support custom layouts without assuming a standard QWERTY geometry,
- let the user express intent through small micro-gestures,
- learn from corrections without immediately rewriting the user’s language model,
- keep private and temporary data separate from durable learned data,
- expose clear management controls for learned words,
- and make the behavior understandable enough for developers, QA, and technical writers to describe it accurately.

The sources consistently point toward a single principle: gesture typing is both recognition and learning. A good engine has to solve both.

---

# 1. USER WORDS, LEARNING, AND THE INTERNAL DATA MODEL

## 1.1 TERMINOLOGY: THE SAME IDEA HAS MULTIPLE NAMES

The source documents use several terms for the same general concept. Those terms are not fully interchangeable in product implementation, but they do refer to the same broad category of user-learned vocabulary:

- user history,
- personal dictionary,
- learned words,
- internal personal dictionary,
- internal learned words,
- user-added words,
- keyboard-learned words.

Recommended design: keep the terminology flexible in the UI if needed, but in the implementation treat these as related categories that may differ by source, trust level, or persistence rules. For example, a user-added word is not the same thing as a word inferred from a gesture correction.

The sources also distinguish between:

- built-in dictionary words,
- imported word lists,
- Android user dictionary words,
- and the keyboard’s own internal learned words.

Those should not be silently collapsed into one bucket.

## 1.2 CORE LEARNING PIPELINE

The intended flow is:

1. A word is typed or gestured.
2. The engine records the event in a short-term holding area.
3. If the word is confirmed, corrected, or explicitly accepted, it may graduate into durable learned storage.
4. The learned record then affects ranking, suggestion behavior, and future gesture matching.

Recommended design: do not immediately promote every visible word to permanent memory. Use a holding pen or similar staging area to avoid accidental training on typos, passwords, temporary codes, and other noise.

## 1.3 DATA SOURCES THAT SHOULD REMAIN DISTINCT

The learned-word system may draw from several sources:

- built-in dictionary words,
- imported word lists,
- Android user dictionary entries,
- words the engine observed in the user’s typing history,
- words the user explicitly accepted or added,
- gesture records that connect a path with a word.

Recommended design: these sources may all influence ranking, but they should retain provenance. A word imported from contacts should not behave as if the user repeatedly selected it from gestures.

## 1.4 THE HOLDING PEN: SHORT-TERM STORAGE BEFORE LONG-TERM LEARNING

Recommended design: recent typing events should first enter a temporary review area before becoming permanent learned data.

A holding-pen record can remember:

- the inserted word,
- whether the input was a gesture or tap sequence,
- the original gesture path if there was one,
- the tap sequence if there was one,
- the candidate words that were shown,
- whether the user selected another suggestion,
- whether the user deleted or edited the word,
- the keyboard layout and language in use,
- the time and session in which the event occurred.

This is useful for proofreading support, because the user can move back to a word later and re-open the original suggestions without re-entering the gesture.

Recommended design: hold this data only for a limited time or session. It should support recovery and correction, not become a permanent shadow transcript of everything typed.

## 1.5 WHAT SHOULD GRADUATE INTO DURABLE LEARNED STORAGE

A word is a candidate for long-term learning when it is:

- a real word from an allowed source,
- accepted or confirmed by the user,
- not immediately corrected,
- and not blacklisted or malformed.

Stronger evidence:

- explicit user addition,
- direct selection from the suggestion strip,
- repeated use over time.

Weaker evidence:

- something that merely remained on screen,
- something that survived a correction pass,
- something inferred from a single session.

Recommended design: graduation should be consistent and explainable. It should not silently over-train on one-off events.

## 1.6 WHAT SHOULD NOT GRADUATE AUTOMATICALLY

The engine should be cautious about automatically storing:

- random strings,
- accidental key sequences,
- temporary verification codes,
- password-like text,
- text from secure or private fields,
- words that were immediately replaced,
- words that were immediately deleted during correction,
- punctuation-only entries,
- URLs, email addresses, and file paths unless that is an explicit product goal,
- words created only because autocorrection changed the user’s original input.

Recommended design: keep secure/private-input policies strict by default.

## 1.7 LEARNING FROM CORRECTIONS AND CONFLICTING WORDS

A gesture can match more than one word. Common examples include pairs like `not` and `nor`, or `can` and `van`. The user may use both words, so the engine should not permanently transfer a gesture from one word to another after a single correction.

Recommended design:

- when the user changes an automatically selected word to another suggestion, the chosen word should gain evidence,
- the rejected word should lose some evidence for that association,
- the adjustment should be gradual rather than all-or-nothing.

This means the engine should track gesture-to-word relationships, not only word frequency.

Useful relationship data includes:

- how many times the path was accepted for that word,
- how many times it was rejected,
- how many times the user selected another word,
- how recently the relationship was used,
- whether the relationship came from a clear gesture or a correction,
- whether it belongs to a particular layout,
- whether it was learned from a real path or a generated variation.

This is more useful than a simple word count because it lets the engine learn the user’s actual gesture habits instead of assuming the population average is correct for every person.

## 1.8 MULTIPLE GESTURE EXAMPLES PER WORD

Recommended design: store more than one gesture example per word. Users do not draw the same word the same way every time.

Useful stored examples might represent:

- the user’s most common route,
- a slower or more careful route,
- a fast route with fewer visible turns,
- a route used on a different keyboard size,
- a route used on a different supported layout,
- a route confirmed after correcting a competing word.

The documents suggest roughly five to ten real and generated paths as a practical target, with fast fuzzy learning that can increase a path’s usefulness as it is seen more often.

Recommended design: keep generated variations separate from clearly observed paths so the engine can remember where the evidence came from.

## 1.9 SUGGESTED CONTENTS OF THE LEARNED DATASET

At minimum, the learned dataset should contain:

- the normalized word,
- display form and capitalization information,
- the word’s learned strength or weight,
- how many times it was accepted,
- how recently it was used,
- one or more gesture paths or gesture signatures,
- how often each gesture path was used,
- correction and rejection evidence,
- language and keyboard-layout association,
- whether the word was user-added, imported, tapped, or gesture-learned,
- whether the word is disabled or blacklisted,
- optional short phrase relationships,
- optional proofreading information retained for the current session.

Recommended design: treat this as a structured record, not a flat word list.

## 1.10 MANAGEMENT AND SETTINGS FOR LEARNED WORDS

A complete product should expose controls for learned vocabulary management.

Useful management functions include:

- search words,
- delete a word and all associated data,
- edit a word and its related data,
- reset rank or rank evidence,
- sort by age, weight, times used, or alphabetical order,
- sync words to the Android user dictionary,
- import/export words to and from the Android user dictionary,
- deal with duplicates and capitalization correctly.

Recommended design: if there is a slider handle on the right side for fast navigation, make it wide enough to use comfortably.

---

# 2. GESTURE ENGINE

## 2.1 ENGINE FAMILIES

There are three main types of gesture typing engines:

1. Neural
2. Modified DTW
3. L2

Current/tested and recommended for this document: the modified DTW approach.

The neural approach is the most complicated, requires substantial gesture data and training, and is beyond the scope of this guide. It can be excellent, and the sources point to FUTO’s open-source gesture work as a strong reference if a team wants to take that route.

The L2 approach has the lowest computational cost, but it is weaker for unique custom layouts. Because this guide is aimed at projects that allow custom layouts, modified DTW is the clear practical winner in the source material.

The detailed computational specification is beyond the author’s original expertise, but the working conclusion is clear: modified DTW is the best choice unless a team wants to take on the extra burden of a neural gesture engine.

## 2.2 USER INTENTS: MICRO-GESTURES THAT COMMUNICATE MEANING

Recommended design: the gesture engine should support small intentional motions that help the user tell the engine what they mean, instead of forcing the engine to guess everything.

These micro-gestures include:

### DWELL TIME

A brief pause over a letter, often around 150 ms or more, can indicate emphasis or a repeated letter.

### CIRCLE

A loop around a key can signal that the letter belongs in the word and may indicate a double letter. The important detail from the sources is that the letters outside the circle should be excluded rather than included, and the center of the circle should become the focus point.

Current/tested: this idea worked, but not especially well in the author’s gesture engine.

### WIGGLE

A short back-and-forth motion, usually a quick 180° move, and possibly a 90° variant, can emphasize a letter. The source suggests combining wiggle with dwell time.

Current/tested: this needs significant improvement in the author’s engine.

### SNAKE

A winding route through close keys can help on crowded layouts such as QWERTY.

Current/tested: this never really worked well for the author.

### UNDER OR OVER

Passing below or above a key can indicate a choice to include or avoid a letter. This is useful for separating words such as `write`, `wrote`, and `wire` that can otherwise look identical on a standard layout.

## 2.3 GESTURE GEOMETRY AND RECOGNITION TUNING

Recommended design: the engine should pay attention to geometry, not just letter order.

Important geometry concepts include:

- direction-change points or nodes,
- separate tolerances for the start, end, and middle of a gesture,
- fly-over letters,
- edge key behavior,
- path direction and path time.

### DIRECTION-CHANGE POINTS

Meaningful changes in direction can reveal the user’s intended keys. These nodes should carry more weight than a pure line-fit score would.

### TOLERANCES AND ERROR RADIUS

The beginning, middle, and end of a gesture do not mean the same thing.

Recommended design: expose separate controls for:

- start-key tolerance,
- end-key tolerance,
- middle-path tolerance.

### FLY-OVER LETTERS

A quick pass over a key is not always the same as selecting it. The engine should distinguish a real visit from a fly-over using direction and time.

### EDGE KEY OPTIMIZATION

When a gesture starts or ends near an edge, the error radius should be constrained to adjacent keys to reduce ambiguity.

## 2.4 USABILITY ENHANCEMENTS

These are the small features that make gesture typing feel fast instead of fussy.

### APOSTROPHE KEY

Recommended design: let the user define one key to act as the apostrophe anchor. The sources suggest the spacebar is often the best choice, though WMKeyboard also experimented with period, comma, apostrophe, and spacebar.

Current/tested conclusion from the sources: stick to one user-definable key rather than multiple gesture anchors, because multiple keys did not behave well in the engine.

### APPENDING `'s`

Allow a small punctuation action that appends `'s` to the last completed word and automatically removes any temporary space that may have appeared.

This matters because possessives are common, and the engine should reduce repetitive dictionary clutter once the pattern has been learned.

### GESTURE PUNCTUATION AND SYMBOLS

Allow the user to gesture from a defined key to the spacebar for punctuation. The sources also note that this could be extended to symbols, though that may create many settings.

Recommended design: keep the setting user-definable because layouts vary and hardcoding behavior may break on unusual keyboards.

### SUGGESTION STRIP CONTROLS

A long-press on a suggestion should open a menu with actions such as:

- Add Word
- Delete Word
- Increase Rank
- Decrease Rank
- Delete Word and Blacklist
- Remove gesture-string association

Recommended design: actions should affect only the intended data and should not accidentally destroy unrelated evidence.

### SPACE CLEANUP

The engine should handle taps, gestures, and punctuation without leaving awkward extra spaces.

## 2.5 INDEXING

The index should match the gesture engine’s search style.

Useful index dimensions include:

- first/last letter groups,
- direction-change patterns,
- word-length groups,
- language and layout,
- learned versus built-in source,
- frequency ranges,
- shortcut handling,
- non-alphabet characters where supported.

Recommended design: the index must defend against large dictionaries that could cause RAM problems or rebuild storms. It should detect stale indexes and rebuild them safely.

A good index should also carry a fingerprint of the dictionary, layout, and relevant settings so stale data can be rejected when anything important changes.

## 2.6 MODES: USER-FACING POLICIES FOR VOCABULARY BREADTH AND BEHAVIOR

Modes are not merely UI labels. They are policy choices about how broad the engine’s search should be and how the engine should behave at a given moment.

The source documents refer to several mode ideas:

- Normal
- Sandbox
- Sandbox only
- Automatic
- Manual
- Early prediction
- Learning

### NORMAL MODE

Searches everyday typing vocabulary, frequency-limited words, and internal learned words.

### SANDBOX MODE

A “look first” policy where the engine strictly prefers learned personal words and falls back to the broader dictionary only when confidence is weak.

### SANDBOX ONLY

A stricter sandbox that stays within the learned vocabulary unless the product explicitly allows a fallback.

### AUTOMATIC / MANUAL / LEARNING / EARLY PREDICTION

These are additional policy states discussed in the source material, but they are not fully specified in a way that should be treated as final behavior here.

Recommended design: the most important point is that mode controls should be understandable to the user and should map to clear search and learning policies.

## 2.7 LAYOUTS

The DTW engine is layout agnostic, but layout-specific optimizations can significantly improve clarity on custom layouts.

Examples from the sources include:

- [ClearFlow](https://clearflowkeyboard.github.io/)
- [KASROZ](https://futo.tech/blog/swipe-keyboard)
- [Vowel Vortex Clarity](https://github.com/cinnabar777/Vowel-Vortex-Keyboard-Layouts)

### VOWEL VORTEX CLARITY LAYOUT

The source describes this layout as:

- Row 1: `q w r t y p l`
- Row 2: `a , e u i . o`
- Row 3: `s d f g h j k`
- Row 4: `z x c v b n m`

Recommended layout behavior:

- treat punctuation keys as gaps or walls,
- require the gesture to fully enter `a` or `o` because they sit on edges and are separated by punctuation,
- keep `e` and `i` more central so their area is clearer,
- let `u` retain its full area,
- use frequent direction changes in the vowel row as node points.

The layout discussion matters because gesture recognition on custom keyboard geometries is not the same as gesture recognition on plain QWERTY.

---

# 3. PRIVACY, STORAGE, AND SAFETY

## 3.1 SEPARATE TEMPORARY AND DURABLE DATA

Recommended design: temporary cache data should not be confused with permanent learned storage.

The user should not lose learned words just because the app cache was cleared.

Durable storage should answer:

- where the data lives,
- whether it is included in backup,
- whether the user can export it manually,
- whether it can be imported on another install,
- whether it is versioned,
- how incomplete writes are detected,
- how corruption is recovered,
- how the user clears all learned data,
- how the user removes one word,
- how layout or language changes affect stored paths.

## 3.2 SAFE SAVE AND RECOVERY

Recommended design:

- write a complete temporary copy first,
- verify it,
- then replace the previous valid copy,
- save in the background so typing is not blocked,
- keep the previous valid data until the new copy has been successfully written.

The format should be versioned from the beginning so future additions such as capitalization, multiple paths, blacklists, phrase relationships, or context information can be added deliberately.

## 3.3 PRIVATE MODE

Current/tested test guidance indicates that private mode should prevent new counts or paths from being saved.

Recommended design: private mode should be a real storage boundary, not just a UI label.

---

# 4. TESTING THE COMPLETE USER EXPERIENCE

The engine should be tested as a learning product, not just as a mathematical matcher.

A good test plan includes:

- gesture a common word, close the keyboard, and verify that learning remains available later,
- correct a selected word from the strip and verify that ranking changes gradually,
- use conflicting words repeatedly, such as `not` / `nor`, and verify that both remain usable,
- enable private mode and verify that no new counts or paths are saved,
- test punctuation immediately after a gesture to verify that no unwanted space remains,
- clear the app cache and verify that durable learned data is not erased,
- add a new word explicitly and verify that capitalization is preserved correctly,
- remove a learned word and verify that its gesture data and relationships are removed when requested,
- interrupt a save and verify that the previous valid data still loads,
- use a large dictionary and verify that index building does not freeze or crash input,
- use a custom layout and verify that start, end, edge, and punctuation behavior matches the actual layout,
- long-press a suggestion and verify that each learning action only affects the intended data.

The most useful measurements are not only top-ranked accuracy. Also measure:

- how often the user immediately corrects a result,
- how often a short word loses to a longer word,
- how often a learned correction is remembered,
- how often the engine fails to recover without a second gesture.

---

# 5. WHAT IS CURRENT, RECOMMENDED, THEORETICAL, OR OUT OF SCOPE

## CURRENT / TESTED IN THE SOURCE MATERIAL

- The modified DTW engine is the current working direction described by the author.
- Sandbox-style narrowing of vocabulary was tested and found useful.
- Circle and wiggle micro-gestures were tried but need improvement.
- Snake gestures did not work well in the author’s build.
- Multiple gesture-anchor keys behaved poorly; one user-definable anchor worked better.

## RECOMMENDED DESIGN

- Use modified DTW as the main gesture engine for custom-layout keyboards.
- Keep learned words, imported words, and built-in words distinct.
- Use a holding pen before durable learning.
- Store multiple gesture examples per word.
- Give users clear management and sync controls.
- Keep private mode and durable storage boundaries strict.
- Use layout-aware tolerances and index fingerprints.

## THEORETICAL OR SUGGESTED BUT NOT FULLY PROVEN IN THE SOURCE MATERIAL

- Neural gesture engines can be excellent but require far more data and training.
- The “individualized small language model” idea is a useful framing, but the source treats it as a concept rather than a finalized implementation.
- Early prediction, automatic mode switching, and some advanced scoring details are discussed but not fully specified.

## OUT OF SCOPE FOR THIS DRAFT

- The dual-engine pipeline.
- A separate L2 engine running beside DTW.
- Early or live word prediction before a gesture is complete.
- Engine-specific parallel execution details.
- Exact binary field layouts.
- Exact numerical scoring formulas.
- Platform-specific method names and source-file integration steps.

# 6. SOURCE LINKS

- [OpenSwipe](https://github.com/cinnabar777/OpenSwipe)
- [OpenSwipe.kt](https://github.com/cinnabar777/OpenSwipe/blob/main/OpenSwipe.kt)
- [Java dual engine zip](https://github.com/cinnabar777/OpenSwipe/blob/main/Java%20dual%20engine.zip)
- [LeanType](https://github.com/LeanBitLab/LeanType)
- [HeliBoard](https://github.com/HeliBorg/HeliBoard)
- [WMKeyboard](https://github.com/wasi-master/wmkeyboard)
- [SonderKey](https://github.com/Verisonder/SonderKey)
- [ClearFlow keyboard](https://clearflowkeyboard.github.io/)
- [FUTO swipe blog post](https://futo.tech/blog/swipe-keyboard)
- [Vowel Vortex Keyboard Layouts](https://github.com/cinnabar777/Vowel-Vortex-Keyboard-Layouts)
- [HeliBoard pull request 2351](https://github.com/HeliBorg/HeliBoard/pull/2351)
- [LeanType indexing bug commit](https://github.com/LeanBitLab/LeanType/commit/81ccd24e4da11e37cc50635fba4bb887c7c04d7a)
