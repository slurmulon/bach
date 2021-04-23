(ns bach.ast
  (:require #?(:cljs [instaparse.core :as insta :refer-macros [defparser]]
               :clj [instaparse.core :as insta :refer [defparser]])))

; TODO!:
;  - Think about `Config!` operator, allowing users to specify run-time configuration flags per track
;  - Allows bach engines to dynamically adapt interpretation of tracks on an individual level
;  - Use cases:
;    * Track is large and you don't want the quantized "signals" exported via `bach.compose/compose`
;  
(defparser parse
  "(* Core *)
    track = statement*
    statement = token (<empty> token)*

    <token>    = elem | assign | header | play
    <expr>     = [<empty>] term | add | sub [<empty>]
    (* <elem>     = [<empty>] atom | prim | expr | pair | coll | identifier [<empty>] *)
    <elem>     = [<empty>] entity | prim | expr [<empty>]
    <entity>   = [<empty>] atom | coll | pair | identifier [<empty>]
    <item>     = entity | when
    <seq>      = [<empty>] list | loop [<empty>]
    <coll>     = [<empty>] seq | set [<empty>]
    <prim>     = [<empty>] string | number | meter [<empty>]
    <init>     = <'('> arguments <')'>
    atom       = [<empty>] keyword [<empty>] init [<empty>]

    set        = [<empty>] <'{'> [item (<','|empty> item)* [<','>]] <'}'> [<empty>]
    list       = [<empty>] <'['> [item (<','|empty> item)* [<','>]] <']'> [<empty>]
    loop       = [<empty>] int [<empty>] <'of'> [<empty>] (set | list) [<empty>]
    when       = [<empty>] <'when'> <empty> int <empty> <'then'> <empty> (atom | identifier | set | list) [<empty>]

    (* TODO: Rename pair to beat *)
    (* ORIG *)
    (* pair       = expr <'->'> elem [<empty>,<empty>] *)
    (* V3, FIXME *)
    pair       = expr <'->'> atom | set | identifier [<empty>,<empty>]
    assign     = identifier <'='> elem
    header     = meta <'='> elem
    attribute  = name [<empty>] <':'> [<empty>] prim
    identifier = [<empty>] <':'> name [<empty>]
    arguments  = ((identifier | string | attribute | expr) [<empty> <','> <empty>])*
    meta       = [<empty>] <'@'> name [<empty>]
    (* TODO: Rename to kind *)
    keyword    = [<empty>] <'~'> | name [<empty>]
    play       = [<empty>] #'[pP]lay!' [<empty>] elem
    meter      = [<empty>] <int> <'|'> <int> [<empty>]
    (* duration   = [<empty>] (expr <'.'> #'(b|m)') | *)
    string     = #'[\\'|\"](.*?)[\"|\\']'
    word       = #'[a-zA-Z]+'
    name       = #'[a-zA-Z_]+[a-zA-Z0-9_-]*'
    int      = #'(0|([1-9][0-9]*))'
    float    = #'(0|([1-9][0-9]*))(\\.[0-9]+)?'
    <number>     = int | float
    <empty>    = #'(\\r\\n|\\n|\\r|\\s)*'
    <comment> = #'\\/\\/\\s.*?$'

    (* Math *)
    add = term <'+'> expr
    sub = term <'-'> expr
    <term> = [<empty>] number | mul | div | factor [<empty>]
    mul = factor <'*'> factor
    div = factor <'/'> factor
    <factor> = [<empty>] [<'('>] expr [<')'>] [<empty>]")
