(ns bach.ast
  (:require #?(:cljs [instaparse.core :as insta :refer-macros [defparser]]
               :clj [instaparse.core :as insta :refer [defparser]])))

(defparser parse
  "(* Core *)
    track = statement*
    statement = token (<empty> token)*

    <token>    = elem | assign | header | play
    <expr>     = [<empty>] term | add | sub [<empty>]
    (* <elem>     = [<empty>] atom | prim | expr | pair | coll | identifier [<empty>] *)
    <elem>     = [<empty>] entity | prim | expr [<empty>]
    (* TODO: Use in elem *)
    (* <entity>   = [<empty>] atom | pair | coll | identifier [<empty>] *)
    (* <entity>   = [<empty>] atom | coll | identifier [<empty>] *)
    <entity>   = [<empty>] atom | coll | pair | identifier [<empty>]
    <seq>      = [<empty>] list | loop [<empty>]
    <coll>     = [<empty>] seq | set [<empty>]
    <prim>     = [<empty>] string | number | meter [<empty>]
    <init>     = <'('> arguments <')'>
    atom       = [<empty>] keyword [<empty>] init [<empty>]

    (* set        = [<empty>] <'{'> [elem (<','|empty> elem)* [<','>]] <'}'> [<empty>]
    list       = [<empty>] <'['> [elem (<','|empty> elem)* [<','>]] <']'> [<empty>] *)

    (* WARN: Might need to tighten these up, i.e. something more specific than `entity` *)
    (*  - One reason to NOT tighten this up is to hoist all validation to bach.compose, reducing/preventing parallel logic between AST parser and compiler *)
    set        = [<empty>] <'{'> [entity (<','|empty> entity)* [<','>]] <'}'> [<empty>]
    list       = [<empty>] <'['> [entity (<','|empty> entity)* [<','>]] <']'> [<empty>]
    loop       = [<empty>] int [<empty>] <'of'> set | list [<empty>]

    (* TODO: Rename pair to beat *)
    (* pair       = expr <'->'> elem [<empty>,<empty>] *)
    pair       = expr <'->'> atom | set | identifier [<empty>,<empty>]
    assign     = identifier <'='> elem
    header     = meta <'='> elem
    attribute  = name [<empty>] <':'> [<empty>] prim
    identifier = [<empty>] <':'> name [<empty>]
    arguments  = ((identifier | string | attribute | expr) [<empty> <','> <empty>])*
    meta       = [<empty>] <'@'> name [<empty>]
    keyword    = [<empty>] <'~'> | name [<empty>]
    (* play       = [<empty>] <'!Play'> [<empty>] elem *)
    play       = [<empty>] <'Play!'> [<empty>] elem
    meter      = [<empty>] int <'|'> int [<empty>]
    string     = #'[\\'|\"](.*?)[\"|\\']'
    word       = #'[a-zA-Z]+'
    name       = #'[a-zA-Z_]+[a-zA-Z0-9_-]*'
    <int>      = #'(0|([1-9][0-9]*))'
    <float>    = #'(0|([1-9][0-9]*))(\\.[0-9]+)?'
    number     = int | float
    (* color      = #'#[a-fA-F0-9xX]{3,6}' *)
    <empty>    = #'(\\r\\n|\\n|\\r|\\s)*'
    (* TODO: Support comments *)

    (* Math *)
    add = term <'+'> expr
    sub = term <'-'> expr
    <term> = [<empty>] number | mul | div | factor [<empty>]
    mul = factor <'*'> factor
    div = factor <'/'> factor
    <factor> = [<empty>] [<'('>] expr [<')'>] [<empty>]")

; (defparser parse
;   "(* Core *)
;     track = statement*
;     statement = token (<empty> token)*

;     <token>    = elem | assign | header | play
;     <expr>     = [<empty>] term | add | sub [<empty>]
;     <elem>     = [<empty>] atom | prim | expr | pair | list | set | identifier [<empty>]
;     <prim>     = [<empty>] string | number | meter | color [<empty>]
;     <init>     = <'('> arguments <')'>
;     atom       = [<empty>] keyword [<empty>] init [<empty>]
;     set        = [<empty>] <'{'> [elem (<','|empty> elem)* [<','>]] <'}'> [<empty>]
;     list       = [<empty>] <'['> [elem (<','|empty> elem)* [<','>]] <']'> [<empty>]
;     pair       = expr <'->'> elem [<empty>,<empty>]
;     assign     = identifier <'='> elem
;     header     = meta <'='> elem
;     attribute  = name [<empty>] <':'> [<empty>] prim
;     identifier = [<empty>] <':'> name [<empty>]
;     arguments  = ((identifier | string | attribute | expr) [<empty> <','> <empty>])*
;     meta       = [<empty>] <'@'> name [<empty>]
;     keyword    = [<empty>] <'~'> | name [<empty>]
;     play       = [<empty>] <'!Play'> [<empty>] elem
;     (* FIXME: Only support `int` here *)
;     meter      = [<empty>] number <'|'> number [<empty>]
;     string     = #'[\\'|\"](.*?)[\"|\\']'
;     word       = #'[a-zA-Z]+'
;     name       = #'[a-zA-Z_]+[a-zA-Z0-9_-]*'
;     <int>      = #'(0|([1-9][0-9]*))'
;     <float>    = #'(0|([1-9][0-9]*))(\\.[0-9]+)?'
;     number     = int | float
;     color      = #'#[a-fA-F0-9xX]{3,6}'
;     <empty>    = #'(\\r\\n|\\n|\\r|\\s)*'
;     (* TODO: Support comments *)

;     (* Math *)
;     add = term <'+'> expr
;     sub = term <'-'> expr
;     <term> = [<empty>] number | mul | div | factor [<empty>]
;     mul = factor <'*'> factor
;     div = factor <'/'> factor
;     <factor> = [<empty>] [<'('>] expr [<')'>] [<empty>]")
