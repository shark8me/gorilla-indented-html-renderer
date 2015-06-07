(ns html-indent.core
  (:import
           [java.io StringWriter])
  (:require [net.cgrand.enlive-html :as html]
            [hiccup.core :as hc]
            [gorilla-renderable.core :refer [render Renderable]]
            [clojure.xml :as x]
            [clojure.zip :as z]
            [zip.visit :as zv]
            [clojure.core.match :refer [match]]
            [clojure.java.io :as io]))
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}
;; <=

;; @@
;test strings
(def s "<div><a><m>mcontent</m></a><b id='ddd'></b></div>")
(def s1 "<div><a></a>con2<b id='ddd'></b>con1<bc id='ddd'></bc></div>")
(def root (z/xml-zip (x/parse (java.io.ByteArrayInputStream. (.getBytes s)))))
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-var'>#&#x27;html-view/root</span>","value":"#'html-view/root"}
;; <=

;; @@
(defn pre [{:keys [tag content attrs] :as m}]
  (do ;(println "pre " tag)
	(if (not (nil? tag))  		 
          (let [con (first content)
                m1 {:tag tag :state :open}
                m2 (if (nil? attrs) m1 (assoc m1 :attrs attrs))]
          	(if (and (instance? java.lang.String con) (not (nil? con)))
            	 (assoc m2 :content con) m2)))))

(defn post [{:keys [tag content attrs] :as m}]
  (do ;(println "post " tag)
  (if (not (nil? tag))
  	{:tag tag :state :closed})))

(defn slashopen [{:keys [tag content attrs] :as m}]
  (if (instance? clojure.lang.PersistentStructMap (first content))
    :n))
(defn slashclose [{:keys [tag content attrs] :as m}]
  (if (not (nil? tag)) :n))
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-var'>#&#x27;html-view/slashclose</span>","value":"#'html-view/slashclose"}
;; <=

;; @@
(defn indentopen [evt {:keys [tag content attrs] :as n} {:keys [prev ] :as m} ind]
  (let [preventry (last prev)
         istr (match [(first preventry) evt]
     [:pre :pre] (inc ind)
     [:post :post] (dec ind)
                      :else ind)]
    (if (nil? tag) ["" istr]
  [(clojure.string/join (take istr (repeat " "))) istr])))

  
(defn vis 
  [evt {:keys [tag content attrs] :as n} {:keys [s prev ind] :as m}]
  (try
    (let [[indentstr indent-val] (indentopen evt n m ind)]
  {:state {:s (conj s (cond 
    (= evt :pre) [indentstr (pre n) (slashopen n)]
    (= evt :post) [ indentstr (post n) (slashclose n)]
    )) :prev (conj prev [evt tag]) :ind indent-val}})
    (catch IllegalArgumentException e
        (println "got exception " n)))
  )

(comment
(def k
(let [res
(filter (comp not (and nil? empty?)) (:s (:state (zv/visit root {:s [] :prev [[:post :x]] :ind 0} [vis]))))
   res2 (filterv (fn[[x y]] (and ((comp not nil?) x) ((comp not empty?)x) ))
          res) ]
 res2))

(def k2
(filter (fn[[x y z]] (not (nil? y))) 
        (:s (:state (zv/visit root {:s [] :prev [[:post :x]] :ind 0} [vis])))))
k2
  )

;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}
;; <=

;; @@
(defn redfun3 [{:keys [accu prevstate] :as m} [indent {:keys [tag] :as t} n]]
    (let [curstate (if (= n :n) :closed :open)
          m2 (assoc t :indent indent)]
      ;(println " accu " accu " prevstate " prevstate " tag " tag " n " n " curstate " curstate)
  (assoc m 
    :accu     (match [prevstate curstate]
                      [:closed :closed] (conj accu [m2])
                      [:closed :open] (conj accu [m2])
      				  [:open :closed] (conj (pop accu) [(last (last accu)) m2]))
    :prevstate curstate )))

(comment 
(def k3 
  (:accu 
  (reduce redfun3 {:prevstate :closed :accu []} k2)))
k3
  )
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}
;; <=

;; @@
(defn emit-element 
  "emits a single tag, either starting or ending, given a map "
  [{:keys [indent tag state content attrs] :as m}]
  (with-open [w (StringWriter.)]
    ;(.write w indent)
    (if (= state :open)
        (do 
          (.write w (str "<" (name tag)))
          (if attrs
            (doseq [attr attrs]
              (.write w (str " " (name (key attr)) "='" (val attr)"'"))))
          (.write w ">")
          (if content
            (.write w content))
          )
      (.write w (str "</" (name tag) ">"))
		)
      (.toString w)))

(defn emit-elem-group
  [i]
  (let [ind (:indent (i 0))]
    (str ind (clojure.string/join (mapv emit-element i)))
    ))


