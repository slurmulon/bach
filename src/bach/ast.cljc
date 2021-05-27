; ns bach.syntax
; ns bach.lang
(ns bach.ast
  (:require #?@(:cljs [[instaparse.core :as insta :refer-macros [defparser]]]
                :clj [[instaparse.core :as insta :refer [defparser]]])
            [bach.data :refer [problem]]))

; TODO!:
;  - Think about `Config!` operator, allowing users to specify run-time configuration flags per track
;  - Allows bach engines to dynamically adapt interpretation of tracks on an individual level
;  - Use cases:
;    * Track is large and you don't want the quantized "signals" exported via `bach.compose/compose`
(defparser parse
  "(* Core *)
    track = statement*
    statement = token (<empty> token)*

    <token>    = elem | assign | header | play | <comment> | <empty>
    <expr>     = term | add | sub
    <entity>   = atom | rest | coll | beat | identifier
    <elem>     = entity | prim | expr
    <item>     = [<empty>] entity | when | <comment> [<empty>]
    <seq>      = list | loop
    <coll>     = seq | set
    <prim>     = string | number | meter
    <init>     = <'('> arguments <')'>
    atom       = [<empty>] kind [<empty>] init [<empty>]
    rest       = [<empty>] <'_'> [<empty>]

    set        = [<empty>] <'{'> [item (<','|empty> item)* [<','>]] <'}'> [<empty>]
    list       = [<empty>] <'['> [item (<','|empty> item)* [<','>]] <']'> [<empty>]
    loop       = [<empty>] int [<empty>] <'of'> [<empty>] (set | list | identifier) [<empty>]
    when       = [<empty>] <'when'> <empty> when-expr <empty> <'do'> <empty> when-do [<empty>]

    <when-do>  = (atom | identifier | set | list)
    when-match = #'(even|odd|last|first)' <'?'>
    when-comp  = #'(gte|gt|lte|lt|factor)' <'? '> int
    <when-cond> = [<empty>] (int | range | when-match | when-comp) [<empty>]
    when-all   = [<empty>] <'['> [when-expr (<','|empty> when-expr)* [<','>]] <']'> [<empty>]
    when-any   = [<empty>] <'{'> [when-expr (<','|empty> when-expr)* [<','>]] <'}'> [<empty>]
    when-not   = <'!'> (when-all | when-any)
    <when-expr> = [<'('>] when-all | when-any | when-not | when-cond [<')'>]

    beat       = expr <'->'> (atom | rest | set | identifier) [<empty>,<empty>]
    assign     = identifier [<empty>] <'='> [<empty>] elem
    header     = meta [<empty>] <'='> [<empty>] (prim | expr)
    attribute  = name [<empty>] <':'> [<empty>] prim
    identifier = [<empty>] <':'> name [<empty>]
    arguments  = ((identifier | string | attribute | expr) [<empty> <','> <empty>])*
    meta       = <'@'> name
    (* kind       = [<empty>] <'~'> | name [<empty>] *)
    kind       = [<empty>] name [<empty>]
    play       = <#'(?i)play!'> [<empty>] elem
    meter      = [<empty>] int <'|'> int [<empty>]
    bool       = #'(true|false)'
    string     = #'[\\'|\"](.*?)[\"|\\']'
    word       = #'[a-zA-Z]+'
    name       = #'[a-zA-Z_]+[a-zA-Z0-9_-]*'
    int        = #'(0|([1-9][0-9]*))'
    float      = #'(0|([1-9][0-9]*))(\\.[0-9]+)+'
    <number>   = int | float
    <empty>    = #'(\\r\\n|\\n|\\r|\\s)*'
    (* WARN: This is likely not very optimal. Revisit. *)
    <comment>  = #'#{2}(.+)'

    (* Math *)
    add = term <'+'> expr
    sub = term <'-'> expr
    <term> = [<empty>] number | mul | div | factor | duration [<empty>]
    mul = factor <'*'> factor
    div = factor <'/'> factor
    <factor> = [<empty>] [<'('>] expr [<')'>] [<empty>]
    range = [<empty>] (int <'..'> int) [<empty>]
    <duration> = duration-dynamic | duration-static
    duration-dynamic = #'(beat|bar)'
    duration-static = #'(2|4|8|16|32|64|128|256)' <'n'>
  ")

(def parsed? (comp not insta/failure?))

; (defn failure
;   [code]
;   (->> code (insta/parses parse) insta/get-failure))

; (defn parse!
;   [code]
;   (let [result (parse code)]
;     (if-not (insta/failure? result) code (problem result))))
