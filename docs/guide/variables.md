# Variables

More often than not you will need to play the same element multiple times in a track.

Variables allow you to assign a name to a value and then reference that value later by the variable's name.

This helps to reduce duplication and human error and makes changing the track much easier later on.

Variables are declared using the `:` character, immediatelly followed by a unique name, a `=` character, and then a value (a primitive, element, collection, etc.).

```bach
:Bm = Chord('Bm')
```

Once a variable is declared it may be referenced and used in any area of the track following it.

This means that a variable **must** be declared before it can be used elsewhere.

The recommended convention is to declare your variables immediately after your headers, as it helps people quickly determine all of the elements used in the track.

## Example

Our primary example track already uses variables, so let's compare and see what it would look like without them.

Here is how the track looks as-is, with variables.

```bach
@Meter = 4|4
@Tempo = 44

:B = Chord('Bm')
:E = Chord('Em')
:F = Chord('F#m7')

!Play [
  4 -> {
    Scale('B minor')
    :B
  }
  2 -> :E
  2 -> :B
  2 -> :F
  2 -> :B
]
```

Here's how it looks without any variables:

```bach
@Meter = 4|4
@Tempo = 44

!Play [
  4 -> {
    Scale('B minor')
    Chord('Bm')
  }
  2 -> Chord('Em')
  2 -> Chord('Bm')
  2 -> Chord('F#m7')
  2 -> Chord('Bm')
]
```

We can see that the example using variables has more a couple more lines, but more importantly it has less repetition.

This is advantageous because if we wanted to say, change `Bm` to `Bm7`, we only have to make that change in one place instead of three.

If you use a value more than once it's recommended that you always assign it to a variable.

If you only use a value once, as is the case with `Scale('B minor')`, then it's up to you whether not to assign it to a variable.

## Limitations

As of now there are a couple of limitations to variables regarding lists.

Assigning a list to a variable is perfectly valid, but there are currently some undesirable limitations that prevent lists from being combined and re-used.

As we discussed earlier, collection nesting, particularly around lists, is limited by design.

These limits are beneficial and justified, but right now they inhibit how much lists can benefit from variables.

Consider the following track:

```bach
@Tempo = 130

!Play [
  16 -> {
    Scale('E mixolydian')
    Chord('E')
  }

  1 -> Chord('A')
  1/2 -> Chord('C')
  1/2 -> Chord('B7')
  4 -> Chord('E')

  1 -> Chord('A')
  1/2 -> Chord('C')
  1/2 -> Chord('B7')
  4 -> Chord('E')
]
```

Note how one part in the list is repeated:

```bach
1 -> Chord('A')
1/2 -> Chord('C')
1/2 -> Chord('B7')
4 -> Chord('E')
```

The natural instinct is to assign this part to a variable as its own list.

This would reduce duplication in the track quite a bit:

```bach
@Tempo = 130

:Part = [
  1 -> Chord('A')
  1/2 -> Chord('C')
  1/2 -> Chord('B7')
  4 -> Chord('E')
]

!Play [
  16 -> {
    Scale('E mixolydian')
    Chord('E')
  }

  :Part
  :Part
]
```

The issue is that, at least right now, `bach` will interpret this as a list nested in a list, and this is not supported.

The ideal behavior would be for `bach` to sort of merge `:Part` into the main list, that way `bach` only deals with one list and avoids violating the nesting constraints.

This lacking feature is especially problematic for complex or long tracks that repeat not only the same musical elements but also the same phrases or parts several times over.

In the near future `bach` will elegantly handle this situation, but for now this must be noted as a limitation to work around.

> If you are a programmer who is interested in contributing to `bach` and solving this problem, please check out the [Contribute](/contribute) page.
