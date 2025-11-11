(ns mrhota.coding-train.flow-field
  (:require [cljs.pprint :refer [pprint]]
            [quil.core :as q :include-macros true]
            [quil.middleware :as m]
            [thi.ng.geom.core :as g]
            [thi.ng.geom.vector :as v :refer [vec2]]
            [mrhota.coding-train.particle :as p]))

(defn show-frame-rate []
  (q/text-size 20)
  (q/with-fill [0 0 200]
    (q/text-num (q/floor (q/current-frame-rate)) 40 40)))

(defn incremental-noise
  "Get noise value from n-dimensional Perlin space.
   Each dimensionspec is a vector of [scale position] pairs, one per dimension.

   Thus, we can vary the \"speed\" by which we move through Perlin space
   independently per dimension."
  [[[xscl xpos] [yscl ypos] [zscl zpos]]]
  (q/noise (* xscl xpos) (* yscl ypos) (* zscl zpos)))

(defn get-grid-cell [pos scale cols rows]
  (let [x (v/x pos)
        y (v/y pos)
        x-grid (min (dec cols) (q/floor (/ x scale)))
        y-grid (min (dec rows) (q/floor (/ y scale)))]
    [x-grid y-grid]))

(defn index [x y cols]
  (+ (* y cols) x))

(defn grid-cell->corner
  "Convert grid cell coordinates to canvas corner coordinates."
  [x-grid y-grid scale]
  [(* x-grid scale) (* y-grid scale)])

(defn calculate-theta
  "Calculate the angle for a flow field vector at grid position (x-grid, y-grid).
   Computes canvas coordinates internally to avoid vector allocation.
   Uses 3D Perlin noise mapped to angular range."
  [increment x-grid y-grid scale z]
  (let [x (* x-grid scale)
        y (* y-grid scale)]
    (* 8 q/PI
       (incremental-noise [[increment x]
                           [increment y]
                           [increment z]]))))

(defn setup []
  ;; here I can adjust the stroke's alpha value to blend particle paths over time
  (q/stroke 0 5)
  (let [scale 20
        increment 0.003
        particle-count 1000
        cols (q/floor (/ (q/width) scale))
        rows (q/floor (/ (q/height) scale))
        particles (mapv (fn [_] (p/particle (vec2 (q/random (q/width)) (q/random (q/height)))
                                            (vec2 (q/random -1 1) (q/random -1 1))
                                            (vec2 0 0)))
                        (range particle-count))
        angles (into [] (for [x-grid (range cols)
                              y-grid (range rows)]
                          (calculate-theta increment x-grid y-grid scale 0)))]
    {:scale scale
     :cols cols
     :rows rows
     :increment increment
     :particles particles
     :angles angles}))

(defn update-state [{:keys [increment particles angles scale cols rows] :as state}]
  (let [frame (q/frame-count)
        new-angles (mapv (fn [idx]
                           (let [x-grid (mod idx cols)
                                 y-grid (quot idx cols)]
                             (calculate-theta increment x-grid y-grid scale frame)))
                         (range (count angles)))
        new-particles (mapv (fn [{:keys [pos] :as particle}]
                              (let [[x-grid y-grid] (get-grid-cell pos scale cols rows)
                                    idx (index x-grid y-grid cols)
                                    theta (nth new-angles idx)
                                    force (g/as-cartesian (vec2 1 theta))]
                                (->> particle
                                     (p/apply force)
                                     p/update)))
                            particles)]
    (-> state
        (assoc :angles new-angles)
        (assoc :particles new-particles))))

(defn draw-vector-field
  "Draw the flow field as vectors to visualize the field itself"
  [{:keys [scale angles cols]}]
  (dotimes [idx (count angles)]
    (let [x-grid (mod idx cols)
          y-grid (quot idx cols)
          [x y] (grid-cell->corner x-grid y-grid scale)
          theta (nth angles idx)]
      (q/with-translation [x y]
        (q/with-rotation [theta]
          (q/stroke-weight 1)
          (q/stroke 0 150)
          (q/line 0 0 scale 0)
          ;; Draw arrowhead
          (q/with-translation [scale 0]
            (q/line 0 0 -3 -3)
            (q/line 0 0 -3 3)))))))

(defn draw-particles
  "Draw the particles with their grid positions"
  [{:keys [scale particles]}]
  (doseq [particle particles]
    (let [x-text (q/floor (/ (v/x (:pos particle)) scale))
          y-text (q/floor (/ (v/y (:pos particle)) scale))]
      (p/draw particle))))

(defn draw-noise-heatmap
  "Draw the raw Perlin noise values as a grayscale heatmap.
   Useful for diagnosing artifacts, discontinuities, or patterns in the noise field."
  [{:keys [scale increment cols rows]}]
  (q/no-stroke)
  (dotimes [idx (* cols rows)]
    (let [x-grid (mod idx cols)
          y-grid (quot idx cols)
          [x y] (grid-cell->corner x-grid y-grid scale)
          ;; Sample the current noise value at this position
          noise-val (incremental-noise [[increment x]
                                        [increment y]
                                        [increment (q/frame-count)]])
          ;; Map noise (0-1) to grayscale (0-255)
          brightness (* 255 noise-val)]
      (q/with-fill [brightness]
        (q/rect x y scale scale)))))

(defn draw [state]
  ;; Clear background for vector field/noise modes (comment out for particle trails)
  ;; (q/background 255)
  ;; (show-frame-rate)
  ;; Toggle between visualization modes by commenting/uncommenting:
  ;; (draw-vector-field state)
  ;; (draw-noise-heatmap state)
  (draw-particles state)
  ;;
  )

#_{:clj-kondo/ignore [:unresolved-symbol]}
(q/defsketch flow-field
  :host "app"
  :size [640 480]
  :setup setup
  :update update-state
  :draw draw
  :middleware [m/fun-mode])
