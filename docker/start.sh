#!/bin/sh
##export JAVA_TOOL_OPTIONS="-Djavax.net.debug=all -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,address=55555,server=y,suspend=n"

mkdir -p /dev/net
mknod /dev/net/tun c 10 200
chmod 600 /dev/net/tun

java -jar /openvpn/webui/webui.jar

echo "\n\n\n\n\n############### ERROR ############# \n\n\n\n\n"
sleep 100000