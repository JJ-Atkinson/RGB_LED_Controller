(ns sound-analysis.onset-detection)


(defn flux
  ([offt nfft]
   (flux offt nfft (range (count offt))))
  ([offt nfft bands]
   (->> bands (map (fn [i] (- (nth nfft i) (nth offt i)))) (reduce +))))
