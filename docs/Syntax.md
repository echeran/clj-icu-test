# Syntax of Universal Language for Kalai
<!-- https://xkcd.com/927/ -->

## Overview

* Certain languages have requirements that are narrower than other languages
* Supporting all languages requires supporting the narrowest requirement

Ex: statically typed languages requires type annotations for new identifiers

## Requirements in order to support all target languages

### Types

* Need type info on variables due to statically typed languages as target languages

### Switch / Match

* Rust requires default branch (arm) on a match expression
  * Default is required when arm case values do not exhaustively cover the full space of values for the type
* Java requires the switch argument to be a primitive or String or Enum (?)

## Functionality omissions due to current lack of need 

### Enums (or lack thereof)

### Arrays (or lack thereof)

### Try / catch

* May not be possible to support
  * Because Rust has panics and Results

## Gaps in target language support filled in by us to match Kalai/Clojure expressiveness

### If statements as expressions

* We support this in Java with some extra work (using groups)
* Rust will not compile when conditionals as expressions don't have
  an "else" branch (that is, only has a "then" branch)
  - Therefore, we only support conditionals as expressions with the "else" branch, too
  - We do not validate the input for having the "else" branch, and therefore rely on the target language compiler(s) downstream to throw errors to the user 
  
### Cond expressions

* Default cases must only use a keyword (ex: `:else`, `:default`, or any other keyword). Do not use literal `true`.

### Data literals for collections

* Languages like Java don't have any collection literal syntax
* Languages like Rust have literal syntax for some but not all collections
  * Rust: vectors -> `vec!`, but not for set and map)
    C++: can support literal values in initialization statement only, for some versions of C++ and later only