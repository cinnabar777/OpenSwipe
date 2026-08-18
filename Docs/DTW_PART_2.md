Produced by Replit based on compiling old documentation. 

# PART 2

>[!note] Added material from earlier project documents
>The sections below gather relevant ideas from the older Java engine, DTW notes, settings notes, and project summaries. They intentionally exclude the abandoned dual-engine architecture and the theoretical early-prediction work. Every new section and paragraph is highlighted with `…` so it can be reviewed, edited, moved, or removed easily.

## A. THE PERSONAL LEARNING SYSTEM IS PART OF THE GESTURE ENGINE

A gesture engine should not be treated as only a path-matching algorithm. The long-term personal learning system is one of the most important parts of the product. A modest matching algorithm with a well-designed personal word system can become much more useful over time, while a powerful matching algorithm with no useful learning can remain frustrating.

The personal learning system should be thought of as an internal record of what this particular user actually types, how they gesture it, how often they use it, which words they correct, and which words they choose instead. It is different from the main dictionary, different from a spell-checking dictionary, and different from a word list the user imported.

Different keyboard projects use different names for these sources. One project may call a source “personal dictionary,” another may call it “user history,” and another may call it “learned words.” The documentation should always explain whether it means:

1. A built-in dictionary supplied by the keyboard.
2. A large dictionary used mainly for spelling or rare-word lookup.
3. Words imported by the user.
4. Words supplied by the operating system’s user dictionary.
5. Words the engine observed in the user’s typing history.
6. Words the user explicitly accepted or added.
7. Gesture records that connect a path with a word.

These sources may be combined for ranking, but they should not be silently treated as the same kind of data. A word imported from contacts, for example, should not automatically be treated as though the user repeatedly accepted it from a gesture.

## B. THE HOLDING PEN: DELAY LONG-TERM LEARNING

Words should not necessarily enter permanent learned storage the instant they appear on screen. A safer design first places recent typing events into a temporary “holding pen.” The holding pen is a short-term review area that gives the user time to correct mistakes before the engine permanently changes its behavior.

A holding-pen record can remember:

- The word that was inserted.
- Whether it came from a gesture or individual taps.
- The original gesture path, when there was one.
- The tap-letter sequence, when the word was tapped.
- The candidate words that were shown, if the product wants to restore the original suggestions later.
- Whether the user selected a different suggestion.
- Whether the user deleted or edited the word.
- The language and keyboard layout in use.
- The time and session in which the event occurred.

The holding pen should normally be processed when the keyboard session closes, when the user explicitly confirms a word, or when another clearly defined graduation event occurs. The exact trigger is a product decision, but it must be consistent and explained.

The purpose of delayed graduation is not to make learning slow. It is to prevent a typo, an accidental suggestion, a temporary name, or a word used only once from permanently changing future recognition.

### B.1 WHAT SHOULD GRADUATE

A word can be considered for long-term learning when it is a real word from an allowed source, was accepted or confirmed by the user, and was not immediately corrected. Words that are blacklisted, rejected, malformed, or clearly part of a temporary input should not graduate.

A word that the user explicitly adds to their dictionary is stronger evidence than a word that merely remained on screen. A word that the user selects from the suggestion strip is stronger evidence than a word that was automatically inserted and never touched again.

### B.2 WHAT SHOULD NOT GRADUATE AUTOMATICALLY

The engine should be cautious about automatically storing:

- Random strings.
- Accidental key sequences.
- Temporary verification codes.
- Password-like text.
- Words typed in secure or private fields.
- Words that were immediately replaced.
- Words that were immediately deleted as part of correcting a sentence.
- Punctuation-only entries.
- Text that is known to be a URL, email address, or file path unless that is an explicit product goal.
- Words that are present only because an automatic correction changed the user’s original input.

### B.3 PROOFREADING SUPPORT

A particularly useful gesture-typing feature is allowing the user to correct a word later using the original gesture information. The user should not always have to repeat a difficult gesture after moving the cursor back to a word. If the original gesture or tap sequence is still available in the short-term cache, the engine can use it to show the suggestions that belonged to that original input.

This is different from generating new suggestions from the final output word. A proofreading system should preserve the original input evidence, because the final word may be exactly the word that was wrong.

A practical policy is to keep this information only for a limited time or session, set a memory limit, and remove it when it is no longer useful. The user should not be surprised that the keyboard retains a large permanent record of every sentence they have typed.

