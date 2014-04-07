#!/bin/sh

usage() {
    echo "Usage: $0 <password>"
    exit 1
}

[ $# -gt 0 ] || usage

java -cp ../lib/jetty-6.1.23.jar:../lib/jetty-util-6.1.23.jar org.mortbay.jetty.security.Password $1
