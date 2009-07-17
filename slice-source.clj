(ns slice-source
  (:import [javax.sound.sampled AudioSystem]
           [java.io File]
           [javax.swing JFrame JPanel TransferHandler]
           [java.awt Color Dimension]
           [java.awt.image BufferedImage]))

(defn get-transfer-handler [component-map]
  (proxy [TransferHandler] []
    (canImport [a] true)
    (importData [a] 
                (let [t (.getTransferable a)
                      f (first (.getTransferDataFlavors t))
                      o (first (.getTransferData t f))]
                  (dosync (alter component-map assoc :file o))
                  (.repaint (:panel @component-map))))))

(defn get-audio-input-stream [in-file]
  (println in-file)
  (AudioSystem/getAudioInputStream in-file))

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

(defn render [g w h component-map]
  (let [img (new BufferedImage w h BufferedImage/TYPE_INT_ARGB)
        values (get-values (get-audio-input-stream (:file @component-map)) w)]
    (render-wav values img)
    (.drawImage g img 0 0 nil)
    (.dispose (.getGraphics img))))

(defn new-slice-source [w h]
  (let [component-map (ref {})
        the-return (proxy [JPanel] [] (paint [g] (render g w h component-map)))]
    (dosync (alter component-map
                   assoc :file (File. "farm.wav") :panel the-return))
    (doto the-return
      (.setPreferredSize (Dimension. w h))
      (.setTransferHandler (get-transfer-handler component-map)))))

(defn go []
  (let [frame (JFrame. "test-ui")
        panel (new-slice-source 800 200)]
    (doto frame (.add panel) (.pack) (.show))))

(go)