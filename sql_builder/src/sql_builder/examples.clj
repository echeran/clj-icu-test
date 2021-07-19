(ns sql-builder.examples
  (:require [sql-builder.core :as sql]))

(defn f1 ^{:t :string} []
  (let [^{:t {:mmap [:string :any]}} query-map {:select ^{:t {:mvector [:any]}} [:a :b :c]
                                                :from   ^{:t {:mvector [:any]}} [:foo]
                                                :where  ^{:t {:mvector [:any]}} [:= :f.a "baz"]}]
    (sql/format query-map)))