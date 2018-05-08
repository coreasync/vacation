(ns app.core
  (:require [quil.core :as q]
            [quil.middleware :as m]
            [clojure.edn :as e]
            [incanter.interpolation :as i]))

(defn setup []
  (q/frame-rate 15)
  {:paused true
   :current 0
   })

(defn update-state[state]
  (update state :current #(-> % inc inc (mod 245))))

(defn draw-state[state]
  (q/background 0xFFFF0000)
  (q/text-font (q/create-font "DejaVu Sans" 22 true))
  (q/text "ful  Pls do the needful" 2 24)
  ;;(q/text "Pls revert back Pls revert back" 2 24)
  (let [x (state :current)
        y 0
        xd 32
        yd 32
        ni (q/create-image 32 32 :rgb)]
    (q/stroke 0 )
    (q/stroke-weight 1)
    (q/line (+ x)    (- y   ) (+ x   ) (+ y yd))
    (q/line (+ x xd) (- y   ) (+ x xd) (+ y yd))
    (q/copy (q/current-graphics) ni [x 0 xd yd] [0 0 xd yd])
    (.save ni (format "outputImage%03d.gif" x)))
  state)

(defn run-editor[]
  (q/defsketch app
    :title "please"
    :size [480 100]
    :setup setup
    :update update-state
    :draw draw-state
    :features [:keep-on-top]
    :middleware [m/fun-mode]))

;;convert -delay 2 -loop 0 outputImage*.gif o.gif
