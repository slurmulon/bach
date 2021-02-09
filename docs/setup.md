# Setup

This guide is for software developers interested in experimenting with `bach` in their projects.

`bach` is written in Clojure and can be used in both the JVM and NodeJS ecosystems.

## Clojure

### Installation

#### Environment

Before you use `bach` in a Clojure or ClojureScript project, you must first install the following dependencies to your system:

 - [OpenJDK](https://openjdk.java.net/install/) (version 8 or later)
 - [Leinengen](https://leiningen.org/#install)

If you wish to use `bach` in NodeJS instead, head to the [JavaScript](#javascript) section.

#### Leinengen/Boot

`[bach "2.1.0-SNAPSHOT"]`

#### Gradle

`compile "bach:bach:2.1.0-SNAPSHOT"`

#### Maven

```xml
<dependency>
  <groupId>bach</groupId>
  <artifactId>bach</artifactId>
  <version>2.1.0-SNAPSHOT</version>
</dependency>
```

### Usage

#### CLI

First be sure that you have a binary executable (requires `lein` to be installed) available on your `PATH`:

```sh
$ lein bin
```

Then you can execute the resulting binary like so:

```sh
$ target/default/bach-2.1.0-SNAPSHOT -i /path/to/track.bach compile
```

The executable currently supports the following actions:

- `parse`: creates an Abstract Syntax Tree (AST) from vanilla `bach` data
- `compile`: parses and compiles vanilla `bach` data into `bach.json`, an intermediary JSON micro-format that allows for simple interpretation of tracks
- `help`

#### Library

```clojure
(ns my.namespace
  (:require [bach.track :refer [compose]]))

; Parses, optimizes and compiles bach data into an interpretable hash-map
(compose "!Play [1 -> Chord('A'), 1 -> Chord('C')]")
```

#### Repl

```sh
$ lein -U repl
```

```clojure
(use 'bach.track :reload)

(compose "!Play [1 -> Chord('A'), 1 -> Chord('C')]")
```

## JavaScript

### Installation

#### Environment

Before you use `bach` for JavaScript, you must have the following dependencies installed to your system:

 - [NodeJS](https://nodejs.org/en/download/) (version 10 or later)
 - [NPM](https://npmjs.com) (installed with NodeJS)

#### Node

The core `bach` library is available on NPM as `bach-cljs`:

```sh
$ npm i bach-cljs
```

You should now see `bach-cljs` under `dependencies` in `package.json`.

#### Usage

##### ES6+
```js
import bach from 'bach-cljs'
```

##### CommonJS

```js
const { bach } = require('bach-cljs')
```

```js
const bach = require('bach-cljs').default
```

##### Example

```js
import bach from 'bach-cljs'

const json = bach(`@Tempo = 65
  !Play [
    1 -> Chord('E')
    1/2 -> Chord('G#min')
    1/2 -> Chord('B')
  ]
`)

console.log(JSON.stringify(json, null, 2))
```


