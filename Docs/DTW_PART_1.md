THIS IS A WORK IN PROGRESS AND HAS NOT BEEN PROOFED! 

___

Source document: [OpenSwipe](https://github.com/cinnabar777/OpenSwipe)

Target Projects:

- [WMKeyboard](https://github.com/wasi-master/wmkeyboard) (primary focus due to it hanging a built-in gesture engine.)
- AOSP keyboards:
    - [LeanType](https://github.com/LeanBitLab/LeanType) 
    - [HeliBoard](https://github.com/HeliBorg/HeliBoard) 
    - [SonderKey](https://github.com/Verisonder/SonderKey)

Document structure:

1. Part 1: my general information and structure for the gesture engine
2. Part 2: Replit's attempt at consolidating old documentation. 

___

Note: I use several terms and phrases throughout that are all intended to mean the same thing, they concern the words the keyboard learns from the user's typing. The reason I use different terms is because developers do. WMKeyboard uses "personal dictionary" in the settings for the user history words. AOSP keyboard forks use "user history" and automatically import words from the android user dictionary whereas WMKeyboard utilizes those words like the built-in dictionary and allows the user to also import word lists but none of them are automatically added to the internal "personal dictionary". Because of this I attempt to be redundant and make the effort that I'm talking about the learned words based on the user typing and are stored internally within the keyboard but are not the main dictionary words or an imported word list.

Note: part 1 I am writing on a phone so forgive the typos etc. Part 2 was Replit combining info from older documents but looks like it took info from part 1 as well. I had Gemini and Replit combine parts 1 and 2 however they seemed to summarize more than combine, but they are significantly shorter: [Gemini consolidation](https://github.com/cinnabar777/OpenSwipe/blob/main/Docs/DTW_Gemini_consolidation.md) and [Replit consolidation](https://github.com/cinnabar777/OpenSwipe/blob/main/Docs/DTW_Replit_consolidation.md).


___
___

# PART 1


## INTRODUCTION

I am NOT a software engineer, I worked with multiple LLM on this, mainly Deepseek, Gemini, and Replit. I did this on a pixel 6a, using GitHub mobile which is horrible, it was painful!!! 

[LeanType](https://github.com/LeanBitLab/LeanType) added a Java gesture engine from this [pull request](https://github.com/HeliBorg/HeliBoard/pull/2351)  which gave me, a non-developer, a playground to test some ideas. 

I was able to test out my sandbox idea, and it worked well. Then I set out with Deepseek to design and build out a complicated dual engine gesture typing engine. It actually worked rather well but the Deepseek chat broke down, as LLMs do when the chat is very long and complex, which caused that Java project to break down while integrating options into the settings. 

I removed the DTW engine from the original L2 Java, converted it into kotlin, and it performed much better. I reintegrated several of the features from the Java build, gave Deepseek a shot at optimizing it then Replit, which did really well. This is the current "modified DTW" gesture engine in the [OpenSwipe.kt](https://github.com/cinnabar777/OpenSwipe/blob/main/OpenSwipe.kt) file. LeanType integrated it for a while but had a major [bug with the indexing](https://github.com/LeanBitLab/LeanType/commit/81ccd24e4da11e37cc50635fba4bb887c7c04d7a). The performance is solid though it still needs optimizing and many of the ideas in the [Java build](https://github.com/cinnabar777/OpenSwipe/blob/main/Java%20dual%20engine.zip) reintegrated to really be a great gesture engine, however, this one engine can work in different modes therefore no second engine is needed. 

The basic sandbox idea comes from the simple reality that the top 1,000 words in English account for 89% of communication, therefore why is every gesture I do compared to 150,000+ words? Secondly, the average person only uses a few thousand words in their written/typing communication, again, why is every gesture compared to 150,000+ words? 

The sandbox hypothesis tested out in the [Java build](https://github.com/cinnabar777/OpenSwipe/blob/main/Java%20dual%20engine.zip) proved how dramatic word clarity can be increased with a little innovation, force the engine to focus on learned words. 

Many of the ideas can be pulled from the Java build. It has about 50-70% of the ideas in it.

This document is an attempt to make a blueprint that a developer could use with AI to quickly build out a gesture engine. 

### TAP VS SWIPE TYPING 

There are key differences between how the user interacts with the keyboard and therefore the software engineering required. 

A user tap typing is in full control of the typing and therefore the output, thus, every means of prediction, spell checking, and language model can be used immediately without concern, the user is in constant control. 

When swipe typing the user is totally dependent on the gesture engine and the prediction to get the word right. Anyone who gestures over 20 wpm, which is easy, is going faster than the preview is being displayed, and watching the preview distracts from focus on the keyboard. They do not stop to look at the suggestions, that slows down typing, and therefore are not tapping the spacebar looking for the next word suggestion because, by the time they do all of that they could have already gestured the next word and probably more. 

Swype told users to forget about the errors and suggestions and just gesture as fast as you can, then during proofreading Swype would offer up the original gesture with the original suggestions for error correction, something no other keyboard has done. 🤔

Also notice how keyboards require that you type a word a certain number of times before it becomes a learned word, where Swype you tapped it once and it asked if you wanted to add the word to your dictionary. This shows that one keyboard was designed for swipe typing and the others are tap typing keyboards that added gesture typing. This causes fundamental issues with gesture typing which are unrecognizable to tap typers. I hope I can make these differences obvious and demonstrate that aging these necessities for gesture typing will enhance tap typing as well. 

### DICTIONARY SIZE  MATTERS

For those thumb typers the size of the dictionary is irrelevant, they are narrowing down the dictionary one tap at a time, 10,000 or 300,000 words in the dictionary mean little difference. The prediction engine quickly narrows down the words and the keyboard learns the words they use regularly thus suggesting them a few taps into the word. 

For swipe typing the dictionary size is one of the most important things that directly affect word clarity, more words in the dictionary means lower word clarity and more word noise competing for every gesture. This ought to be obvious yet very large dictionaries are being used for gesture typing. Swype keyboard used a reasonably small dictionary, about 50,000 words. Most English keyboards are shipping with dictionaries that are over 150,000 words, and sometimes double that. 

Why is this so important? The gesture engine has to compare a gesture against the words in the dictionary therefor, the more words the more likely there will be multiple gesture matches. 

Why then are they doing it? Mostly because they are tap typing and are unaware of the effect on gesture typing and haven't thought of a way to have both, the spell checking benefit of a large dictionary with the word clarity for gesture typing that a small dictionary offers. 

Is there a way to have both? Yes! And it isn't difficult. A simple setting for gesture typing to limit the words used in a dictionary by frequency would do it with a properly structured dictionary

But then the gesture typing user might not get that one unique word, they'll have to tap it in. Yes, but that is actually rare. The myth that people use 100,000+ words is just that, a total myth. Most people use only a few thousand words for writing. The top 1000 words in English account for 89% of all writing. So all those other words are for that 11%! The New York Times in its entire history has used ~30,000 different words. No, you don't need 150,000+ words. This is all easily searchable. 

### Swipe first keyboard 

The keyboards are designed for tap typing first then gesture typing is added, however implementing a swipe first design will actually improve tap typing whereas the opposite is true when tap typing is the primary design. There are fundamental software engineering that is needed for good swipe typing that is not needed for tap typing however, tap typing can benefit from that extra software engineering. That's the goal of this document, swipe first design that also benefits tap typing. 

___

## DATA

### TYPING DICTIONARY

As discussed above a large dictionary is not good for gesture typing, and it really isn't that helpful for tap typing. But you want to have those unique rarely used words available. The dimple solution is two dictionaries. By that is redundant. No. One dictionary is small focused on the top ~50,000 or so actual dictionary words. The second does not contain those same words, it can be whatever size desired and is only utilized in a "manual search" for gesture typing and tap types can enable or disable the extra, but it always remains for spell checking. 🤔

Why burden the gesture engine, the prediction engine, and the user with having to deal with tens of thousands of words they will never use, all the time, when they can access those words only when they need that really unique word not found in a more sane typing sized dictionary? 

This split dictionary model will show dramatic improvements for both typing styles, immediately. I'll discuss accessing the larger portion in the modes section. 

#### WORD FREQUENCY

Word frequencies were derived by scraping the internet, they have little direct relation to the average person typing on a phone because they are heavily influenced by news publications, corporations, government documents, etc. Five minutes with a few good scrips and a developer can rearrange a dictionary moving all those nouns and acronyms that most people will rarely, if ever, use to a lower frequency and out of the typing dictionary. There are many online resources that anyone can use to build a dictionary based on real dictionary words, no scraped together garbage with all kinds of non-words. Don't make your user have to blacklist thousands of words because that's what gesture typing users have to do to get good results, or if lucky they can replace the inbuilt dictionary with their own custom dictionary. 

### LEARNED WORDS

Clarify the different named used by projects and where words are pulled in from, android user dictionary, built in dictionary, imported word list, etc. 

Without a well designed internal "learned words dataset" a great gesture engine will perform poorly, with a great "database" a weak gesture engine will seem exceptional. This is the brain for the gesture engine, and it needs to be far more complex than a tap typing bigram dataset. 

This is why Swype keyboard is still the best gesture typing keyboard years after it was discontinued. It mapped the gesture string to the word, and cached the words plus their gesture for proofreading to ensure proper learning. **These two elements are key and cannot be overstated how important they are.**  

#### WORD CACHE

Before words enter the internal long term word storage certain steps need to take place to ensure that the keyboard's learned user words are kept free from typos, non-words, punctuation, and other stuff that does not belong in the internally learned "user's dictionary". 

All typed words must be cached until the user closes the keyboard — this must be explained to the user do they know how to ensure their keyboard learns optimally. The words typed, gesture or tap, are retained in cache with their gesture string or tap letters, if they are not an exact match. My original design retained the word with the gesture string plus all the suggested words for that gesture. This may not be necessary. There are a couple ways to work this. Understand the goal is to return useful suggestions for proofreading. Most keyboards show you suggestions for the word when you go back to edit, again this is a tap first design, but what we want are the actual suggested words that were originally given with the gesture, or tap sequence, not suggestions related to the output word. 

The options for this are: 1. Cache the word with the string and all the returned suggested words, though this could be problematic when many words are cached. 2. Trigger a suggestion output, essentially a manual gesture, based on the cached string data when the user puts the cursor on a word that is still in cache — this certainly is not something you want happening all the time, only during a proofreading. I have not tested this second approach. And I think Swype used the first approach. 

The point of this is to give the user time to correct mistakes before they are committed to long-term storage. With that in mind the developer needs to ensure that only real words are being added to long-term storage. For gesture typing only words that exist in one of the dictionaries or wordlist can be output with a gesture therefore there ought not be a concern there, only with m when a word is tap typed. When the user taps a word out that cannot be matched to a word in a used dictionary then certain measures need to take place to ensure that the word is correctly entered if so desired by the user. 

First the new word should be checked against all available words, if it is unique then it needs to be offered to "add to dictionary" for the user, though capitalization needs to be ensured that it is correct — ensure that it is not capitalized due to author-capitalization. 

Along these same lines, ensure that learned words reflect proper capitalization. Entering all words as non-capitalized is just as big of a mistake as entering auto-capitalized words. 

##### AI AUTOCORRECT

Some developers have added AI correction tools. This requires a unique setup — how do you know the gesture string is for that word? AI might have changed it. This requires special attention from the developer to ensure that the gesture string for the word is correct and what the user intended. 

#### INTERNAL USE PERSONAL DICTIONARY

Once the user has proofread their typing when they close the keyboard all cached words plus their gesture string, or tapped sequence of letters, are committed to the internal data. This is a "sandbox" dictionary of what the user actually types over time. This is the brain of the gesture engine, the more complete and through it is the better the gesture typing, and tap typing, will be

##### STRUCTURE

Often this is a simple bigram structure which offers little benefit, and rarely does it store the gesture data. 

The minimum that ought to be employed is a trigram + unigram word relation structure, with table that stores about 10 gesture strings, and a few tap letter sequences. Deepseek talked me out of using an N-gram word rational dataset, but I still feel that is the way to go — using a highly individualized internally developed language model, IMO, would serve many benefits over the long run. 

This internal "personal dictionary" must contain word relations, gesture data, and word weight, at a minimum. 

Should blacklist words be in the same database? That is something I'm not certain about, but they need to be considered in the overall design to ensure they are respected. 

Some developers are using language models with their keyboards to improve typing, however, like the dictionaries the LM are not specific to the individual. A well designed "personal dictionary" can be used in the same ways, and be much more specific to the individual. Nonetheless, if you ate using a LM be aware that applying it the same as tap typing might cause problems. If the LM is allowed to override the gesture that can lead to output the user did not intend, therefore, it is best used as a booster during gesture, and if confidence is stop low after output flag the word until further context is added then re-evaluate that flagged word. This is something I've only seen Swype keyboard do — you could see it change a word after you've typed several words beyond it. This is unique to gesture typing because the user is not using the preview and suggestions the same way a tap typer is, however, this can be used for tap typing just as well, especially those who tap very quickly. 

#### FAST FUZZY LOGIC LEARNING

Mapping the gesture string to the word in the internally learned words wordlist is how the gesture engine learns. To increase this learning I applied the ideas that one gesture should equal 3 gestures — the real gesture then two synthetic gestures as if the user gestured the word a little to the left or right and a little high or lower. This adds three gesture strings to the learned word which resulted in very fast learning. Over time the synthetic gestures are replaced by real gestures. In my design only 5 gestures were stored per word, though I think that is low especially for long words, maybe 10, but be careful this does not cause any memory issues. 

May need to add a number for the gesture string for how many times it is used for a word to be compared to another word that may be in conflict. 

The issue is dealing with words that are potential conflicts that the user may use one and not the other or may use one frequently and the other rarely. Examples of this are not vs nor and can vs van. 

I don't use the word van, therefore mapping that gesture to the word can is beneficial in my case. But what if I suddenly start using the word van? Does the learning pull the gesture string from can and give it to van immediately and penalize the word can? 

In the normal learning proceed when the user corrects a word via the suggestion strip the gesture string should be she added to that chosen word and the incorrect word should have that gesture string removed and reduced in weight. This is done because the gesture engine is mathematical. If you are off on your gesture to the point that mathematically it matches another word then you get that word but we want it to learn that the gesture was actually ment for another word. Without this mapping your engine will be cold math and frustrating to use over time. However we have to consider these cases where conflicts arise and the user wants the word rarely used. 

If we attach a number to the gesture string that indicates the user's actual usage of that gesture for that word we add additional information that can be used to inform the gesture engine whether or not to pull the gesture string from a word or add the same gesture string to a second word. 

I use "not" more than "nor" but I due use them both and there No doubt will be times that my sloppy gesture typing will get me the wrong one of the two and I will have to coerce it via the suggestion strip. If both words are "learned words" in the internal personal dictionary then the gesture engine learning needs to act properly when I make such mistakes and not just pull the gesture string from one word and then next time do the reverse. 

This adds a level of complex learning that I did not have in my gesture engines and therefore did not test. I had the blunt all or nothing learning, and to be fair it was far better than no learning at all and really only required the user to be careful on known word conflicts. 

The catch here is, if this additional number is added to the data it increased the stored data and requires another check by the engine during proofreading, though it mat be well worth it. 

### LEARNING MODE 

A learning mode should be added to quickly build the user's internal learned word, personal dictionary. This cab be activate by a toolbar key or magic gesture, p → spacebar, and user configurable in settings. 

The user places the cursor in any document they've written, activated this mode, the cursor is moved to the beginning of the document and "reads" the words in the document word by word, adding any words that are not already in the internal learned personal dictionary with the same strict rules for adding words in general. When a word is reached that is unknown via any dictionary available for matching a pop-up should be presented to the user to add the word, ignore the word, or change the word. 

This offers a quick way for the "individualized small language model" to be built for the user based on their previous writings. 



### Import words into learned words

Each keyboard handles words from the android user dictionary differently and therefore there needs to be some consideration to importing and exporting words to and from the keyboard's user learned words, personal dictionary, to and from the android user dictionary. 








CleverKeys hierarchy for for capitalization. I cannot fix the information, need link. 



## DATA STRUCTURE OUTLINE


1. **Dictionary**
    1. Typing dictionary 
        1. ~50,000 words
        2. Minimize capitalized words
        3. Stick to real dictionary words
    2. Spell checking dictionary 
        1. Big as you want
        2. Watch for memory and indexing issues when very large
        3. Can block per frequency
2. **Word cache**
    1. Keep words from long term storage until keyboard closes
    2. Add unique words via user authorization
        1. Ensure proper capitalization
    3. Protect against non-words entering long term storage
    4. Retain gesture data
        1. Retain suggested words? 
3. **Learned words (internal user history "personal dictionary")**
    1. Minimum trigram+unigram structure
        1. Word relations
        2. Individualized small language model? 
    2. Store gesture string, ~10
        1. Fast fuzzy logic learning 3x→5x
        2. Penalize wrong word
        3. Boost correct word
        4. Add number to gesture string for how many times that string is used for that word
    3. Store word weight 
    4. learning mode 
    5. Import
    6. Settings: 
        1. Search words
        2. Delete word and all associated data 
        3. Edit word and all its related data 
        4. Sort: 
            1. Age: 
                1. Oldest →newest
                2. Newest →oldest
            2. Word weight: 
                1. Highest →lowest
                2. Lowest →highest
            3. Times used: 
                1. Most →least
                2. Least →most
            4. Alphabetical: 
                1. A→Z
                2. Z→A
            5. Slider handle on right side, wide enough to grab and quickly navigate through the words
            6. Sync words to android user dictionary 
                1. Push to
                2. Pull from
                    1. Dealing with duplicates and capitalization. 
            7. Import/export words
                1. To/from android user dictionary
        5. 

___

## GESTURE ENGINE

There are three main types of gesture typing engines: 

1. Neural
2. Modified DTW
3. L2

The neutral gesture engine is by far the most complicate, requires lots of gesture data and training, and is far beyond this document. It can be a great gesture engine, FUTO keyboard developed a very good open source neutral gesture engine, and anyone that wants to go that route, FUTO swipe is my recommendation. 

The modified DTW is a noticeable steep up from the L2, however there are certain computational requirements that need to be addressed — far beyond my understanding but that is where a good LLM does the heavy lifting. 

The L2 is has the lowest computational cost but is not as good as the DTW for unique custom layouts, and since this is geared toward projects that allow for custom layouts the DTW is the clear winner. 

Therefore this document is about creating the best "modified DTW" gesture engine possible, though the ideas can be utilized for any of these types. 

### OpenSwipe.kt DTW FEATURES

- **Atomic path storage** – Replaced torn-read-prone `Long` fields with a single `AtomicReference<LongArray>` for thread-safe, atomic updates of packed gesture paths.  
- **LB_Keogh pruning** – Added a lower-bound pre‑filter using the input envelope; candidates with LB > `bestNorm * avgLen * 1.15` are skipped early.  
- **Safe DTW with early abandon** – Uses a shared, pre‑filled `FloatArray` matrix (to avoid GC) and aborts a candidate if its partial DTW cost exceeds `bestNorm * avgLen * 1.05`.
- **Band‑limited DTW** – Enforces Sakoe‑Chiba band (`DTW_BAND = 3`) to limit the warping path and speed up computation.  
- **Unmapped‑key guard** – Skips letters with `[0,0]` positions in `wordPath()` and `isSequenceMatch()` to avoid injecting false origin waypoints into the reference path.  
- **Bucket cap & user‑data trimming** – Limits per‑first‑letter index entries to `8 000` and trims user‑boost/path maps to `2 000` / `1 000` entries at save time to prevent OOM and unbounded growth.

The detailed specifications of a gesture engine are beyond me, I simply tested them and through multiple LLM chats and many builds I think the modified DTW is the best choice unless the developer wants to do the heavy lifting of a neural gesture engine. 

However, what I am going to discuss are the additions to the gesture engine that help to make it truly functional. 

### USER INTENTS

These are micro gestures that the gesture engine can pickup to help it know the coerce word, another area Swype is still king, and allows the user to dictate the output instead of the software deciding, minimizing the user saying #@!? autocorrect! 

Dwell time is the most commonly used, a pause over a letter for milliseconds, if over ~150 milliseconds the user has most likely paused because they forgot the spelling. 

The circle over a letter to emphasize that it belongs to the word and is probably a double letter is necessary for words like good vs god, but not required, none of these should be required. The user can always just let the software guess. The key to this intent is ensuring that letters on the outside of the gestured circle are excluded not included which is what an L2 and DTW will normally do, and that the center becomes a cross hair to focus on the letter that is wanted. This one worked but not great in my gesture engine. 

The wiggle is typically a quick micro 180° gesture, however including a micro 90° might be helpful. This gesture should be combined with dwell time. This one needs significant improvement in my gesture engine. 

The snake gesture is winding through very close letters, like trying to get QWERTY to properly gesture type on the QWERTY layout. This one never really worked for me.

The under or over method is used to avoid letters, especially useful for differentiating words like write, wrote, and wire which are all identical gestures on QWERTY. 

### USABILITY ENHANCEMENTS

These are the little things that make gesture typing quick. 

The **apostrophe** key: the gesture engine should be able to recognize a user selected key as the apostrophe key. FUTO keyboard added the ability to use the apostrophe key, if there is on in the layout, which works well but is not user definable. WMKeyboard made it user definable between the period, comma, apostrophe, and spacebar. I prefer the spacebar, works perfect. I tested using multiple keys and the engine did not like it therefore **stick to one user definable key**. 

In the same idea, allow the user to define the period, comma, or apostrophe key to be used to spend 's to the last word gestured, ensure any space is removed that is present after the word that is to get the 's appended to it. This little trick allows the dictionary to be reduced of all those possessives — the user only needs to do this once for a word they use and it ought to be added to the internal words and therefore fully gestureable from then on. 

Allow the user to define keys that they can gesture from for regular punctuation. Best option is from a key to the spacebar, because there are unique layouts hard coding this probably is not a good idea and best to be a user definable setting. 

This could also be used for symbols, however that could be a lot of settings. 

A long press on any word in the suggestion strip ought to being up a menu with the following items: 

- Add Word 
- Delete Word 
- Increase Rank 
- Decrease Rank 
- Delete Word And Blacklist
- Remove gesture string from word

#### Dealing with spaces when tap and gesture typing combined and punctuation. 

Because gesture typing requires spaces to be added for, good experience this can cause problems with punctuation, often resulting in a space between the punctuation and the word. It can also cause problems when a word is tapped between two words that are gestured. Therefore this needs to be addressed to ensure proper flow during gesture typing. 

### GESTURE OPTIMIZATIONS

Letters where a change in direction take place are node points that can be leveraged to optimize for word clarity. This can be a user definable setting on how many node points must match. This is also something to consider for the indexing. 

The "error radius", I know that's wrong terminology, for the start, end, and pathway can be adjusted and should be a user definable setting. This allows for the developer to fine tune the default values without new builds and those users that st using custom layouts that have significantly larger keys can utilize these settings as well. 

Fly over letters is an issue for L2 and DTW gesture engines. These are letters that the user quickly flies over, maybe just on the edge even, and the gesture engine wants to include these letters in the word. This issue can be reduced via word weight but may need direct attention as well. 

Edge key optimization is telling the gesture engine when the start or end of the gesture is on a key that is on the edges that the error radius should only include the letters adjacent thus narrowing down the potential letters to three at most. 

### INDEXING

The indexing needs to match the gesture engine not just index by frequency or alphabetical. Depending on the optimizations the index may need to be different. 

More importantly the developer needs to ensure that corruption doesn't take place, indexing storms, or out of memory issues due to excessively large dictionaries — why are there 400,000+ word dictionaries 🤷🏻‍♂️

### MODES

This is my unique idea to side step the large dictionaries always having an influence on my gesture typing. Simply put, over time the user internal learned words grows and fewer and fewer words are added as time goes on, therefore, as the user's personal dictionary becomes mature the gesture engine slowly enters a sandbox mode where it focused on those words and only looks at the larger dictionary if confidence falls below a certain level. This confidence level should increase over time and as the user adds fewer words to their internal words, and at some algorithm point or manually triggered by the user, they enter a sandbox only mode. This is when the gesture engine ONLY looks at the integrally learned words and ignores everything else. Of course there will be times when you want a word that hasn't been added to the internal words yet therefore, there needs to be a toolbar key that triggers a manual search that utilizes the cached gesture to search everything. That's the basic idea. Now I'll break down each mode. 

#### NORMAL MODE

The gesture engine searched all words available using its normal functioning. The only limit should be on the default dictionary words via a frequency limiter in the user settings — generally word frequency is 0-255 bit some keyboards may use different numbers. I think WMKeyboard used 0-1000.  

#### MANUAL MODE

This is an unlimited gesture search against every possible word, the learned internal personal dictionary, the android user dictionary, the built-in dictionary, etc. No frequency limiting. This mode is manually triggered either by a toolbar key or a "magic gesture" — gesture from q to the center of the space bare, changeable in the user settings. This mode uses the cached gesture string for the search. This mode only worked when the user placed the cursor in or next to a word that has already been typed and is in the word cache holding. This is for proofreading when the output is incorrect and non of the suggestions match what the user wanted. This mode is especially necessary when using the sandbox modes or limiting the search via word frequency. 

#### SANDBOX MODE

This is a "look first mode" where the gesture engine is directed to search only the learned words, not the main dictionary. If the gesture engine confidence drops below a creation point then it can leave the sandbox and search via normal mode. 

#### SANDBOX ONLY MODE

This is the most restrictive mode, the engine only searches the internal user learned words, No dictionary words at all, ever. This is for the user that has built up a substantial internal personal dictionary based on their typing. Word clarity is exceptional in this mode, however this comes with a catch. Because the engine only uses the learned words there will be times that a word you meant to gesture is not in those words and therefore will be incorrect. This is another reason for the words and their gesture to be cached. When this happens the user, when proofreading, simply placed the cursor on the woo word and activates the manual search via a toolbar key or a "magic gesture" —the magic gesture sold be q to the center of the spacebar but can be modified in the user settings. When the user uses this manual search this essentially gives then a quick spell checker that searched everything abs gives then suggestions based on the original gesture, they do not need to re-gesture. One a word is selected it is added to the learned words with that gesture string. 

#### AUTOMATIC MODE

This should be the default mode. This mode is an algorithm that moves the user from normal mode to sandbox then to sandbox only, based on their typing and how many new words are being added compared to how many words they use. These settings ought to be user configurable in the settings. However, if we revisit the default discussion on word usage we can see that the top 1,000 English words account for ~90% of typing, therefore, the algorithm can be based off this assumption. Furthermore it would be wise to seed the internal user words with thu the top words for a better out of the box experience. 

A notification asking the user if they want to move to the next level, when the algorithm triggers it, would be wise. This would allow the user that hasn't bothered learning about the modes to decide if they want to go that route or not. 

#### EARLY PREDICTION MODE (theoretical)

This is a theoretical most which I was unable to get working with Deepseek or Replit. This mode should bot be attempted until after all the desired features are working perfectly and the developer as a great gesture engine. 

What is it? Gboard can give users a preview of a word after only a few letters that is much longer. The word dictionary is a good example. Because I typed that word a lot gboard would offer it as the preview when I gesture d→i→c i.e. already have "dictionary" and if I lift my finger I might get the word. This is where gboard falls down. Sometimes you get the early preview and sometimes it gives you another word. Nonetheless this is the idea, to get longer words early like tap typing gives you. 

I've gotten conflicting information as to what types of gesture engines can actually do early prediction. I've seen the original L2 suggest words like "jujitsu" after j→u, but there is the problem, that's not a word I used. 

From my attempts that failed I think the key to this working is a strict index of the learned, sandbox, words in alphabetical order. The second part is a fundamental change in the way gesture engines work. 

Gesture engines work in a dual prediction mode, predicting from the first letter to the last and from the last to the first. The engine assumes that every letter is the last and predicts based on that. Gboard used to do this as does the google library used by HeliBoard for gesture typing, however, a few years ago gboard changed to a strictly linear suggestion/prediction engine — the engine predicts from the first letter and builds with each new letter in the gesture, exactly as if you were tap typing. The negative to this is, the user must gesture the coerce spelling of the word, there is no spelling forgiveness like there is with a gesture engine that is predicting in both directions. 

For that reason and others, I think this mode should only be used with one of the sandbox modes as a sub mode. This requires a much smaller alphabetical index for it therefore reducing the likelihood of memory issues. Furthermore this allows for the more generous dual prediction algorithm to be used for the larger word set, which increases search speed because it can narrow the word from both ends. 


Reference link to HeliBoard discussion: https://github.com/HeliBorg/HeliBoard/issues/668 where it was mentioned that there is a research paper on how to achieve this early prediction.  

## GESTURE ENGINE OUTLINE

1. User Intents: 
    1. Dwell time
    2. Circle
    3. Wiggle
    4. Snake
    5. Under or over
2. Gesture optimizations 
    1. Node points
    2. Edge keys
    3. Error radius
        1. Start
        2. End
        3. Pathway
    4. Fly over letters 
    5. I know I'm missing a lot here 
3. Modes: 
    1. Normal
    2. Sandbox
    3. Sandbox only
    4. Automatic
    5. Manual
    6. Early prediction
    7. Learning
4. Indexing: 
    1. Match to gesture engine
    2. Large dictionaries → ram issues
    3. Fingerprinting: index storm
    4. Shortcuts
    5. Non-alphabet characters
5. Usability enhancements: 
    1. Apostrophe
    2. Append 's 
    3. Gesture punctuation
    4. Gesture symbols
    5. Suggestion strip
        1. Long press 
            1. Add word 
            2. Delete word 
            3. Increase rank 
            4. Decrease rank 
            5. Delete word and blacklist
    6. 


### LAYOUTS

The DTW engine is layout agnostic but that does not mean that certain optimizations cannot be applied to increase word clarity for unique layouts like [ClearFlow](https://clearflowkeyboard.github.io/), [KASROZ](https://futo.tech/blog/swipe-keyboard), and my [Vowel Vortex Clarity](https://github.com/cinnabar777/Vowel-Vortex-Keyboard-Layouts). 

#### Vowel Vortex Clarity 

Row 1: q w r t y p l
Row 2: a  , e u i  . o
Row 3: s d f g h j k
Row 4: z x c v b n m

The bottom row is all functional keys: ctrl, symbols, shift, spacebar, enter, delete. 

All consonant keys are sized the same, 0.143. A E O are also that size. The comma and period keys are) 0.11. And E and I are larger at 0.175. While this layout increases word clarity over QWERTY by increasing node points it does introduce some vowel ambiguity at times. Therefore it might be possible for the engine to know when this layout is being used and apply some specific optimizations to reduce those issues and increase word clarity even further. The "edge key" if applied in general would be very beneficial to this layout, ClearFlow, and KASROZ because of the increased number of rows. 

The Vowel Vortex Clarity layout though can use optimization for the vowel row. Because a and o are on edges and separated but punctuation keys the engine ought to ensure that those punctuation keys act as gaps or walls and require that the gesture fully enter the a or o key to trigger it, and if it does from an angel at speed then changes it is a clear node point. The punctuation keys act as separators between a and e, and i and o and must be respected as separator and punctuation keys not as extensions of the neighboring vowel. This leaves us with a larger e and I key than the u key. Therefore the u key should be given its full area at all times because e and I are easier to hit because of their increased size thus the gesture ought to be required to be more central on the e and I to trigger the them, not just grazing by them. Finally an overall optimization for this layout is the fact that the vowel row increases the need for change in direction to go to it and away from it, and this should be leveraged as node points and clarification points of emphasis. 



___

Gesture typing vs gestures

Dual finger gesture typing




___
___
___
The following documentation was produced by Replit and I'll be adding and editing throughout it. This is intended to be a compressive guide for a developer that wants to build a gesture engine but does not gesture type and does not want to spend hundreds of hours studying the subject. The goal of this document is to be comprehensive enough that it can be given to an LLM of choice with some basic starting code, like the [OpenSwipe.kt](https://github.com/cinnabar777/OpenSwipe/blob/main/OpenSwipe.kt),  [Java build](https://github.com/cinnabar777/OpenSwipe/blob/main/Java%20dual%20engine.zip), or [WMKeyboard gesture engine](https://github.com/wasi-master/wmkeyboard/tree/ded45624ac272d8142d223e85d3148cc7e7a87a0/core/prediction/src/main/java/com/wasimaster/wmkeyboard/core/gesture), that a developer can quickly build on with LLM assistance. 

___
___
___

# PART 2