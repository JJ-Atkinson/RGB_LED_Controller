(ns sound-analysis.sound-interface
  (:require [medley.core :as m])
  (:import (processing.sound AudioIn
                             Amplitude)
           (processing.core PApplet)))


(defonce pap (PApplet.))

(defonce input (doto (AudioIn. pap 0)
                 (.start)))

(defonce amp (doto (Amplitude. pap)
               (.input input)))

(defn curr-amplitude [] (.analyze amp))