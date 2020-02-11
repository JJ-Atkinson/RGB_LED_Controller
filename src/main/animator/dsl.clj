(ns animator.dsl
  (:require [light-communications.color-utils :as cu]
            [sc.api :as scc]
            [medley.core :as mc]
            [thi.ng.color.core :as col]
            [thi.ng.color.gradients :as cgr]
            [thi.ng.math.core :as thim]
            [common.utils :as u]))

(def num-leds 300)
(def led-ids (range num-leds))


(defn define-animation [& animators]
  (fn [env state] (reduce (fn [s f] (f env s)) state animators)))

(defn isolate [tag & animations]
  (fn [env state] (update state tag #((apply define-animation animations) env %))))


(def info {:center-f 45
           :center-m 165
           :center-b 255
           :center-s 105})

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

(defn- map-v-only-points
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
        (assoc s :new-points (into {}
                                   (map (fn [v] [v 1]))
                                   points))))))


(defn n-points [& {:keys [count-fn count]
                   :or   {count 1}}]
  (let [count-fn (or count-fn (fn [env state] count))]
    (fn [env s]
      (let [points (map (fn [x] (rand-int num-leds)) (range (count-fn env s)))]
        (assoc s :new-points (into {}
                                   (map (fn [v] [v 1]))
                                   points))))))


(defn sweep-point [& {:keys [rate count]
                      :or   {rate 1 count 1}}]
  (fn [env s]
    (let [last-p (get s :sweep/last-p 0)
          r (cond (> last-p num-leds) (- rate)
                  (< last-p 0) rate
                  :default (:sweep/rate s rate))
          next-p (+ r last-p)]
      (assoc s :sweep/last-p next-p
               :sweep/rate r
               :new-points {next-p 1}))))


(defn relative-points [& {:keys [dist-fn fill-in-between?] :or {fill-in-between? false}}]
  (fn [env s]

    (let [curr-v (dist-fn env s)
          last-v (:relative-points/last-v s 0)
          range (range (min curr-v last-v) (inc (max curr-v last-v)))
          point-fill (if fill-in-between?
                       (into {} (map (fn [v] [v 1])) range)
                       {curr-v 1})]
      (assoc s :new-points point-fill
               :relative-points/last-v curr-v))))

;; Geometric Motion

(defn- clip [p min max]
  (when (< min p max) p))

(defn- center-and-reflect-direct [center dist p]
  [(clip (+ center p) (- center dist) (+ center dist))
   (clip (- center p) (- center dist) (+ center dist))])

(defn- center-and-reflect-all-axis [p]
  (->> (mapcat (partial apply center-and-reflect-direct)
               [[(info :center-m) 45 p]
                [(info :center-f) 45 p]
                [(info :center-b) 45 p]])
       (filter identity)))

(defn center-and-reflect
  "Take a location int and mirror on all strips"
  [& {:keys []}]
  (let [ptmap (fn [[k v]]
                (->> (center-and-reflect-all-axis k)
                     (into {} (map (fn [pt] [pt v])))))]
    (fn [env s]
      (update s :new-points (fn [np] (into {} (map ptmap) np))))))

;; Color Providers

(defn fill-solid [& {:keys [color]}]
  (fn [env s]
    (into s (map (fn [[k v]] [k color])) (or (:new-points s) {}))
    ))


(defn fill-random [& {:keys [colors]}]
  (fn [env s]
    (into s (map (fn [[k v]] [k (rand-nth colors)])) (or (:new-points s) {}))
    ))

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
  (fn [env s]
    (let [ms (map #(get s %) priority-map)
          get-v (fn [id] (->> ms
                              (map #(get % id))
                              ;(filter identity)
                              (some identity)))]
      (into s (map (fn [id] [id (get-v id)])) led-ids))))


(defn blend-normal [priority-map]
  (fn [env s]
    (let [ms (map #(get s %) priority-map)
          get-v (fn [id] (->> ms
                              (map #(get % id))
                              (filter identity)
                              cu/overlay-colors))]
      (into s (map (fn [id] [id (get-v id)])) led-ids))))

(comment

  (def random-anim
    (define-animation (random-points :prob 0.01)
                      (fill-solid :color col/RED)
                      (decay)))

  (take 4 (iterate #(random-anim {} %) {})))

