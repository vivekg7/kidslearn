# Kids Learn

An ad-free Android app for early learning, for children aged about 2–7 and made with
Indian families in mind: the English alphabet, Hindi varnamala, numbers, tables, colours
and shapes, with every card spoken aloud.

## Why it exists

Kids' learning apps on the Play Store are full of ads, and ads are worst in front of a
toddler: they are tapped by accident, they lead out of the app, and many track the child.
Kids Learn has no ads, no tracking, no analytics, no account, no links out, and **no
internet permission at all**. It is free and open source (MIT).

## Features

- **English ABC**: A for Apple … Z for Zebra, capital and small letter, with a picture.
- **Hindi स्वर and व्यंजन**: all 13 swar and 36 vyanjan (including क्ष, त्र, ज्ञ), each with
  a word and picture, read as "क से केला".
- **Numbers 1–100**: digit, Devanagari digit, English and Hindi words, and a picture of
  the number as tens (🔟) and ones (⭐) to count.
- **Tables 1–20**: tap a line to hear "7 times 3 is 21", or play the whole table.
- **Colours and shapes**, named in English and Hindi.
- **Quiz**: "Tap the letter B", "क को छुओ", "Which one is red?", "Which one is the
  triangle?". A right answer earns a star and a cheer; a wrong answer just fades that
  card and says "Try again!". No timer, no lives, no score that goes down.
- Big cards: swipe left/right, or use the large previous / say-again / next buttons. Tap
  a card to hear it. Works in portrait and landscape, light and dark.
- **Parent gate**: settings (speech speed, auto-speak, voice settings) open only after
  pressing and holding a button for 3 seconds.
- Material You colours for the chrome, fixed bright colours per section.

## Permissions

None. The manifest declares no permissions, so the app cannot reach the network, read
files or use the camera. It declares one `<queries>` entry for the
`android.intent.action.TTS_SERVICE` intent; that is not a permission, it is what lets
Android 11+ bind to the phone's text-to-speech engine.

## Speech

Speech uses Android's built-in `TextToSpeech` with Indian English (`en-IN`, falling back
to `en-GB`/`en-US`) and Hindi (`hi-IN`). No audio files are bundled.

If the Hindi voice is not installed (or not downloaded for offline use), Hindi sections
show a friendly note with an **Install voice** button. That button leaves the app (it
opens the engine's voice-data screen, falling back to system TTS settings), so it sits
behind the parent gate too. Hindi audio is skipped rather than read by an English voice,
and Hindi questions are left out of the quiz until the voice is there, since a pre-reader
cannot answer a prompt they cannot hear.

## Build

This Mac has no system JDK; Android Studio's bundled JBR works:

```sh
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew assembleDebug testDebugUnitTest
./gradlew assembleRelease   # unsigned unless keystore.properties exists
```

Release signing is optional: create `keystore.properties` (git-ignored) at the repo root
with `storeFile`, `storePassword`, `keyAlias` and `keyPassword`. Without it the release
APK is built unsigned.

Requires Android 12 (API 31) or newer.

## Design decisions

- **Content lives in data files**, `app/src/main/assets/content/*.tsv` (letters, words,
  pictures, colours, shapes, Hindi number words), not in UI code. Fixing a word is a
  one-line edit, and the unit tests load the same files the app ships.
- **Pictures are emoji from the system font, or shapes drawn in code.** No image is
  downloaded or copied, so there is nothing with unclear licensing, and the APK stays
  tiny. Every emoji is from Emoji 13.1 or earlier so Android 12 can render it. Words were
  chosen so the emoji shows exactly that thing: अ से अनानास (🍍) instead of अनार, because
  there is no pomegranate emoji; ए से एड़ी (🦶). ष uses a drawn hexagon for षट्कोण.
- **Letters no word starts with** (ङ, ञ, ण, अः, क्ष) are marked `like` in the data and
  read "ण, जैसे हिरण" rather than pretending "ण से …" exists. The tests check that every
  other word really starts with its letter, and that `like` words contain it.
- **Hindi number words are a list, English ones are generated.** Hindi numbers below 100
  are irregular (उनतीस, उनचास, नवासी), so they are data; English follows rules, so it is
  code. Both are tested for 1–100.
- **Quiz numbers stop at 20**: past that it becomes a reading test, not counting. Every
  quiz option is distinct, the answer never repeats as a distractor, and the same answer
  is never asked twice in a row. These are proven over thousands of seeded questions.
- **Speech is slow (0.75×) by default**, which small children follow more easily; a
  parent can switch it to normal.
- **Hold-for-3-seconds parent gate**: toddlers tap, they rarely hold still that long, and
  an adult reads the instruction. It is a speed bump, not security.
- **Plain Kotlin core** (`content/`): parsing, number words, tables and quiz generation
  have no Android dependencies and are covered by JVM unit tests.
- Not built (optional in the brief): locking the app to one section.

## Tests

```sh
./gradlew testDebugUnitTest
```

Content integrity (every letter has a word and picture, complete swar/vyanjan, colours
and shapes), number words 1–100 in English and Hindi, place-value pictures, tables, and
quiz generation.

## License

MIT, see [LICENSE](LICENSE). Privacy: see [PRIVACY.md](PRIVACY.md).
