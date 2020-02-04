(ns main.quil-runner
  (:require [main.main :as m]
            [quil.core :as q])
  (:import (java.util Properties)))

(.. System (getProperties) (setProperty "javax.accessibility.assistive_technologies" ""))


(defonce restart-sketch? (atom false))


(when @restart-sketch?
  (q/defsketch proj
               :title "runner"
               :setup m/setup
               :draw m/draw
               :size [400 400]) 
  (reset! restart-sketch? false))
