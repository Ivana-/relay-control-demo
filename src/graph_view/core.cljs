(ns graph-view.core
  (:require [reagent.core :as r]
            [garden.core :as garden]
            [clojure.string :as str]
            [graph-view.widgets :as ws]
            [graph-view.model :as model]
            [cljs.pprint :as prp]))

(defn make-style [css] [:style (garden/css css)])

(def style
  [:.page {:display "flex"
           :height :-webkit-fill-available}

   [:.bar {:display "inline-flex"
           :flex-direction "column"
           :margin "0 10px"}]

   [:.svg-container {:width "100%"
                     :background-color :black}]

   [:.field {:background-color :black}]

   ;; [:.draggable {:cursor :move}]
   [:.vertex-name {:font-family :system-ui
                   :font-size "10px"
                   :color :gray}]

   [:.btn {:font-size "20px"
           :cursor :pointer
           :background-color :gray
           :border :none
           :outline :none}]
   [:.red {:background-color "#cc0000"}]
   [:.green {:background-color "#009900"}]

   [:.splitter {:height "20px"}]

   [:.input {:outline :none
             :font-size "16px"
             ;; :color :honeydew
             :background-color :aliceblue ;; :black
             :border "1px solid #888"
             :margin-top "5px"}]

   [:.label {:margin "10px 5px 0 0"
             :color :darkgray}]

   [:.from-to {:display :flex
               :margin-top "10px"}
    [:.input {:flex-grow 1
              :margin-left "5px"}]]

   [:.text-area-input {:resize :none
                       :flex-grow 1
                       :width "350px"
                       :spell-check false}]
   ])


(defn on-tik [state]
  (let [{:keys [system rocket modelling-speed] :as st} @state
        dt (/ modelling-speed 5000)]
    (swap! state assoc :system (model/move-system system dt) :rocket (model/move-rocket st dt))
    (:simulation-on @state)))

(def timeout-ms 100)

