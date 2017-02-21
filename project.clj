(defproject app "0.1.0-SNAPSHOT"
  :description "vacation"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [quil "2.3.0"]
                 [incanter "1.5.7"]]
  :jvm-opts ["-Xss128m" "-Xmx12g" "-XX:MaxInlineLevel=16" "-XX:+UseSerialGC"])
