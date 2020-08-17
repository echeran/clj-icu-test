(ns kalai.pass.java1-syntax
  (:require [meander.strategy.epsilon :as s]
            [meander.epsilon :as m]))

;;; -------- language constructs to syntax
;; expanded s-expressions below


;; what is an expression?
;; can occur in a statement, block, conditional etc...

;; what is a statement?
;; what expressions cannot be statements? block, x/if
;; expression semicolon
;;  3;
;;  x++;
;; if (x==1) x++;
;; if (x==1) {
;;    x++;
;; }
;; expression statements, declaration statements, and control flow statements


;; f(x+1, 3);
;; f(g(x+1), 3);

;; what is a block?
;; public static void f() { }
;; { {} }

;; what is an assignment?
;; what is a conditional?
;; what is a function definition?
;; what is an invocation

;; what expressions can be arguments to functions?
;; invocations, assignments maybe, literals, variables, ternaries maybe

;; expressions can contain other expressions
;; expressions cannot contain statements
;; statements can contain other statements
;; statements can contain expressions
;; block must contain statements (not expressions)

;; half Clojure half Java
(def expression
  (s/rewrite
    ;; operator usage
    (operator ?op ?x ?y)
    (j/operator ?op (m/app expression ?x) (m/app expression ?y))

    (operator ?op ?x ?y)
    (j/operator ?op (m/app expression ?x) (m/app expression ?y))

    ;; TODO: these shouldn't be necessary, kalai_constructs will have handled them... try removing
    (invoke clojure.core/deref ?x)
    (m/app expression ?x)

    ;; TEMPORARY
    (invoke atom ?x)
    (m/app expression ?x)

    ;; function invocation
    (invoke ?f . !args ...)
    (j/invoke ?f [(m/app expression !args) ...])

    ;; TODO: lambda function
    (lambda ?name ?docstring ?body)
    (j/lambda ?name ?docstring ?body)

    ;; conditionals as an expression must be ternaries, but ternaries cannot contain bodies
    (if ?condition ?then)
    (j/ternary (m/app expression ?condition) (m/app expression ?then) nil)

    (if ?condition ?then ?else)
    (j/ternary (m/app expression ?condition) (m/app expression ?then) (m/app expression ?else))

    ?x
    ?x))

(def init
  (s/rewrite
    (init (m/and ?name (m/app meta {:t ?type :tag ?tag})))
    (j/init ~(or ?type ?tag) ?name)

    (init (m/and ?name (m/app meta {:t ?type :tag ?tag})) (m/app expression ?value))
    (j/init ~(or ?type ?tag) ?name (m/app expression ?value))))

(def statement
  (s/choice
    init
    (s/rewrite
      (return ?x)
      (j/expression-statement (j/return (m/app expression ?x)))

      (while ?condition . !body ...)
      (j/while (m/app expression ?condition)
               (j/block . (m/app statement !body) ...))

      (foreach & ?more)
      (j/for & ?more)

      ;; conditional
      ;; note: we don't know what to do with assignment, maybe disallow them
      (if ?test ?then)
      (j/if (m/app expression ?test)
        (j/block (m/app statement ?then)))

      (if ?test ?then ?else)
      (j/if (m/app expression ?test)
        (j/block (m/app statement ?then))
        (j/block (m/app statement ?else)))

      (do . !xs ...)
      (j/block . (m/app statement !xs) ...)

      (assign ?name ?value)
      (j/assign ?name (m/app expression ?value))

      ?else (j/expression-statement (m/app expression ?else)))))

(def function
  (s/rewrite
    ;; function definition
    (function ?name ?return-type ?docstring ?params . !body ...)
    (j/function ?return-type ?name ?docstring ?params
                (j/block . (m/app statement !body) ...))))

(def top-level-form
  (s/choice
    function
    init
    (s/rewrite
      ?else ~(throw (ex-info "Expected a top level form" {:else ?else})))))

(def rewrite
  (s/rewrite
    (namespace ?ns-name . !forms ...)
    (j/class ?ns-name
             (j/block . (m/app top-level-form !forms) ...))

    ?else ~(throw (ex-info "Expected a namespace" {:else ?else}))))
