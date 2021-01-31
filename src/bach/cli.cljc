(ns bach.cli
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clojure.string :as string]
            [bach.ast :refer [parse]]
            [bach.track :as track :refer [compose]]
            [bach.data :as data :refer [to-json]])
  #?(:clj (:gen-class)))

(def cli-options
  [["-i" "--input DATA" "The bach file to use as input"
    :id :input
    :default ""]
   ["-h" "--help"]])

(def handlers
  {:parse (fn [data]
            (let [input (:input data)
                  output (-> input slurp parse)]
              (println output)))
   :compile (fn [data]
              (let [input (:input data)
                    output (-> input
                               slurp
                               compose
                               to-json)]
                (println output)))})

(defn help [options]
  (->> ["bach is a semantic musical notation"
        ""
        "Usage: bach [options] action"
        ""
        "Options:"
        options
        ""
        "Actions:"
        "  parse        Parses UTF-8 encoded bach data into an Abstract Syntax Tree (AST)"
        "  compile      Compiles UTF-8 bach data into bach.json, an easy to interpret JSON micro-format"]
       (string/join \newline)))

(defn error-msg [errors]
  (str "There were errors processing the command line arguments\n\n"
       (string/join \newline errors)))

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn -main [& args]
  "bach"
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    (cond
      (:help options) (exit 0 (help summary))
      (not= (count arguments) 1) (exit 1 (help summary))
      errors (exit 1 (error-msg errors)))
    (let [handler ((keyword (first arguments)) handlers)]
      (if handler
        (handler options)
        (exit 0 (help summary))))))
