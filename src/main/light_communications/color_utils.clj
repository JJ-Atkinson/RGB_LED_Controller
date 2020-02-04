(ns light-communications.color-utils
  (:require [medley.core :as mc]
            [thi.ng.color.core :as col]
            [thi.ng.math.core :as thim]
            [thi.ng.color.core :as thic]))



(defn color->writable-vec [color]
  (if color
    (as-> @(col/as-rgba color) [^double r ^double g ^double b a] 
          [(* 255 r) (* 255 g) (* 255 b)])
    [0 0 0]))

(comment
  (color->writable-vec col/GREEN))

(def black (col/rgba 0 0 0))
(def transparent (col/rgba 0 0 0 0))

(defn overlay-colors
  "Apply colors on top of each other"
  [colors]
  (reduce
    (fn [acc c] (thic/adjust-alpha (thim/mix acc c (col/alpha c)) 1))
    transparent
    colors))

(defn black? [color]
  (if-not (nil? color)
    (> 0.05 (col/brightness color))
    true))