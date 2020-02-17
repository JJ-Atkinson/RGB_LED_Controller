(ns sound-analysis.onset-detection
  (:require [sound-analysis.sound-interface :as si]
            [mount.core :refer [defstate]]
            ))


(defn flux
  ([offt nfft]
   (flux offt nfft (range (count offt))))
  ([offt nfft bands]
   (->> bands (map (fn [i] (- (nth nfft i) (nth offt i)))) (reduce +))))

(defn compress-bands [full-fft band-depth]
  (->> full-fft (partition band-depth) (map (partial reduce #(+ %1 (Math/abs ^double %2) 0)))))


(defonce fft-history (atom {:samples    []
                            :start-time (System/currentTimeMillis)}))






(def sample-life 10000)
(def band-depth 2)
(def nbands (/ si/fft-band-count band-depth))

(defn fft-update-loop []
  (loop [prior-bands (repeat 1024 0)
         use-short-delay? false]
    (Thread/sleep (if use-short-delay? 2 10))
    (let [fft (si/fft-anal)
          banded (compress-bands fft band-depth)
          diff (map #(Math/abs ^double (- %1 %2)) banded prior-bands)
          non-zero (some (complement zero?) diff)
          uptake-time (->> @fft-history :start-time (- (System/currentTimeMillis)))
          store {:sample-time uptake-time :samples diff}]
      (when non-zero
        (swap! fft-history (fn [m]
                             (update m :samples #(cons store (take sample-life %))))))
      (when-not (Thread/interrupted)
        (recur banded (not non-zero))))))

(defstate                                                   ;^{:on-reload :noop}
  live-fft-anal
  :start (doto (Thread. (fn [] (fft-update-loop)))
           (.start))
  :stop (.interrupt live-fft-anal))

(def thing live-fft-anal)

(defn last-samples [n]
  (->> @fft-history :samples (take n)))

(def flux-band
  #_[[band * band * band *] ...]
  [[0 1 1 2 3 1 9 2 10 2 20 0]
   [3 5 5 5]
   [0 2 1 2 3 2 4 -1 5 -1]])

(defn from-band [samples-list band-def]
  (reduce (fn [acc [index multiplier]]
            (+ acc (* multiplier (nth samples-list index)))) 0 (partition 2 band-def)))

(defn replace0 [zero? repl]
  (if (= 0 zero?) repl zero?))

(defn average [samples]
  (/ (apply + samples) (replace0 (count samples) 1)))


(defn beat-detect
  [& {:keys [buffer-size ignore-beats-from-average exceed-factor prior-state new-sample
             ignore-sample max-bpm]
      :or {buffer-size (* 50 3)
           ignore-beats-from-average true
           ignore-sample false
           exceed-factor 1.5
           max-bpm 200}}]
   (let [avg (average (:samples prior-state))
         rest-time (* 1000 (/ 60 max-bpm))
         now (System/currentTimeMillis)
         can-beat? (> (- now (:last-beat-time prior-state 0))
                      rest-time)
         factor (/ new-sample (replace0 avg 1))
         beat? (and (> factor exceed-factor) can-beat?)
         ignore-sample? (or ignore-sample 
                            (and ignore-beats-from-average beat?))
         new-state (assoc prior-state
                     :samples (cons (if ignore-sample? avg new-sample)
                                    (take buffer-size (:samples prior-state)))
                     :last-beat-time (if beat? now
                                               (:last-beat-time prior-state 0)))]
     [new-state beat?]))