(ns kalai.emit.impl.rust-test
  (:require [kalai.common :refer :all]
            [kalai.emit.api :refer :all]
            [kalai.emit.langs :as l]
            [kalai.emit.impl.util.curlybrace-util :as cb-util]
            [kalai.testing :as testing]
            [clojure.tools.analyzer.jvm :as az]
            [expectations.clojure.test :refer :all])
  (:import kalai.common.AstOpts))

;;
;; Rust
;;

(reset-indent-level)

;; bindings

;; bindings - def

(defexpect bindings-def 
  (let [ast (az/analyze '(def x 3))]
    (expect
"lazy_static! {
  static ref x = 3;
}"
     (emit (map->AstOpts {:ast ast :lang ::l/rust}))))
  (let [ast (az/analyze '(def ^Integer x 5))]
    (expect
"lazy_static! {
  static ref x: i32 = 5;
}" (emit (map->AstOpts {:ast ast :lang ::l/rust})))))

;; language - multiple expressions

;; language - multiple expressions - do block

(defexpect lang-mult-expr-do-block
  (let [ast (az/analyze '(do (def x 3) (def y 5)))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/rust}))
["lazy_static! {
  static ref x = 3;
}"
"lazy_static! {
  static ref y = 5;
}"])) 
  (let [ast (az/analyze '(do (def ^Boolean x true) (def ^Long y 5)))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/rust}))
["lazy_static! {
  static ref x: bool = true;
}"
"lazy_static! {
  static ref y: i64 = 5;
}"])))

;; bindings

;; bindings - atoms

(defexpect bindings-atoms
  (let [ast (az/analyze '(def x (atom 11)))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/rust}))
"lazy_static! {
  static ref mut x = 11;
}")))

;; bindings - reset!

(defexpect bindings-reset
  (let [ast (az/analyze '(do (def x (atom 11)) (reset! x 13)))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/rust}))
["lazy_static! {
  static ref mut x = 11;
}"
 "x = 13;"]))
  (let [ast (az/analyze '(do (def ^Long x (atom 11)) (reset! x 13)))]
    (expect (emit {:ast ast :lang ::l/rust})
["lazy_static! {
  static ref mut x: i64 = 11;
}"
 "x = 13;"])))

;; bindings - let

;; bindings - let - 1 expression

(defexpect bindings-let-1-expr
  (let [ast (az/analyze '(let [x 1] (+ x 3)))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/rust}))
"{
  let x = 1;
  x + 3;
}")))

;; bindings - let - 1 expression - type signature on symbol

(defexpect bindings-let-1-expr-type-signature
  (let [ast (az/analyze '(let [^Integer x 1] (+ x 3)))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/rust}))
"{
  let x: i32 = 1;
  x + 3;
}")))

;; bindings - let - 2 expressions

(defexpect bindings-let-2-expr
  (let [ast (az/analyze '(let [x 1] (+ x 3) (+ x 5)))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/rust}))
"{
  let x = 1;
  x + 3;
  x + 5;
}")))

;; bindings - let - 2 bindings

(defexpect bindings-let-2-bindings
  (let [ast (az/analyze '(let [x 1 y 2] (* x y)))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/rust}))
"{
  let x = 1;
  let y = 2;
  x * y;
}")))

;; bindings - let - 2 bindings - expression in binding

(defexpect bindings-let-2-bindings-with-exprs
  (let [ast (az/analyze '(let [x 5 y (* x x)] (+ x y)))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/rust}))
"{
  let x = 5;
  let y = x * x;
  x + y;
}")))

;; bindings - let - nesting of let forms

(defexpect bindings-let-nested
  (let [ast (az/analyze '(let [x 5] (let [y (* x x)] (/ y x))))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/rust}))
"{
  let x = 5;
  {
    let y = x * x;
    y / x;
  }
}")))

;; bindings - let - atom (as bound value)

(defexpect bindings-let-atom
  (let [ast (az/analyze '(let [a (atom 23)] (+ 3 5)))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/rust}))
"{
  let mut a = 23;
  3 + 5;
}")))

;; bindings - let - atom (as bound value) and reset!

(defexpect bindings-let-atom-with-reset
  (let [ast (az/analyze '(let [a (atom 23)] (reset! a 19)))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/rust}))
"{
  let mut a = 23;
  a = 19;
}")))

;; bindings - let - atom (as bound value) and reset! - type signature

(defexpect bindings-let-atom-with-reset-type-signature
  (let [ast (az/analyze '(let [^Integer a (atom 23)] (reset! a 19)))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/rust}))
"{
  let mut a: i32 = 23;
  a = 19;
}")))

;; language - nested operands

(defexpect lang-nested-operands
  (let [ast (az/analyze '(+ 3 5 (+ 1 7) 23))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/rust})) "3 + 5 + (1 + 7) + 23"))
  (let [ast (az/analyze '(/ 3 (/ 5 2) (/ 1 7) 23))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/rust})) "3 / (5 / 2) / (1 / 7) / 23"))
  (let [ast (az/analyze '(let [x 101] (+ 3 5 (+ x (+ 1 7 (+ x x))) 23)))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/rust}))
"{
  let x = 101;
  3 + 5 + (x + (1 + 7 + (x + x))) + 23;
}"))


  (let [ast (az/analyze '(/ 3 (+ 5 2) (* 1 7) 23))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/rust})) "3 / (5 + 2) / (1 * 7) / 23")))

;; defn

(defexpect defn-test
  (let [ast (az/analyze '(defn compute ^void [^Integer x ^Integer y] (let [^Integer a (+ x y)] (* a y))))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/rust}))
"pub fn compute(x: i32, y: i32)
{
  {
    let a: i32 = x + y;
    a * y;
  }
}"))

  (let [ast (az/analyze '(defn doStuff ^void [^Integer x ^Integer y] (str (+ x y)) (println "hello") 3))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/rust}))
"pub fn doStuff(x: i32, y: i32)
{
  format!(\"{}\", (x + y).to_string());
  println!(\"{}\", format!(\"{}\", String::from(\"hello\")));
  3;
}"))

  (let [ast (az/analyze '(defn returnStuff ^Integer [^Integer x ^Integer y] (let [^Integer a (+ x y)] (return a))))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/rust}))
"pub fn returnStuff(x: i32, y: i32) -> i32
{
  {
    let a: i32 = x + y;
    return a;
  }
}")))

;; classes

(defexpect classes
  (do
    (require '[kalai.common :refer :all])
    (let [ast (az/analyze '(defclass "MyClass" (def ^Integer b 3) (defn x ^void [] (+ b 1))))]
      (expect (emit (map->AstOpts {:ast ast :lang ::l/rust}))
"lazy_static! {
  static ref b: i32 = 3;
}

pub fn x()
{
  b + 1;
}"))))

;; enums

(defexpect enums
  (do
    (require '[kalai.common :refer :all])
    (let [ast (az/analyze '(defenum "Day"
                             SUNDAY MONDAY TUESDAY WEDNESDAY THURSDAY FRIDAY SATURDAY))]
      (expect (emit (map->AstOpts {:ast ast :lang ::l/rust}))
"enum Day
{
  SUNDAY,
  MONDAY,
  TUESDAY,
  WEDNESDAY,
  THURSDAY,
  FRIDAY,
  SATURDAY
}"))))

;; fn invocations

(defexpect strlen-test
  (let [ast (az/analyze '(do (def ^String s "Hello, Martians") (strlen s)))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/rust}))
["lazy_static! {
  static ref s: String = String::from(\"Hello, Martians\");
}"
 "s.len();"])))

;; loops (ex: while, doseq)

(defexpect loops
  (let [ast (az/analyze '(while true (println "e")))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/rust}))
"while (true)
{
  println!(\"{}\", format!(\"{}\", String::from(\"e\")));
}")))

;; other built-in fns (also marked with op = :static-call)

(defexpect get-test
  (let [ast (az/analyze '(do (def ^{:mtype [Map [String Integer]]} numberWords {"one" 1
                                                                                "two" 2
                                                                                "three" 3})
                             (get numberWords "one")))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/rust}))
["lazy_static! {
  static ref mut numberWords: HashMap<String,i32> = HashMap::new();
  numberWords.insert(String::from(\"one\"), 1);
  numberWords.insert(String::from(\"two\"), 2);
  numberWords.insert(String::from(\"three\"), 3);
}"
 "*numberWords.get(&String::from(\"one\")).unwrap();"])))

(defexpect nth-test
  (let [ast (az/analyze '(do (def ^{:mtype [List [Integer]]} numbers [13 17 19 23])
                             (nth numbers 2)))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/rust}))
["lazy_static! {
  static ref numbers: Vec<i32> = vec![13, 17, 19, 23];
}"
 "numbers[2 as usize];"]
)))

;; not

(defexpect not-test
  (let [ast (az/analyze '(not (= 3 (/ 10 2))))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/rust}))
            "!(3 == (10 / 2))")))

;; new

(defexpect new-test
  (let [ast (az/analyze '(StringBuffer.))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/rust}))
            "StringBuffer()"))
  (let [ast (az/analyze '(StringBuffer. "Initial string value"))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/rust}))
            "StringBuffer(String::from(\"Initial string value\"))")))

;; string buffer - new

(defexpect stringbuffer-new
  (let [ast (az/analyze '(new-strbuf))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/rust}))
            "String::new().chars().collect()"))
  (let [ast (az/analyze '(atom (new-strbuf)))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/rust}))
            "String::new().chars().collect()")) 
  (let [ast (az/analyze '(let [^StringBuffer sb (atom (new-strbuf))] sb))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/rust}))
"{
  let mut sb: Vec<char> = String::new().chars().collect();
  sb;
}"))
  (let [ast (az/analyze '(new-strbuf "hello"))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/rust}))
            "String::from(\"hello\").chars().collect()")))

;; string buffer - insert

(defexpect stringbuffer-insert-char
  (let [ast (az/analyze '(let [^StringBuffer sb (atom (new-strbuf))] (insert-strbuf-char sb 0 \x)))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/rust}))
"{
  let mut sb: Vec<char> = String::new().chars().collect();
  sb.insert(0, 'x');
}")))

(defexpect stringbuffer-insert-string
  (with-redefs [cb-util/new-name (partial testing/new-name-testing-fn 1)]
    (let [ast (az/analyze '(let [^StringBuffer sb (atom (new-strbuf))] (insert-strbuf-string sb 0 "hello")))]
      (expect (emit (map->AstOpts {:ast ast :lang ::l/rust}))
"{
  let mut sb: Vec<char> = String::new().chars().collect();
  let mut sb_temp1: Vec<char> = String::from(\"hello\").chars().collect();
  sb.splice(0..0, sb_temp1);
}"))))

;; string buffer - length

(defexpect stringbuffer-length
  (with-redefs [cb-util/new-name (partial testing/new-name-testing-fn 1)]
    (let [ast (az/analyze '(let [^StringBuffer sb (atom (new-strbuf))]
                                           (insert-strbuf-string sb 0 "hello")
                                           (length-strbuf sb)))]
                    (expect (emit (map->AstOpts {:ast ast :lang ::l/rust}))
"{
  let mut sb: Vec<char> = String::new().chars().collect();
  let mut sb_temp1: Vec<char> = String::from(\"hello\").chars().collect();
  sb.splice(0..0, sb_temp1);
  sb.len();
}"))))

;; string buffer - prepend

(defexpect stringbuffer-prepend
  (with-redefs [cb-util/new-name (partial testing/new-name-testing-fn 1)]
    (let [ast (az/analyze '(let [^StringBuffer sb (atom (new-strbuf))] (prepend-strbuf sb "hello")))]
      (expect (emit (map->AstOpts {:ast ast :lang ::l/rust}))
              "{
  let mut sb: Vec<char> = String::new().chars().collect();
  let mut sb_temp1: Vec<char> = String::from(\"hello\").chars().collect();
  sb.splice(0..0, sb_temp1);
}"))))

;; string - equals

(defexpect str-eq-test
  (let [ast (az/analyze '(do (def ^String s1 "house")
                             (def ^String s2 "home")
                             (str-eq s1 s2)))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/rust}))
["lazy_static! {
  static ref s1: String = String::from(\"house\");
}"
"lazy_static! {
  static ref s2: String = String::from(\"home\");
}"
 "s1 == s2;"])))

;; sequential collection - length

(defexpect seq-length-test
  (let [ast (az/analyze '(do (def ^{:mtype [List [Character]]} formattedDigits []) (seq-length formattedDigits)))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/rust}))
["lazy_static! {
  static ref formattedDigits: Vec<char> = vec![];
}"
 "formattedDigits.len();"])))

;; sequential collection - append

(defexpect seq-append-test
  (let [ast (az/analyze '(do (def ^{:mtype [List [Character]]} formattedDigits []) (seq-append formattedDigits \1)))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/rust}))
["lazy_static! {
  static ref formattedDigits: Vec<char> = vec![];
}"
 "formattedDigits.push('1');"])))

;; demo code

(defexpect demo
  (with-redefs [cb-util/new-name (partial testing/new-name-testing-fn 1)]
    (let [ast (az/analyze '(defclass "NumFmt"
                             (defn format2 ^String [^Integer num]
                               (let [^Integer i (atom num)
                                     ^StringBuffer result (atom (new-strbuf))]
                                 (while (not (= @i 0))
                                   (let [^Integer quotient (quot @i 10)
                                         ^Integer remainder (rem @i 10)]
                                     (prepend-strbuf @result (str remainder))
                                     (reset! i quotient)))
                                 (return (tostring-strbuf @result))))))]
      (expect (emit (map->AstOpts {:ast ast :lang ::l/rust}))
"pub fn format2(num: i32) -> String
{
  {
    let mut i: i32 = num;
    let mut result: Vec<char> = String::new().chars().collect();
    while (!((i) == 0))
    {
      {
        let quotient: i32 = (i) / 10;
        let remainder: i32 = (i) % 10;
        let mut result_temp1: Vec<char> = format!(\"{}\", (remainder).to_string()).chars().collect();
        result.splice(0..0, result_temp1);
        i = quotient;
      }
    }
    return result.into_iter().collect();
  }
}")))
  ;; TODO: make emitters for args to a static call / function call invoke discard the parens around derefs.
  ;; Then this test should be removed, and test above can have a simplified output.
  (with-redefs [cb-util/new-name (partial testing/new-name-testing-fn 1)]
    (let [ast (az/analyze '(defclass "NumFmt"
                             (defn format2 ^String [^Integer num]
                               (let [^Integer i (atom num)
                                     ^StringBuffer result (atom (new-strbuf))]
                                 (while (not (= i 0))
                                   (let [^Integer quotient (quot i 10)
                                         ^Integer remainder (rem i 10)]
                                     (prepend-strbuf @result (str remainder))
                                     (reset! i quotient)))
                                 (return (tostring-strbuf @result))))))]
      (expect (emit (map->AstOpts {:ast ast :lang ::l/rust}))
"pub fn format2(num: i32) -> String
{
  {
    let mut i: i32 = num;
    let mut result: Vec<char> = String::new().chars().collect();
    while (!(i == 0))
    {
      {
        let quotient: i32 = i / 10;
        let remainder: i32 = i % 10;
        let mut result_temp1: Vec<char> = format!(\"{}\", (remainder).to_string()).chars().collect();
        result.splice(0..0, result_temp1);
        i = quotient;
      }
    }
    return result.into_iter().collect();
  }
}"))))

(defexpect contains-test
  (let [ast (az/analyze '(do
                           (def ^{:mtype [Map [String Integer]]} numberWords {"one" 1})
                           (contains? numberWords "ten")))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/rust}))
["lazy_static! {
  static ref mut numberWords: HashMap<String,i32> = HashMap::new();
  numberWords.insert(String::from(\"one\"), 1);
}"
 "numberWords.contains_key(&String::from(\"ten\"));"])))

(defexpect invoke-test
  (let [ast (az/analyze '(do
                           (defn f ^Integer [^Integer a1 ^Integer a2 ^Integer a3]
                             (return (+ a1 a2 a3)))
                           (f 1 2 3)))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/rust}))
            ["pub fn f(a1: i32, a2: i32, a3: i32) -> i32
{
  return a1 + a2 + a3;
}"
             "f(1, 2, 3);"])))
