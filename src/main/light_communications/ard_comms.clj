(ns light-communications.ard-comms
  (:require [light-communications.color-utils :as cu]
            [common.utils :as u]
            [thi.ng.color.core :as col]
            [medley.core :as mc]
            [clojure.pprint :as pp]
            )
  (:import (jssc SerialPort
                 SerialPortEvent
                 SerialNativeInterface
                 SerialPortEventListener)))

;; interop
;; good page - http://clojure-doc.org/articles/language/interop.html
;; if you want to implement events - http://clojure-doc.org/articles/language/interop.html#extending-java-classes-with-proxy
;; usage in processing - https://github.com/processing/processing/blob/349f413a3fb63a75e0b096097a5b0ba7f5565198/java/libraries/serial/src/processing/serial/Serial.java


(defprotocol Bytable
  (to-bytes [this] "Converts the type to bytes"))






(defn create-port [] (doto (SerialPort. "/dev/ttyUSB1")
                       (.openPort)
                       (.setParams 921600 8 1 SerialPort/PARITY_NONE)
                       (.setFlowControlMode SerialPort/FLOWCONTROL_NONE)
                       ))

(defonce port (atom nil))

;(defn close)


(defn ensure-!=-1 [n]
  (if (= n 254)
    255
    n))

(defn open! []
  (swap! port #(if-not % (create-port) %)))
(defn close! []
  (when @port (.closePort @port))
  (reset! port nil))

(defn write [vs]
  (.writeInt @port vs))

(defn read []
  (-> (.readBytes @port 1 ) seq first))

(defn purge [] (.purgePort @port SerialPort/PURGE_RXCLEAR))

(defn vectorize-colors-map [colors-map]
  (->> (mapcat #(-> colors-map (get % cu/black) cu/color->writable-vec) (range 300))
       (map ensure-!=-1)))



(defn full-write! [colors-map]
  (write -2)                                      ;; reset the arduino to the head
  (doall (map write (let [x (vectorize-colors-map colors-map)]
                      ;(println (take 105 x))
                      ;(println (type (first x)))
                      x))))


(comment
  
  (write 21)
  (read)

  (def k (reduce (fn [acc n]
                   (println n)
                   (write n)
                   ;(Thread/sleep 100)
                   (assoc acc n (read)))
                 {}
                 (range 255)))
  (do (doall (map (fn [n] (println n (bit-and (k n) 0xFF))) (range 255))) nil)
  (purge)
  (open!)
  (close!)
  (let [bs (to-bytes (range 10))]
    (amap ^bytes bs
          idx
          ret
          (ensure-!=-1 (aget ^bytes bs idx)))))
