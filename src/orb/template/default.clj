(ns orb.template.default
  (:require [net.cgrand.enlive-html :as html]))

(defn make-template-fns [{:keys [snippets page post index archive]
                          :or {snippets "orb/template/snippets.html"
                               page     "orb/template/page.html"
                               post     "orb/template/post.html"
                               index    "orb/template/index.html"
                               archive  "orb/template/archive.html"}}
                         sitemeta]
  {:head (html/snippet snippets [:head]
                       [ctx]
                       [(and (html/has [:meta]) (html/attr-has :name "author"))] 
                       (html/set-attr :content (get-in ctx [:attribs :author]))
                       [(and (html/has [:meta]) (html/attr-has :name "description"))] 
                       (html/set-attr :content (get-in ctx [:attribs :description]))
                       [:title] (html/content (get-in ctx [:attribs :title])))
   :header (html/snippet snippets [:#header] [ctx] 
                         [:#sitename] (html/content (get-in sitemeta [:title]))
                         [:#sitename] (html/set-attr :href (get-in sitemeta [:baseurl]))
                         )
   :footer (html/snippet snippets [:#footer] [ctx] identity)
   :post (html/template post [ctx]
                        [:head]     (html/substitute ((get-in ctx [:templates :head]) ctx))
                        [:#header]  (html/substitute ((get-in ctx [:templates :header]) ctx))
                        [:#footer]  (html/substitute ((get-in ctx [:templates :footer]) ctx))
                        [:#title]   (html/content (get-in ctx [:attribs :title]))
                        [:#author]  (html/content (get-in ctx [:attribs :author]))
                        [:#date]    (html/content (get-in ctx [:attribs :date]))
                        [:#content] (html/html-content (:html ctx)))
   :page (html/template page [ctx]
                        [:head]     (html/substitute ((get-in ctx [:templates :head]) ctx))
                        [:#header]  (html/substitute ((get-in ctx [:templates :header]) ctx))
                        [:#footer]  (html/substitute ((get-in ctx [:templates :footer]) ctx))
                        [:#title]   (html/content  (get-in ctx [:attribs :title]))
                        [:#author]  (html/content (get-in ctx [:attribs :author]))
                        [:#date]    (html/content (get-in ctx [:attribs :date]))
                        [:#content] (html/html-content (:html ctx)))

   :idx-entries (html/snippet snippets [:#idx-entry]
                              [entries]
                              [:div.entry]
                              (html/clone-for [entry entries]
                                              [:h2 :a] (html/content (:title entry))
                                              [:h2 :a] (html/set-attr :href (:link entry))
                                              [:p]     (html/content (:body  entry))))
   :index (html/template index [ctx entries]
                         [:title] (html/content (:title sitemeta))
                         [:#title] (html/content (:title sitemeta))
                         [:div.entry] (html/clone-for 
                                       [e entries]
                                       [:h2 :a] (html/content (get-in e [:conversion :attribs :title]))
                                       [:h2 :a] (html/set-attr :href (get-in e [:url]))
                                       [:p] (html/content (get-in e [:conversion :attribs :description]))))
   :archive (html/template archive [ctx]
                           identity)})

