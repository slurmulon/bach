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
     mul = [<empty>] mul-div [<empty>] <'*'> [<empty>] term [<empty>]
     div = [<empty>] mul-div [<empty>] <'/'> [<empty>] term [<empty>]
     <term> = number | [<'('>] add-sub [<')'>]
     <atom> = keyword init
     <elem> = atom | term | pair | identifier
     list = <'['> [<empty>] elem* [<empty>] <']'> [<empty>]
     pair = term [<empty>] <'->'> [<empty>] (elem | list) [<empty>]
     assign = identifier [<empty>] <'='> [<empty>] (atom | list) [<empty>]
     identifier = #':[a-zA-Z]+'
     init = keyword [<empty>] <'('> [<empty>] identifier* [<empty>] <')'> [<empty>]
     keyword = #'Loop|Note|Scale|Chord|Rest|Times|Forever'"))

; (tokenize ":Foo = [:Bar]")
(tokenize ":Foo = [1 -> [:Bar]]")

