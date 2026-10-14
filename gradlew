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
    echo "Downloading Gradle wrapper jar..."
    mkdir -p "$DIRNAME/gradle/wrapper"
    if command -v curl >/dev/null 2>&1; then
        curl -fsSL https://raw.githubusercontent.com/gradle/gradle/v8.5.0/gradle/wrapper/gradle-wrapper.jar -o "$CLASSPATH" 2>/dev/null || \
        curl -fsSL https://repo.gradle.org/artifactory/libs-releases/org/gradle/gradle-wrapper/8.5/gradle-wrapper-8.5.jar -o "$CLASSPATH" 2>/dev/null
    elif command -v wget >/dev/null 2>&1; then
        wget -q -O "$CLASSPATH" https://raw.githubusercontent.com/gradle/gradle/v8.5.0/gradle/wrapper/gradle-wrapper.jar 2>/dev/null
    fi
fi

if [ ! -f "$CLASSPATH" ]; then
    if command -v gradle >/dev/null 2>&1; then
        exec gradle "$@"
    fi
fi

exec "$JAVACMD" "-Dorg.gradle.appname=$APP_BASE_NAME" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
