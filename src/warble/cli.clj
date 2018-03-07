(ns warble.cli
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clojure.string :as string]
            [warble.ast :as ast]
            [warble.track :refer [compile-track]]
            [warble.data :refer [to-json]])
  (:gen-class))

(def cli-options
  [["-i" "--input DATA" "The warble data to use as input"
    :id :input
    :default ""]
   ["-h" "--help"]])

(def handlers
  {:parse (fn [data]
            (let [input (:input data)
                  output (ast/parse (slurp input))]
              (println output)))
   :compile (fn [data]
              (let [input (:input data)
                    parsed (ast/parse (slurp input))
                    output (-> parsed compile-track to-json)]
                (println output)))})

(defn help [options]
  (->> ["warble is a pragmatic notation for representing musical tracks and loops"
        ""
        "Usage: warble [options] action"
        ""
        "Options:"
        options
        ""
        "Actions:"
        "  parse        Parses plain-text warble data into an Abstract Syntax Tree (AST)"
        "  compile      Compiles plain-text warble data into warble.json, an easy to interpret JSON micro-format"]
       (string/join \newline)))

(defn error-msg [errors]
  (str "There were errors processing the command line arguments\n\n"
       (string/join \newline errors)))

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn -main [& args]
  "warble"
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    (cond
      (:help options) (exit 0 (help summary))
      (not= (count arguments) 1) (exit 1 (help summary))
      errors (exit 1 (error-msg errors)))
    (let [handler ((keyword (first arguments)) handlers)]
      (if handler
        (handler options)
        (exit 0 (help summary))))))
