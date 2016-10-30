(ns ramp-example.title
  (:require [ramp.core :refer :all]
            [clojure.tools.nrepl.server :refer [start-server stop-server]]
            [ramp.g2d :refer :all]
            [ramp.input :as i]
            [ramp.cache :as c]
            [ramp.gradients :as g]
            [ramp-example.game]
            [ramp.utils :as ut]))


(defonce batch (delay (spritebatch)))

(defonce allb (delay (texture "allblack.png")))

(defonce bg (delay (texture "tt-bg.png")))

(defonce lg (delay (texture "logo.png")))

(defonce p2s (delay (texture "press2start.png")))

(defonce gamestate (atom {:black 255 :timer 0 :black-again -1}))

(defn create []
  (swap! gamestate assoc :timer 0))

(defn updato []
  (comment println (:black-again @gamestate))
  (if (and (i/pressed? :space) (> (:timer @gamestate) 240) (neg? (:black-again @gamestate)))
    (swap! gamestate assoc :black-again 1))
  (if (pos? (:black-again @gamestate))
    (swap! gamestate update :black-again inc))
  (if (> (:black-again @gamestate) 240)
    (reset-screen! (get-game) 'ramp-example.game))
  (swap! gamestate update :timer inc))

(defn render []
  (let [b @batch
        black @allb
        bgt @bg
        ba (:black-again @gamestate)
        timer (:timer @gamestate)
        bop (if (neg? ba)
              (if (> timer 240) 0 (- 1 (/ timer 240)))
              (if (> ba 240) 1.0 (+ 0 (/ ba 240.0))))
        d (if (neg? ba)
            100.0
            20.0)
        o (/ (g/bounce-between 0 d 50 timer) d)]
    (begin-batch b
                 (draw! b bgt 0 0)
                 (draw! b @lg 0 0)
                 (set-color! b (color 1 1 1 o))
                 (draw! b @p2s 0 0)
                 (set-color! b (color 1 1 1 1))
                 (drawr! b black {:x 0 :y 0 :color (color 0 0 0 bop)}))))