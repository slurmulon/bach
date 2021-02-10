# Limitations

 - `bach` is a new project that is still taking shape and finding its place in the world (so, there's not much tooling, yet :sob:)
 - No working sandbox/editor for `bach` (in the works :construction:)
 - No interpretation or application of music theory, such as the notes that make up a scale or code (this is the responsibility of [`bach-js`](/dev#bach-js))
 - No reserved aliases for common semantic durations (e.g. `:bar`, `1/2 * :bar`, etc)
 - No official support yet for clef
 - No official semantics around instruments (you can use an `@Instrument` header to indicate this)
 - No local repeater symbols, all tracks are currently considered loopable by default
 - Converting `bach.json` into `bach` again (i.e. inverse conversion) is not supported yet
