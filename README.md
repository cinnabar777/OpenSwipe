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

[LeanType](https://github.com/LeanBitLab/LeanType) added a Java gesture engine from this [pull request](https://github.com/HeliBorg/HeliBoard/pull/2351) which gave me, a non-developer, a playground to test some ideas.

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

### DICTIONARY SIZE MATTERS

For those thumb typers the size of the dictionary is irrelevant, they are narrowing down the dictionary one tap at a time, 10,000 or 300,000 words in the dictionary mean little difference. The prediction engine quickly narrows down the words and the keyboard learns the words they use regularly thus suggesting them a few taps into the word.

For swipe typing the dictionary size is one of the most important things that directly affect word clarity, more words in the dictionary means lower word clarity and more word noise competing for every gesture. This ought to be obvious yet very large dictionaries are being used for gesture typing. Swype keyboard used a reasonably small dictionary, about 50,000 words. Most English keyboards are shipping with dictionaries that are over 150,000 words, and sometimes double that.

Why is this so important? The gesture engine has to compare a gesture against the words in the dictionary therefor, the more words the more likely there will be multiple gesture matches.

Why then are they doing it? Mostly because they are tap typing and are unaware of the effect on gesture typing and haven't thought of a way to have both, the spell checking benefit of a large dictionary with the word clarity for gesture typing that a small dictionary offers.

Is there a way to have both? Yes! And it isn't difficult. A simple setting for gesture typing to limit the words used in a dictionary by frequency would do it with a properly structured dictionary

But then the gesture typing user might not get that one unique word, they'll have to tap it in. Yes, but that is actually rare. The myth that people use 100,000+ words is just that, a total myth. Most people use only a few thousand words for writing. The top 1000 words in English account for 89% of all writing. So all those other words are for that 11%! The New York Times in its entire history has used ~30,000 different words. No, you don't need 150,000+ words. This is all easily searchable.

### SWIPE FIRST KEYBOARD

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

### IMPORT WORDS INTO LEARNED WORDS

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

The detailed specifications of a gesture engine are beyond me, I simply tested them and through multiple LLM chats and many builds I think the modified DTW is the best choice unless the developer wants to do the heavy lifting of a neural gesture engine.

However what I am going to discuss are the additions to the gesture engine that help to make it truly functional.

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

#### DEALING WITH SPACES WHEN TAP AND GESTURE TYPING COMBINED AND PUNCTUATION

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

#### EARLY PREDICTION MODE (THEORETICAL)

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

### LAYOUTS

The DTW engine is layout agnostic but that does not mean that certain optimizations cannot be applied to increase word clarity for unique layouts like [ClearFlow](https://clearflowkeyboard.github.io/), [KASROZ](https://futo.tech/blog/swipe-keyboard), and my [Vowel Vortex Clarity](https://github.com/cinnabar777/Vowel-Vortex-Keyboard-Layouts).

#### VOWEL VORTEX CLARITY

Row 1: q w r t y p l

Row 2: a , e u i . o

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

The following documentation was produced by Replit and I'll be adding and editing throughout it. This is intended to be a compressive guide for a developer that wants to build a gesture engine but does not gesture type and does not want to spend hundreds of hours studying the subject. The goal of this document is to be comprehensive enough that it can be given to an LLM of choice with some basic starting code, like the [OpenSwipe.kt](https://github.com/cinnabar777/OpenSwipe/blob/main/OpenSwipe.kt), [Java build](https://github.com/cinnabar777/OpenSwipe/blob/main/Java%20dual%20engine.zip), or [WMKeyboard gesture engine](https://github.com/wasi-master/wmkeyboard/tree/ded45624ac272d8142d223e85d3148cc7e7a87a0/core/prediction/src/main/java/com/wasimaster/wmkeyboard/core/gesture), that a developer can quickly build on with LLM assistance.

___

___

___

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
