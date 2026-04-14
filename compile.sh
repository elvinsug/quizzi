#!/bin/bash
# Quizzi — Compile all servlets
# Usage: ./compile.sh [CATALINA_HOME]
#
# If CATALINA_HOME is not passed as an argument, the script checks the
# CATALINA_HOME environment variable, then falls back to common locations.

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
WEB_DIR="$SCRIPT_DIR/quizzi-web"
CLASSES_DIR="$WEB_DIR/WEB-INF/classes"
LIB_DIR="$WEB_DIR/WEB-INF/lib"

# Resolve CATALINA_HOME
if [ -n "$1" ]; then
    CATALINA_HOME="$1"
elif [ -z "$CATALINA_HOME" ]; then
    for candidate in /usr/local/tomcat /opt/tomcat /opt/homebrew/opt/tomcat/libexec; do
        if [ -d "$candidate" ]; then
            CATALINA_HOME="$candidate"
            break
        fi
    done
fi

if [ -z "$CATALINA_HOME" ]; then
    echo "ERROR: Cannot find Tomcat. Set CATALINA_HOME or pass it as an argument."
    exit 1
fi

SERVLET_API="$CATALINA_HOME/lib/servlet-api.jar"
if [ ! -f "$SERVLET_API" ]; then
    echo "ERROR: servlet-api.jar not found at $SERVLET_API"
    exit 1
fi

MYSQL_JAR=$(find "$LIB_DIR" -name "mysql-connector-*.jar" 2>/dev/null | head -1)
if [ -z "$MYSQL_JAR" ]; then
    echo "WARNING: MySQL connector JAR not found in $LIB_DIR — compilation may fail for DB classes."
    MYSQL_JAR=""
fi

CP="$SERVLET_API"
[ -n "$MYSQL_JAR" ] && CP="$CP:$MYSQL_JAR"

echo "Compiling Quizzi servlets..."
echo "  CATALINA_HOME : $CATALINA_HOME"
echo "  CLASSPATH     : $CP"
echo ""

javac -cp "$CP" -d "$CLASSES_DIR" "$CLASSES_DIR"/com/quizzi/*.java

echo ""
echo "Compilation successful. Deploy quizzi-web/ to \$CATALINA_HOME/webapps/quizzi"
