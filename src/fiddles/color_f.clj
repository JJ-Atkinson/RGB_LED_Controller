(ns color-f
  (:require [light-communications.color-utils :as cu]
            [thi.ng.color.core :as thic]
            [thi.ng.math.core :as thim]
            ))


(def red (thic/rgba 1 0 0))
(def blue (thic/rgba 0 0 1))
(def redhsl (thic/hsla 0 1 1))
(def greenhsl (thic/hsla (/ 120 360) 1 1))

(comment
  (cu/color->writable-vec (thim/mix red blue 0.1))

  @(thic/as-rgba blue)

  @(thic/as-hsva (thim/mix redhsl greenhsl))

  (cu/color->writable-vec (thic/as-hsva red))
  (col/brightness blue)
  
  (def c (atom thic/WHITE))
  
  @(swap! c #(thim/mix % cu/transparent 0.1))
  @(cu/overlay-colors [thic/WHITE thic/RED]))
