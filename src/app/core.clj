(ns app.core
  (:require [quil.core :as q]
            [quil.middleware :as m]
            [clojure.edn :as e]
            [incanter.interpolation :as i]))

(defn s-to-edn [f d] (spit f (with-out-str (pr d))))
(defn s-from-edn [f] (e/read-string (slurp f)))

(def truncate 1500);;FIXME if lowered for testing, might fail due to current > truncate
(def typicalsourceframe [1920 1080])

(defn create-cir-map[x] (into {} (map vec (partition 2 1 (conj (vec x) (first x))))))

(def hdframes [[640 720] [1280 720]]) ;;first must be half size, assumption below
(def hdframes-map-first (first hdframes))
(def hdframes-map (create-cir-map hdframes))

(def itrps [[:cubic-hermite] [:linear]]) ;;[:polynomial] [:cubic] [:linear-least-squares]
(def itrps-first (first itrps))
(def itrps-map (create-cir-map itrps))

(defn mk-int-f[s args]
  (apply (partial i/interpolate s) args))

(defn add-int-f [state]
  (let [centers (:centers state)]
    (if (<= 2 (count centers)) ;;FIXME if center points removed, all interpol fun remains
      (let [xys (for [[i [x y]] centers] [[i x] [i y]])
            int-center-xf (mk-int-f (map first xys) (:inter state))
            int-center-yf (mk-int-f (map second xys) (:inter state))]
        (assoc state :int-center-xf int-center-xf :int-center-yf int-center-yf))
      state)))

(defn add-int-zoom-f [state]
  (let [zoom (:zoom state)]
    (if (<= 2 (count zoom)) ;;FIXME if center points removed, all interpol fun remains
      (assoc state :int-zoom-f (mk-int-f (into [] zoom) (:inter state)))
      state)))

(defn add-frame-f [state]
  (let [[dx dy] (:hdframe state)
        scale (:thumbscale state)]
    (if (:orig-size state)
      (assoc state :frame [dx dy])
      (assoc state :frame [(/ dx scale) (/ dy scale)]))))

(defn add-images-f [state]
  (let [source (if (:orig-size state) :origjpegs :thumbjpegs)
        images (->> state source clojure.java.io/file file-seq rest (take truncate) vec) ;;FIXME Could not find a method to load .../.DS_Store
        cnt (count images)]
    (assoc state
           :all-images (mapv q/load-image images)
           :num-of-frames cnt)))

(defn setup [project-state]
  (q/frame-rate 30);;30
  (add-frame-f
   (add-int-zoom-f
    (add-int-f
     (add-images-f
      (merge
       {:mode :define ;;:replay :traj
        :paused true
        :centers {}
        :zoom {}
        :hdframe hdframes-map-first
        :inter itrps-first
        :int-center-xf nil
        :int-center-yf nil
        :int-zoom-f nil
        :current 0
        :viddump {}
        :num-of-frames 0
        :all-images []}
       project-state))))))

