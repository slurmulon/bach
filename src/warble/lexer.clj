(ns warble.lexer
  (:require [instaparse.core :as insta]))

; TODO: move grammar to its own file (@see https://github.com/Engelberg/instaparse#input-from-resource-file)
(def tokenize
  (insta/parser
    "root = statement*
     statement = token (<empty> token)*
     <token> = keyword | !keyword (elem | list) | assign
     empty = #'\\s+'
     word = letter+
     number = digit+
     <letter> = #'[a-zA-Z]'
     <digit> = #'[0-9]'
     <add-sub> = mul-div | add | sub
     add = [<empty>] add-sub [<empty>] <'+'> [<empty>] mul-div [<empty>]
     sub = [<empty>] add-sub [<empty>] <'-'> [<empty>] mul-div [<empty>]
     <mul-div> = term | mul | div
     mul = mul-div <'*'> term
     div = mul-div <'/'> term
     <term> = [<empty>] number | [<'('>] add-sub [<')'>] [<empty>]
     <atom> = [<empty>] keyword init [<empty>]
     <elem> = [<empty>] atom | term | pair | identifier [<empty>]
     list = [<empty>] <'['>(elem [<','> elem])* <']'> [<empty>]
     pair = term <'->'> (elem | list) [<empty>,<empty>]
     assign = identifier <'='> (atom | list)
     identifier = [<empty>] #':[a-zA-Z]+' [<empty>]
     init = keyword <'('> identifier* <')'> [<empty>]
     keyword = [<empty>] #'Loop|Note|Scale|Chord|Rest|Times|Forever' [<empty>]"))

(tokenize ":Foo = [1 -> [:Bar], 2 -> [:Zaz]]")

