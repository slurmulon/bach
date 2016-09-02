// http://lisperator.net/pltut/parser/

export class Stream {

  constructor (input) {
    this.input = input
    this.pos   = 0
    this.line  = 1
    this.col   = 0
  }

  next () {
    let ch = this.input.charAt(this.pos++)

    if (ch === '\n') {
      this.line++
      this.col = 0
    } else {
      this.col++
    }

    return ch
  }

  peek () {
    return this.input.charAt(this.pos)
  }

  isEOF() {
    return this.peek() === ''
  }

  error (msg) {
    throw new Error(`${msg} (${this.line}: ${this.col})`)
  }

}

export class TokenStream extends Stream {

  constructor (input) {
    this.input = input
  }

  readNext() {

  }

  readMeta() {

  }

  readString() {

  }

  readNumber() {

  }

}