(defn periodic [f v]
  (-> (js/Promise. (fn [resolve] (js/setTimeout #(resolve (f v)) timeout-ms)))
      (.then #(if % (periodic f v)))
      (.catch prn)))

(defn stop-go [state]
  (if-not (:simulation-on @state) (periodic on-tik state))
  (swap! state update :simulation-on not))


(defn area-component [state]
    (r/create-class
     {:component-did-mount (fn [this]
                              (let [root (r/dom-node this)
                                    bounds (.getBoundingClientRect root)]
                                (swap! state assoc
                                       :area-node root
                                       :area-width  (- (.-right bounds)  (.-left bounds))
                                       :area-height (- (.-bottom bounds) (.-top bounds)))))
       :reagent-render (fn [state]
                         (let [{:keys [area-width area-height system rocket show-names r-max]} @state
                               zoom (fn [x limit] (* 0.45 limit (/ x r-max)))
                               norm (fn [x limit] (+ (* 0.5 limit) (zoom x limit)))
                               flat-system (model/flatten-system system)]
                           [:div.svg-container
                            [:svg.field  {:width "100%" :height "100%"}

                             (for [{:keys [xp yp r n]} flat-system]
                               ^{:key n}
                               [:ellipse {:cx (norm xp area-width) :cy (norm yp area-height)
                                          :rx (zoom r area-width)
                                          :ry (zoom r area-height)
                                          :stroke-width 1
                                          :stroke "#505050"
                                          :fill-opacity 0}])

                             (for [{:keys [x y m n r]} flat-system]
                               ^{:key n}
                               [:circle (merge {:cx (norm x area-width) :cy (norm y area-height) :r 2 :fill :aliceblue}
                                               (cond
                                                 (or (nil? r) (= 0 r)) {:r 6 :fill :yellow}
                                                 (> m 0.2) {:stroke-width 2 :stroke :lightblue}))])

                             (if show-names
                               (for [{:keys [x y m n r]} flat-system]
                                 ^{:key n}
                                 [:text.vertex-name {:x (+ 8 (norm x area-width)) :y (+ 8 (norm y area-height)) :stroke "gray"} n]))

                             (if rocket
                               (let [{:keys [x y vx vy]} rocket]
                                 [:circle {:cx (norm x area-width) :cy (norm y area-height) :r 2 :fill :red}]))]]))}))

(def test-system
  {:n "Sun" :m 50
   :i [{:n "Mercury" :m 0.3 :r 1 :v 3}
       {:n "Venus" :m 4.8 :r 2 :v 2.5}
       {:n "Earth" :m 6 :r 3 :v -2
        :i [{:n "Moon" :m 0.05 :r 0.3 :v 12.9
             :i [{:n "mo-1" :m 0.01 :r 0.15 :v -17}]}]}
       {:n "Mars" :m 0.6 :r 4 :v 1.5
        :i [{:n "Phobos" :m 0.07 :r 0.2 :v 12.1}
            {:n "Deimos" :m 0.05 :r 0.3 :v -7.9}]}
       {:n "Jupiter" :m 20 :r 5 :v -1.7
        :i [{:n "Ganymede" :m 0.1 :r 0.2 :v 8.1}
            {:n "Callisto" :m 0.09 :r 0.3 :v -10.5}
            {:n "Io" :m 0.08 :r 0.37 :v -4.3}
            {:n "Europa" :m 0.07 :r 0.45 :v -7.2}]}
       {:n "Saturn" :m 15 :r 6 :v 0.5
        :i [{:n "Titan" :m 0.07 :r 0.22 :v 6.3}]}
       {:n "Uranus" :m 9 :r 7 :v -1.3
        :i [{:n "Oberon" :m 0.05 :r 0.3 :v 8.4
             :i [{:n "ob-1" :m 0.01 :r 0.15 :v -12.3}]}]}
       {:n "Neptune" :m 10 :r 8 :v 1
        :i [{:n "Triton" :m 0.09 :r 0.24 :v -5.1}]}]})

(defn main-page [params]
  (let [system-params (fn [system]
                        (let [names (->> system model/flatten-system (map :n))]
                          {:system (model/move-system system)
                           :r-max (model/r-max system)
                           :names names
                           :start-point (first names)
                           :finish-point (first names)}))
         state (r/atom (merge
                       {:modelling-speed 50
                       :max-rocket-force 50
                       :user-input-system (with-out-str (prp/pprint test-system))}
                       (system-params test-system)))]
    (fn [params]
      [:div.page
       [make-style style]
       [:span.bar
        [:button.btn {:on-click #(stop-go state)
                      :class (if (:simulation-on @state) :red :green)} (if (:simulation-on @state) "Stop" "Go!")]
        [:label.label "modelling speed"]
        [ws/input-range {:state state
                         :min 10
                         :path [:modelling-speed]}]
        [:label.label "max rocket force"]
        [ws/input-range {:state state
                         :min 10
                         :path [:max-rocket-force]}]
        [:div.names
         [:label.label "show names"]
         [ws/input-checkbox {:state state
                             :path [:show-names]}]]
        [:div.splitter]
        [:button.btn {:on-click (fn [_] (swap! state assoc :rocket
                                  (when-not (:rocket @state)
                                    (let [st @state
                                          p (->> st
                                                 :system
                                                 model/flatten-system
                                                 (filter #(= (:start-point st) (:n %)))
                                                 first)]
                                      (select-keys p [:x :y :vx :vy])))))
                      :class (if (:rocket @state) :red :green)} (str "Rocket " (if (:rocket @state) "stop" "start!"))]
        [:div.from-to
         [:label.label "from / to"]
         (ws/input-select {:state state
                          :path [:start-point]
                          :items (:names @state)})
         (ws/input-select {:state state
                          :path [:finish-point]
                          :items (:names @state)})]
        [:div.splitter]
        [:button.btn {:on-click #(let [system (cljs.reader/read-string (:user-input-system @state))]
                                  (swap! state merge (system-params system)))} "Apply input"]
        [ws/input-textarea {:state state
                            :path [:user-input-system]
                            :class "input text-area-input"}]]
       [area-component state]])))
