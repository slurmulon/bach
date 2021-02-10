# bach

> :musical_score: Semantic music notation

---

[![build](https://img.shields.io/circleci/project/github/RedSparr0w/node-csgo-parser.svg?style=for-the-badge)](https://circleci.com/gh/slurmulon/bach)
[![npm](https://img.shields.io/npm/v/bach-cljs.svg?style=for-the-badge)](https://npmjs.com/bach-cljs)
[![clojars](https://img.shields.io/clojars/v/bach.svg?style=for-the-badge)](https://clojars.org/bach)

## Introduction

`bach` is a semantic music notation designed to be both human and computer friendly.

Although its primary domain is music, `bach` enables the synchronization of rhythmic timelines with just about anything.

> :warning: The project is still considered experimental should **not** be used in production.

## Documentation

 - **Home**: https://bach.github.io
 - **Guide**: https://bach.github.io/#/guide
 - **Development**: https://bach.github.io/#/dev
 - **Examples**: https://bach.github.io/#/examples
 - **Contribute**: https://bach.github.io/#/contribute

Before diving into the docs, please note that `bach` is a new data format, so naturally there is almost no tooling or integration support for it today.

But whether you're an adventerous musician or a developer exploring `bach` for their project, we advise that you read our [Guide](https://bach.github.io/#/guide) page since it provides the most comphrensive overview of `bach` available.

## Example

The following `bach` data represents the loopable chord progression of a rock backing track:

```bach
@Tempo = 65

!Play [
  1 -> {
    Scale('E lydian')
    Chord('E')
  }
  1/2 -> Chord('G#min')
  1/2 -> Chord('B')
]
```

You can find more examples of `bach` in the [Examples](https://bach.github.io/#/examples) page of the documentation.

## License

MIT
