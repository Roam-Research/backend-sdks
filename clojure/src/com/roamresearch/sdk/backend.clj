(ns com.roamresearch.sdk.backend
  (:require [cheshire.core :as json])
  (:import [java.net.http HttpClient HttpClient$Redirect HttpRequest$BodyPublishers
            HttpRequest HttpResponse HttpResponse$BodyHandlers]
           [java.net URI]))

(def frontdesk-url  "https://api.roamresearch.com")

(defn- opt-get [^java.util.Optional opt]
  (when (.isPresent opt)
    (.get opt)))

(defonce graph-base-urls (atom {}))

(defn api [{:keys [token graph client]} path method body]
  (try
    (let [^HttpClient client
          (or client
            (-> (HttpClient/newBuilder) (.followRedirects HttpClient$Redirect/NORMAL) .build))
          base-url (@graph-base-urls graph)
          uri (str (or base-url frontdesk-url) path)
          response (.send client
                     (-> uri URI. HttpRequest/newBuilder
                       (.method (name method)
                         (HttpRequest$BodyPublishers/ofString (json/generate-string body)))
                       (.headers (into-array String ["Content-Type", "application/json; charset=utf-8", "Authorization",
                                                     (str "Bearer " token), "x-authorization",
                                                     (str "Bearer " token)]))
                       .build)
                     (HttpResponse$BodyHandlers/ofString))]
      (when-some [^HttpResponse previous-response (opt-get (.previousResponse response))]
        (when-some [^String location (-> previous-response .headers (.firstValue "Location") opt-get)]
          (let [uri (URI. location)]
            (swap! graph-base-urls assoc graph
              (str (.getScheme uri) "://" (.getHost uri) ":" (.getPort uri))))))
      (some->
        (case (.statusCode response)
          200 nil
          400
          (str "Error: " (or (:message (json/parse-string (.body response) true)) "HTTP 400"))
          401
          "Invalid token or token doesn't have enough privileges."
          500
          (str "HTTP Status: " (.statusCode response))
          503
          (str "HTTP Status: " (.statusCode response) ". Your graph is not ready yet for a request, please retry in a few seconds."))
        (ex-info {:response response})
        throw)

      (if (-> response .body .isBlank)
        {}
        (json/parse-string (.body response) true)))
    (catch RuntimeException e (throw e))
    (catch Exception e (throw (ex-info "Caught while calling api"
                                {:path path :method method :body body}
                                e)))))

(defn do-command [conn cmd]
  (api conn (str "/api/graph/" (:graph conn) "/write") :POST cmd))

(defn q [conn query & args]
  (:result
   (api conn (str "/api/graph/" (:graph conn) "/q") :POST {:query query
                                                           :args args})))

(defn pull [conn pattern eid]
  (api conn (str "/api/graph/" (:graph conn) "/pull") :POST
    {:eid eid :selector pattern}))

(defn pull-many [conn pattern eids]
  (api conn (str "/api/graph/" (:graph conn) "/pull-many") :POST
    {:eids eids :selector pattern}))

(defn delete-block [conn uid-or-cmd]
  (let [uid (cond-> uid-or-cmd (map? uid-or-cmd) (-> :block :uid))]
    (do-command conn {:action :delete-block :block {:uid uid}})))

(defn move-block
  ([conn {{:keys [uid]} :block  location :location}]
   (move-block conn uid location))
  ([conn uid {:keys [parent-uid order]}]
   (do-command conn {:action :move-block :location {:order order :parent-uid parent-uid}})))

(defn create-block
  ([conn {:keys [block location]}]
   (create-block conn location (:string block) block))
  ([conn location string block]
   (do-command conn
     {:action :create-block
      :location (select-keys location [:order :parent-uid])
      :block
      (-> block
        (select-keys [:uid :open :heading :text-align :children-view-type])
        (assoc :string string))})))

(defn update-block
  ([conn uid block]
   (do-command conn {:action :update-block
                     :block (select-keys block [:uid :open :heading :text-align :children-view-type :string])}))
  ([conn {:keys [uid block]}]
   (update-block conn uid block)))

(defn create-page
  ([conn title page]
   (do-command conn {:action :create-page
                     :page
                     (-> page
                       (select-keys [:uid :children-view-type])
                       (assoc :title title))}))
  ([conn {:keys [page]}]
   (create-page conn (:title page) page)))

(defn update-page
  ([conn uid page]
   (do-command conn {:action :update-page
                     :page
                     (-> page
                       (select-keys [:title :children-view-type])
                       (assoc :uid uid))}))
  ([conn {:keys [page]}]
   (update-page conn (:uid page) page)))

(defn delete-page [conn uid-or-cmd]
  (do-command conn {:action :delete-page
                    :page {:uid (cond-> uid-or-cmd (map? uid-or-cmd) :uid)}}))

(defn batch [conn & cmds]
  (do-command conn {:action :batch-actions :actions cmds}))

(comment
  (q {:token ""
      :graph "Clojuredart"}
     "[:find ?block-uid ?block-str :in $ ?search-string :where [?b :block/uid ?block-uid] [?b :block/string ?block-str] [(clojure.string/includes? ?block-str ?search-string)]]"
     "apple")

  (pull {:token ""
      :graph "Clojuredart"}
    "[{:block/children [:block/string]}]"  "[:block/uid \"5dTDlS_I3\"]")

  (=
    (pull-many {:token ""
                :graph "Clojuredart"}
      "[{:block/children [:block/string]}]"
      ["[:block/uid \"5dTDlS_I3\"]" "[:block/uid \"U6rvJ7XJC\"]"]
      #_"[[:block/uid \"5dTDlS_I3\"] [:block/uid \"U6rvJ7XJC\"]]")
    (pull-many {:token ""
      :graph "Clojuredart"}
    "[{:block/children [:block/string]}]"
    #_["[:block/uid \"5dTDlS_I3\"]" "[:block/uid \"U6rvJ7XJC\"]"]
    "[[:block/uid \"5dTDlS_I3\"] [:block/uid \"U6rvJ7XJC\"]]"))

  (create-block {:token ""
                 :graph "Clojuredart"}
                {:block {:string "hi from clj sdk"}
                 :location {:parent-uid "01-17-2023" :order :last}})
  )
