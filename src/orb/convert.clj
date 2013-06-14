(ns orb.convert
  (:require [orgmode.core :as org]))

(defn ext-or-dir
  "Return the extension of f if a file, dir otherwise. Throw
  exceptions for non-existant or specialized files."
  [f]
  (letfn [(ext [n]
            (let [s (.toString n)
                  d (.lastIndexOf s ".")]
              (.substring s (inc d))))]
    (cond
     (.isFile f) (ext f)
     (.isDirectory f) "dir"
     (not (.exists f)) (throw (Exception. (str "File not found: " f)))
     :else (throw (Exception. (str "Not a file or directory: " f))))))

(defn change-ext [f x]
  "Return a (base) filename of f with extension changed to x"
  (clojure.string/replace (.getName f) #"(?<=\.).*?$" x))

(defmulti convert
  "Convert a given file if such a conversion exsits. Otherwise, return
  nil as the file requires no conversion" 
  ext-or-dir)

(defmethod convert "org" [f]
  "Using org/parse, return the data struture representing f"
  {:name (change-ext f "html")
   :conversion (future (org/parse f))})

(defmethod convert :default [f]
  "By default, no conversion is provided and nil is returned"
  nil)
