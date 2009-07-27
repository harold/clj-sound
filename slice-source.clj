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
  (AudioSystem/getAudioInputStream in-file))

(defn get-values [in-stream desired-count]
  (let [the-stream-length (* 2 (.getFrameLength in-stream))
        the-skip-factor (/ the-stream-length desired-count)
        the-buffer (make-array Byte/TYPE 2)
        out-values (make-array Short/TYPE desired-count)]
    (doseq [index (range desired-count)]
      (.read in-stream the-buffer 0 2)
      (.skip in-stream the-skip-factor)
      (let [a (aget the-buffer 0)
            b (aget the-buffer 1)] ;; There's an endianness bug here
        (aset-short out-values index (+ (bit-shift-left b 8) a))))
    out-values))

(defn render [g w h component-map]
  (let [values (get-values (get-audio-input-stream (:file @component-map)) w)]
    (doto g
      (.setColor Color/white)
      (.fillRect 0 0 w h)
      (.setColor Color/black))
    (doseq [x (range w)]
      (let [v (aget values x)]
        (.drawLine g x (/ h 2) x (+ (/ h 2) (/ (* v h) 32768)))))))

(defn new-slice-source [w h]
  (let [component-map (ref {})
        the-panel (proxy [JPanel] [] (paint [g] (render g w h component-map)))]
    (dosync (alter component-map assoc :file (File. "farm.wav") :panel the-panel))
    (doto the-panel
      (.setPreferredSize (Dimension. w h))
      (.setTransferHandler (get-transfer-handler component-map)))))

(defn go []
  (let [frame (JFrame. "test-ui")
        panel (new-slice-source 800 200)]
    (doto frame (.add panel) (.pack) (.show))))

(go)