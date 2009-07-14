(ns wav-render
  (:import [javax.sound.sampled AudioSystem]
           [java.io File DataInputStream]
           [java.awt Color Graphics Dimension]
           [java.awt.image BufferedImage]
           [javax.swing JPanel JFrame]))

(def *width* 800)
(def *height* 200)

(defn get-values [in-path]
  (let [the-file (File. in-path)
        the-stream (AudioSystem/getAudioInputStream the-file)
        the-stream-length (* 2 (.getFrameLength the-stream))
        the-skip-factor (/ the-stream-length *width*)
        the-buffer (make-array Byte/TYPE 2)
        the-return (make-array Short/TYPE *width*)]
    (doseq [index (range *width*)]
      (.read the-stream the-buffer 0 2)
      (.skip the-stream the-skip-factor)
      (let [a (aget the-buffer 0)
            b (aget the-buffer 1)]
        (aset-short the-return index (+ (bit-shift-left b 8) a))))
    the-return))

(defn render [g]
  (let [img (new BufferedImage *width* *height* BufferedImage/TYPE_INT_ARGB)
        bg (.getGraphics img)
        the-values (get-values "farm.wav")]
    (doto bg
      (.setColor Color/white)
      (.fillRect 0 0 (.getWidth img) (.getHeight img))
      (.setColor Color/black))
    (doseq [x (range *width*)]
      (let [v (aget the-values x)]
        (.drawLine bg x (/ *height* 2)
                      x (+ (/ *height* 2) (/ (* v *height*) 32768)))))
    (.drawImage g img 0 0 nil)
    (.dispose bg)))

(def panel (doto (proxy [JPanel] [] (paint [g] (render g)))
             (.setPreferredSize (new Dimension *width* *height*))))

(def frame (doto (new JFrame "wav-render") (.add panel) .pack .show))