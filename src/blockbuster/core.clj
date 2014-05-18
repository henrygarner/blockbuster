(ns blockbuster.core
  (:require [redstone.client :as mc]
            [clojure.string :as s]
            [clojure.java.io :refer [input-stream resource reader file]]
            [clojure.java.shell :refer [sh]]
            [clojure.set :refer [rename-keys]]
            [me.raynes.fs :refer [temp-file temp-dir]]
            [clojure.tools.cli :refer [parse-opts]])
  (:gen-class :main true))

(def server
  {:host "localhost"
   :port 4711})

(defn byte-seq [reader]
  (let [result (.read reader)]
    (if (= result -1)
      (do (.close reader) nil)
      (lazy-seq (cons result (byte-seq reader))))))

(defn distance-squared [c1 c2]
  (->> (map - c1 c2)
       (map #(* % %))
       (reduce +)))

(def block-colours 
  {[221 221 221] :wool
   [219 126 63] :orange-wool
   [179 81 188] :magenta-wool
   [108 138 201] :light-blue-wool
   [177 166 40] :yellow-wool
   [66 175 58] :lime-wool
   [208 132 153] :pink-wool
   [65 65 65] :gray-wool
   [154 161 161] :light-gray-wool
   [47 111 137] :cyan-wool
   [127 62 181] :purple-wool
   [47 57 141] :blue-wool
   [80 51 32] :brown-wool
   [54 71 28] :green-wool
   [150 53 49] :red-wool
   [26 23 23] :black-wool})

(defn rgb->block [colour-map rgb-triple]
  (colour-map
   (apply min-key (partial distance-squared rgb-triple) (keys colour-map))))

(defn ppm-parser
  "Removes newlines and lines beginning with # until we have read three lines.
   These correspond to the magic code, dimensions and max value headers.
   Following lines are pixel data and are passed through unfiltered."
  [lines-read line]
  (if (and (<= (count lines-read) 3)
           (or (-> line first (= 35))
               (-> line first (= 10))))
    lines-read
    (conj lines-read line)))

(defn read-ppm [byte-seq]
  (let [[_ dimensions _ & image-bytes] (->> byte-seq
                                            (partition-by #(= % 10))
                                            (reduce ppm-parser []))
        [width height] (->> dimensions
                            (map char)
                            (apply str)
                            (#(s/split % #" "))
                            (map #(Long/parseLong %)))
        pixels (->> image-bytes
                    (apply concat)
                    (partition 3))]
    {:dimensions {:width width :height height} :pixels pixels}))

(defn image->ppm! [path width]
  (let [tmpfile (str (doto (temp-file "minecraftimage" ".ppm") .deleteOnExit))]
    (println tmpfile)
    (sh "convert" path "-resize" (str width) tmpfile)
    tmpfile))

(defn draw-ppm! [path {:keys [position] :as options}]
  (let [{:keys [dimensions pixels]} (read-ppm (-> path input-stream byte-seq))
        {:keys [x y z]} position
        {:keys [width height]} dimensions
        blocks (map vector (for [y1 (range height)
                                 x1 (range width)]
                             [x1 (- height y1)]) pixels)]
     (doseq [[[x1 y1] pixel] blocks]
       (let [color (rgb->block block-colours pixel)]
         (mc/set-block-at! server {:x (+ x x1) :y (+ y y1) :z z} color)))))

(defn draw-image! [{:keys [frame-path width] :as options}]
  (-> frame-path
      (image->ppm! width)
      (draw-ppm! options)))

(defn draw-images! [{:keys [frames-dir every-n pause] :as options}]
  (println "Drawing frames...")
  (doseq [frame-path (->> frames-dir
                          file
                          file-seq
                          (drop 1)
                          (take-nth every-n))]
    (-> options
        (assoc :frame-path (str frame-path))
        (draw-image!))
    (Thread/sleep pause)))

(defn draw-movie! [{:keys [path position] :as options}]
  (let [frames-dir (temp-dir "minecraft")]
    (println "Processing frames...")
    (sh "ffmpeg" "-i" path "-y" (str frames-dir "/%09d.jpg"))
    (draw-images! (assoc options :frames-dir frames-dir))))

(defn clear-space! [{:keys [position width]}]
  (let [{:keys [x y z]} position]
    (println "Clearing space...")
    (doseq [x (range x (+ x width))
            y (range y (+ y width))
            z (range z (+ z width))]
      (mc/set-block-at! server {:x x :y y :z z} :air))))

(def cli-options
  [["-f" "--file FILE" "File path"]
   ["-w" "--width BLOCKS" "Width of the screen in blocks"
    :parse-fn #(Long/parseLong %)
    :default 25]
   ["-p" "--position X,Y,Z" "The position of the screen"
    :default {:x 0 :y 0 :z 0}
    :parse-fn (fn [arg]
                (->> (s/split arg #",")
                     (map #(Long/parseLong %))
                     (zipmap [:x :y :z])))]
   ["-n" "--every-n FRAMES" "Draw every n frames"
    :default 1
    :parse-fn #(Long/parseLong %)]
   ["-m" "--pause MILLIS" "Milliseconds to pause between frames"
    :default 0
    :parse-fn #(Long/parseLong %)]
   ["-c" "--clear" "Whether space should be cleared for the screen"
    :default true]])

(defn -main [& args]
  (let [parsed-args (parse-opts args cli-options)
        options (-> parsed-args
                    :options
                    (rename-keys {:file :path}))]
    (clear-space! options)
    (draw-movie! options)))
