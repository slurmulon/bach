# Primitives

Let's begin with the simplest values found in a track, collectively referred to as "primitives" or "primitive values".

Because `bach` is minimal by design, it only supports two kinds of primitive values: numbers and strings.

You have already encountered numbers and strings in the [Tracks](#tracks) section, so after reading through the next sections see if you can identify the primitives in the example track.

## Numbers

Numerical values can be expressed as either an integer (`1`) or a double (`1.5`).

Numbers cannot include commas, underscores or any other special symbol besides a period (for doubles).

Mathematical expressions like `1 + 1/2` are also supported, but we will cover them later in the [Durations](#durations) section since this is where they are mostly used.

### Integers

An integer is a [whole number](https://en.wikipedia.org/wiki/Integer), or a number without a decimal.

```js
1
64
1024
```

### Doubles

A double is a [rational number](https://en.wikipedia.org/wiki/Rational_number), a number that includes a decimal.

```js
184.63
7.302756
1.0
```

## Strings

The final primitive that `bach` supports is called a "string".

The term "string" may feel unfamiliar, but it's appropriately named since strings are found in nearly every programming language.

All you need to know is that a string is just a representation of literal text.

For instance, in apps strings are frequently used to represent a person's name, email, street address, etc.

In `bach` strings are used to express info like chord and scale values, such as `"Cmin7"` and `"C mixolydian"`.

Any sequence of characters surrounded by either matching single-quotes or double-quotes is considered a string.

```
"D major"
'Eb minor'
"Hello world"
```
