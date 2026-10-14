#!/bin/sh
# Gradle wrapper execution script for POSIX systems
DIRNAME=$(dirname "$0")
APP_BASE_NAME=$(basename "$0")

# Use JAVA_HOME if set
if [ -n "$JAVA_HOME" ] ; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

CLASSPATH="$DIRNAME/gradle/wrapper/gradle-wrapper.jar"
if [ ! -f "$CLASSPATH" ]; then
    # Fallback if wrapper jar is absent: try system gradle if present
    if command -v gradle >/dev/null 2>&1; then
        exec gradle "$@"
    fi
fi

exec "$JAVACMD" "-Dorg.gradle.appname=$APP_BASE_NAME" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
