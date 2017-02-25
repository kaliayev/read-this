(ns read-this.formatting
  (:require [clojure.string :as s] 
            [io.aviso.ansi :as colour]))

(defn trunc
  [s n]
  (subs s 0 (min (count s) n)))

(defn pad-right [m]
  (let [title-length (count (:mock-title m))
        url-length (count (:mock-url m))
        length (max title-length url-length)
        diff (Math/abs (- title-length url-length))]
    (apply str (if (>= title-length url-length)
                 [(str (:title m) " |\n") (str "|" (apply str (repeat length " ")) "|\n") (str (:url m) (apply str (repeat diff " ")) " |\n")]
                 [(str (:title m) (apply str (repeat diff " ")) " |\n") (str "|" (apply str (repeat length " ")) "|\n") (str (:url m) " |\n")]))))

(defn cap [text]
  (let [str-l (- (count text) 3)]
    (str "+" (apply str (repeat str-l "-")) "+\n")))

(defn divider [text]
  (str (apply str (repeat (+ (count text) 2) "-")) "\n"))

(defn source-frame
  [k]
  (let [source-title (str "| " (s/upper-case (name k)) " |\n") 
        formatted-title (str "| " (colour/magenta (s/upper-case (name k))) " |\n") 
        cap (cap source-title)]
    (str "\n" cap formatted-title cap)))

(defn colour-format
  [m]
  (let [trunker (fn [s] (if (> (+ 2 (count s)) 137)
                          (str (trunc s 137) "...")
                          s))
        title (trunker (:title m))
        url (trunker (:url m))]
    {:title (str "| " (colour/bold-red title))
     :mock-title (str "| " title)
     :url (str "| " (colour/cyan url))
     :mock-url (str "| " url)
     :divider (divider (if (>= (count title) (count url))
                         (str "| " title)
                         (str "| " url)))}))

(defn str-titles
  [v]
  (reduce (fn [acc m] (let [m (colour-format m)]
                        (str acc  (:divider m) (pad-right m) (:divider m)))) "" v))

(defn split-on-space [word] 
  (s/split word #"\s"))

(defn str-insert
  "Insert c at index i, in string s."
  [s c i]
  (str (subs s 0 i) c (subs s i)))

(def line-breaks #{\- \space})

(defn adj-break? [s i] (if (or (contains? line-breaks (nth s i)) (contains? line-breaks (nth s (dec i)))) true false))

(defn line-breaker
  [s]
  (if (< (count s) 95)
    s
    (str (trunc s 94) (if (adj-break? s 94) "\n" "-\n") (line-breaker (subs s 94)))))

(defn deep-trim [text]
  (->> text
       split-on-space
       (filter #(not (s/blank? %))) 
       (s/join " ")))

(defn str-with-p [strs] (apply str
                               (interleave (repeat "\n\n")
                                           (map line-breaker strs))))
