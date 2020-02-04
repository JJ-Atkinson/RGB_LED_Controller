(ns light-communications.ard-comms
  (:require [light-communications.color-utils :as cu]
            [common.utils :as u]
            [thi.ng.color.core :as col]
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

(extend-protocol Bytable
  (class (byte-array 0))
  (to-bytes [this] this)

  Number
  (to-bytes [this] (byte-array 1 (.byteValue this)))

  clojure.lang.Sequential
  (to-bytes [this] (byte-array (count this) (map #(.byteValue ^Number %) this))))

(defn round-to-byte [^double x]
  (if x
    (do
      (when (= 1 x) (println "got err")
                    )
      (.byteValue ^Long (Math/round x)))
    0))


(defn create-port [] (doto (SerialPort. "/dev/ttyUSB0")
                       (.openPort)
                       (.setParams 1000000 8 1 SerialPort/PARITY_MARK false false)
                       (.setFlowControlMode SerialPort/FLOWCONTROL_NONE)))

(defonce port (atom nil))

;(defn close)


(defn ensure-!=-1 [^Byte n]
  (cond (= n -2) (byte -1)
        (= n 126) (byte 127)
        ;(> 128 n) (byte 0)
        :default n))

(defn open! []
  (swap! port #(if-not % (create-port) %)))
(defn close! []
  (when @port (.closePort @port))
  (reset! port nil))

(defn write [vs]
  #_(.writeBytes @port (to-bytes vs))
  (.writeInt @port ^byte vs))

(defn vectorize-colors-map [colors-map]
  (->> (mapcat #(-> colors-map (get % cu/black) cu/color->writable-vec) (range 300))
       (map ensure-!=-1)))

(defn writable-bytes [colors-map]
  (let [x (to-bytes (vectorize-colors-map colors-map))]
     (amap ^bytes x
                 idx
                 ret
                 (ensure-!=-1 (aget ^bytes x idx)))))

(defn full-write! [colors-map]
  (write -2)                                      ;; reset the arduino to the head
  (doall (map write (vectorize-colors-map colors-map))))


(comment
  (write (map round-to-byte (cu/color->writable-vec col/MAGENTA)))
  (write 1)
  (open!)
  (let [bs (to-bytes (range 10))]
    (amap ^bytes bs
          idx
          ret
          (ensure-!=-1 (aget ^bytes bs idx)))))
