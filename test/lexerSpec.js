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

})
