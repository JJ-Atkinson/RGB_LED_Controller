(ns animator.dsl
  (:require [light-communications.color-utils :as cu]
            [sc.api :as scc]
            [medley.core :as mc]
            [thi.ng.color.core :as col]
            [thi.ng.color.gradients :as cgr]
            [thi.ng.math.core :as thim]))

(def num-leds 300)
(def led-ids (range num-leds))


(defn define-animation [& animators]
  (fn [env state] (reduce (fn [s f] (f env s)) state animators)))

(defn isolate [tag & animations]
  (fn [env state] (update state tag #((apply define-animation animations) env %))))


(comment
  (def simple-anim
    (define-animation
      (fn [env s]
        (update s :counter-animation inc))
      (fn [env s]
        (assoc s :counter-animation-copy (:counter-animation s)))
      (isolate :animation-separate
               ^{:name :anim-sep-fn}
               (fn [env s]
                 (update s :counter #(+ (:count-inc env) %))))))


  (simple-anim {:count-inc 1} {:animation-separate {:counter 3}})

  (take 10 (iterate #(simple-anim {:count-inc 2} %) {:counter-animation  0
                                                     :animation-separate {:counter 0}})))


;; Data structure example for animation

#_{1           cu/black
   2           cu/green
   ..          ..
   :env        {:frame-count ...}
   :animation1 {1    nil
                2    nil
                :env {}                                     ; local stuffs. globals are passed in.
                }}

(defn map-v-only-points
  "Map the values of leds. (f index value) -> new-value"
  [f xs]
  (into xs
        (comp (filter #(and (number? (key %)) (<= 0 (key %) 299)))
              (map (fn [[k v]] [k (f k v)])))
        xs))

;; Point providers

(defn init-state [is]
  (fn [env s]
    (if s s is)))

(defn random-points [& {:keys [prob point-selection prob-fn]
                        :or   {prob            0.01
                               point-selection led-ids
                               }}]
  (let [prob-fn (or prob-fn (fn [env state] prob))]
    (fn [env s]
      (let [points (random-sample (prob-fn env s) point-selection)]
        (into s
              (map (fn [v] [v :new-point]))
              points)))))

(defn sweep-point [& {:keys [rate count]
                      :or   {rate 1 count 1}}]
  (fn [env s]
    (let [last-p (get s :last-p 0)
          r (cond (> last-p num-leds) (- rate)
                  (< last-p 0) rate
                  :default (:rate s rate))
          next-p (+ r last-p)]
      (assoc s :last-p next-p
               next-p :new-point
               :rate r))))

;; Geometric Motion

;; Color Providers

(defn fill-solid [& {:keys [color only-new?] :or {only-new? true}}]
  (fn [env s]
    (map-v-only-points #(if (or (not only-new?) (= :new-point %2)) color %2) s)))

;; Point Transitions


(defn decay [& {:keys [target-color rate] :or {target-color cu/transparent
                                               rate         0.1}}]
  (fn [env s]
    (map-v-only-points #(if-not (or (= %2 :new-point) (nil? %2))
                          (thim/mix %2 target-color rate)
                          %2) s)))


(defn cull-black []
  (fn [env s]
    ;(map-v-only-points (fn [k v] (if (cu/black? v) nil v)) s)
    (mc/filter-kv (fn [k v]
                    (if (or (not (number? k))
                            (and (< 0 k num-leds)
                                 (not (cu/black? v))))
                      v
                      nil))
                  s)))

;; Blend Fns

(defn blend-overwrite [priority-map]
  (let [priority-map (reverse priority-map)]                ;; priority-map is confused when using some. Best to reverse it for nice usage.
    (fn [env s]
      (let [ms (map #(get s %) priority-map)
            get-v (fn [id] (->> ms
                                (map #(get % id))
                                ;(filter identity)
                                (some identity)))]
        (into s (map (fn [id] [id (get-v id)])) led-ids)))))


(defn blend-normal [priority-map]
  (let [priority-map (reverse priority-map)]
    (fn [env s]
      (let [ms (map #(get s %) priority-map)
            get-v (fn [id] (->> ms
                                (map #(get % id))
                                (filter identity)
                                cu/overlay-colors))]
        (into s (map (fn [id] [id (get-v id)])) led-ids)))))

(comment

  (def random-anim
    (define-animation (random-points :prob 0.01)
                      (fill-solid :color col/RED)
                      (decay)))

  (take 4 (iterate #(random-anim {} %) {})))

