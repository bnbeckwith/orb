(ns orb.template
  (:require [me.raynes.laser :as l]))

(defn render [elements])

(defn page [ctx] 
  (l/document (l/parse (-> (clojure.lang.RT/baseLoader)
                           (.getResourceAsStream "base.html")))
              (l/and
               (l/element= :meta)
               (l/attr= :name "description")) 
              (l/attr :content "BNBECKWITH SITE, BABY")
               
              (l/and
               (l/element= :meta)
               (l/attr= :name "author")) 
              (l/attr :content "ME, BABY")
               
               (l/id= "content") 
               (l/content ctx)
               
               (l/or
                (l/id= "featured")
                (l/id= "extras")) 
               (l/remove)))

(defn post [ctx]
  (l/document (l/parse (-> (clojure.lang.RT/baseLoader)
                           (.getResourceAsStream "base.html")))
              (l/id= :content) (l/content ctx)
              (l/or
               (l/id= "featured")
               (l/id= "extras")) (l/remove)))

(defn blog-entry [e p]
  (l/at e
        (l/child-of (l/class= :entry-title) (l/element= :a))
        (l/content (get-in p [:conversion :attribs :title]))
        
        (l/child-of (l/class= :entry-title) (l/element= :a))
        (l/attr :href (get-in p [:url]))

        (l/child-of (l/class= :tag-list) (l/element= :li))
        #(for [tag (clojure.string/split (get-in p [:conversion :attribs :filetags] "") #"\s+")]
           (l/at %
               (l/element= :a) (l/content tag)
               (l/element= :a) (l/attr :href (str "/tags/" tag "/"))))))
  

(defn blog-posts [ctx ps]
  (let [t (l/parse (-> (clojure.lang.RT/baseLoader)
                       (.getResourceAsStream "base.html")))]
    (l/document t
                (l/id= :title) (l/content (get-in ctx [:attribs :title]))
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
                 (l/element= :a) (l/content (get-in p [:conversion :attribs :title] "FIXME"))
                 (l/element= :a) (l/attr :href (get-in p [:url] "FIXME"))))))

(defn summary [ctx gs]
  (let [t (l/parse (-> (clojure.lang.RT/baseLoader)
                       (.getResourceAsStream "base.html")))]
    (l/document t
                (l/id= :title) (l/content (get-in ctx [:attribs :title] "FIXME"))
                (l/child-of (l/id= :summary-list) (l/element= :li))
                #(for [g (keys gs)]
                   (summary-section % g (get-in gs [g])))
                (l/or
                 (l/id= "featured")
                 (l/id= "posts-list")
                 (l/id= "extras"))
                (l/remove))))

