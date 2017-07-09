(ns reagent-modals.modals
  (:require [reagent.core :as r :refer [atom]]
            [goog.dom :as dom]
            [goog.events :as events])
  (:import [goog.events EventType]))

;;; Make sure to create the modal-window element somewhere in the dom.
;;; Recommended: at the start of the document.


(def modal-content (atom {}))

(def modal-id (atom nil))

(defn init-empty-modals [modal-ids]
  (reset! modal-content (zipmap [modal-ids] (repeat {:content [:div] :shown nil :size nil}))))

(defn get-modal []
  (dom/getElement @modal-id))

(defn- with-opts [opts]
  (let [m (js/jQuery (get-modal))]
    (.call (aget m "modal") m opts)
    (.call (aget m "modal") m "show")
    m))

(defmulti show-modal! (fn [args] (map? args))) ;; backward compatibility
(defmethod show-modal! true
  [{:keys [keyboard backdrop] :or {keyboard true backdrop true}}]
  (with-opts #js {:keyboard keyboard :backdrop  backdrop}))

(defmethod show-modal! false [keyboard]
  (with-opts #js {:keyboard keyboard}))

(defn close-modal! []
  (let [m (js/jQuery (get-modal))]
    (.call (aget m "modal") m "hide")))

(defn close-button
  "A pre-configured close button. Just include it anywhere in the
   modal to let the user dismiss it." []
   [:button.close {:type "button" :data-dismiss "modal"}
     [:span.glyphicon.glyphicon-remove {:aria-hidden "true"}]
     [:span.sr-only "Close"]])



(defn modal-window* [m-id]
  (let [{:keys [content size]} (get @modal-content m-id)
        size-class {:lg "modal-lg"
                    :sm "modal-sm"}]
    [:div.modal.fade {:id m-id :tab-index -1 :role "dialog"}
     [:div.modal-dialog {:class (get size-class size)}
      [:div.modal-content
       content]]]))

(defn modal-window [m-id]
  (with-meta
    (modal-window* m-id)
    {:component-did-mount
     (fn [e] (let [m (js/jQuery (get-modal))]
               (.call (aget m "on") m "hidden.bs.modal"
                      #(do (when-let [f (:hidden (get @modal-content m-id))] (f))
                           (swap! modal-content assoc m-id {:content [:div]}))) ;;clear the modal when hidden
               (.call (aget m "on") m "shown.bs.modal"
                      #(when-let [f (:shown (get @modal-content m-id))] (f)))
               (.call (aget m "on") m "hide.bs.modal"
                      #(when-let [f (:hide (get @modal-content m-id))] (f)))))}))


;;; main function


(defn modal!
  "Update and show the modal window. `reagent-content' is a normal
   reagent component. `configs' is an optional map of advanced
   configurations:

   - :shown -> a function called once the modal is shown.
   - :hide -> a function called once the modal is asked to hide.
   - :hidden -> a function called once the modal is hidden.
   - :size -> Can be :lg (large) or :sm (small). Everything else defaults to medium.
   - :keyboard -> if true, `esc' key can dismiss the modal. Default to true.
   - :backdrop -> true (default): backdrop.
                  \"static\" : backdrop, but doesn't close the model when clicked upon.
                  false : no backdrop."
  ([m-id reagent-content]
    (reset! modal-id m-id)
    (modal! reagent-content nil))
  ([m-id reagent-content configs]
   (reset! modal-id m-id)
   (swap! modal-content assoc m-id (merge {:content reagent-content} configs))
   (show-modal! (select-keys configs [:keyboard :backdrop]))))
