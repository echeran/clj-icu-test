(ns kalai.pass.kalai.a-annotate-ast
  (:require [meander.strategy.epsilon :as s]
            [meander.epsilon :as m]
            [clojure.tools.analyzer.ast :as ast]
            [kalai.util :as u])
  (:import (clojure.lang IMeta)))

(def ref-vars
  #{#'atom
    #'ref
    #'agent})

(def ast-type
  (s/rewrite
    {:op :const
     :val ?val}
    ~(type ?val)

    {:op :invoke
     :fn {:var (m/pred ref-vars)}
     :args [?value . _ ...]}
    ~(ast-type ?value)

    {:op :with-meta
     :meta {:op :map
            :form {:t ?t :tag ?tag}}
     :as ?with-meta}
    ~(or ?t ?tag)

    ?else
    nil))

(defn propagate-ast-type [from to]
  (if (and (instance? IMeta to)
           (not (u/type-from-meta to)))
    (u/set-meta to :t (ast-type from))
    to))

(def substitute-aliased-types
  (s/rewrite
    ;; replace type aliases with their definition
    {:name (m/and (m/pred some? ?name)
                  (m/app meta {:as ?name-meta
                               :t  (m/pred symbol? ?t)}))
     :meta {:as   ?meta
            :keys [!as ..?n {:val :t} . !bs ..?m]
            :vals [!cs ..?n
                   {:var (m/app meta {:kalias (m/pred some? ?kalias)})}
                   . !ds ..?m]}
     &     ?ast}
    ;;->
    {:name ~(with-meta ?name (assoc ?name-meta :t ?kalias))
     :meta ?meta
     &     ?ast}

    ;; annotate vars with their var as metadata so they can be identified later in the pipeline
    {:op   :var
     :var  (m/pred some? ?var)
     :form (m/pred some? ?form)
     &     ?ast}
    ;;->
    {:op   :var
     :var  ?var
     :form (m/app u/set-meta ?form :var ?var)
     &     ?ast}

    ;; We propagate type information which is stored in metadata
    ;; from the the place where they are declared on a symbol
    ;; to all future usages of that symbol in scope.
    ;; When the type metadata is not provided and the type of the
    ;; initial value is known, we use the type of the value.
    ;; TODO: function call type inference would be nice
    {:op   :local
     :form ?symbol
     :env  {:locals {?symbol {:form ?symbol-with-meta
                              :init {:form ?init-form
                                     :as   ?init}}
                     :as     ?locals}
            :as     ?env}
     &     ?ast}
    ;;->
    {:op   :local
     :form ~(propagate-ast-type ?init ?symbol-with-meta)
     :env  ?env
     &     ?ast}

    ;; otherwise leave the ast as is
    ?else
    ?else))

;; TODO: Fix for files.
;; For some reason, az/analyze+eval and az/analyze-ns produce different ASTs
;; so our tests and examples don't produce the same AST.
(def erase-type-alias
  (s/rewrite
    {:op   :def
     :meta {:val {:kalias (m/pred some?)}}}
    ;;->
    nil

    ?else
    ?else))

(defn rewrite
  "There is contextual information in the AST that is not available in s-expressions.
  The purpose of this pass is to capture that information and modify the s-expressions to contain what we need."
  [ast]
  (-> ast
      (ast/prewalk substitute-aliased-types)
      erase-type-alias))