(defn get-html-content
  [s]
  (let [root (z/xml-zip (x/parse (java.io.ByteArrayInputStream. (.getBytes s))))
         k2	(filter (fn[[x y z]] (not (nil? y))) 
        	(:s (:state (zv/visit root {:s [] :prev [[:post :x]] :ind 0} [vis]))))
         k3 (:accu (reduce redfun3 {:prevstate :closed :accu []} k2))]
  (mapv emit-elem-group k3)
  ))

;(get-html-content s)

;; @@
;; =>
;;; {"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;&lt;div&gt;&quot;</span>","value":"\"<div>\""},{"type":"html","content":"<span class='clj-string'>&quot; &lt;a&gt;&quot;</span>","value":"\" <a>\""},{"type":"html","content":"<span class='clj-string'>&quot;  &lt;m&gt;mcontent&lt;/m&gt;&quot;</span>","value":"\"  <m>mcontent</m>\""},{"type":"html","content":"<span class='clj-string'>&quot; &lt;/a&gt;&quot;</span>","value":"\" </a>\""},{"type":"html","content":"<span class='clj-string'>&quot; &lt;b id=&#x27;ddd&#x27;&gt;&lt;/b&gt;&quot;</span>","value":"\" <b id='ddd'></b>\""},{"type":"html","content":"<span class='clj-string'>&quot;&lt;/div&gt;&quot;</span>","value":"\"</div>\""}],"value":"[\"<div>\" \" <a>\" \"  <m>mcontent</m>\" \" </a>\" \" <b id='ddd'></b>\" \"</div>\"]"}
;; <=

;; @@
(defrecord HtmlIndentView [contents opts])
(defn- escape-html
  [str]
  ;; this list of HTML replacements taken from underscore.js
  ;; https://github.com/jashkenas/underscore
  (clojure.string/escape str {\& "&amp;", \< "&lt;", \> "&gt;", \" "&quot;", \' "&#x27;"}))

(defn- span-render
  [thing]
  {:type :html
   :content (str "<span class='unk'>" (escape-html thing) "</span>")
   :value (pr-str thing)})

(defn view [contents & opts]
  (HtmlIndentView. contents opts))

(extend-type HtmlIndentView
  Renderable
  (render [self]
     {:type :list-like
     :open (str "<span class='clj-nil'></span>")
     :close "<span class='clj-nil'></span>"
     :separator "\n"
     :items (map span-render (get-html-content (:contents self)))
	 :value (pr-str self)}
            ))
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}
;; <=

;; @@
(view (hc/html [:div [:span {:id "abcd"}] [:span {:id "content2"}]]))
;; @@
;; =>
;;; {"type":"list-like","open":"<span class='clj-nil'></span>","close":"<span class='clj-nil'></span>","separator":"\n","items":[{"type":"html","content":"<span class='unk'>&lt;div&gt;</span>","value":"\"<div>\""},{"type":"html","content":"<span class='unk'> &lt;span id=&#x27;abcd&#x27;&gt;&lt;/span&gt;</span>","value":"\" <span id='abcd'></span>\""},{"type":"html","content":"<span class='unk'> &lt;span id=&#x27;content2&#x27;&gt;&lt;/span&gt;</span>","value":"\" <span id='content2'></span>\""},{"type":"html","content":"<span class='unk'>&lt;/div&gt;</span>","value":"\"</div>\""}],"value":"#html_view.HtmlIndentView{:contents \"<div><span id=\\\"abcd\\\"></span><span id=\\\"content2\\\"></span></div>\", :opts nil}"}
;; <=

;; @@
(view s1)
;; @@
;; =>
;;; {"type":"list-like","open":"<span class='clj-nil'></span>","close":"<span class='clj-nil'></span>","separator":"\n","items":[{"type":"html","content":"<span class='unk'>&lt;div&gt;</span>","value":"\"<div>\""},{"type":"html","content":"<span class='unk'> &lt;a&gt;&lt;/a&gt;</span>","value":"\" <a></a>\""},{"type":"html","content":"<span class='unk'> &lt;b id=&#x27;ddd&#x27;&gt;&lt;/b&gt;</span>","value":"\" <b id='ddd'></b>\""},{"type":"html","content":"<span class='unk'> &lt;bc id=&#x27;ddd&#x27;&gt;&lt;/bc&gt;</span>","value":"\" <bc id='ddd'></bc>\""},{"type":"html","content":"<span class='unk'>&lt;/div&gt;</span>","value":"\"</div>\""}],"value":"#html_view.HtmlIndentView{:contents \"<div><a></a>con2<b id='ddd'></b>con1<bc id='ddd'></bc></div>\", :opts nil}"}
;; <=

;; @@

;; @@
