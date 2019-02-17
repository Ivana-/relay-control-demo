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

   [:.a-log-container {:width "200px"
                       :background-color :ivory
                       :overflow :auto
                       :flex-grow 1}
    [:.field {:width "100%"
              :height "2000px"}]]

   [:.svg-container {:width "100%"}
    [:.field {:background-color :black}]]

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
             :background-color :aliceblue
             :border "1px solid #888"
             :margin-top "5px"}]

   [:.label {:margin "10px 5px 0 0"
             :color :darkgray}]

   [:.from-to {:display :flex
               :margin-top "10px"}
    [:.input {:flex-grow 1}]]

   [:.text-area-input {:resize :none
                       :flex-grow 1
                       :white-space :nowrap
                         ;; :width "350px"
                       :margin 0}]])


(defn on-tik [state]
  (let [{:keys [system rocket modelling-speed] :as st} @state
        dt (/ modelling-speed 5000)]
    (swap! state merge {:system (model/move-system system dt)} (model/move-rocket st dt))
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
                            flat-system (model/flatten-system system)
                            planet-r 3]
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
                            [:circle (merge {:cx (norm x area-width) :cy (norm y area-height) :r planet-r :fill :lightblue}
                                            (cond
                                              (or (nil? r) (= 0 r)) {:r 6 :fill :lightyellow}
                                              ;; (> m 0.2) {:stroke-width 2 :stroke :lightblue}
                                              ))])
                          
                          (if show-names
                            (for [{:keys [x y m n r]} flat-system]
                              ^{:key n}
                              [:text.vertex-name {:x (+ 8 (norm x area-width)) :y (+ 8 (norm y area-height)) :stroke "gray"} n]))

                          (if rocket
                            (let [{:keys [x y vx vy]} rocket]
                              [:circle {:cx (norm x area-width) :cy (norm y area-height) :r planet-r :fill :magenta}]))]]))}))

(defn a-log-component [params]
  (let [state (r/atom nil)
        resize 100
        padding 10]
    (r/create-class
     {:component-did-mount (fn [this]
                             (let [root (r/dom-node this)
                                   bounds (.getBoundingClientRect root)]
                               (swap! state assoc
                                      :node root
                                      :width  (- (.-right bounds)  (.-left bounds))
                                      :height (- (.-bottom bounds) (.-top bounds)))))
      :component-did-update
      (fn [this _]
        (let [root (r/dom-node this)
              m (r/props this)]
          ;; (prn (keys m))
          (when (= 0 (rem (-> m :a-log count) resize))
            (.scrollTo root 0 (.-scrollHeight root)))))

      :reagent-render (fn [{:keys [a-log max-rocket-force]}]
                        (let [{:keys [width height]} @state
                              zoom (fn [x limit] (* 0.45 limit (/ x max-rocket-force)))
                              norm (fn [x limit] (+ (* 0.5 limit) (zoom x limit)))
                              chart-area-width (- width 10)]
                          [:div.a-log-container
                           [:svg.field
                            {:style {:height (+ padding resize (* resize (Math.floor (/ (count a-log) resize))))}}
                            (map (fn [[ax1 ax2] i]
                                   ^{:key i}
                                   [:line {:x1 (norm ax1 chart-area-width) :y1 i
                                           :x2 (norm ax2 chart-area-width) :y2 (inc i)
                                           :stroke "black"
                                           :stroke-width 1}]) (partition 2 1 a-log) (range))]]))})))

(def test-system
  {:n "Sun"
   :i [{:n "Mercury" :r 1 :v 3}
       ;; {:n "Venus" :r 2 :v 2.5}
       {:n "Mars" :r 2 :v 2.5
        :i [{:n "Phobos" :r 0.2 :v 12.1}
            {:n "Deimos" :r 0.3 :v -7.9}]}
       {:n "Earth" :r 3 :v -2
        :i [{:n "Moon" :r 0.3 :v 12.9
             :i [{:n "mo-1" :r 0.15 :v -17}]}]}
       {:n "Jupiter" :r 4 :v -1.7
        :i [{:n "Ganymede" :r 0.2 :v 8.1}
            ;; {:n "Callisto" :r 0.3 :v -10.5}
            {:n "Io" :r 0.37 :v -4.3}
            ;; {:n "Europa" :r 0.45 :v -7.2}
            ]}
       ;; {:n "Saturn" :r 6 :v 0.5 :i [{:n "Titan" :r 0.22 :v 6.3}]}
       {:n "Uranus" :r 5 :v -1.3
        :i [{:n "Oberon" :r 0.3 :v 8.4
             :i [{:n "ob-1" :r 0.15 :v -12.3}]}]}
       ;; {:n "Neptune" :r 8 :v 1 :i [{:n "Triton" :r 0.24 :v -5.1}]}
       ]})

(defn main-page [params]
  (let [system-params (fn [system]
                        (let [names (->> system model/flatten-system (map :n))]
                          {:system (model/move-system system)
                           :r-max (model/r-max system)
                           :names names
                           :start-point (first names)
                           :finish-point (first names)}))
        state (r/atom (merge
                       {:modelling-speed 33
                        :max-rocket-force 100
                        :flag-optimize false
                        :user-input-system (with-out-str
                                             (binding [prp/*print-right-margin* 40]
                                               (prp/pprint test-system)))}
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
                         :min 20
                         :max 200
                         :step 2
                         :path [:max-rocket-force]}]
        [:div.label.checkboxes
         [:label.label "optimize"]
         [ws/input-checkbox {:state state
                             :path [:flag-optimize]}]
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
         (ws/input-select {:state state
                           :path [:start-point]
                           :items (:names @state)})
         [:label.label]
         (ws/input-select {:state state
                           :path [:finish-point]
                           :items (:names @state)})]
        [:div.splitter]
        [:button.btn {:on-click #(let [system (cljs.reader/read-string (:user-input-system @state))]
                                   (swap! state merge (system-params system)))} "Apply input"]
        [ws/input-textarea {:state state
                            :path [:user-input-system]
                            :class "input text-area-input"}]
        [:div.splitter]
        [:button.btn {:on-click #(swap! state dissoc :a-log)} "Clear log"]]
       [a-log-component (select-keys @state [:a-log :max-rocket-force])]
       [area-component state]])))
