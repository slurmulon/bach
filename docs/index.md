# bach

> :musical_score: Semantic music notation

`bach` is a semantic music notation designed to be both human and computer friendly.

Although its primary domain is music, `bach` enables the synchronization of rhythmic timelines with just about anything.

The project is pre-alpha and is not should **not** be considered stable for production use.

## Getting Started

If you are a musician and would like to use `bach` to notate music then head to the [Guide](/guide) page.

If you are a programmer and would like to use `bach` in your own application or library then check out [Syntax](/syntax) and [Development](/development) pages.

If you would like to learn more about the rationale for `bach` and the problems it attempts to solve then go to the [Background](/background) page.

## Features

- Supports semantic music constructs such as chords and scales
- Seamlessly synchronizes with audio or other data by minimizing the complexities around timing
- Allows for alternative real-time representations of music (e.g. visual instead of just audio)
- Easy to read and write for both musicians and computers
- Easy to translate from sheet music
- Small learning curve
- Highly productive

## Design

`bach` tracks are ultimately interpreted by a higher-level `bach` engine, such as [`gig`](https://github.com/slurmulon/gig).

This module, by itself, can only parse and compile plaintext `bach` data into [`bach.json`](https://github.com/slurmulon/bach-json-schema).

`bach.json` is a JSON micro-format that makes it trivial for `bach` engines to sequentially process a `bach` music track and synchronize it in real-time with audio.

This library can be used in Clojure, [ClojureScript and NodeJS](/dev#javascript).

## License

MIT

