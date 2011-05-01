(ns org.scaldwell.irc
  (use java.net Socket InetAddress))

(defrecord irc-client [socket nickname] )

(create-irc-client [nickname]
  (irc-client. (. Socket) nickname))
