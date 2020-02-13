(ns animator.prebuilt
  (:require [animator.dsl :refer :all]
            [thi.ng.color.core :refer :all]
            [light-communications.color-utils :as cu]
            [light-communications.ard-comms :as ac]
            [animator.main :as m]
            [common.utils :as u]
            [medley.core :as mc]
            [sound-analysis.sound-interface :as si]
            [thi.ng.math.core :as thim]
            [thi.ng.color.presets :as pre]
            [thi.ng.color.core :as thic]
            [sound-analysis.onset-detection :as onset-detect]
            [thi.ng.color.core :as col]))

(defn spy-state [env s]
  (u/spy s))

(defn spy-env [env s]
  (println env)
  s)


(defn fft-env [e]
  (let [nfft (si/fft-anal)
        offt (:fft e)
        high-band (range 350 500)
        low-band (range 30  )]
    (assoc e :fft nfft
             :fft-flux (when offt (onset-detect/flux offt nfft))
             :fft-flux-high (when offt (onset-detect/flux offt nfft high-band))
             :fft-flux-low (when offt (onset-detect/flux offt nfft low-band)))))

(defn hihat-trigger-env [e]
  (let [ov (:hihat/old-val e 2)
        nv (-> (:fft e []) vec (get 110 0))]
    (assoc e :hihat? (> nv (* ov 10))
             :hihat/old-val nv)))

(defn amplitude-env [e]
  (assoc e :amplitude (si/curr-amplitude)))

(defn framecount-env [e]
  (update e :frames (fnil inc 0)))

(def all-env-fn (comp amplitude-env framecount-env hihat-trigger-env #'fft-env))

(def fft->brightness
  (define-animation
    (fn [e s]
      (let [mapx (fn [x] (if (> x 90) (+ x 30) x))]
        (into (or s {}) (map-indexed (fn [i x] (as-> (* x 20) v [(mapx i) (thic/rgba v v v)])))
              (or (:fft e) []))))
    ))


(def blue-w-sweep
  (define-animation
    (isolate :green-background
             (init-state {})
             (random-points :prob-fn
                            (fn [env s] (max 0
                                             (- (/ (:amplitude env) 6) 0.038))))
             (fill-random :colors (->> (select-keys pre/colors [:green :purple :blue]) vals (map int24)))
             (decay :rate 0.4)
             (cull-black))
    (isolate :sprinkle-red
             (init-state {})
             (decay :rate 0.27)
             (random-points :prob-fn
                            (fn [env s] (max 0
                                             (- (/ (:amplitude env) 0.9) 0.050))))
             (fill-solid :color GREEN)
             (cull-black))
    (isolate :sprinkle-green
             (init-state {})
             (decay :rate 0.27)
             (random-points :prob-fn
                            (fn [env s] (max 0
                                             (- (/ (:amplitude env) 3) 0.060))))
             (fill-solid :color BLUE)
             (cull-black))

    (isolate :wipe-white
             (init-state {})
             (decay :rate 0.7)
             (random-points :prob-fn
                            (fn [env s] (if (> (:amplitude env) 0.495) 1 0)))
             (fill-solid :color WHITE #_(rgba 180 70 70 25))
             (cull-black))
    ;(fn [e s] (println s) s)
    (isolate :sweep
             (init-state {})
             (decay :rate 0.15)
             (sweep-point :rate 1)
             (fill-solid :color GREEN)
             (cull-black))

    (isolate :bounce
             (init-state {})
             (relative-points :dist-fn (fn [env s] (int (max 0
                                                             (* (:amplitude env) 50))))
                              :fill-in-between? true)
             (center-and-reflect)
             (fill-solid :color (int24 (:purple pre/colors)))
             (decay :rate 0.13)
             (cull-black)
             )

    (isolate :sprinkles
             (n-points :count-fn (fn [env s] (let [count (-> env :fft-flux-high (or 0) double Math/abs (* 280))]
                                               (if (< 30 count)
                                                 (+ 5 (int (/ count 5))) 0))))
             (fill-solid :color GREEN)
             (cull-black)
             (decay :rate 0.7))


    (blend-normal [:wipe-white :green-background :bounce :sprinkles])))


(def bouncing-bars
  (define-animation
    (isolate :spice blue-w-sweep)
    (isolate :bounce
             (init-state {})
             (relative-points :dist-fn (fn [env s] (int (max 0
                                                             (* (:amplitude env) 90))))
                              )
             (center-and-reflect)
             (fill-solid :color GREEN)
             (decay :rate 0.15)
             )

    (isolate :wipe-green
             (init-state {})
             (decay :rate 0.7)
             (random-points :prob-fn
                            (fn [env s] (if (> (:amplitude env) 0.488) 1 0)))
             (fill-solid :color (rgba 0 255 0 50))
             (cull-black))
    (isolate :new-point
             (init-state {})
             (decay :rate 1)
             (n-points :count-fn (fn [e s] (if (:hihat? e) 4 0)))
             (fill-solid :color RED))

    (blend-normal [:bounce :new-point :wipe-green])))



(def flux-analysis
  (define-animation
    (isolate :level-shower-high
             (relative-points :dist-fn (fn [env s] (-> env :fft-flux-high (or 0) double Math/abs (* 280) (max 0) int)))
             (fill-solid :color WHITE)

             (decay :rate 0.3))

    (isolate :sprinkles
             (n-points :count-fn (fn [env s] (let [count (-> env :fft-flux-high (or 0) double Math/abs (* 280))]
                                               (if (< 30 count)
                                                 (+ 5 (int (/ count 5))) 0))))
             (fill-solid :color GREEN)
             (cull-black)
             (decay :rate 0.7))

    (isolate :sprinkles-low
             (n-points :count-fn (fn [env s] (let [count (-> env :fft-flux-low (or 0) double Math/abs (* 100))]
                                               (if (< 30 count)
                                                 (+ 5 (int (/ count 5))) 0))))
             (fill-solid :color (-> pre/colors :purple col/int24))
             (cull-black)
             (decay :rate 0.7))

    (isolate :level-shower-low
             (relative-points :dist-fn (fn [env s] (-> env :fft-flux-low (or 0) double Math/abs (* 100) (max 0) int)))
             (fill-solid :color RED)

             (decay :rate 0.3))
    (blend-normal [:level-shower-high :sprinkles  :sprinkles-low])))

(comment
  (ac/close!)
  ac/port
  


  (m/reset-light-animation! #'all-env-fn #'blue-w-sweep)
  (m/reset-light-animation! #'all-env-fn #'bouncing-bars)
  (m/reset-light-animation! #'all-env-fn #'fft->brightness)
  (m/reset-light-animation! #'all-env-fn #'flux-analysis)
  (m/stop-loop!)
  m/runner)