;; (defn make-template-fns [{:keys [snippets base page post index archive]
;;                           :or {snippets "base.html"
;;                                base     "base.html"
;;                                page     "base.html"
;;                                post     "base.html"
;;                                index    "base.html"
;;                                archive  "base.html"}}
;;                          sitemeta]
;;   {:base (html/template base [ctx]
;;                         [:head] (html/substitute ((get-in ctx [:templates :head]) ctx))
;;                         [:#banner]  (html/substitute ((get-in ctx [:templates :header]) ctx))
;;                         [:#contentinfo]  (html/substitute ((get-in ctx [:templates :footer]) ctx))
;;                         [:#title]   (html/content (get-in ctx [:attribs :title]))
;;                         [:#featured] (html/substitute ((get-in ctx [:templates :featured]) ctx))
;;                         [:#extras]  (html/substitute ((get-in ctx [:templates :extras]) ctx))
;;                         [:#content] (html/content (:basecontent ctx)))

;;    :head (html/snippet snippets [:head]
;;                        [ctx]
;;                        [(and (html/has [:meta]) (html/attr-has :name "author"))] 
;;                        (html/set-attr :content (get-in ctx [:attribs :author]))
;;                        [(and (html/has [:meta]) (html/attr-has :name "description"))] 
;;                        (html/set-attr :content (get-in ctx [:attribs :description]))
;;                        [:title] (html/content (get-in ctx [:attribs :title])))
;;    :header (html/snippet snippets [:#banner] [ctx] 
;;                          [:#sitename] (html/do-> 
;;                                        (html/content (get-in sitemeta [:title]))
;;                                        (html/set-attr :href (get-in sitemeta [:baseurl]))))
;;    :featured (html/snippet snippets 
;;                            [:#featured] [ctx] 
;;                            #(when-let [c (get-in % [:featured])]
;;                               (html/content c)))
;;    :extras (html/snippet snippets 
;;                          [:#extras] [ctx] 
;;                          #(when-let [c (get-in % [:extras])]
;;                             (html/content c)))
;;    :copyright (html/snippet snippets [:#copyright] [ctx]
;;                             (html/content (str "©" (.get (java.util.Calendar/getInstance) java.util.Calendar/YEAR) (get-in sitemeta [:author]))))
;;    :footer (html/snippet snippets [:#contentinfo] [ctx] 
;;                          #(if-let [c (get-in % [:footer])]
;;                             (html/content c)
;;                             ((get-in ctx [:templates :copyright]) ctx)))
;;    :post  (let [bc (html/snippet post [:article.hentry] [ctx]
;;                                  [:.entry-title]   (html/content (get-in ctx [:attribs :title]))
;;                                  [:.fn]  (html/content (get-in ctx [:attribs :author]))
;;                                  [:.published] (html/content (get-in ctx [:attribs :date]))
;;                                  [:.entry-content] (html/html-content (:html ctx)))]
;;             (fn [ctx] ((get-in ctx [:templates :base]) (merge ctx {:basecontent (bc ctx)}))))
;;    :page  (let [bc (html/snippet post [:article.hentry] [ctx]
;;                                  [:.entry-title]   (html/content (get-in ctx [:attribs :title]))
;;                                  [:footer] (html/substitute nil)
;;                                  [:.published] (html/content (get-in ctx [:attribs :date]))
;;                                  [:.entry-content] (html/html-content (:html ctx)))]
;;             (fn [ctx] ((get-in ctx [:templates :base]) (merge ctx {:basecontent (bc ctx)}))))

;;    :post-tags (html/snippet snippets [:ul.tag-list] 
;;                             [tagstr]
;;                             [:li]
;;                             (html/clone-for [t (clojure.string/split tagstr #"\s+")]
;;                                             [:a]
;;                                             (html/do-> (html/content t)
;;                                                        (html/set-attr :href (str "/tags/" (clojure.string/lower-case t))))))
                                             
;;    :index (fn [ctx entries]
;;             (let [idx (html/snippet index [:#posts-list]
;;                                     [entries]
;;                                     [:li] 
;;                                     (html/clone-for 
;;                                      [e entries]
;;                                      [:h2.entry-title :a] 
;;                                      (html/do-> (html/content (get-in e [:conversion :attribs :title]))
;;                                                 (html/set-attr :href (get-in e [:url])))
;;                                      [:div.entry-content] (html/content (get-in e [:conversion :attribs :description]))
;;                                      [:ul.tag-list]
;;                                       (if-let [ts (get-in e [:conversion :attribs :filetags])]
;;                                         (html/substitute ((get-in ctx [:templates :post-tags]) ts))
;;                                         (html/substitute nil))
;;                                      [:abbr.published]
;;                                      (html/do-> (html/content (get-in e [:conversion :attribs :date]))
;;                                                 (html/set-attr :title (get-in e [:conversion :attribs :date])))))]
;;               ((get-in ctx [:templates :base]) (merge ctx {:basecontent (idx entries)}))))
;;    :summary-entries (fn [name entries]
;;                       (let [s (html/snippet snippets [:ol.summary-entries [:li (html/nth-of-type 1)]] [es]
;;                                             [:li] (html/clone-for [e es]
;;                                                              [:li :a.summary-entry-link]
;;                                                              (html/do-> (html/content (get-in e [:conversion :attribs :title]))
;;                                                                         (html/set-attr :href (get-in e [:url])))))]
;;                         (s entries)))
;;    :summary (fn [ctx groups]
;;               (let [s (html/snippet snippets [:#summary-list]
;;                                     [gs]
;;                                     [:li]
;;                                     (html/clone-for 
;;                                      [k (keys gs)
;;                                       :let [es (get gs k "FIXME")
;;                                             n (if (keyword? k) (name k) k)]]
;;                                      [:h2.summary-type] (html/content (clojure.string/capitalize n))
;;                                      [:ol.summary-entries]
;;                                      (html/content ((get-in ctx [:templates :summary-entries]) n es))))]
;;                 ((get-in ctx [:templates :base]) (merge ctx {:basecontent (s groups)}))))
;;    :archive (html/template archive [ctx]
;;                            identity)})