## C. LEARNING FROM CORRECTIONS AND CONFLICTING WORDS

A gesture can mathematically resemble more than one word. Common conflicts include words such as `not` and `nor`, or `can` and `van`. The user may use both words, so the engine must not permanently transfer a gesture from one word to the other after a single correction.

When the user changes an automatically selected word to another suggestion, that action is valuable feedback: the chosen word should gain evidence for that gesture, and the rejected word should lose some evidence for that particular association. The adjustment should be gradual rather than all-or-nothing.

A mature learning record should therefore track the relationship between a gesture and a word, not only the word itself. It should be possible for one gesture pattern to be associated with more than one word, with each association having its own strength. This allows a user to use both `not` and `nor` without the engine endlessly replacing one with the other.

Useful information for a gesture-to-word relationship includes:

- How many times the path was accepted for that word.
- How many times the path was rejected for that word.
- How many times the user selected another word instead.
- How recently the relationship was used.
- Whether the relationship came from a clear gesture or a correction.
- Whether the relationship belongs to a particular keyboard layout.
- Whether the relationship was learned from a real path or a generated variation.

This is more useful than a simple “word count” because it lets the engine learn that the user’s version of a path may differ from the population’s average path.

## D. MULTIPLE GESTURE EXAMPLES PER WORD

One learned path per word is a useful starting point, but users do not draw the same word exactly the same way every time. A good long-term design can retain several representative gesture examples for a word instead of replacing the old path every time.

The stored examples can represent:

- The user’s most common route.
- A slower or more careful route.
- A fast route with fewer visible turns.
- A route used on a different keyboard size.
- A route used on a different supported layout.
- A route that was confirmed after correcting a competing word.

The number of examples must be bounded. Keeping approximately five to ten representative paths per word is a design option, not a fixed requirement. Longer words and frequently confused words may justify more examples, while rarely used words may need only one. The storage limit and eviction policy should be explicit.

### D.1 GENERATED VARIATIONS

An optional fast-learning idea is to create small variations of a confirmed path, such as a slightly shifted version to the left, right, above, or below. This can help the engine recognize the same user’s gesture sooner, but it should be treated as an approximation rather than a real user gesture.

Generated variations should be replaced or outweighed by real paths over time. They should never be allowed to multiply without a limit, and they should not be used to hide poor matching quality. This idea requires careful testing before it is enabled by default.

## E. WORD FREQUENCY AND THE SIZE OF THE ACTIVE DICTIONARY

The number of words competing for a gesture directly affects clarity. Tap typing narrows the possible word list one letter at a time, but gesture typing may compare many complete word routes at once. A very large dictionary can therefore add noise and create more words with similar paths.

A useful design separates the everyday typing vocabulary from the full spelling vocabulary:

- The everyday typing vocabulary contains common, useful words that should compete during ordinary gesture recognition.
- The full spelling vocabulary contains rare, technical, regional, or unusual words that remain available for spelling support or an explicit search.

This does not require deleting rare words. It means that rare words do not need to compete with common words during every ordinary gesture. A frequency limit can be used to control which words are active for regular recognition, while a manual or explicit lookup can reach the larger vocabulary when needed.

Frequency values should be treated as helpful evidence, not absolute truth. Public word-frequency lists can overrepresent news, organizations, formal writing, and unusual names. A dictionary should be reviewed for real words, non-words, acronyms, and words that are inappropriate for ordinary gesture suggestions.

The user should be able to choose how broad the active vocabulary is, or the engine can adjust it based on measured behavior. Any automatic adjustment should be gradual and reversible.

## F. WORD CACHE VERSUS LONG-TERM LEARNED STORAGE

The short-term word cache and the long-term learned dataset serve different purposes:

- The short-term cache remembers recent input and the original gesture evidence for proofreading.
- The long-term dataset remembers durable user preferences, accepted words, gesture paths, and word relationships.

The cache should have a clear expiration rule. It may be cleared when the keyboard closes, after a time limit, when its size limit is reached, or after the user completes proofreading. The long-term dataset should be stored separately so that clearing temporary cache data does not erase learned words.

A developer should document whether the cache remembers all suggestions originally shown or only the original path and input sequence. Remembering every suggestion can make proofreading more faithful but uses more memory. Re-running a search later uses less storage but may produce different results after the dictionary or user history has changed.

## G. CONTEXT AND WORD RELATIONSHIPS

A gesture engine can use the words immediately before the current word as supporting evidence, but context should not be allowed to erase a strong gesture match. Context is best treated as a booster or tie-breaker.

