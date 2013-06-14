(ns orb.template.default
  (:require [net.cgrand.enlive-html :as html]
            [orgmode.core :as org]
            [orgmode.html :as oh]))

(html/deftemplate post "orb/template/post.html" [ctx]
  [(and (html/has [:meta]) (html/attr-has :name "author"))] (html/set-attr :content (get-in ctx [:attribs "AUTHOR"]))
  [(and (html/has [:meta]) (html/attr-has :name "description"))] (html/set-attr :content (get-in ctx [:attribs "DESCRIPTION"]))
  [:title] (html/content (get-in ctx [:attribs "TITLE"]))
  [:#title]   (html/content (get-in ctx [:attribs "TITLE"]))
  [:#author]  (html/content (get-in ctx [:attribs "AUTHOR"]))
  [:#date]    (html/content (get-in ctx [:attribs "DATE"]))
  [:#content] (html/html-content (org/convert ctx)))

(html/deftemplate page "orb/template/page.html" [ctx]
  [:#title] (html/content (get-in ctx [:attribs "TITLE"]))
  [:#author]  (html/content (get-in ctx [:attribs "AUTHOR"]))
  [:#date]    (html/content (get-in ctx [:attribs "DATE"]))
  [:#content] (html/html-content (org/convert ctx)))
  
(defn make-page [ctx]
  (if (= "Blog" (get-in ctx [:attribs "CATEGORY"]))
    (apply str (post ctx))
    (apply str (page ctx))))