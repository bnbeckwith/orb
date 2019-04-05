(ns orb.template
  (:require [me.raynes.laser :as l]
            [orb.config      :as cfg]
            [orb.file        :as f]
            [orb.util        :as u]))

(defn set-templates! [tdir]
  (swap! cfg/*siteconfig* 
         merge 
         {:templates 
          (apply 
           merge
           (f/fs-to-map tdir))}))
  
(defn get-template [n]
  (l/parse (get-in @cfg/*siteconfig* [:templates n]
                   (get-in @cfg/*siteconfig* [:templates :base] 
                           (-> (clojure.lang.RT/baseLoader)
                               (.getResourceAsStream "base.html"))))))

(defn blog-entry [e p]
  (l/at e
        (l/child-of (l/class= :entry-title) (l/element= :a))
        (l/content (get-in p [:conversion :attribs :title]))
        
        (l/child-of (l/class= :entry-title) (l/element= :a))
        (l/attr :href (get-in p [:url]))

        (l/class= :published)
        (l/content (u/make-date-str (get-in p [:conversion :attribs :date])))

        (and (l/class= :published)
             (l/class= :longdate))
        (l/content (u/make-date-str (get-in p [:conversion :attribs :date])))


        (l/child-of (l/class= :tag-list) (l/element= :li))
        #(for [tag (clojure.string/split (get-in p [:conversion :attribs :filetags] "") #"\s+")]
           (l/at %
               (l/element= :a) (l/content tag)
               (l/element= :a) (l/attr :href 
                                       (str "/tags/" 
                                            (clojure.string/lower-case tag) 
                                            "/"))))))

(defn generics [e ctx]
  (l/at e 
        (l/and
         (l/element= :meta)
         (l/attr= :name "description")) 
        (l/attr :content (get-in @cfg/*siteconfig* [:description]))
        
        (l/id= :nav-last)
        (comp (l/insert :right
                        (for [n (get-in @cfg/*siteconfig* [:nav-extra])]
                          (l/node :li :content
                                  (l/node :a :content (first n) :attrs {:href (second n)}))))
              (l/id ""))
        
        (l/and
         (l/element= :meta)
         (l/attr= :name "author")) 
        (l/attr :content (get-in ctx [:attribs :author] ""))
        
        (l/id= :sitename)
        (comp
         (l/attr :href "/")
         (l/content (get-in @cfg/*siteconfig* [:title])))
        
        (l/id= :copyright)
        (l/content (get-in @cfg/*siteconfig* [:copyright]))
        
        (l/or
         (l/element= :title)
         (l/id= :content-title))
        (l/content (get-in ctx [:attribs :title] ""))
        
        (l/id= :blogmeta)
        (if (get-in ctx [:conversion :attribs :filetags] false)
          #(blog-entry % ctx)
          (l/remove))

        (l/id= :featured)
        (if-let [f (:featured ctx)]
          (l/content f)
          (l/remove))

        (l/id= :extras)
        (if-let [f (:extras ctx)]
          (l/content f)
          (l/remove))))

(defn page [ctx] 
  (l/document (get-template :page)

              (l/element= :html)
              #(generics % ctx)

              (l/id= :content) 
              (l/content (l/parse-fragment (:html ctx)))
              
              (l/or
               (l/id= :summary-list)
               (l/id= :posts-list))
              (l/remove)))  

(defn blog-posts [ctx ps]
  (let [t (get-template :summary)]
    (l/document t

                (l/element= :html)
                #(generics % ctx)
                
                (l/child-of (l/id= :posts-list) (l/element= :li))
                #(for [p ps]
                   (blog-entry % p))
                (if (:older ctx)
                  [(l/id= :older)   (l/attr :href (:older ctx))]
                  [(l/id= :older)   (l/content "")])
                (if (:newer ctx)
                  [(l/id= :newer)   (l/attr :href (:newer ctx))]
                  [(l/id= :newer)   (l/content "")])
                (l/or
                 (l/id= "featured")
                 (l/id= "summary-list")
                 (l/id= "extras"))
                (l/remove))))

(defn summary-section [node n ps]
  (l/at node
        (l/element= :h2) (l/content (if (keyword? n) (name n) (str n)))
        (l/child-of (l/class= :summary-entries) (l/element= :li))
        #(for [p ps]
           (l/at %
                 (l/element= :a) 
                 (comp  (l/content (get-in p [:conversion :attribs :title] "FIXME"))
                        (l/attr :href (get-in p [:url] "FIXME")))))))

(defn summary [ctx gs]
  (let [t (get-template :summary)]
    (l/document t
                
                (l/element= :html)
                #(generics % ctx)

                (l/child-of (l/id= :summary-list) (l/element= :li))
                #(for [g (keys gs)]
                   (summary-section % g (get-in gs [g])))
                (l/or
                 (l/id= "featured")
                 (l/id= "posts-list")
                 (l/id= "extras"))
                (l/remove))))
