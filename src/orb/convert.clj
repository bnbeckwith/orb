(ns orb.convert
  (:require [orgmode.core :as org]
            [me.raynes.fs :as fs])
  (:use [plumbing.core]))

(defn ext-or-dir
  "Return the extension of f if a file, dir otherwise. Throw
  exceptions for non-existant or specialized files."
  [f] 
  (cond
   (fs/file? f) (second (fs/split-ext f))
   (fs/directory? f) "dir"
   (not (.exists f)) (throw (Exception. (str "File not found: " f)))
   :else (throw (Exception. (str "Not a file or directory: " f)))))

(defn make-date 
  "Takes a string date and return a Java Object"
  [s]
  (.. (java.text.SimpleDateFormat. "yyyy-MM-dd")
      (parse s)))

(defmulti convert
  "Convert a given file if such a conversion exsits. Otherwise, return
  nil as the file requires no conversion" 
  ext-or-dir)

(defmethod convert ".org" [f]
  "Using org/parse, return the data struture representing f"
  {:type :org
   :ext ".html"
   :conversion (org/parse f)})

(defmethod convert "dir" [f]
  "Set the type to be :dir"
  {:type :dir})

(defmethod convert :default [f]
  "By default, no conversion is provided and nil is returned"
  nil)


  
