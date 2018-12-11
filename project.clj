(defproject star-system "0.1.0-SNAPSHOT"
  :description "star-system"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :min-lein-version "2.7.1"

  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.10.238"]
                 [reagent "0.7.0"]
                 [garden "1.3.5"]]

  :plugins [[lein-figwheel "0.5.16"]
            [lein-cljsbuild "1.1.7" :exclusions [[org.clojure/clojure]]]]

  :source-paths ["src"]

  :cljsbuild {:builds
              [{:id "dev"
                :source-paths ["src"]
                :figwheel {:on-jsload "star-system.core/on-js-reload"
                           :open-urls ["http://localhost:3449"]}
                :compiler {:main star-system.core
                           :asset-path "js/compiled/out"
                           :output-to "resources/public/js/compiled/star_system.js"
                           :output-dir "resources/public/js/compiled/out"
                           :source-map-timestamp true
                           :preloads [devtools.preload]}}
               {:id "min"
                :source-paths ["src"]
                :compiler {:output-to "resources/public/js/compiled/star_system.js"
                           :main star-system.core
                           :optimizations :advanced
                           :pretty-print false}}]}

  :figwheel {:http-server-root "public"
             :server-port 3449
             :nrepl-port 7003}

  :profiles {:dev {:dependencies [[binaryage/devtools "0.9.9"]
                                  [figwheel-sidecar "0.5.16"]
                                  [cider/piggieback "0.3.1"]]
                   :source-paths ["src" "dev"]
                   :clean-targets ^{:protect false} ["resources/public/js/compiled" :target-path]}})
