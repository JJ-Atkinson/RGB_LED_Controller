(ns main.main
  (:require [quil.core :as q])
  #_(:import ()))






(defn setup [])


(defn draw []
  (q/fill (q/mouse-x))
  (q/rect (q/mouse-x) (q/mouse-y) 10 10 ))



