#!/bin/bash

echo "cleaning target.."
mvn clean &>/dev/null
echo "installing target.."
mvn install &>/dev/null
echo "copying jar.."
cp target/xmpp-proxy-server-1.0-SNAPSHOT-jar-with-dependencies.jar . 
echo "running jar.."
java -cp xmpp-proxy-server-1.0-SNAPSHOT-jar-with-dependencies.jar ar.edu.itba.proxy.MainProxy
