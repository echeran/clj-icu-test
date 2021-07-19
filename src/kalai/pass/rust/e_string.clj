(ns kalai.pass.rust.e-string
  (:require [meander.strategy.epsilon :as s]
            [meander.epsilon :as m]
            [clojure.string :as str]
            [camel-snake-kebab.core :as csk]
            [puget.printer :as puget]
            [clojure.java.io :as io]
            [kalai.types :as types]
            [kalai.util :as u])
  (:import (clojure.lang IMeta Keyword)
           (java.util Map Set Vector)))

(declare stringify)

;;;; These are helpers

(defn- parens [x]
  (str "(" x ")"))

(defn- comma-separated [xs]
  (str/join ", " xs))

(defn- params-list [params]
  (parens (comma-separated params)))

(defn- args-list [args]
  (parens (comma-separated (map stringify args))))

(defn- space-separated [& xs]
  (str/join " " xs))

(defn- line-separated [& xs]
  (str/join \newline xs))

(defn statement [s]
  (str s ";"))


;;;; These are what our symbols should resolve to

(defn expression-statement-str [x]
  (statement (stringify x)))

(defn block-str [& xs]
  (line-separated
    "{"
    (apply line-separated (map stringify xs))
    "}"))

(defn assign-str [variable-name value]
  (statement (str variable-name " = " (stringify value))))

;; TODO: do we need an :object type?
(def kalai-type->rust
  {:map     "PMap"
   :mmap    "std::collections::HashMap"
   :set     "PSet"
   :mset    "std::collections::HashSet"
   :vector  "PVector"
   :mvector "std::vec::Vec"
   :bool    "bool"
   :byte    "i8"
   :char    "char"
   :int     "i32"
   :long    "i64"
   :float   "f32"
   :double  "f64"
   :string  "String"
   :void    "()"
   :any     "kalai::Value"})

;; Forward declaration of `t-str` to break cycle of references.
;; We expect this not to create an infinite loop in practice, otherwise
;; the types specified in types/lang-types-mapping is not configured correctly.
(declare t-str)

(defn rust-type
  "`t` is either the :t value we would find in metadata (in the S-exprs) or
  would be data of the same form for a target-language specific type
  (ex: `:usize`)."
  [t]
  (or (get kalai-type->rust t)
      ;; TODO: breaking the rules for interop... is this a bad idea?
      (when t
        ;; TODO: refactor (move) the `get-in` call upstream in the pipeline so that
        ;; values for `:t` conform to a narrow consistent spec.
        (if-let [custom-rust-type-data (get-in types/lang-type-mappings [:kalai.emit.langs/rust t])]
          (t-str custom-rust-type-data)
          (if (keyword? t)
            (name t)
            (pr-str t))))
      types/TYPE-MISSING-STR))

(def t-str
  "?t may be a symbol, but could also be a
  a data structure, just as we might find in `:t ` of the metadata
  map upstream in the S-exprs."
  (s/rewrite
    {?t [& ?ts]}
    ~(str (rust-type ?t)
          \< (str/join \, (for [t ?ts]
                            (t-str t)))
          \>)

    ?t
    ~(str (rust-type ?t))))

(defn where [{:keys [file line column]}]
  (when file
    (str (.getName (io/file file)) ":" line ":" column)))

(defn maybe-warn-type-missing [t x]
  (when (str/includes? t types/TYPE-MISSING-STR)
    (binding [*print-meta* true]
      (println "WARNING: missing type detected" x
               (where (meta (:expr (meta x))))))))

(defn type-str [variable-name]
  (let [{:keys [t]} (meta variable-name)]
    (-> (t-str t)
        (doto (maybe-warn-type-missing variable-name)))))

