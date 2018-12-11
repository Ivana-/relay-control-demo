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

(defn move-rocket [{:keys [system rocket max-rocket-force start-point finish-point]} dt]
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
          amax (* 2 max-rocket-force)
          {xf :x yf :y vxf :vx vyf :vy} (->> flat-system (filter #(= finish-point (:n %))) first)
          ax (* amax (calculate-force-direction (- xr xf) (- vxr vxf) amax))
          ay (* amax (calculate-force-direction (- yr yf) (- vyr vyf) amax))
          vx (+ vxr (* ax dt))
          vy (+ vyr (* ay dt))
          x (+ xr (* vxr dt) (* 0.5 ax dt dt))
          y (+ yr (* vyr dt) (* 0.5 ay dt dt))]
      (assoc rocket :x x :y y :vx vx :vy vy))))
