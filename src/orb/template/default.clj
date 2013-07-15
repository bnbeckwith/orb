(ns orb.template.default
  (:require [net.cgrand.enlive-html :as html]
            [orgmode.core :as org]
            [orgmode.html :as oh]))

(html/defsnippet head "orb/template/snippets.html" [:head]
  [ctx]
  [(and (html/has [:meta]) (html/attr-has :name "author"))] 
  (html/set-attr :content (get-in ctx [:attribs :author]))
  [(and (html/has [:meta]) (html/attr-has :name "description"))] 
  (html/set-attr :content (get-in ctx [:attribs :description]))
  [:title] (html/content (get-in ctx [:attribs :title])))

(html/deftemplate container "orb/template/page.html" 
  [ctx content]
  [:head]     (html/substitute (head ctx))
  [:#title]   (html/content (get-in ctx [:attribs :title]))
  [:#author]  (html/content (get-in ctx [:attribs :author]))
  [:#date]    (html/content (get-in ctx [:attribs :date]))
  [:#content] content)
  
(html/deftemplate post "orb/template/post.html" [ctx]
  [:head]     (html/content (head ctx))
  [:#title]   (html/content (get-in ctx [:attribs :title]))
  [:#author]  (html/content (get-in ctx [:attribs :author]))
  [:#date]    (html/content (get-in ctx [:attribs :date]))
  [:#content] (html/html-content (org/convert ctx)))

(html/deftemplate page "orb/template/page.html" [ctx]
  [:head]     (html/content (head ctx))
  [:#title]   (html/content (get-in ctx [:attribs :title]))
  [:#author]  (html/content (get-in ctx [:attribs :author]))
  [:#date]    (html/content (get-in ctx [:attribs :date]))
  [:#content] (html/html-content (org/convert ctx)))

(html/defsnippet idx-entries "orb/template/snippets.html" [:#idx-entry]
  [entries]
  [:div.entry]
  (html/clone-for [entry entries]
                  [:h2 :a] (html/content (:title entry))
                  [:h2 :a] (html/set-attr :href (:link entry))
                  [:p]     (html/content (:body  entry))))

(html/deftemplate index "orb/template/index.html" [title entries]
  [:title] (html/content title)
  [:#title] (html/content title)
  [:div.entry] (html/clone-for [{:keys [title body]} entries]
                          [:h2 :a] (html/content title)
                          [:p] (html/content body)))


(html/deftemplate archive "orb/template/archive.html" [ctx]
  identity)

(defn make-page [ctx]
  (let [content (html/html-content (org/convert ctx))]
    (apply str (container ctx content))))

(defn make-index [ctx entries]
  (let [content (html/content (idx-entries entries))]
    (apply str (container ctx content))))
