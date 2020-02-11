(ns sound-analysis.sound-interface
  (:require [medley.core :as m]
            [mount.core :refer [defstate]])
  (:import (processing.sound AudioIn
                             Amplitude FFT)
           (processing.core PApplet)))


(defonce pap (PApplet.))

(defstate ^{:on-reload :noop} input :start (doto (AudioIn. pap 1)
                                             (.start)
                                             )
          :stop (.stop input))



(defstate ^{:on-reload :noop} amp 
          :start (doto (Amplitude. pap)
                   (.input input)))

(def fft-band-count 1024)
(defstate ^{:on-reload :noop}
          fft :start (doto (FFT. pap fft-band-count)
                       (.input input)))

(defn curr-amplitude [] (.analyze amp))

(defn fft-anal []
  (.analyze fft)
  (seq (.-spectrum fft)))

