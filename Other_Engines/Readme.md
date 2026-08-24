Open source engines that are good examples for developers to use with LLM. 

[Dodona](https://github.com/sangaline/dodona)

> Dodona is a framework for constructing keyboards and evaluating their performance with user interaction models. It makes it easy to define a model for inputting text on a keyboard and then do things like optimize the layout of the keyboard. It can be used to perform analyses like the one that led to the Dvorak keyboard but it's also very applicable to modern mobile devices.


[LeantypeDual](https://github.com/AsafMah/LeanType/tree/feat/two-thumb-experimental/app/src/main/java/helium314/keyboard/latin/gesture) uses a L2 java gesture engine built off the original one leantype added. 


[Slide keyboard](https://github.com/Robertg761/Slide/tree/main/engine/src/main/kotlin/com/slide/engine/gesture) uses FUTO swipe with a secondary L2 gesture engine. 


[kinetica](https://github.com/EZ-eta/kinetica/tree/main/app/src/test/java/com/kinetica/keyboard/engine)

- dual gesture
- DTW + geometric scoring
- L2 Euclidean distance
- Trie DFS + DTW ranking
- Arc-length resampling



[OpenSwift](https://github.com/SysAdminDoc/OpenSwift)


[SwiftFloris](https://github.com/SysAdminDoc/SwiftFloris)


[RimBoard](https://github.com/An0nym010/rimboard/tree/main/app/src/main/java/com/rimboard/keyboard/engine)

[CleverKeys](https://github.com/tribixbite/CleverKeys) uses an open source neural engine. 

[Neve](https://github.com/winters27/Neve/tree/master/app/src/main/java/app/winters/keys/input/swipe) 

> This is a neural network-based sequence-to-sequence (seq2seq) gesture engine using transformer-style encoder-decoder architecture with dictionary-constrained beam search.

- ONNX-based encoder-decoder: Uses two pre-trained ONNX models (swipe_encoder.onnx and swipe_decoder.onnx)
- Transformer attention mechanism: The SwipeModel loads and runs encoder/decoder sessions from ONNX Runtime
- Attention-based memory passing: The encoder produces a memory tensor that the decoder steps through one token at a time

[Open source neural gesture engine](https://github.com/proshian/neural-swipe-keyboard-android) currently only supports russian. 


[HeliBoard_and_heatmap](https://github.com/Ozaku/HeliBoard_and_heatmap)

[libswipetype](https://github.com/libswipetype/libswipetype) :

> An open-source, keyboard-agnostic glide (swipe) typing engine.

> libswipetype provides accurate gesture-based word recognition for soft keyboards on Android. The core algorithm is written in portable C++17 with no external dependencies. An Android JNI wrapper and a reference HeliBoard adapter are included.


[VoxBoard](https://github.com/Predator04/VoxBoard/tree/14963c74bf55396b309830e63c8a4ee2e47391cb/app/src/main/kotlin/com/voxboard/ime/text/gestures)



