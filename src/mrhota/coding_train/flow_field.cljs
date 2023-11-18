(ns mrhota.coding-train.flow-field
  (:require [quil.core :as q :include-macros true]
            [quil.middleware :as m]
            [thi.ng.geom.core :as g]
            [thi.ng.geom.vector :as v :refer [vec2]]
            [mrhota.coding-train.particle :as p]))

(defn show-frame-rate []
  (q/text-size 20)
  (q/text (str (q/floor (q/current-frame-rate))) 40 40))

(defn incremental-noise
  "Get noise value from n-dimensional Perlin space.
   Each dimensionspec is a vector of [scale position] pairs, one per dimension.
   
   Thus, we can vary the \"speed\" by which we move through Perlin space
   independently per dimension."
  [dimensionspecs]
  (apply q/noise (map (fn [[scale position]] (* scale position)) dimensionspecs)))

(defn get-grid-cell [pos scale cols rows]
  (let [x (v/x pos)
        y (v/y pos)
        x-grid (if (>= x (* scale cols))
                 (dec cols)
                 (q/floor (/ x scale)))
        y-grid (if (>= y (* scale rows))
                 (dec rows)
                 (q/floor (/ y scale)))]
    [x-grid y-grid]))

(defn index [x y cols]
  (+ (* y cols) x))

;; TODO: turn cells-and-angles into a function
(defn setup []
  ;; here I can adjust the stroke's alpha value to blend particle paths over time
  (q/stroke 0 5)
  (q/background 255)
  (let [scale 20
        increment 0.01
        particle-count 1000
        cols (q/floor (/ (q/width) scale))
        rows (q/floor (/ (q/height) scale))
        y-vals (take-nth scale (range (q/height)))
        x-vals (flatten (map #(repeat (count y-vals) %)
                             (take-nth scale (range (q/width)))))
        corners (partition 2 (interleave x-vals (cycle y-vals)))
        particles (repeatedly particle-count #(p/particle (vec2 (q/random (q/width)) (q/random (q/height)))
                                                          (vec2 (q/random -1 1) (q/random -1 1))
                                                          (vec2 0 0)))
        cells-and-angles (map (fn [[x y :as corner]]
                                (let [theta (* 2 q/PI 4
                                               (incremental-noise [[increment x]
                                                                   [increment y]
                                                                   [0.003 0]]))]
                                  [corner theta]))
                              corners)]
    {:scale scale
     :cols cols
     :rows rows
     :increment increment
     :particles particles
     :cells-and-angles cells-and-angles}))

(defn update-state [{:keys [increment particles cells-and-angles scale cols rows] :as state}]
  (let [new-cells-and-angles (mapv (fn [[[x y :as corner] _]]
                                     (let [theta (* 2 q/PI 4
                                                    (incremental-noise [[increment x]
                                                                        [increment y]
                                                                        [0.003 (q/frame-count)]]))]
                                       [corner theta]))
                                   cells-and-angles)
        new-particles (map (fn [{:keys [pos] :as particle}]
                             (let [[x y] (get-grid-cell pos scale cols rows)
                                   index (index x y cols)
                                   theta (second (nth new-cells-and-angles index))
                                   force (g/as-cartesian (vec2 1 theta))]
                               (p/apply force particle)))
                           particles)]
    (-> state
        (assoc :cells-and-angles new-cells-and-angles)
        ;; i think I want to compose p/apply and p/update
        ;; p/apply accepts a force and a particle and returns a particle
        ;; p/update accepts a particle and returns a particle
        ;; so (comp p/update p/apply) is a function that takes
        ;; a force and a particle and returns a particle
        (assoc :particles (map p/update new-particles)))))

(defn draw [{:keys [scale particles cells-and-angles]}]
  (show-frame-rate)
  ;; (doseq [[[x y] noise] cells-and-angles]
  ;;   (q/with-translation [x y]
  ;;     (q/with-rotation [noise]
  ;;       (q/stroke-weight 1)
  ;;       (q/stroke 0 50)
  ;;       (q/line 0 0 scale 0))))
  (doseq [particle particles]
    (let [x-text (q/floor (/ (v/x (:pos particle)) scale))
          y-text (q/floor (/ (v/y (:pos particle)) scale))]
      (p/draw particle (str "(" x-text ", " y-text ")")))))

#_{:clj-kondo/ignore [:unresolved-symbol]}
(q/defsketch flow-field
  :host "app"
  :size [640 480]
  :setup setup
  :update update-state
  :draw draw
  :middleware [m/fun-mode])
