(ns wav-render
  (:import [javax.sound.sampled AudioSystem]
           [java.io File]
           [java.awt Color Graphics Dimension]
           [java.awt.image BufferedImage]
           [javax.swing JPanel JFrame]))

(def *width* 800)
(def *height* 200)

(defn get-audio-input-stream [in-path]
  (AudioSystem/getAudioInputStream (File. in-path)))

(defn get-values [in-stream desired-count]
  (let [the-stream-length (* 2 (.getFrameLength in-stream))
        the-skip-factor (/ the-stream-length desired-count)
        the-buffer (make-array Byte/TYPE 2)
        the-return (make-array Short/TYPE desired-count)]
    (doseq [index (range desired-count)]
      (.read in-stream the-buffer 0 2)
      (.skip in-stream the-skip-factor)
      (let [a (aget the-buffer 0)
            b (aget the-buffer 1)] ;; There's an endianness bug here
        (aset-short the-return index (+ (bit-shift-left b 8) a))))
    the-return))

(defn render-wav [in-values io-image]
  (let [the-graphics (.getGraphics io-image)
        w (.getWidth io-image)
        h (.getHeight io-image)]
    (doto the-graphics
      (.setColor Color/white)
      (.fillRect 0 0 w h)
      (.setColor Color/black))
    (doseq [x (range w)]
      (let [v (aget in-values x)]
        (.drawLine the-graphics x (/ h 2) x (+ (/ h 2) (/ (* v h) 32768)))))))

(defn render [g]
  (let [img (new BufferedImage *width* *height* BufferedImage/TYPE_INT_ARGB)]
    (render-wav (get-values (get-audio-input-stream "farm.wav") *width*) img)
    (.drawImage g img 0 0 nil)
    (.dispose (.getGraphics img))))

(def panel (doto (proxy [JPanel] [] (paint [g] (render g)))
             (.setPreferredSize (new Dimension *width* *height*))))

(def frame (doto (new JFrame "wav-render") (.add panel) .pack .show))