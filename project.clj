(defproject blockbuster "0.1.0"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [redstone "0.1.4"]
                 [org.clojure/tools.cli "0.3.1"]
                 [me.raynes/fs "1.4.5"]]
  :aot [blockbuster.core]
  :main blockbuster.core)
