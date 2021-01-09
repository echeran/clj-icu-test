(ns kalai.pass.rust.a-syntax
  (:require [kalai.util :as u]
            [meander.strategy.epsilon :as s]
            [meander.epsilon :as m]))

(declare statement)

(def expression
  (s/rewrite
    ;; Data Literals
    ;;;; vector []
    (m/and [!x ...]
           ?expr
           (m/app meta ?meta)
           (m/let [?t (:t ?meta)
                   ?tmp (u/tmp ?t ?expr)]))
    ;;->
    (r/block
      (r/init ?tmp (r/new ?t))
      . (r/expression-statement (r/method push ?tmp (m/app expression !x))) ...
      ?tmp)

    ;;;; map {}
    (m/and {}
           ?expr
           (m/seqable [!k !v] ...)
           (m/app meta ?meta)
           (m/let [?t (:t ?meta)
                   ?tmp (u/tmp ?t ?expr)]))
    ;;->
    (r/block
      (r/init ?tmp (r/new ?t))
      . (r/expression-statement (r/method insert ?tmp
                                          (m/app expression !k)
                                          (m/app expression !v))) ...
      ?tmp)

    ;;;; set #{}
    (m/and #{}
           ?expr
           (m/seqable !k ...)
           (m/app meta ?meta)
           (m/let [?t (:t ?meta)
                   ?tmp (u/tmp ?t ?expr)]))
    ;;->
    (r/block
      (r/init ?tmp (r/new ?t))
      . (r/expression-statement (r/method insert ?tmp (m/app expression !k))) ...
      ?tmp)

    ;; Interop
    (new ?c . !args ...)
    (r/new ?c . (m/app expression !args) ...)

    ;; operator usage
    (operator ?op . !args ...)
    (r/operator ?op . (m/app expression !args) ...)

    ;; function invocation
    (invoke ?f . !args ...)
    (r/invoke ?f . (m/app expression !args) ...)

    (method ?method ?object . !args ...)
    (r/method ?method (m/app expression ?object) . (m/app expression !args) ...)

    ;; TODO: lambda function
    (lambda ?name ?docstring ?body)
    (r/lambda ?name ?docstring ?body)

    ;; faithfully reproduce Clojure semantics for do as a collection of
    ;; side-effect statements and a return expression
    (do . !x ... ?last)
    (r/block
      . (m/app statement !x) ...
      (m/app expression ?last))

    ;; let

    ;; TODO: how to do this? maybe through variable assignment?
    (case ?x {& (m/seqable [!k [_ !v]] ...)})
    (r/switch (m/app expression ?x)
              (r/block . (r/case !k (r/expression-statement (m/app expression !v))) ...))

    ?else
    ?else))

(def init
  (s/rewrite
    (init ?name)
    (r/init ?name)

    (init ?name ?x)
    (r/init ?name (m/app expression ?x))))

(def top-level-init
  (s/rewrite
    (init ?name)
    (r/init (m/app u/maybe-meta-assoc ?name :global true))

    (init ?name ?x)
    (r/init (m/app u/maybe-meta-assoc ?name :global true) (m/app expression ?x))))

(def statement
  (s/choice
    init
    (s/rewrite
      (return ?x)
      (r/expression-statement (r/return (m/app expression ?x)))

      (while ?condition . !body ...)
      (r/while (m/app expression ?condition)
               (r/block . (m/app statement !body) ...))

      (foreach ?sym ?xs . !body ...)
      (r/foreach ?sym (m/app expression ?xs)
                 (r/block . (m/app statement !body) ...))

      ;; conditional
      (if ?test ?then)
      (r/if (m/app expression ?test)
        (r/block (m/app statement ?then)))

      (if ?test ?then ?else)
      (r/if (m/app expression ?test)
        (r/block (m/app statement ?then))
        (r/block (m/app statement ?else)))

      (case ?x {& (m/seqable [!k [_ !v]] ...)})
      (r/switch (m/app expression ?x)
                (r/block . (r/case !k (r/expression-statement (m/app expression !v))) ...))

      (do . !xs ...)
      (r/block . (m/app statement !xs) ...)

      (assign ?name ?value)
      (r/assign ?name (m/app expression ?value))

      ?expr
      (r/expression-statement (m/app expression ?expr)))))

(def function
  (s/rewrite
    ;; function definition
    (function ?name ?params . !body ...)
    (r/function ?name ?params
                (r/block . (m/app statement !body) ...))))

(def top-level-form
  (s/choice
    function
    top-level-init
    (s/rewrite
      ?else ~(throw (ex-info "Expected a top level form" {:else ?else})))))

(def rewrite
  (s/rewrite
    (namespace ?ns-name . !forms ...)
    (r/module . (m/app top-level-form !forms) ...)

    ?else ~(throw (ex-info "Expected a namespace" {:else ?else}))))
