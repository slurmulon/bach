import { InputStream, TokenStream } from '../src/lexer'
import chai from 'chai'
import chaiThings from 'chai-things'

chai.should()
chai.use(chaiThings)

describe('Stream', () => {

  describe('next', () => {
    it('should bump the line number reset the column when a line-break is encountered', () => {

    })

    it('should bump the column number when the next character is not a line-break', () => {

    })

    it('should return the current character at the new position', () => {

    })
  })

  describe('cursor', () => {
    it('should return the character at the current position', () => {

    })
  })

  describe('eof', () => {
    it('should return true if the current character is null', () => {

    })
  })

})

describe('TokenStream', () => {

  describe('cursor', () => {

  })

  describe('next', () => {

  })

  describe('eof', () => {

  })

  describe('isKeyword', () => {

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
