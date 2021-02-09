# Development

If you wish to write code that uses `bach` or to make changes to `bach` itself, this guide is for you.

Before proceeding, but sure you have followed the steps in the Setup guide in order to install everything necessary for development

## Intro

This guide will show you how to integrate `bach` into your own projects, and, if you're interested in contributing to `bach`, setup a development environment.

## Architecture

`bach` tracks are ultimately interpreted by a higher-level `bach` engine, such as [`gig`](https://github.com/slurmulon/gig).

The core library, by itself, can only parse and compile plaintext `bach` data into [`bach.json`](https://github.com/slurmulon/bach-json-schema).

`bach.json` is a JSON micro-format that makes it trivial for `bach` engines to sequentially process a `bach` track and synchronize it in real-time with audio or other data.

`bach` is written in Clojure and can be used in either [JavaScript](/dev#javascript) via ClojureScript or NodeJS.

## Clojure

### Dependencies

Before you can develop `bach` for Clojure/Script, you must first install the following dependencies to your system:

 - [OpenJDK](https://openjdk.java.net/install/) (version 8 or later)
 - [Leinengen](https://leiningen.org/#install)

If you wish to use `bach` in NodeJS instead, head to the [JavaScript](#javascript) section.

### Setup

To setup a development environment for Clojure, first clone the repo:

```sh
$ git clone git@github.com:slurmulon/bach.git
```

Then change your current directory to wherever you cloned `bach`, and:

```sh
$ lein install
```

If you want to simultaneously develop `bach` and another library using it, [follow this guide on checkout dependencies](https://github.com/technomancy/leiningen/blob/master/doc/TUTORIAL.md#checkout-dependencies).

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

If using Clojure or the JVM in general is not an option, `bach` can also be used via ClojureScript and/or NodeJS.

Since installation and usage is identical between Clojure and ClojureScript, this section focuses on using `bach` in NodeJS via `npm`.

As of now only `bach.track/compose` is exported and accessible in NodeJS, since parsing and validating `bach` into `bach.json` is the primary feature.

### Dependencies

Before you can develop `bach` for JavaScript, you must first install the following dependencies to your system:

 - [NodeJS](https://nodejs.org/en/download/) (version 10 or later)
 - [NPM](https://npmjs.com) (installed with NodeJS)

> **Note**
>
> Making changes to `bach` requires making changes to Clojure/Script code, which depends on the JVM.
>
> If you simply want to install `bach` in your NodeJS project then progress to the next section.
>
> Otherwise be sure to follow the [Clojure Setup](#setup) guide before moving forward.

### Setup

To setup a development environment for JavaScript, first clone the repo if you haven't done so already:

```sh
$ git clone git@github.com:slurmulon/bach.git
```

Then install the core NodeJS `bach` library, `bach-cljs`:


```sh
$ npm i bach-cljs
```

You should now see `bach-cljs` under `dependencies` in `package.json`.

**ES6+**
```js
import bach from 'bach-cljs'
```

**CommonJS**

```js
const { bach } = require('bach-cljs')
```

```js
const bach = require('bach-cljs').default
```

### Usage

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

### Development

```sh
$ npm i
$ npm run dev
```

### Releasing

```sh
$ npm run build
```

### Testing

```sh
$ npm test
```

## Help

### Cheat Sheet
