(ns read-this.formatting
  (:require [clojure.string :as s]))

(defn trunc
  [s n]
  (subs s 0 (min (count s) n)))

(defn pad-right [str1 str2]
  (let [l1 (count str1)
        l2 (count str2)
        length (max l1 l2)
        diff (Math/abs (- l1 l2))]
    (apply str (if (>= l1 l2)
                 [(str str1 " |\n") (str "|" (apply str (repeat length " ")) "|\n") (str str2 (apply str (repeat diff " ")) " |\n")]
                 [(str str1 (apply str (repeat diff " ")) " |\n") (str "|" (apply str (repeat length " ")) "|\n") (str str2 " |\n")]))))

(defn cap [text]
  (let [str-l (- (count text) 3)]
    (str "+" (apply str (repeat str-l "-")) "+\n")))

(defn divider [text]
  (str (apply str (repeat (+ (count text) 2) "-")) "\n"))

(defn source-frame
  [k]
  (let [source-s (str "| " (s/upper-case (name k)) " |\n")
        cap (cap source-s)]
    (str "\n" cap source-s cap)))

(defn str-titles
  [v]
  (reduce (fn [acc m] (let [raw-title (str "| " (:title m))
                            title (if (> (count raw-title) 137)
                                    (str (trunc raw-title 137) "...")
                                    raw-title)
                            raw-url (str "| " (:url m))
                            url (if (> (count raw-url) 137)
                                  (str (trunc raw-url 137) "...")
                                  raw-url)
                            longest-str (if (>= (count title) (count url)) title url)
                            divider (divider longest-str)]
                        (str acc  divider (pad-right title url) divider))) "" v))

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
