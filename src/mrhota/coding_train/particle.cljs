(ns mrhota.coding-train.particle
  [:refer-clojure :exclude [update apply]]
  [:require
   [thi.ng.math.core :as m]
   [thi.ng.geom.vector :as v :refer [vec2]]
   [quil.core :as q]])

(defn particle [pos vel acc]
  {:pos pos
   :prev pos
   :vel vel
   :acc acc})

(defn ^:private update-edges [{:keys [pos] :as particle}]
  (let [new-x (cond
                (< (v/x pos) 0) (q/width)
                (> (v/x pos) (q/width)) 0
                :else (v/x pos))
        new-y (cond
                (< (v/y pos) 0) (q/height)
                (> (v/y pos) (q/height)) 0
                :else (v/y pos))
        new-pos (vec2 new-x new-y)]
    (-> particle
        (assoc :pos new-pos)
        (assoc :prev new-pos))))

(def max-velocity 2)

(defn update-velocity [{:keys [vel acc] :as particle}]
  (assoc particle :vel (m/limit (m/+ vel  acc) max-velocity)))

(defn update-position [particle]
  (assoc particle :pos (m/+ (:pos particle) (:vel particle))))

(defn update [particle]
  (-> particle
      (update-velocity)
      (update-position)
      (assoc :acc (vec2 0 0))
      (update-edges)))

(defn apply [force particle]
  (let [{:keys [acc]} particle
        new-particle (assoc particle :acc (m/+ acc force))]
    new-particle))

(defn draw [{:keys [pos prev]} label]
  ;; (q/text label (+ (v/x pos) 3) (- (v/y pos) 3))
  (q/stroke-weight 1)
  ;; (q/point (v/x pos) (v/y pos)))
  (q/line (v/x prev) (v/y prev) (v/x pos) (v/y pos)))
