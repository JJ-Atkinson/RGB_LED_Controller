(ns animation-f
  (:require [animator.dsl :as dsl]
            [light-communications.color-utils :as cu]

            [thi.ng.color.core :as thic]
            [thi.ng.math.core :as thim]
            [light-communications.ard-comms :as ac]
            [animator.main :as m]
            [sound-analysis.sound-interface :as si]
            [clojure.inspector :as ins]))


(comment
  (def animation
    (dsl/define-animation
      ;(dsl/isolate :green-background
      ;             (dsl/init-state {})
      ;             (dsl/decay :target-color thic/RED :rate 0.02)
                   ;(dsl/random-points :prob 0.05
                   ;                   #_(fn [env s] (max 0
                   ;                                    (- (/ (:amplitude env) 10) 0.008))))
                   ;(dsl/fill-solid :color thic/GREEN))
      ;(dsl/cull-black)
      ;(dsl/isolate :sprinkle-red
      ;             (dsl/init-state {})
      ;             (dsl/decay :rate 0.05)
      ;             (dsl/random-points :prob 0.04)
      ;             (dsl/fill-solid :color thic/WHITE)
      ;             (dsl/cull-black)
      ;             )
      (dsl/isolate :sweep)
      (dsl/init-state {})
      (dsl/decay :rate 0.05)
      (dsl/sweep-point :rate 1)
      (dsl/fill-solid :color thic/WHITE)
      (dsl/cull-black)

      ;(dsl/isolate :sprinkle-green
      ;             (dsl/init-state {})
      ;             (dsl/random-points :prob-fn
      ;                                (fn [env s] (max 0
      ;                                                 (- (/ (:amplitude env) 18) 0.008))))
      ;             (dsl/decay :target-color cu/black :rate 0.5)
      ;             (dsl/fill-solid :color cu/green)
      ;             (dsl/cull-black))
      ;(dsl/isolate :baseline-blue
      ;             (dsl/init-state {})
      ;             (dsl/random-points :prob 1)
      ;             (dsl/fill-solid :color cu/blue))
      #_(dsl/blend-normal [:sweep   #_:sprinkle-green ])))

  (ac/close!)
  (ac/open!)

  (def temp (atom nil))
  (ins/inspect-tree @temp)
  (m/stop-loop!)
  
  (println (->> @temp :sweep :rate))
  
  ;(ac/full-write! @temp)
  ;(def bytes-to-write (ac/writable-bytes @temp))
  ;(def idx (atom -1))
  ;(ac/write -2)
  ;(ac/write (aget ^bytes bytes-to-write (swap! idx inc)))
  ;(ac/write (range 5))
  ;@idx
  ;(println (take 10 (drop 357 (seq bytes-to-write))))

  ;(def x (-> @temp :sprinkle-white))

  (map #(println (cu/color->writable-vec (get @temp %))) (range 50))

  (m/reset-loop!
    (fn [state]
      (let [ns (animation
                 {:amplitude (si/curr-amplitude)}
                 state)]
        ;(reset! temp ns)
        ;(ac/full-write! ns)
        ;(println :hi)
        (ac/full-write! 
          (into {} 
                (map (fn [x] [x (thic/rgba (* x 1/60) (* x 1/60) (* x 1/60))])
                     (range 60))))
        ns))
    )
  
  m/runner
  
  
  (ac/full-write! {1 thic/GREEN 88 thic/BLUE 159 thic/RED 289 thic/MAGENTA})
  )
