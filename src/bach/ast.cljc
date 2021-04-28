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

    <token>    = elem | assign | header | play | <comment>
    <expr>     = [<empty>] term | add | sub [<empty>]
    <elem>     = [<empty>] entity | prim | expr [<empty>]
    <entity>   = [<empty>] atom | coll | beat | identifier [<empty>]
    <item>     = entity | when
    <seq>      = [<empty>] list | loop [<empty>]
    <coll>     = [<empty>] seq | set [<empty>]
    <prim>     = [<empty>] string | number | meter [<empty>]
    <init>     = <'('> arguments <')'>
    atom       = [<empty>] keyword [<empty>] init [<empty>]

    set        = [<empty>] <'{'> [item (<','|empty> item)* [<','>]] <'}'> [<empty>]
    list       = [<empty>] <'['> [item (<','|empty> item)* [<','>]] <']'> [<empty>]
    loop       = [<empty>] int [<empty>] <'of'> [<empty>] (set | list) [<empty>]
    when       = [<empty>] <'when'> <empty> when-expr <empty> <'do'> <empty> when-do [<empty>]

    <when-do>  = (atom | identifier | set | list)
    when-match = #'(even|odd|last|first)' <'?'>
    when-comp  = #'(gte|gt|lte|lt)' <'? '> int
    <when-cond> = [<empty>] (int | range | when-match | when-comp) [<empty>]
    when-all   = [<empty>] <'['> [when-expr (<','|empty> when-expr)* [<','>]] <']'> [<empty>]
    when-any   = [<empty>] <'{'> [when-expr (<','|empty> when-expr)* [<','>]] <'}'> [<empty>]
    when-not   = <'!'> (when-all | when-any)
    <when-expr> = [<'('>] when-all | when-any | when-not | when-cond [<')'>]

    beat       = expr <'->'> (atom | set | identifier) [<empty>,<empty>]
    assign     = identifier <'='> elem
    header     = meta <'='> (prim | expr)
    attribute  = name [<empty>] <':'> [<empty>] prim
    identifier = [<empty>] <':'> name [<empty>]
    arguments  = ((identifier | string | attribute | expr) [<empty> <','> <empty>])*
    meta       = [<empty>] <'@'> name [<empty>]
    (* TODO: Rename to kind *)
    keyword    = [<empty>] <'~'> | name [<empty>]
    play       = [<empty>] <#'(?i)play!'> [<empty>] elem
    meter      = [<empty>] int <'|'> int [<empty>]
    bool       = #'(true|false)'
    string     = #'[\\'|\"](.*?)[\"|\\']'
    word       = #'[a-zA-Z]+'
    name       = #'[a-zA-Z_]+[a-zA-Z0-9_-]*'
    int        = #'(0|([1-9][0-9]*))'
    float      = #'(0|([1-9][0-9]*))(\\.[0-9]+)+'
    <number>   = int | float
    <empty>    = #'(\\r\\n|\\n|\\r|\\s)*'
    <comment>  = #'#{2}(.*?)(\\n|\\r)'

    (* Math *)
    add = term <'+'> expr
    sub = term <'-'> expr
    <term> = [<empty>] number | mul | div | factor | duration [<empty>]
    mul = factor <'*'> factor
    div = factor <'/'> factor
    <factor> = [<empty>] [<'('>] expr [<')'>] [<empty>]
    range = [<empty>] (int <'..'> int) [<empty>]
    duration = #'(beat|bar)'
  ")

