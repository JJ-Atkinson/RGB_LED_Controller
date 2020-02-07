(ns animator.prebuilt
  (:require [animator.dsl :refer :all]
            [thi.ng.color.core :refer :all]
            [light-communications.color-utils :as cu]
            [light-communications.ard-comms :as ac]
            [animator.main :as m]
            [common.utils :as u]
            [medley.core :as mc]))

(defn spy-state [env s]

  (u/spy s))

(def blue-w-sweep
  (define-animation
    (isolate :green-background
             (init-state {})
             (random-points :prob 0.05)
             (fill-solid :color WHITE)
             (cull-black))
    (isolate :sprinkle-red
             (init-state {})
             (decay :rate 0.07)
             (random-points :prob-fn
                            (fn [env s] (max 0
                                             (- (/ (:amplitude env) 1.2) 0.030))))
             (fill-solid :color RED)
             (cull-black))
    (isolate :sweep
             (init-state {})
             (decay :rate 0.15)
             (sweep-point :rate 1)
             (fill-solid :color GREEN)
             (cull-black))
    
    (blend-normal [:green-background :sprinkle-red :sweep #_:sprinkle-green])))


(def bouncing-bars
  (define-animation
    (isolate :bounce
             (init-state {})
             (relative-points :dist-fn (fn [env s] (int (max 0
                                                             (* (:amplitude env) 80)))))
             (center-and-reflect)
             (fill-solid :color GREEN)
             ;spy-state
             (decay :rate 0.15)
             )
    (blend-normal [:bounce])))

(comment
  (ac/close!)
  (ac/open!)
  
  (m/reset-light-animation! #'blue-w-sweep)
  (m/reset-light-animation! #'bouncing-bars)
  m/runner)
