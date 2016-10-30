(ns ramp-example.game
  (:require [ramp.core :refer :all]
            [clojure.tools.nrepl.server :refer [start-server stop-server]]
            [ramp.g2d :refer :all]
            [ramp.rinput :as i]
            [ramp.cache :as c]
            [ramp.utils :as ut]
            [ramp.audiomanager :as am]
            [ramp.gradients :as g]))

(defonce gamestate
         (atom
           {:entities []
            :camera nil
            :actor {:x 50 :y 0 :dx 0 :dy 0 :w 63 :h 138 :frame [:idle 0] :timer 0 :left false}
            :dialogs [0]
            :popups []}))

(def GW 768)
(def GH 576)

(defn cur-dialog []
    (first (:dialogs @gamestate)))


(defn consume-dialog! []
  (swap! gamestate #(update % :dialogs rest)))

(defn push-dialog! [id]
  (swap! gamestate #(update % :dialogs (fn [it] (conj it id)))))

(defonce batch (delay (spritebatch)))

(defonce act-tex (delay (texture "char.png")))

(defonce bg-tex (delay (texture "bg.png")))

(defonce ab-tex (delay (texture "allblack.png")))

(defonce attack-tex (delay []))

(defonce walking-tex (delay (map (comp texture #(str "anim/wk" % ".png")) (range 0 14))))

(defonce dialogs
         (delay [(texture "dialogs/d1.png")
                 (texture "dialogs/d2.png")
                 (texture "dialogs/d3.png")
                 (texture "dialogs/d4.png")
                 (texture "dialogs/d5.png")
                 (texture "dialogs/d6.png")
                 (texture "dialogs/d7.png")
                 (texture "dialogs/d8.png")
                 (texture "dialogs/d9.png")]))

(defn see-dialog? []
  (> (:x (:actor @gamestate)) 0))

(defn push-popup! [id]
  (swap! gamestate #(update % :popups (fn [it] (conj it {:id id :timer 0})))))

(defn update-popups [coll]
  (map (fn [pu] (-> pu
                    (update :timer inc)))
       coll))

(defonce overlay-tex (delay (texture "overlay.png")))

(defonce popups
         (delay (map texture ["popups/item1.png"])))

(defonce idle-tex
         (delay
           (let [t (texture "actor-idle.png")
                 w (:width t)
                 h (:height t)
                 w10 (quot w 10)]
                 (for [i (range 10)]
                   (texture t (* i w10) 0 w10 h)))))

(defn create []
  (am/bgm! "PracticePrologue.mp3")
  (swap! gamestate assoc :camera (-> (ortho-cam false GW GH)
                                     (camera-pos! (/ GW 2) (/ GH 2))))
  (update-cam! (:camera @gamestate)))

(defn dispose [])

(defn on-ground? [e]
  (<= (:y e) 0.1))

(defn incb [n bd amt]
  (min (+ n amt) bd))

(defn decb [n bd amt]
  (max (- n amt) bd))

(defn decb-abs [n bd amt]
  (let [ori (if (pos? n) 1 -1)]
    (* ori (decb n bd amt))))

(defn apply-dx [e]
  (-> e
      (update :x (comp (partial max 44) (partial + (:dx e))))))

(defn apply-dy [e]
  (-> e
      (update :y + (:dy e))
      (update :dy - 0.5)
      (update :y (partial max 0))))

(defn frame-id [e]
  (second (:frame e)))

(defn camera-follow-actor! [a]
  (let [cam-min (/ GW 2)
        v (+ cam-min (:x a) -384)
        v' (max 0 (- (:x a) 1460))]
    (camera-pos! (:camera @gamestate) (max (+ v v') cam-min) (+ (/ GH 2) -150)))
  (update-cam! (:camera @gamestate)))

(defn get-actor-tex' [e]
  (if (not (on-ground? e))
    (let [dy (:dy e)]
      (cond
        (< 0 dy 30) @act-tex
        (< -30 dy 0) @act-tex
        0 @act-tex))
    (case (first (:frame e))
      :idle (let [t (nth @idle-tex (second (:frame e)))]
              t)
      :move (let [t (nth @walking-tex (second (:frame e)))] t)
      :attack (nth @attack-tex (second (:frame e))))))

(defn get-actor-tex [e]
  (let [t (get-actor-tex' e)]
    (if (:left e)
      t
      (flip-texture t))))

(defn draw-player! [btch a]
  (let [t (get-actor-tex a)
        hw (/ (tex-width t) 2)]
    (draw! btch t (- (:x a) hw) (:y a))))


(defn draw-popup! [btch pu]
  (let [y -30]
    (comment TODO)
    nil))

(def js-tex (delay [(texture "js0.png")
                    (texture "js1.png")
                    (texture "js2.png")
                    (texture "js3.png")]))

(defn render []
  (clear!)
  (let [{:keys [entities actor camera popups]} @gamestate
        actt (get-actor-tex actor)
        bgt @bg-tex
        timer (:timer actor)
        bt @batch]
    (do
      (projection-matrix bt camera)
      (begin-batch bt
                   (draw! bt bgt 0 -135)
                   (draw-player! bt actor)
                   (draw! bt (nth @js-tex (mod (quot timer 15) 4)) 2001 0)

                   (drawr! bt @ab-tex {:x 0 :y 0 :color [1 1 1 (+ 0 (g/trend-until-ceiling 1 0 20 (-> @gamestate :actor :timer)))]})
                   (draw! bt @overlay-tex 0 -135)
                   (if (and (see-dialog?) (cur-dialog))
                     (let [cd (cur-dialog)]
                       (let [t (nth @dialogs cd)]
                         (do
                           (println t)
                           (draw! bt t 0 -100)))))))))

(defn frame-type [a]
  (first (:frame a)))

(defn frame-type? [a t]
  (= (frame-type a) t))

(defn next-walking [a]
  (let [wt (if (frame-type? a :move) (if (< (frame-id a) 5) 2 4) 5)]
    (if (= (mod (:timer a) wt) 0)
      (if (and (frame-type? a :move))
        (update-in a [:frame 1] (fn [n] (if (= n 13) 8 (inc n))))
        (assoc a :frame [:move 0]))
      a)))

(defn stop-walking [a]
  (if (= (mod (:timer a) 2) 0)
    (if (and (frame-type? a :move))
      (if (= (frame-id a) 0)
        (assoc a :frame [:idle 0])
        (update-in a [:frame 1] (comp (partial min 5) dec)))
      (update a :frame identity))
    a))

(defn update-lr [e]
  (let [da (if (on-ground? e) 1 2)]
    (cond
      (i/press? :right) (-> e
                            (update :dx #(incb % 7 da))
                            (next-walking))
      (i/press? :left) (-> e
                           (update :dx #(decb % -7 da))
                           (next-walking))
      :default (-> e
                   (stop-walking)
                   (update :dx (fn [dx]
                                 (cond
                                   (pos? dx) (dec dx)
                                   (neg? dx) (inc dx)
                                   :default dx)))))))

(defn update-u [e]
  (cond
    (i/trigger? :x) (if (on-ground? e) (assoc e :dy 14) e)
    (i/released? :x) (if (> (:dy e) 7)
                        (update e :dy (fn [dy] 7))
                        e)
    :default e))

(defn update-atk [e]
  e)

(def update-movement (comp update-u update-lr))

(defn update-astate [e]
  (-> e
      (update :timer inc)
      (update :frame
              (fn [st] (if (= (mod (:timer e) 8) 0)
                         (case (frame-type e)
                           :attack (if (= (second st) 4)
                                     [:idle 0]
                                     [:attack (inc (second st))])
                           :idle (if (and (= (second st) 9))
                                   [:idle 0]
                                   [:idle (inc (second st))])
                           :move st)
                         st)))))

(defn attacking? [e]
  (frame-type? e :attack))

(defn update-input [e]
  (if (attacking? e)
    e
    ((comp update-atk update-movement) e)))




(defn update-actor [a]
  (if (and (cur-dialog) (see-dialog?))
    (update-astate a)
    ((comp
       (fn [e] (update e :left
                       #(cond (neg? (:dx e)) true (pos? (:dx e)) false :default %)))
       update-astate apply-dy apply-dx update-input) a)))

(defn updato []
  (i/update!)
  (if (and (i/trigger? :space) (see-dialog?) (cur-dialog))
    (consume-dialog!))
  (let [c (:camera @gamestate)]
    (zoom! c (g/trend-until-ceiling 0.3 1 30 (-> @gamestate :actor :timer))))
  (camera-follow-actor! (:actor @gamestate))
  (swap! gamestate #(update % :popups update-popups))
  (swap! gamestate #(update % :actor update-actor)))