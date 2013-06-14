(ns orb.convert
  (:require [orgmode.core :as org]))


(defn ext-or-dir
  "Return the extension of f if a file, dir otherwise"
  [f]
  (if (.isFile f)
    (let [s (.toString f)
          d (.lastIndexOf s ".")]
      (.substring s (inc d)))
    "dir"))

(defmulti convert-type 
  "Convert a given type (extension) of file. For each type of the
  methods, return a map of {filename conversion}" 
  first)

(defn convert 
  "Work through the :source-files of cfg and provide a conversion"
  [cfg]
  (assoc cfg :conversion
         (apply merge
                (let [fs (group-by 
                          ext-or-dir
                          (:source-files cfg))]
                  (for [kv fs]
                    {(first kv) (convert-type kv)})))))
  
(defmethod convert-type "org" [[_ fs]]
  (apply merge
         (for [o fs]
           (try 
             {(.toString o) 
              {:org (future (org/parse o))}}
             (catch Exception e 
               (println (str "Whoops: " o " " e)))))))

(defmethod convert-type :default [[_ & fs]]
  (flatten fs))