The internal personal word system can store relationships such as “after these two words, this word is often used.” This is commonly called a trigram relationship, but the plain-language idea is simply a record of short phrases the user repeatedly writes.

Context information can include:

- The previous word.
- The word before the previous word.
- The current candidate.
- How many times that phrase pattern was used.
- How recently it was used.
- The language and writing context.

Context should be stored with the same privacy caution as word and gesture data. It can reveal more about a user than an isolated word. It should be bounded, locally controlled, and excluded from secure input.

## H. GESTURE HINTS: SMALL MOTIONS THAT EXPRESS INTENT

Some users need a way to emphasize a letter without relying entirely on the engine’s guess. These small intentional motions can be treated as optional hints. They should improve recognition when used, but ordinary gesture typing should remain possible without them.

### H.1 DWELL

A dwell is a brief pause over a key. It may mean that the user wants that key to be included, is emphasizing a repeated letter, or is thinking about the spelling. A dwell should be measured relative to the user’s normal movement and should not require an exact universal number of milliseconds.

Dwell evidence can help with words where a repeated letter is not obvious from the route, such as the difference between `god` and `good`. It should be supporting evidence, not an automatic command to insert a repeated letter every time the finger slows down.

### H.2 CIRCLE

A small circle or loop around a key can express emphasis, especially for a repeated letter. The engine should focus on the center key and avoid treating every key touched around the outside of the circle as part of the word. This behavior is difficult to perfect and should be optional.

### H.3 WIGGLE

A short back-and-forth motion can be used as an intentional hint. A quick turn of approximately half a reversal, or possibly a smaller reversal, may communicate emphasis or a repeated letter. Wiggle detection should consider dwell and overall path direction so that ordinary hand tremor is not treated as a command.

### H.4 SNAKE

A winding route between very close keys can be used to distinguish letters that are difficult to separate on a crowded layout. This is especially relevant on QWERTY and other layouts where several words share nearly the same broad path. Snake behavior should be considered experimental until it is tested with real users.

### H.5 UNDER OR OVER

A route that intentionally passes below or above a key can communicate that the key should be avoided or included. This may help distinguish words with nearly identical routes, such as `write`, `wrote`, and `wire`. The engine should document which direction means what, and should not require the user to learn a complicated secret gesture language.

## I. GESTURE GEOMETRY IMPROVEMENTS

The engine can use more than the total shape of the path. Meaningful changes in direction, the beginning of the gesture, the end of the gesture, and the keys along the edges of the keyboard can all improve word clarity.

### I.1 DIRECTION-CHANGE POINTS

A meaningful change in direction can indicate that the finger intended a particular key. These points can be used to compare the observed gesture with the important turns in a word’s expected route. The engine should ignore tiny changes caused by touch noise and should not require every letter to create a dramatic turn.

The developer should decide how many meaningful direction changes must agree before a candidate is considered strong. This can be a tuning option, but exposing too many low-level controls can overwhelm users.

### I.2 SEPARATE START, END, AND PATHWAY TOLERANCE

The beginning, middle, and end of a gesture do not have identical meaning. The first key and final key are often especially valuable because they constrain the possible word. The middle path needs enough tolerance for natural movement but should not accept every nearby key.

A useful settings design treats these as separate concepts:

- Start-key tolerance.
- End-key tolerance.
- Middle-path tolerance.
- Direction-change tolerance.
- Optional emphasis tolerance for intentional hints.

These values should be based on the active keyboard geometry so that a large custom key does not behave like a tiny phone key.

### I.3 FLY-OVER LETTERS

A fly-over letter is a key that the finger passes near or across without intending to select it. This is a common source of wrong words. The engine should distinguish a meaningful visit from a quick pass, using path direction, time near the key, surrounding route, candidate spelling, and the importance of the key to the word.

A word should not win merely because it contains every letter that the path happened to cross. The path must support the order and role of those letters.

### I.4 EDGE KEYS

When a gesture starts or ends near the edge of the keyboard, the possible neighboring keys are naturally limited. The engine can use that fact to reduce ambiguity instead of applying the same large circle of tolerance in every direction.

Edge behavior is particularly useful on layouts with several rows, unusual key shapes, or punctuation keys separating letter keys.

## J. PUNCTUATION, APOSTROPHES, AND SYMBOLS

Gesture typing should include a clear plan for punctuation. A user should not have to leave the gesture workflow for every apostrophe or possessive ending.

