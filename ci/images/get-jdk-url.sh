#!/bin/bash
set -e

case "$1" in
	java17)
		 echo "https://github.com/bell-sw/Liberica/releases/download/17.0.10+13/bellsoft-jdk17.0.10+13-linux-amd64.tar.gz"
	;;
	java21)
	   echo "https://github.com/bell-sw/Liberica/releases/download/21.0.2+14/bellsoft-jdk21.0.2+14-linux-amd64.tar.gz"
	;;
	*)
		echo $"Unknown java version"
		exit 1
esac
