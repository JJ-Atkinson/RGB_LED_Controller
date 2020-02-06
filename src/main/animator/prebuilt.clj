(ns animator.prebuilt
  (:require [animator.dsl :refer :all]
            [thi.ng.color.core :refer :all]
            [light-communications.color-utils :as cu]
            [light-communications.ard-comms :as ac]))


(def blue-w-sweep
  (define-animation
    (isolate :green-background
             (init-state {})
             (random-points :prob 0.05)
             (fill-solid :color BLUE)
             (cull-black))
    (isolate :sprinkle-red
             (init-state {})
             (decay :rate 0.05)
             (random-points :prob-fn
                            (fn [env s] (max 0
                                             (- (/ (:amplitude env) 10) 0.008))))
             (fill-solid :color WHITE)
             (cull-black))
    (isolate :sweep
             (init-state {})
             (decay :rate 0.15)
             (sweep-point :rate 1)
             (fill-solid :color WHITE)
             (cull-black))



    (blend-normal [:sweep :sprinkle-red :green-background #_:sprinkle-green])))


(comment
  (ac/close!)
  (ac/open!)
  )
