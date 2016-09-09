// http://lisperator.net/pltut/parser/the-parser

export const PRECEDENCE = {
  '=': 1,
  '/': 2,
  '+': 3
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

  // FIXME: update the pattern of these is* methods, strange how it supports arity 0

  isPunc(ch) {
    const token = this.input.cursor()

    return token && token.type === 'punc' && (!ch || token.value === ch) && token
  }

  isKeyword(keyword) {
    const token = this.input.cursor()

    return token && token.type === 'kw' && (!keyword || token.value === keyword) && token
  }

  isOperator(op) {
    const token = this.input.cursor()

    return token && token.type === 'op' && (!op || token.value === op) && token
  }

  skipPunc(ch) {
    if (this.isPunc(ch)) {
      this.input.next()
    } else {
      this.input.error(`expected punctuation but got ${ch}`)
    }
  }

  parseInput() {
    const track = []

    while (!this.input.eof()) {
      track.push(this.parseExpression())
    }

    return { type: 'track', track }
  }

  // TODO: parseAtom()

  parseObject() {
    return {
      type: 'object',
      vars: this.delimited('(', ')', ',' this.parseName),
      body: this.parseExpression()
    }
  }

  parseArray() {
    return {
      type: 'array',
      body: this.delimited('[', ']', ',', this.parseExpression)
    }
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
      args: this.delimited('(', ')', ',', this.parseExpression()),
      func
    }
  }

  maybeBinary(left, prec) {
    const token = this.isOperator()

    if (token) {
      const otherPrec = PRECEDENCE[token.value]

      if (otherPrec > prec) {
        this.input.next()

        return this.maybeBinary({
          type     : token.values === '=' ? 'assign' : 'binary',
          operator : token.value,
          left     : left,
          right    : this.maybeBinary(this.parseAtom(), otherPrec)
        }, otherPrec)
      }
    }

    return left
  }

  maybeCall(expr) {
    if (expr instanceof Function) {
      expr = expr()

      return this.isPunc('(') ? this.parseCall(expr) : expr
    }
  }

}
