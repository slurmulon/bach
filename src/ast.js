// http://lisperator.net/pltut/parser/the-parser

export const PRECEDENCE = {
  '=': 1,
  '+': 2
}

export class AST {

  constructor(input) {
    this.input = input
  }



  delimited(start, stop, separator, parser) {
    let delimited = [], first = true

    this.skipPunc(start)

    while (!this.input.eof()) {
      if (this.isPunc(stop)) break

      if (first) {
        first = false
      } else {
        this.skipPunc(separator)
      }

      if (this.isPunc(stop)) break

      delimited.push(this.parser())
    }

    this.skipPunc(stop)

    return delimited
  }

  skipPunc(ch) {
    // if (this.isPunc
  }

  parseInput() {
    const track = []

    while (!this.input.eof()) {
      track.push(this.parseExpression())
    }

    return { type: 'track', track }
  }

  parseObject() {
    return {
      type: 'object',
      vars: this.delimited('(', ')', this.parseName),
      body: this.parseExpression()
    }
  }

  parseArray() {

  }

  parseTuple() {

  }

  parseName() {
    const name = this.input.next()

    if (name.type !== 'var') this.input.error('expected variable name')

    return name.value
  }

  parseExpression() {

  }

  parseCall(func) {
    return {
      type: 'call',
      args: this.delimited('(', ')', ',', this.parseExpression())
      func
    }
  }

  maybeBinary(left, prec) {

  }

  maybeCall(expr) {

  }

}
