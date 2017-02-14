(ns read-this.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [net.cgrand.enlive-html :as html]
            [cheshire.core :as json]
            [hiccup.page :refer [html5 include-css]]
            [hiccup.element :refer [link-to]]
            [read-this.formatting :as f])
  (:import [java.net URL]))

(def site-data {:3am {:title-selector [:ul :h3 :a]
                      :url (URL. "http://www.3ammagazine.com/3am/")
                      :protocol :http}
                :paris-review {:title-selector [:main :ul :li :article :h1 :a]
                               :url (URL. "https://www.theparisreview.org/blog/")
                               :protocol :https
                               :p-selector [:main :p]}
                #_:larb #_{:title-selector [:a :a :p]
                       :url (URL. "https://lareviewofbooks.org/essays/")
                       :protocol :https}
                :nyrb {:title-selector [:article :h2 :a]
                       :url (URL. "https://www.nybooks.com/daily/")
                       :protocol :https
                       :p-selector [:article :section :p]}
                :hacker-news {:title-selector [:td.title :a]
                              :url (URL. "https://news.ycombinator.com/")
                              :protocol :http}
                :cbc {:title-selector [:ul.moreheadlines-list :li :a]
                      :url (URL. "http://www.cbc.ca/world/")
                      :protocol :http}
                :r-nba {:title-selector [:p.title :a]
                        :url (URL. "https://www.reddit.com/r/nba/")
                        :protocol :https}
                :the-ringer {:title-selector [:a]
                             :url (URL. "https://theringer.com/nba/home")
                             :protocol :https}})

(def cs-sources [:hacker-news])
(def lit-sources [:3am :nyrb :paris-review])
(def news-sources [:cbc])
(def nba-sources [:r-nba :the-ringer])

(def all-sources (keys site-data))

(defn https-resource
  [url]
  (with-open [inputstream (-> url
                              .openConnection
                              (doto (.setRequestProperty "User-Agent"
                                                         "Mozilla/5.0 ..."))
                              .getContent)]
    (html/html-resource inputstream)))

(defmulti headline-url-map
  (fn [html type] type))

(defmethod headline-url-map :hacker-news
  [html type]
  (reduce (fn [coll x]
            (let [k (first (:content x))]
              (cond
                (and (string? k) (not= "More" k)) (conj coll {:title k :url (get-in x [:attrs :href])})
                :else coll)))
          [] html))

(defmethod headline-url-map :default
  [html type]
  (reduce (fn [coll x]
            (let [title (apply str (map html/text (:content x)))
                  url-sel (if (= type :r-nba) [:attrs :data-href-url] [:attrs :href])
                  href (get-in x url-sel)
                  url (cond (= type :cbc) (str "http://www.cbc.ca" href)
                            (and (= type :r-nba) (not= (first href) \h) (not= href nil) ) (str "https://www.reddit.com" href)
                            :else href)]
              (if (and url (not= "" title)) (conj coll {:title title :url url}) coll)))
          [] html))

(defn cbc-filter [m]
  (filter #(not (re-find #"commentwrapper" (:url %))) m))

(defn ringer-filter [v]
  (let [fakes #{"Jordan Ritter Conn" "Sign in / Sign up" "The Ringer NBA Show" "About The Ringer"}
        token-count (fn [s] (count (clojure.string/split s #" ")))]
    (reduce (fn [acc m] (if (or (contains? fakes (:title m))
                                (<= (token-count (:title m)) 2))
                          acc
                          (conj acc m)))
            [] v)))

(defmulti headline-grabber
  "grabs headlines to a given page passed as a key:
   Something like :3am or :larb or :nyrb"
  (fn [x] (get-in site-data [x :protocol])))

(defmethod headline-grabber :http
  [site]
  (let [url (get-in site-data [site :url])
        selector (get-in site-data [site :title-selector]) 
        headline-urls (-> url
                          html/html-resource
                          (html/select selector)
                          (headline-url-map site))]
    (cond (= site :cbc) (cbc-filter headline-urls) 
          :else headline-urls)))

(defmethod headline-grabber :https
  [site]
  (let [url (get-in site-data [site :url])
        selector (get-in site-data [site :title-selector])
        headline-urls (-> url
                          https-resource
                          (html/select selector)
                          (headline-url-map site))]
    (cond (= site :the-ringer) (ringer-filter headline-urls)
          :else headline-urls)))

(defmethod headline-grabber :default
  [site]
  "Oops, I couldn't find that source")

(defn get-article-parags
  [url-str source]
  (let [url (URL. url-str)
        p-selector (get-in site-data [source :p-selector])]
    (-> url
        https-resource
        (html/select p-selector))))

(defn read-article
  [url-str source]
  (let [article-text
        (cond
          (= source :paris-review) (->> (get-article-parags url-str source)
                                        (map html/text)
                                        (map #(str "    " %))
                                        (f/str-with-p))
          (= source :nyrb) (->> (get-article-parags url-str source)
                                (map html/text)
                                (map #(str "    " %))
                                (f/str-with-p))
          :else "Oops, I didn't find that article")]
    (html5 {:lang "en"}
           [:body
            [:pre article-text]])))

(defn get-headlines
  [sources]
  (reduce (fn [coll x] (assoc coll x (headline-grabber x))) {} sources))

(defn type->source-list [type]
  (cond (= type "cs") cs-sources
        (= type "lit") lit-sources
        (= type "news") news-sources
        (= type "nba") nba-sources
        :else all-sources))

(defn lucky-shot
  ([]
   (lucky-shot "all"))
  ([type]
   (let [source (rand-nth (type->source-list type))]
     (rand-nth (headline-grabber source)))))

(defn a-couple-articles
  [n type]
  (let [source-list (type->source-list type)
        articles (get-headlines source-list)]
    (reduce-kv (fn [coll k v]
                 (assoc coll k (into [] (take n v))))
               {} articles)))

(defn page [articles] (html5 {:lang "en"}
                             [:body
                              [:pre (reduce-kv (fn [acc k v] (str acc
                                                                  (f/source-frame k)
                                                                  (f/str-titles v)))
                                               "" articles)]]))

(defroutes app-routes
  (GET "/" [] (page (a-couple-articles 2 "all"))) 
  (GET "/from" {{:strs [source read]} :query-params}
       (cond
         (and source read) (read-article
                            (:url (nth (headline-grabber (keyword source)) (dec (Integer/parseInt read))))
                            (keyword source))
             source (page {(keyword source) (headline-grabber (keyword source))})
             :else (route/not-found "Provide source, or source and article number to read")))
  (GET "/ball" {{:strs [n]} :query-params} (page (a-couple-articles (if n (Integer/parseInt n) 4) "nba")))
  (GET "/lit" {{:strs [n]} :query-params} (page (a-couple-articles (if n (Integer/parseInt n) 4) "lit")))
  (GET "/news" {{:strs [n]} :query-params} (page (a-couple-articles (if n (Integer/parseInt n) 8) "news")))
  (GET "/tech" {{:strs [n]} :query-params} (page (a-couple-articles (if n (Integer/parseInt n) 8) "cs")))
  (GET "/lucky" {{:strs [t]} :query-params} (page {:Random-Pick [(lucky-shot (when t t))]}))
  (route/not-found "Not Found"))

(def app
  (wrap-defaults app-routes site-defaults))
