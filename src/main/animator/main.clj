(ns animator.main
  (:require [light-communications.ard-comms :as ac]
            [sound-analysis.sound-interface :as si]
            [animator.dsl :as dsl]
            [clojure.inspector :as ins]
            [light-communications.color-utils :as cu]))


(defonce env (atom {:settings/framerate 0
                    }))


(defonce runner (atom nil))

(defn run-loop [animation-fn & {:keys [init-state max-runs] :or {init-state {} max-runs -1}}]
  (loop [s init-state
         runs-left max-runs]
    (Thread/sleep 20)
    (if (= runs-left 0)
      s
      (recur
        (animation-fn s)
        (dec runs-left)))))


(defn start-loop! [animation-fn]
  (swap! runner #(if % % (future (run-loop animation-fn)))))

(defn stop-loop! []
  (as-> @runner f (when f (future-cancel f)))
  (reset! runner nil))

(defn reset-loop! [animation-fn]
  (stop-loop!)
  (start-loop! animation-fn))


