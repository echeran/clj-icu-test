# Rationale for Kalai

## Why does Kalai exist?
Write once compile everywhere

### Cross language libraries
* We use them all the time:
  - REST apis
  - Internationalization
  - SQL builder
* The more you put in, the more valuable

### Performance and size
* Native binaries
* Dead code elimination when used by your own downstream applications

## What do I get from Kalai?
### The value of data literals
### The value of data oriented programming

## Why is Clojure a good source language?
included in [Why Clojure is good for transpilers](https://elangocheran.com/2020/03/18/why-clojure-lisp-is-good-for-writing-transpilers/)


## What are the other options
* J2OBJC, J2CL, 1:1 translations
* Haxe 1:many from OCaml

[See Design](Design.md)

## Tradeoffs

### Dynamism/REPL

We lose REPL / interactive development style in order to gain cross-lang/platform reach 

