(ns ramp-example.core
  (:require [ramp.desktop :refer :all]
            [ramp-example.title]
            [ramp-example.game])
  (:gen-class))

(defn -main [& args]
  (lwjgl-app! 'ramp-example.title {:title "Rondovania v. Minimal" :width 768 :height 576}))