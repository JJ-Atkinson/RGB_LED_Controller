(ns sound-analysis.vega-vis
  (:require [oz.core :as oz]
            [sound-analysis.sound-interface :as si]
            [sound-analysis.onset-detection :as od]
            [com.hypirion.clj-xchart :as c]
            [mount.core :refer [defstate]]
            [clojure.string :as str])
  (:import (javax.swing JFrame SwingUtilities JPanel)
           (java.awt GridLayout)
           (org.knowm.xchart XChartPanel)))



(def band-depth (* 2 2 2))

#_(def sound-analysis (future
                        (first (reduce (fn [[acc last-fft] idx]
                                         (Thread/sleep 1)
                                         (let [fft (si/fft-anal)
                                               res
                                               (->> (map - fft last-fft) (partition band-depth)
                                                    (map (partial reduce #(+ %1 (Math/abs ^double %2)) 0)))]
                                           [(conj acc res) fft]))
                                       [[] (repeat 1024 0)]
                                       (range 40000)))))


(comment
  (future-done? sound-analysis)
  (count @sound-analysis)

  (count flattened-sound))

(defn flattened-sound []
  (let [samples (od/last-samples 70)]
    (for [s samples idx (range 0 od/nbands 1)]
      {:time (:sample-time s) :band (* band-depth idx) :val (-> s :samples (nth idx))})))



(defn from-band [samples-list band-def]
  (reduce (fn [acc [index multiplier]]
            (+ acc (* multiplier (nth samples-list index)))) 0 (partition 2 band-def)))
(defn band-str [band-def]
  (str/join "," (map (partial str/join "|") (partition 2 band-def))))


(defn sound-to-series []
  (apply merge
         (let [samples (od/last-samples 200)]
           (for [band od/flux-band]
             {(band-str band) [(map :sample-time samples)
                               (map (comp #(from-band % band) :samples) samples)]
              }))))

(od/last-samples 1)






;(defonce server (oz/start-server!))

(defstate jframe :start (doto (JFrame. "XChart")
                          (.setVisible true))
          :stop (.dispose jframe))

(defn view-xchart-same-frame
  "Utility function to render one or more charts in a swing frame."
  [& charts]
  (let [num-rows (int (+ (Math/sqrt (count charts)) 0.5))
        num-cols (inc (/ (count charts)
                         (double num-rows)))
        frame jframe]
    (SwingUtilities/invokeLater
      #(do (.. frame (getContentPane) (removeAll))

           (.. frame (getContentPane) (setLayout (GridLayout. num-rows num-cols)))
           (doseq [chart charts]
             (if chart
               (.add frame (XChartPanel. chart))
               (.add frame (JPanel.))))
           (.pack frame)
           (.setSize frame 1000 1000)
           (.repaint frame)))
    frame))

(comment (oz/view! {:data     {:values (flattened-sound)}
                    :title    "hi"
                    :width    1000
                    :height   500
                    :mark     "line"
                    :encoding {:x     {:field "time"
                                       :type  "quantitative"}
                               :y     {:field "val"
                                       :type  "quantitative"}
                               :color {:field "band"
                                       :type  "ordinal"}}})

         (map (comp ) #(from-band % (first flux-band))  )

         (view-xchart-same-frame (c/xy-chart (sound-to-series)
                                             {:title "hi"})))

