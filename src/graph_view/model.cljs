(ns graph-view.model)

(defn flatten-system [s] (conj (mapcat flatten-system (:i s)) (dissoc s :i)))

(defn r-max [{:keys [r i]}] (->> i (map r-max) (cons 0) (apply max) (+ (or r 0))))

(defn move-system
  ([s] (move-system s 0 0 0 0 0))
  ([s dt] (move-system s dt 0 0 0 0))
  ([{:keys [r v al] :as s} dt x y vx vy]
   (let [r- (or r 0)
         v- (or v 0)
         al- (or al 0)
         cos-al (Math/cos al-)
         sin-al (Math/sin al-)
         al* (+ al- (* dt v-))
         x* (+ x (* r- cos-al))
         y* (+ y (* r- sin-al))
         vabs (* v- r-)
         vx* (+ vx (- (* vabs sin-al)))
         vy* (+ vy (* vabs cos-al))]
     (-> s
         (assoc :x x* :y y* :vx vx* :vy vy* :al al* :xp x :yp y)
         (update :i (fn [i] (mapv #(move-system % dt x* y* vx* vy*) i)))))))

(defn calculate-force-direction [x v absa]
  (let [roots (fn [a] (let [a* a
                            b* (* 2 v)
                            c* (+ x (/ (* v v) (* 2 a)))
                            d (- (* b* b*) (* 4 a* c*))
                            fr (fn [x]
                                 (let [t1 (/ (- (* x (Math/sqrt d)) b*) (* 2 a*))
                                       t2 (+ t1 (/ v a))]
                                   (if (and (> t1 0) (> t2 0)) {:dir x :t1 t1 :t2 t2})))]
                        (if (>= d 0) (filter identity (map fr [1 -1])))))]
    (or (->> [absa (- absa)] (mapcat roots) first :dir) 0)))


;; test filtering relay PWM-type control signal

#_(defn move-rocket [{:keys [system rocket a-log r-log rx-1 ry-1
                           max-rocket-force spline-factor start-point finish-point]} dt]
  (when rocket
    (let [{xr :x yr :y vxr :vx vyr :vy} rocket
          flat-system (flatten-system system)
          #_{:keys [ax ay]}
          #_(reduce (fn [acc {:keys [x y m n]}]
                      (let [dx (- x xr)
                            dy (- y yr)
                            dl2 (+ (* dx dx) (* dy dy))
                            dl (Math/sqrt dl2)
                            ai (/ (* 1 m) (+ 0.01 dl2))]
                        (cond
                          (= start-point n) acc
                          :else (-> acc
                                    (update :ax + (* ai (/ dx dl)))
                                    (update :ay + (* ai (/ dy dl))))))) {:ax 0 :ay 0} flat-system)
          amax max-rocket-force
          {xf :x yf :y vxf :vx vyf :vy} (->> flat-system (filter #(= finish-point (:n %))) first)
          ax* (* amax (calculate-force-direction (- xr xf) (- vxr vxf) amax))
          ay* (* amax (calculate-force-direction (- yr yf) (- vyr vyf) amax))
          
          ;; ri = al*ai + (1-al)*ri_1
          al spline-factor
          ax (+ (* al ax*) (* (- 1 al) (or rx-1 ax*)))
          ay (+ (* al ay*) (* (- 1 al) (or ry-1 ay*)))
          
          ax- ax
          ay- ay

          vx (+ vxr (* ax- dt))
          vy (+ vyr (* ay- dt))
          x (+ xr (* vxr dt) (* 0.5 ax- dt dt))
          y (+ yr (* vyr dt) (* 0.5 ay- dt dt))
          ]
      {:rocket (assoc rocket :x x :y y :vx vx :vy vy)
       :a-log (conj (or a-log []) [ax ay])
       :rx-1 ax
       :ry-1 ay})))

(defn sign [x] (cond (> x 0) 1 (< x 0) -1 :else 0))
(defn abs  [x] (if (>= x 0) x (- x)))

; (defn calculate-force-direction* [x v absa]
;   (let [sx (sign x)
;         sv (sign v)]
;     (if (or (= sx sv) (> (abs x) (/ (* v v) (* 2 absa)))) (- sx) sx))

(defn calculate-force [amax dx dv dt flag-optimize]
  (let [a1 (+ (/ dx dt dt) (/ (* 1.5 dv) dt))
        a2 (- (/ dv dt) a1)]
    ;; (js/isNaN 0)
    (if (and flag-optimize (<= (- amax) a1 amax) (<= (- amax) a2 amax))
      a1
      ;;(* amax (calculate-force-direction (- dx) (- dv) amax))
      (* amax (let [sx (sign dx)
                    sv (sign dv)]
                (if (or (= sx sv) (> (abs dx) (/ (* dv dv) (* 2 amax)))) sx (- sx)))))))

(defn move-rocket [{:keys [system rocket max-rocket-force start-point finish-point a-log flag-optimize]} dt]
  (when rocket
    (let [{xr :x yr :y vxr :vx vyr :vy} rocket
          {xf :x yf :y vxf :vx vyf :vy} (->> system flatten-system (filter #(= finish-point (:n %))) first)
          ax (calculate-force max-rocket-force (- xf xr) (- vxf vxr) dt flag-optimize)
          ay (calculate-force max-rocket-force (- yf yr) (- vyf vyr) dt flag-optimize)

          vx (+ vxr (* ax dt))
          vy (+ vyr (* ay dt))
          x (+ xr (* vxr dt) (* 0.5 ax dt dt))
          y (+ yr (* vyr dt) (* 0.5 ay dt dt))]
      {:rocket (assoc rocket :x x :y y :vx vx :vy vy)
       :a-log (conj (or a-log []) ax)})))
