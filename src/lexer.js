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

  isKeyword() {

  }

  isIdent(ch) {
    return this.isIdentStart(ch) || /[a-zA-Z0-9]/.test(ch)
  }

  isIdentStart(ch) {
    return ch === '#' || ch === '['
  }

  isWhitespace(ch) {
    return ' \t\n'.indexOf(ch) >= 0
  }

  * readWhile(predicate) {
    while (!this.input.eof() && predicate(this.input.cursor()) {
      yield input.next()
    }
  }

  readNext() {
    // super.next()

    this.readWhile(this.isWhitespace)

    if (this.input.eof()) return none

    const ch = this.input.cursor()

    if (this.isMeta(ch)) return readMeta(c))
  }

  readIdent() {
    const ident = [... yield this.readWhile(this.isIdent)]

    return {
      type  : this.isKeyword(ident) ? 'kw' : 'var',
      value : ident
    }
  }

  readMeta() {

  }

  readSection() {

  }

  readBeat() {

  }

  readChord() {

  }

  readScale() {

  }

}

