#!/bin/bash

. /home/play/.profile

cd /home/play/steamwatch/updater/

export _JAVA_OPTIONS="-Xmx96m -Xms32m"
java -Xmx96m -Xms32m -jar updater_2.9.2-1.2.0-one-jar.jar


