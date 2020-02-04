(ns common.utils)


(defn spy [& things]
  (println things)
  (last things))
