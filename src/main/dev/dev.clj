(ns dev.dev
    (:require [midje.repl :as mr]
              [animator.main :as am]
              [sound-analysis.sound-interface :as si]
              [sound-analysis.onset-detection]
              [light-communications.ard-comms :as ac]
              [light-communications.color-utils :as cu]
              [sc.api :as scc]
              [mount.core :as mount]
              [animator.prebuilt]
              [clojure.tools.namespace.repl :as tn]
              [clojure.repl :as rpl]))


(defn start []
  (mount/start))             ;; example on how to start app with certain states

(defn stop []
  (mount/stop))

(defn refresh []
  (tn/refresh :after 'dev.dev/go))

(defn refresh-all []
  (stop)
  (tn/refresh-all))

(defn go
  "starts all states defined by defstate"
  []
  (start)
  :ready)

(defn reset
  "stops all states defined by defstate, reloads modified source files, and restarts the states"
  []
  (stop)
  (tn/refresh :after 'dev.dev/go))

(comment (start)
         sound-analysis.sound-interface/input
         (sound-analysis.sound-interface/fft-anal)
         (mount/start #'sound-analysis.sound-interface/amp)
         (mount/rollback!))