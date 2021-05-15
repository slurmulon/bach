# Development

This guide is for software developers interested in experimenting with `bach` in their projects.

It's also for developers interested in contributing towards `bach` itself, since the same setup is required in both cases.

This guide is written for GNU/Linux or Unix-like systems such as Mac. Windows is not currently supported.

## Architecture

`bach` tracks are ultimately interpreted by a higher-level `bach` engine, such as [`gig`](https://github.com/slurmulon/gig).

The core library, by itself, can only parse and compile UTF-8 encoded `bach` data into [`bach.json`](https://github.com/slurmulon/bach-json-schema).

`bach.json` is a JSON micro-format that makes it trivial for `bach` engines to sequentially process a `bach` track and synchronize it in real-time with audio or other data.

Once `bach` is converted into `bach.json` it becomes portable and understandable to web apps using our official [tools and libraries](#tools).

It's currently not trivial to perform the inverse conversion of `bach.json` into `bach`, so dynamically generating `bach` and then converting it into `bach.json` can be a useful approach until this is supported.

`bach` is written in Clojure and can be used in either [JavaScript](/dev#javascript) via ClojureScript or NodeJS.

## Clojure

### Installation

#### Environment

Before you can use `bach` in a Clojure or ClojureScript project, you first need the following dependencies on your system:

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
(compose "play! [1 -> Chord('A'), 1 -> Chord('C')]")
```

#### Repl

```sh
$ lein -U repl
```

```clojure
(use 'bach.track :reload)

(compose "play! [1 -> Chord('A'), 1 -> Chord('C')]")
```

#### Testing

```sh
$ lein test
```

## JavaScript

### Installation

#### Environment

Before you can use `bach` for JavaScript, you must have the following dependencies installed to your system:

 - [NodeJS](https://nodejs.org/en/download/) (version 10 or later)
 - [NPM](https://npmjs.com) (installed with NodeJS, version 6 or later)

> **Note**
>
> Making changes to `bach` requires making changes to Clojure/Script code, which depends on the JVM.
>
> If you simply want to install `bach` in your NodeJS project then simply carry on reading.
>
> Otherwise be sure to follow the [Clojure Setup](#setup) guide before moving forward.

#### Node

The core `bach` library is available on NPM as [`bach-cljs`](https://npmjs.com/bach-cljs):

```sh
$ npm i bach-cljs
```

You should now see `bach-cljs` under `dependencies` in `package.json`.

### Usage

#### ES6+
```js
import bach from 'bach-cljs'
```

#### CommonJS

```js
const { bach } = require('bach-cljs')
```

```js
const bach = require('bach-cljs').default
```

#### Library

```js
import bach from 'bach-cljs'

const json = bach(`@tempo = 65
  play! [
    1 -> Chord('E')
    1/2 -> Chord('G#min')
    1/2 -> Chord('B')
  ]
`)

console.log(JSON.stringify(json, null, 2))
```

#### Repl

If you need access to the Node.js ecosystem and its APIs during repl, use the following:

```sh
$ npm run repl
```

Otherwise, if you simply want to repl in a plain ClojureScript environment, use:

```sh
$ npm run cljs-repl
```

#### Building

```sh
# npm run build
```

#### Testing

```sh
$ npm test
```

## Tools

`bach` only becomes functional and therefore useful with the help of a `bach` interpreter library and a `bach` engine.

We have provided a web editor an low-level libraries in JavaScript to serve both of these purposes.

### `bach-editor`

#### About

Official web editor/player for `bach`.

Allows you to hear and visualize your tracks, manage track archives and access useful track information.

Stores all data locally via browser storage, so be careful if you're using a private browser and create periodic archives of your tracks.

#### App

[:musical_keyboard:](https://editor.codebach.tech) https://editor.codebach.tech

#### Repo

[![Github Logo](_media/github.svg)](https://github.com/slurmulon/bach-editor) https://github.com/slurmulon/bach-editor

### `gig`

#### About

Official event-driven `bach` engine for the browser and NodeJS.

Consumes `bach` or `bach.json` (via `bach-js`) and synchronizes its rhythmic data with audio data in real-time.

This library should be used if you want to experiment with `bach` in your web app.

#### Repo

[![Github Logo](_media/github.svg)](https://github.com/slurmulon/gig) https://github.com/slurmulon/gig

### `bach-js`

#### About

Official `bach` interpreter library for the browser and NodeJS.

Consumes `bach` or `bach.json` data and provides a basic standard interface for accessing information about a track and performing basic data transformations (such as serialization and re-groupings).

Serves as the back-bone for all other `bach` libraries written in NodeJS, such as `gig`.

If your app or library is only concerned with `bach` data instead of real-time playback, this library should be used instead of `gig`.

#### Repo

[![Github Logo](_media/github.svg)](https://github.com/slurmulon/bach-js) https://github.com/slurmulon/bach-js

### `bach-json-schema`

#### About

Official JSON Schema definition for the `bach.json` micro-format.

Can be used in any language or platform to validate proposed `bach.json` data for usage in an interpreter or engine such as `bach-js` or `gig`.

#### Repo

[![Github Logo](_media/github.svg)](https://github.com/slurmulon/bach-json-schema) https://github.com/slurmulon/bach-js

### `bach-rest-api`

#### About

A minimal Restful HTTP interface wrapping the core `bach` library, only supporting `bach.json` compilation. Can be self-hosted as a micro-service for supporting back-end `bach` to `bach.json` conversions.

When building web apps it's recommended that you use `bach-js` instead of `bach-rest-api`.
`bach-js` can perform `bach.json` compilation in the browser, so this library should only be used if you're bound to JVM or need to work with micro-services in general.

#### Repo

[![Github Logo](_media/github.svg)](https://github.com/slurmulon/bach-json-schema) https://github.com/slurmulon/bach-js