(defn update-state[state]
  (let [newstate (if (:paused state)
                   state
                   (update state :current #(-> % inc (mod (:num-of-frames state)))))
        cu (:current newstate)
        viddump (:viddump newstate)]
    (if (empty? viddump)
      newstate
      (if-let [filen (viddump cu)]
        (do
          (prn "saved" filen)
          (q/save filen)
          (assoc newstate :viddump (dissoc viddump cu)))
        newstate))))

(defn draw-crosshair [c s w x y]
  (q/stroke c)
  (q/stroke-weight w)
  (q/line (- x s) y (+ x s) y)
  (q/line x (- y s) x (+ y s)))

(defn draw-targetframe [c z w fx fy x y]
  (let [xd (* z fx 0.5)
        yd (* z fy 0.5)]
      (q/stroke c)
      (q/stroke-weight w)
      (q/line (- x xd) (- y yd) (+ x xd) (- y yd))
      (q/line (- x xd) (+ y yd) (+ x xd) (+ y yd))
      (q/line (- x xd) (- y yd) (- x xd) (+ y yd))
      (q/line (+ x xd) (- y yd) (+ x xd) (+ y yd))))

(defn draw-state-define [state]
  (q/background 240)
  #_(prn "DEFINE" (dissoc state :all-images :int-center-xf :int-center-yf :int-zoom-f))
  (let [cu (:current state)
        ce (:centers state)
        zo (:zoom state)
        is (:all-images state)
        xf (:int-center-xf state)
        yf (:int-center-yf state)
        [fx fy] (:frame state)
        orig-scale (:orig-size-factor state)]
    (q/set-image 0 0 (is cu))
    (q/text (str cu "/" (-> state :all-images count)) 10 10)
    (when (and xf yf)
      (let [x (* (xf cu) orig-scale)
            y (* (yf cu) orig-scale)
            zf (:int-zoom-f state)
            z (if zf (zf cu) 1.0)
            zw (if (zo cu) 2 1)]
        (draw-crosshair 0xFFFFFF00 20 1 x y)
        (draw-targetframe 0xFFFFFF00 1.0 1 fx fy x y)
        (draw-targetframe 0xFFFF0000 z zw fx fy x y)))
    (when-let [[x y] (ce cu)]
      (draw-crosshair 0 10 2 (* x orig-scale) (* y orig-scale)))))

(defn draw-state-replay [state]
  (q/background 240)
  #_(prn "REPLAY" (dissoc state :all-images :int-center-xf :int-center-yf :int-zoom-f))
  (let [cu (:current state)
        ce (:centers state)
        is (:all-images state)
        xf (:int-center-xf state)
        yf (:int-center-yf state)
        zf (:int-zoom-f state)
        [fx fy] (:frame state)
        orig-scale (:orig-size-factor state)]
    (when (and xf yf zf)
      (let [x (int (* (xf cu) orig-scale))
            y (int (* (yf cu) orig-scale))
            z (zf cu)
            cropped (q/get-pixel (is cu) (- x (* z fx 0.5)) (- y (* z fy 0.5)) (* z fx) (* z fy))
            _ (.resize cropped fx fy)]
        (q/set-image 0 0 cropped)))))

(defn draw-state-traj [state]
  (q/background 240)
  (q/stroke-weight 1)
  ;;check with quil #_(prn (q/width) (q/screen-width) (:orig-geom state) (:orig-size state))
  (let [cu (:current state)
        xf (:int-center-xf state)
        yf (:int-center-yf state)
        zf (:int-zoom-f state)
        fx (/ (q/width) (:num-of-frames state))
        fy 0.5]
    (q/stroke 0xFF000000)
    (q/line (* fx cu) 0 (* fx cu) (q/height))
    (doseq [x (range (:num-of-frames state))]
      #_(prn (-> :frame state) (int (* fx x)) (xf x) (yf x))
      (q/stroke 0xFFFF0000)
      (q/point (* fx x) (* fy (xf x)))
      (q/stroke 0xFFFF00FF)
      (q/point (* fx x) (* fy (yf x)))
      (q/stroke 0xFF0000FF)
      (q/point (* fx x) (* 300 (zf x))))))

(defn draw-state[state]
  ((-> state
       :mode
       {:define draw-state-define :replay draw-state-replay :traj draw-state-traj})
   state))

(def zoom-map {\q 1.0 \Q 1.188 \w 0.9 \W 1.39 \e 0.8 \E 1.61 \r 0.7 \R 1.84 \t 0.6 \T 2.1 \y 0.5 \Y 2.37 \u 0.4 \U 2.657 \i 0.3 \I 2.96 \o 0.2 \O 2.96 \p 0.1 \P 3.27})

(defn define-options [state event]
  (if-let [z (-> event :raw-key zoom-map)]
    (let [zoom (:zoom state)
          current (:current state)
          nzoom (if (zoom current)
                     (dissoc zoom current)
                     (assoc zoom current z))
          #_(prn "NEW ZOOM" z event nzoom)]
      (add-int-zoom-f (assoc state :zoom nzoom)))
    state))

(defn get-now-timestamp[] (. (java.text.SimpleDateFormat. "yyyy-MM-dd-HH-mm-ss") format (java.util.Date.)))

(defn gen-viddump-todo[path mode size]
  (let [ts (get-now-timestamp)]
    ;;skip first frame which in fact is last one
    (into {} (map #(vector % (format "%s/%s.%s.%06d.tiff" path mode ts %)) (range 1 size)))))

(defn find-next-waypoint [state reversed]
  (let [[direction compare] (if reversed [reverse >] [identity <])
        all (direction (sort (keys (merge (:zoom state) (:centers state)))))
        f (first (filter #(compare (:current state) %) all))
        ff (first all)]
    (if f
      f
      (if ff
        ff
        0))))

(defn main-options [state event]
  (case (-> event :raw-key)
    ;; MODES
    \1 (do (prn "DEFINE") (assoc state :mode :define))
    \2 (do (prn "REPLAY") (assoc state :mode :replay))
    \3 (do (prn "TRAJ") (assoc state :mode :traj))

    ;; pause/play/skip/...
    \space (update state :paused not)
    \, (update state :current #(-> % dec (mod (:num-of-frames state))))
    \. (update state :current #(-> % inc (mod (:num-of-frames state))))
    \< (update state :current #(-> % ((partial + -10)) (mod (:num-of-frames state))))
    \> (update state :current #(-> % ((partial + +10)) (mod (:num-of-frames state))))
    \/ (assoc state :current (find-next-waypoint state false))
    \? (assoc state :current (find-next-waypoint state true))

    ;;frame size, interpolation
    \f (add-frame-f (assoc state :hdframe (hdframes-map (:hdframe state))))
    \g (let [ni (itrps-map (:inter state))]
         (prn "NEW INTER" ni)
         (add-int-zoom-f (add-int-f (assoc state :inter ni))))

    ;;data
    \s (do
         (when-not (:orig-size state)
           (prn "SAVED")
           (s-to-edn (:myself state) (dissoc state :orig-size :all-images :int-center-xf :int-center-yf :int-zoom-f)))
           state)
    \S (if (-> state :viddump empty?)
         (do
           (prn "VIDDUMP START")
           (assoc state :viddump (gen-viddump-todo "viddump" (-> state :mode name) (:num-of-frames state))))
         (do
           (prn "VIDDUMP IS PENDING, try later")
           state))
    \d (do
         (println "STATE:" (dissoc state :all-images :int-center-xf :int-center-yf :int-zoom-f))
         state)
    state))

(defn key-press [state event]
  #_(prn "key" event "at" (q/mouse-x) (q/mouse-y))
  (if (-> state :mode (= :define))
    (define-options (main-options state event) event)
    (main-options state event)))

(defn mouse-clicked [state event] ;;e.g. {:x 442, :y 255, :button :left}
  #_(prn event (dissoc state :all-images :int-center-xf :int-center-yf :int-zoom-f)) ;;FIXME factor out list of computed states
  (if (-> state :mode (= :define))
    (let [centers (:centers state)
          current (:current state)
          ncenters (if (centers current)
                     (dissoc centers current)
                     ;;mouse pointer is a bit off
                     (assoc centers current [(+ (:x event) -2) (+ (:y event) -4)]))
          #_(prn "NEW CENTERS" ncenters)]
      (add-int-f (assoc state :centers ncenters)))
    state))

(defn run-editor[project-file orig-size force-half]
  (let [project-state-orig (s-from-edn project-file)
        project-state (if force-half (assoc project-state-orig :hdframe hdframes-map-first) project-state-orig)
        orig-geom (:orig-geom project-state)
        scale (:thumbscale project-state)
        init-size (if orig-size (:hdframe project-state) (mapv #(/ % scale) orig-geom))]
    (prn "INIT SIZE" init-size)
    (q/defsketch app
      :title "vacations"
      :size (identity init-size) ;;FIXME why!
      :setup (partial setup (assoc project-state :orig-size orig-size :orig-size-factor (if orig-size scale 1)))
      :update update-state
      :draw draw-state
      :key-typed key-press
      :mouse-clicked mouse-clicked
      :features [:keep-on-top]
      :middleware [m/fun-mode])))

(defn mk-project [project-file origjpegs-path orig-geom thumbjpegs-path thumbscale]
  (s-to-edn
   project-file
   {:myself project-file
    :origjpegs origjpegs-path
    :orig-geom orig-geom
    :thumbjpegs thumbjpegs-path
    :thumbscale thumbscale}))