### J.1 USER-DEFINED PUNCTUATION KEY

A keyboard may allow one user-selected key to act as a punctuation gesture anchor. Possible choices include the spacebar, period, comma, or apostrophe key. The earlier project testing suggested choosing one configurable key is more reliable than allowing several competing punctuation anchors.

The chosen key can be used as a starting or ending point for regular punctuation gestures. The behavior should be configurable because custom keyboard layouts place punctuation in different locations.

### J.2 APPENDING POSSESSIVE ENDINGS

A small punctuation action can append `'s` to the last completed word. The engine should remove any temporary space before the word, add the apostrophe and `s`, and restore the required space afterward. This can reduce the number of possessive forms that need to be stored as separate dictionary entries.

The same general idea can support other common endings if the keyboard has a clear, discoverable design, but each additional shortcut increases the chance of accidental activation.

### J.3 SPACE AND PUNCTUATION CLEANUP

Gesture typing often inserts a space after a completed word. Punctuation usually belongs directly beside the word, so the engine needs a consistent rule for removing or preventing an unwanted space before punctuation. It should also handle a tapped word placed between two gestured words without creating awkward spacing.

Spacing behavior should be tested for:

- A gestured word followed by a period.
- A gestured word followed by a comma.
- A gestured word followed by an apostrophe action.
- A tapped word between two gestured words.
- Punctuation entered quickly after a gesture.
- Undo and backspace after punctuation cleanup.

### J.4 SYMBOLS

The same configurable gesture idea can eventually support symbols, but symbols should not be added without a clear user interface. A long list of hidden symbol gestures can be difficult to learn and difficult to debug.

## K. SUGGESTION-STRIP CONTROLS

A suggestion strip is not only a display area. It can provide direct ways for the user to teach the engine. A long press on a suggestion can offer actions such as:

- Add the word.
- Delete the word from personal learning.
- Increase its rank or preference.
- Decrease its rank or preference.
- Delete the word and blacklist it.
- Remove the current gesture association from the word while keeping the word available for normal typing.

These actions should be clearly distinguished. “Delete word” should not unexpectedly delete a legitimate word from the main dictionary, and “remove gesture association” should not remove the word from ordinary suggestions.

The suggestion strip should also make correction meaningful. When the user selects a different candidate, that choice should be available to the learning system as a correction event.

## L. DICTIONARY SYNCHRONIZATION AND WORD SOURCES

The engine should decide how it relates to the operating system’s user dictionary and the keyboard’s own user history. Possible policies include:

- Read existing user-dictionary words when the engine starts.
- Watch for newly learned tap-typed words.
- Import selected words rather than importing everything automatically.
- Allow the user to push selected learned words to the operating system dictionary.
- Keep gesture-learned data internal unless the user explicitly asks for synchronization.
- Handle duplicate words with different capitalization.
- Detect when a word was removed from an external dictionary.
- Keep blacklisted words excluded even if another source contains them.

Words explicitly confirmed by the user may optionally be written to the operating system dictionary in the background. This should be an opt-in behavior because external dictionary storage may be shared with other keyboard features.

The engine should not automatically write every gestured word to the operating system’s user history. Internal gesture learning and operating-system dictionary learning serve different purposes and should remain separable.

## M. CAPITALIZATION AND WORD FORM

Learned words should preserve useful capitalization information. Storing every word only in lowercase loses names and intentional capitalization, while storing the auto-capitalized form as though it were the user’s preferred spelling creates a different problem.

The system should distinguish:

- Normal lowercase spelling.
- Sentence-start capitalization.
- A user intentionally entering an uppercase word.
- A proper name or acronym.
- A word capitalized only because the keyboard was at the beginning of a sentence.

When a word is offered for permanent learning, the engine should decide whether its capitalization is part of the word or merely a temporary text-position effect.

## N. INTERNAL PERSONAL DICTIONARY MANAGEMENT

If the engine exposes a screen for managing learned words, it should provide more than a simple list. Useful controls include:

- Search for a word.
- View the word’s usage strength.
- View how often it was accepted.
- View when it was last used.
- View how many gesture examples it has.
- Delete the word and all associated gesture data.
- Edit the word and preserve or remove its gesture associations.
- Remove only the gesture associations.
- Reset the word’s learned rank.
- Sort by oldest or newest.
- Sort by most or least used.
- Sort by highest or lowest learned weight.
- Sort alphabetically.
- Import words.
- Export words.
- Synchronize selected words with the operating system dictionary.

