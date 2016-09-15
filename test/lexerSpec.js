import { InputStream, TokenStream } from '../src/lexer'
import chai from 'chai'
import chaiThings from 'chai-things'

chai.should()
chai.use(chaiThings)

describe('InputStream', () => {

  describe('constructor', () => {
    it('should set the initial position to 0', () => {
      new InputStream(null).pos.should.equal(0)
    })

    it('should set the initial line to 0', () => {
      new InputStream(null).line.should.equal(1)
    })

    it('should set the initial column to 0', () => {
      new InputStream(null).col.should.equal(0)
    })
  })

  describe('next', () => {
    let stream

    beforeEach(() => stream = new InputStream('\n'))

    it('should bump the line number when a line-break is encountered', () => {
      stream.next()
      stream.line.should.equal(2)
    })

    it('should reset the column count when a line-break is encountered', () => {
      stream.next()
      stream.col.should.equal(0)
    })

    it('should bump the column number when the next character is not a line-break', () => {
      stream = new InputStream('x')

      stream.next()
      stream.col.should.equal(1)
    })

    it('should return the current character at the new position', () => {
      stream = new InputStream('x')

      stream.next().should.equal('x')
    })
  })

  describe('cursor', () => {
    it('should return the character at the current position', () => {
      const stream = new InputStream('ab')

      stream.cursor().should.equal('a')
      stream.next()
      stream.cursor().should.equal('b')

    })
  })

  describe('eof', () => {
    it('should return true if the current character is empty', () => {
      const stream = new InputStream('')

      stream.eof().should.be.true
    })
  })

})

describe('TokenStream', () => {

  let noneStream, someStream, multiStream

  beforeEach(() => {
    noneStream  = new TokenStream(new InputStream(''))
    someStream  = new TokenStream(new InputStream('x'))
    multiStream = new TokenStream(new InputStream('xy'))
  })

  describe('cursor', () => {
    it('should return the character at the current position', () => {
      someStream.cursor().value.should.equal('x')
    })
  })

  describe('next', () => {
    it('should return the current token if it is truthy', () => {
      someStream.next().value.should.equal('x')
    })
  })

  describe('eof', () => {
    it('should return true if the cursor is null', () => {
      noneStream.eof().should.be.true
    })
  })

  describe('isKeyword', () => {
    it('should return true if the string is "Loop"', () => {
      someStream.isKeyword('Loop').should.be.true
    })

    it('should return true if the string is "Times"', () => {
      someStream.isKeyword('Times').should.be.true
    })

    it('should return true if the string is "Forever"', () => {
      someStream.isKeyword('Forever').should.be.true
    })

    it('should return true if the string is "Title"', () => {
      someStream.isKeyword('Title').should.be.true
    })
  })

  describe('isIdent', () => {

  })

  describe('isIdentStart', () => {

  })

  describe('isOperator', () => {

  })

  describe('isPunc', () => {

  })

  describe('isDigit', () => {

  })

  describe('isWhitespace', () => {

  })

  describe('isComment', () => {

  })

  describe('isColor', () => {

  })

  describe('readWhile', () => {

  })

  describe('readNext', () => {

  })

  describe('readIdent', () => {

  })

  describe('readTuple', () => {

  })

  describe('readNumber', () => {

  })

  describe('skipComment', () => {

  })

 })
