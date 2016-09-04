// http://lisperator.net/pltut/parser/

import { some, none } from 'optionize'

export class Stream {

  constructor (input) {
    this.input = input
    this.pos   = 0
    this.line  = 1
    this.col   = 0
  }

  * next () {
    let ch = this.input.charAt(this.pos++)

    if (ch === '\n') {
      this.line++
      this.col = 0
    } else {
      this.col++
    }

    yield ch
  }

  cursor () {
    return this.input.charAt(this.pos)
  }

  eof () {
    return this.cursor() === null //this.cursor() === ''
  }

  error (msg) {
    throw new Error(`${msg} (${this.line}: ${this.col})`)
  }

}

export class TokenStream extends Stream {

  constructor (input) {
    super(input)
  }

  isKeyword() {

  }

  isIdent(ch) {
    return this.isIdentStart(ch) || /[a-zA-Z0-9]/.test(ch)
  }

  isIdentStart(ch) {
    return ch === '#' || ch === '['
  }

  isParam(ch) {
    return ch === '{'
  }

  isWhitespace(ch) {
    return ' \t\n'.indexOf(ch) >= 0
  }

  isComment() {
    return ch === '/'
  }

  isMeta() {

  }

  isSection() {

  }

  isBeat() {

  }

  isChord() {

  }

  isScale() {

  }

  isColor() {

  }

  * readWhile(predicate) {
    while (!this.input.eof() && predicate(this.input.cursor())) {
      yield input.next()
    }
  }

  readNext() {
    this.readWhile(this.isWhitespace)

    if (this.input.eof()) return none

    const ch   = this.input.cursor()
    const pipe = ['meta', 'comment', 'ident', 'section', 'beat', 'chord', 'scale', 'color']

    for (stage in pipe) {
      const name = stage.toUppercase()
      const is   = this[`is${name}`]
      const read = this[`read${name}`]

      if (is(ch)) {
        return read()
      }
    }

    this.input.error(`invalid character: ${ch}`)
  }

  readIdent() {
    const ident = [... this.readWhile(this.isIdent)]

    return {
      type  : this.isKeyword(ident) ? 'kw' : 'var',
      value : ident
    }
  }

  readParam() {

  }

  readMeta() {

  }

  readComment() {

  }

  readSection() {

  }

  readBeat() {

  }

  readChord() {

  }

  readScale() {

  }

  readColor() {

  }

}

