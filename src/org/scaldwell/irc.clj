(ns org.scaldwell.irc
  (:use [clojure.string :only (trim)])
  (:import (java.net Socket InetSocketAddress SocketException InetAddress)
          (java.io InputStream OutputStreamWriter)))

(defrecord irc-client [socket nickname realname username lock] )

(defn make-irc-client [nickname username realname]
  (irc-client. (Socket.) nickname realname username (Object.)))


(defn send-msg [irc-client msg]
  (let [out (OutputStreamWriter. (.getOutputStream (:socket irc-client)))
        m (str (trim msg) \return \newline)]
      (locking (:lock irc-client)
        (do 
          (print (str "Sending " (trim m) \newline))
          (.write out m 0 (count m))))))

(defn user [irc-client hostname]
  (send-msg irc-client (str "USER " (:username irc-client) " " (.getHostName (InetAddress/getLocalHost)) " " hostname " :" (:realname irc-client))))

(defn nick [irc-client]
  (send-msg irc-client (str "NICK " (:nickname irc-client))))

(defn connect [irc-client host port]
  (let [s (try
            (doto (:socket irc-client)
            (.connect (InetSocketAddress. host port)))
            (catch SocketException ex nil))]
    (do
      (user irc-client host)
      (nick irc-client))))

(defn- read-line  [rdr]
  (let [sb (StringBuffer.)]
   (loop [chr (.read rdr)]
     (if (= chr -1)
       -1
      (if  (= (char chr) \newline)
        (.toString sb)
        (do
          (.append sb (char chr))
          (recur (.read rdr))))))))

(defn- process [irc-client]
  (let [rdr (.getInputStream (:socket irc-client)) 
        line (read-line rdr)]
    (if (= -1 line)
      -1
      (.write *out* (str "read line: " line \newline)))))
      
(defn- on-thread [fun]
  (.start  (Thread. fun)))

(defn run [irc-client]
  (loop[] 
    (when-not (.isClosed (:socket irc-client))
    (try
      (when-not (= -1 (process irc-client))
        (recur))
      (catch Exception ex 
        (.write *out* (str "Caught exception: " (.getMessage ex) \newline)))))))
