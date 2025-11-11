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

(defn ^:private update-edges [{:keys [pos prev] :as particle}]
  (let [width (q/width)
        height (q/height)
        new-x (cond
                (<= (v/x pos) 0) width
                (>= (v/x pos) width) 0)
        new-y (cond
                (<= (v/y pos) 0) height
                (>= (v/y pos) height) 0)
        prev-x (if (nil? new-x)
                 (v/x prev)
                 new-x)
        prev-y (if (nil? new-y)
                 (v/y prev)
                 new-y)
        new-pos (vec2 (if (nil? new-x) (v/x pos) new-x) (if (nil? new-y) (v/y pos) new-y))]
    (-> particle
        (assoc :pos new-pos)
        (assoc :prev (vec2 prev-x prev-y)))))

(defn update-prev [particle]
  (assoc particle :prev (:pos particle)))

(def max-velocity 4)

(defn update-velocity [{:keys [vel acc] :as particle}]
  (assoc particle :vel (m/limit (m/+ vel  acc) max-velocity)))

(defn update-position [particle]
  (assoc particle :pos (m/+ (:pos particle) (:vel particle))))

(defn update [particle]
  (-> particle
      (update-prev)
      (update-velocity)
      (update-position)
      (assoc :acc (vec2 0 0))
      (update-edges)))

(defn apply [force particle]
  (let [{:keys [acc]} particle
        new-particle (assoc particle :acc (m/+ acc force))]
    new-particle))

;; to have particles, draw a q/point, but to have paths, use the particle's
;; prev pos and its pos to draw a line
(defn draw [{:keys [pos prev]}]
  ;; (q/text label (+ (v/x pos) 3) (- (v/y pos) 3))
  (q/stroke-weight 1)
  ;; (q/point (v/x pos) (v/y pos)))
  (q/line (v/x pos) (v/y pos) (v/x prev) (v/y prev)))
