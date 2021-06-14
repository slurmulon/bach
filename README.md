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

 - **Home**: https://codebach.tech
 - **Guide**: https://codebach.tech/#/guide
 - **Examples**: https://codebach.tech/#/examples
 - **Syntax**: https://codebach.tech/#/syntax
 - **Development**: https://codebach.tech/#/dev
 - **Contribute**: https://codebach.tech/#/contribute

Before diving into the docs, please note that `bach` is a new data format, so naturally there is limited [tooling](https://codebach.tech/#/dev?id=tools) and integration support for it today.

But whether you're an adventerous musician or a developer exploring `bach` for their project, we advise that you read our [Guide](https://codebach.tech/#/guide) page since it provides the most comphrensive overview of `bach` available.

## Example

The following `bach` data represents the loopable progression of a rock backing track.

> :musical_keyboard: Try running it in the [bach editor](https://editor.codebach.tech)!

```bach
@meter = 4|4
@tempo = 83

:A = [
  3/8 -> {
    scale('E aeolian')
    chord('Em9')
  }
  5/8 -> chord('C')
  3/8 -> chord('Em9')
  4/8 -> chord('C')
  9/8 -> chord('C/D')
]

:B = [
  3/8 -> chord('B')
  5/8 -> chord('Em9')
  1 -> chord('Em9')
]

:C = [
  3/8 -> chord('B')
  5/8 -> chord('G')
  1 -> chord('G')
]

play! [
  7 of :A
  2 of :B
  3 of [
    :C
    when !{ last? } do { :B }
  ]
  3/8 -> chord('B')
  5/8 -> chord('Em9')
  1 -> chord('B7b13')
]
```

You can find more examples of `bach` in the [Examples](https://codebach.tech/#/examples) page of the documentation.

## License

MIT
