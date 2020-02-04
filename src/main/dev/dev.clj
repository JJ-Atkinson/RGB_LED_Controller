(ns dev.dev
    (:require [midje.repl :as mr]
              [animator.main :as am]
              [sound-analysis.sound-interface :as si]
              [light-communications.ard-comms :as ac]
              [light-communications.color-utils :as cu]
              [sc.api :as scc]
              [clojure.repl :as rpl]))


(defn start! []
  (ac/open!)
  ;(si/)
  (am/start-loop! nil)
)

(defn printing-meta [f]
  (binding [*print-meta* true]
    f))


(defn scc-locals [ep-id]
  (-> ep-id scc/ep-info :sc.ep/local-bindings))

(comment
  (mr/autotest :files "src/main" "src/fiddles")
  (start!)
  
  (do (ac/close!)
      (ac/open!)))