A scroll handle or another fast navigation method may be useful when the learned list becomes large. Any editing screen should make it clear whether an action affects only the personal dataset or also the system dictionary.

## O. LEARNING MODE

A separate learning mode can help build a personal vocabulary from text the user has already written. The user could select or open a document, activate learning mode, and let the keyboard examine words one at a time. Words already known would be skipped. Unknown words would be offered for the user to add, ignore, or correct.

This can quickly build a personal vocabulary, but it must be designed with strong privacy controls. It should never silently read arbitrary documents, should require an explicit user action, and should make it clear what text will be examined and what will be saved.

Learning mode should apply the same rules as ordinary learning: capitalization must be considered, blacklisted words must remain excluded, secure content must not be read, and unknown words should not be added without a clear user decision.

## P. PRIVACY AND INCOGNITO BEHAVIOR

The engine should have a private or incognito mode that prevents new learning while continuing to use already stored data if the user wants suggestions.

When private mode is active:

- New words are not added to the holding pen.
- Existing learned words may still be used for recognition if the user permits it.
- New gesture paths are not saved.
- Usage counts and context relationships are not increased.
- Graduation and background learning scans are skipped.
- Temporary information is cleared according to the private-session policy.

The user interface should explain whether private mode blocks only learning or also prevents the engine from using previously learned words. Different users may want those two behaviors separately.

## Q. NORMAL RECOGNITION AND MANUAL RECOVERY

The ordinary recognition path should be conservative enough to produce a useful result quickly. When the result is uncertain, the user should have a deliberate way to search more broadly without repeating the gesture.

A manual recovery action can use the cached original gesture or tap sequence to search the larger available vocabulary. This is especially useful when the normal active vocabulary is intentionally focused on common and learned words.

The recovery action should:

- Be available from the toolbar or another discoverable control.
- Use the original input evidence whenever it is still cached.
- Search words that were excluded from ordinary recognition because of frequency or personal-mode settings.
- Show the user that a broader search is taking place.
- Offer the selected result as a candidate for future learning.
- Avoid silently changing a word without the user choosing the result.

## R. MODES AS USER-FACING POLICIES, NOT SEPARATE ENGINES

The older project documents described several modes. These can be understood as user-facing policies for how broad the word search should be, without requiring multiple independent gesture engines.

Possible policies include:

- Broad vocabulary: use the normal active dictionary and the internal learned words.
- Personal vocabulary first: prefer learned words, but allow the broader dictionary when confidence is weak.
- Personal vocabulary only: use only words the user has built or explicitly added.
- Manual broad search: temporarily search everything available for a word being corrected.
- Automatic adaptation: gradually move toward personal vocabulary as the user’s learned dataset becomes mature.

These policies should be presented as choices about word sources and confidence, not as confusing technical engine names. A user who chooses a personal-only policy must still have a clear recovery path for a word they have not learned yet.

Automatic adaptation should be gradual, measurable, and reversible. The user should be notified before the keyboard changes to a more restrictive policy, or the setting should remain entirely manual.

## S. INDEXING AND CACHE SAFETY

The word index should be designed around how the gesture engine actually searches. Sorting words alphabetically or by frequency alone is not enough if the matcher also relies on starting letters, ending letters, direction-change points, word length, or layout geometry.

Useful index groupings may include:

- First-letter groups.
- Last-letter groups.
- Word-length groups.
- Direction-change or node patterns.
- Language and layout.
- Learned versus built-in source.
- Frequency ranges.

The index must also defend against very large dictionaries. It should cap groups, avoid duplicating full word paths unnecessarily, detect repeated rebuilds, and release old temporary structures after a new index is published. A dictionary with hundreds of thousands of entries should not be allowed to cause an input-time memory crash.

The index should have a fingerprint that identifies the dictionary, keyboard layout, and relevant settings used to build it. If any of those inputs change, the old index should be considered stale and rebuilt safely.

## T. STORAGE, BACKUP, AND RECOVERY

Learned gesture data should be stored in application data storage, not a temporary cache directory that the operating system may clear. The user should not lose their learned words simply because they cleared the app cache.

A durable storage plan should answer:

- Where the file or database lives.
- Whether it is included in the keyboard’s normal backup system.
- Whether the user can export it manually.
- Whether the user can import it on another installation.
- Whether the file is versioned.
- How incomplete writes are detected.
- How a corrupt file is recovered.
- How the user clears all learned data.
- How the user removes only one word.
- How layout and language changes affect stored paths.

