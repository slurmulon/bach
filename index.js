// Temporary package export to workaround shadow-cljs complications with ESM

const bach = require('./dist/bach.cjs')

module.exports = Object.assign(bach, { default: bach.compose })