(defn variable-name-type-str [variable-name]
  (let [{:keys [t mut ref]} (meta variable-name)]
    ;; let mut char_vec: &Vec<char> = ;
    ;; let char_vec: &Vec<char> = ;
    ;; let mut char_vec: std::vec::Vec<char> = ;
    ;;
    ;; fn f(char_vec: &mut std::vec::Vec<char>) {...
    ;; fn f(char_vec: &Vec<char>) {...
    ;; NOT POSSIBLE? (or doesn't make sense?): fn f(char_vec: mut std::vec::Vec<char>) {...
    (str (when mut "mut ") (csk/->snake_case variable-name)
         ;; Rust has type inference, so we can leave temp variable types off
         ;; TODO: probably don't want to use "MISSING_TYPE" though
         (when (not= t types/TYPE-MISSING-STR)
           (str ": " (when ref "&") (type-str variable-name))))))

(defn cast-str [identifier t]
  (space-separated (stringify identifier) "as" (t-str t)))

(defn init-str
  ([variable-name]
   (init-str variable-name nil))
  ([variable-name value]
   (let [{:keys [global]} (meta variable-name)]
     (if global
       (line-separated
         "lazy_static::lazy_static! {"
         (statement (space-separated "static ref"
                                     (variable-name-type-str variable-name)
                                     "="
                                     (stringify value)))
         "}")
       (statement (space-separated "let"
                    (variable-name-type-str variable-name)
                    "="
                    (stringify value)))))))

(defn invoke-str [function-name & args]
  (let [varmeta (some-> function-name meta :var meta)]
    (if (and (str/includes? (str function-name) "/") varmeta)
      (str "crate::"
           (csk/->snake_case (str/replace (str (:ns varmeta)) "." "::"))
           "::" (csk/->snake_case (:name varmeta))
           (args-list args))
      (str (csk/->snake_case function-name)
           (args-list args)))))

(defn function-str [name params body]
  (if (= '-main name)
    (do
      (assert (= '& (first params)) "Main method must have signature (defn -main [& args]...)")
      (str
        (space-separated 'pub 'fn 'main (params-list [])
                         (line-separated "{"
                                         (str "let " (csk/->snake_case (second params)) ": std::vec::Vec<String> = std::env::args().collect();")
                                         (stringify body)
                                         "}"))))
    (str
      (space-separated "pub" "fn"
                       (csk/->snake_case name))
      (space-separated (params-list (for [param params]
                                      (space-separated (str (csk/->snake_case param) ":")
                                                       (type-str param))))
                       "->"
                       (type-str params)
                       (stringify body)))))

(defn operator-str
  ([op x]
   (str op (stringify x)))
  ([op x & xs]
   (parens
     (apply space-separated
            (interpose op (map stringify (cons x xs)))))))

(def std-imports "use crate::kalai;")

(defn module-str [& forms]
  (apply line-separated
         std-imports
         (map stringify forms)))

(defn return-str [x]
  (space-separated 'return (stringify x)))

(defn while-str [condition body]
  (space-separated 'while (stringify condition)
                   (stringify body)))

(defn foreach-str [sym xs body]
  (space-separated 'for (space-separated sym "in" (stringify xs))
                   (stringify body)))

(defn if-str
  ([test then]
   (line-separated
     (space-separated 'if (stringify test))
     (stringify then)))
  ([test then else]
   (line-separated
     (space-separated 'if (stringify test))
     (stringify then)
     'else
     (stringify else))))

(defn ternary-str
  [test then else]
  (parens
    (space-separated
      (stringify test)
      "?" (stringify then)
      ":" (stringify else))))

(defn match-str
  [x clauses]
  (space-separated 'match (stringify x)
                   (stringify clauses)))

(defn arm-str [x then]
  (str (space-separated (stringify x) "=>" (str (stringify then) ","))))

(defn method-str [method object & args]
  (str (stringify object) "." method
    (when-let [t (-> method meta :t)]
      (str "::<" (t-str t) ">"))
    (args-list args)))

(defn new-str [t & args]
  (str (if (symbol? t)
         t
         (doto (-> t (keys) (first) (rust-type))
           (maybe-warn-type-missing t)))
       "::new"
       (args-list args)))

(defn literal-str [s]
  (pr-str s))

(defn lambda-str [args body]
  (str "|" (comma-separated (map csk/->snake_case args)) "|"
       (stringify body)))

(defn ref-str [s]
  (if (and (instance? IMeta s)
           (:ref (meta s)))
    ;; this prevents a ref from becoming a double-ref.
    ;; we probably don't want to be doing this?
    (stringify s)
    (str "&" (stringify s))))

(defn deref-str [s]
  (str "*" (stringify s)))

(defn range-str [start-idx end-idx]
  (str (stringify start-idx) ".." (stringify end-idx)))

(defn value-type
  "This is specifically for our custom rust Value enum for heterogeneous collections"
  [x]
  (if (nil? x)
    "Null"
    (if (instance? IMeta x)
      (let [{:keys [t]} (meta x)]
        (if (map? t)
          (case (-> t keys first)
            :map "PMap"
            :mmap "MMap"
            :set "PSet"
            :mset "MSet"
            :vector "PVector"
            :mvector "MVector"
            nil)
          (case t
            :bool "Bool"
            :byte "Byte"
            :int "Int"
            :long "Long"
            :float "Float"
            :double "Double"
            :string "String"
            nil)))
      (condp instance? x
        Byte "Byte"
        Boolean "Bool"
        Float "Float"
        Double "Double"
        Integer "Int"
        Long "Long"
        Keyword "String"
        String "String"
        Map "MMap"
        Set "MSet"
        Vector "MVector"
        nil))))

(defn value-str
  "Specifically for the Value enum for heterogeneous collections"
  [x]
  (if-let [t (value-type x)]
    (str "kalai::Value::" t (parens (stringify x)))
    (stringify x)))

;;;; This is the main entry point

(def str-fn-map
  {'r/module               module-str
   'r/operator             operator-str
   'r/function             function-str
   'r/invoke               invoke-str
   'r/init                 init-str
   'r/assign               assign-str
   'r/block                block-str
   'r/expression-statement expression-statement-str
   'r/return               return-str
   'r/while                while-str
   'r/foreach              foreach-str
   'r/if                   if-str
   'r/ternary              ternary-str
   'r/match                match-str
   'r/arm                  arm-str
   'r/method               method-str
   'r/new                  new-str
   'r/literal              literal-str
   'r/lambda               lambda-str
   'r/cast                 cast-str
   'r/ref                  ref-str
   'r/deref                deref-str
   'r/range                range-str
   'r/value                value-str})

(def stringify
  (s/match
    (?x . !more ... :as ?form)
    (let [f (get str-fn-map ?x)]
      (if f
        (apply f !more)
        (do
          (println "Inner form:")
          (puget/cprint ?form)
          (throw (ex-info (str "Missing function: " ?x)
                          {:form ?form})))))

    (m/pred keyword? ?k)
    (str "String::from(" (pr-str (str ?k)) ")")

    (m/pred char? ?c)
    (str \' ?c \')

    (m/pred string? ?s)
    (str "String::from(" (pr-str ?s) ")")

    nil
    "()"

    ;; identifier
    (m/pred symbol? ?s)
    (let [s-str (str ?s)
          snake-case (csk/->snake_case s-str)]
      (if (= \_ (first s-str))
        (str \_ snake-case)
        snake-case))

    ?else
    (pr-str ?else)))

(defn stringify-entry [form]
  (try
    (stringify form)
    (catch Exception ex
      (println "Outer form:")
      (puget/cprint form)
      (throw ex))))
