(ns star-system.core
  (:require [reagent.core :as r]
            [graph-view.core :as graph-view]))

(defn ^:export init []
  (when goog.DEBUG
    (enable-console-print!)
    (println "dev mode"))
  (r/render [graph-view/main-page] (.getElementById js/document "app")))