A safe save should write a complete temporary copy first, verify it, and then replace the previous valid copy. Saving should happen in the background so it cannot make typing lag. The previous valid data should be preserved until the new copy has been successfully written.

The storage format should be versioned from the beginning. Future versions may add capitalization, multiple paths, word relationships, blacklists, or context information. Older data should be migrated deliberately or safely ignored; it should never be interpreted under a new format by accident.

## U. SUGGESTED CONTENTS OF THE LEARNED DATASET

The internal learned dataset should contain, at minimum:

- The normalized word.
- The display form and capitalization information.
- The word’s learned strength or weight.
- How many times it was accepted.
- How recently it was used.
- One or more gesture paths or gesture signatures.
- How often each gesture path was used for that word.
- Correction and rejection evidence.
- Language and keyboard-layout association.
- Whether the word was user-added, imported, tapped, or gesture-learned.
- Whether the word is disabled or blacklisted.
- Optional short phrase relationships.
- Optional proofreading information retained for the current session.

This structure is more useful than a simple list of words because the engine needs to know not only that the user uses a word, but how the user reaches it and how confidently the engine should choose it over a competing word.

## V. SETTINGS CATEGORIES FOR A COMPLETE PRODUCT

The earlier settings work suggests that developers should plan settings in categories rather than exposing unexplained numbers. Useful categories include:

### RECOGNITION BEHAVIOR

- Enable or disable gesture typing.
- Choose the active word vocabulary.
- Set how conservative recognition should be.
- Set whether uncommon words compete during normal typing.
- Choose whether a manual broad search is available.

### LEARNING BEHAVIOR

- Enable or disable learning.
- Learn tapped words that are already known.

Learn tapped words that are new and explicitly confirmed.

- Choose how many gesture examples to keep per word.
- Choose how quickly a word gains personal strength.
- Choose whether generated path variations are allowed.
- Sync selected words with the operating system dictionary.

### GESTURE-HINT BEHAVIOR

- Dwell sensitivity.
- Wiggle sensitivity.
- Circle or loop sensitivity.
- Whether hints are enabled at all.

### LAYOUT AND TOLERANCE BEHAVIOR

- Start-key tolerance.
- End-key tolerance.
- Middle-path tolerance.
- Direction-change sensitivity.
- Treatment of edge keys.
- Treatment of fly-over letters.

These settings should have plain-language explanations and safe defaults. Advanced controls can be hidden behind an expert section rather than presented to every user.

## W. TESTING THE COMPLETE USER EXPERIENCE

The engine should be tested as a learning product, not only as a mathematical matcher. A useful test plan includes:

- Gesture a common word, close the keyboard, and verify that its learned information remains available later.
- Correct a selected word from the suggestion strip and verify that future ranking changes gradually.
- Use two conflicting words repeatedly and verify that both remain usable.
- Add a new word explicitly and verify that capitalization is preserved correctly.
- Remove a learned word and verify that its gesture data and relationships are also removed when requested.
- Enable private mode and verify that no new counts or paths are saved.
- Add a word to the operating system dictionary and verify the chosen synchronization behavior.
- Clear the app cache and verify that durable learned data is not erased.
- Interrupt a save and verify that the previous valid data can still load.
- Use a large dictionary and verify that index building does not crash or freeze input.
- Use a custom layout and verify that start, end, edge, and punctuation behavior follows the actual layout.
- Test punctuation immediately after a gesture and verify that no unwanted space is left behind.
- Long-press a suggestion and verify that each learning action affects only the intended data.

The most important user-facing measurements are not only top-ranked accuracy. Also measure how often the user immediately corrects a result, how often a short word loses to a longer word, how often a learned correction is remembered, and how often the engine fails to recover without a second gesture.

## X. WHAT SHOULD REMAIN OUT OF SCOPE FOR THIS DRAFT

The following topics were deliberately not brought forward from the older documents because they are either defunct, theoretical, or too technical for this first documentation pass:

- The dual-engine pipeline.
- A separate L2 engine working beside a DTW engine.
- Early or live word prediction before the gesture is complete.
- Engine-specific parallel execution details.
- Exact binary field layouts.
- Exact numerical scoring formulas.
- Platform-specific method names and source-file integration steps.
- Build instructions and settings-code migration history.

Those topics can be revisited later if the single-engine design requires them, but they should not be treated as established behavior in this non-technical guide.
