#!/bin/sh

# Find Java
if [ -n "$JAVA_HOME" ] ; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

# Get Gradle wrapper jar
WRAPPER_JAR="gradle/wrapper/gradle-wrapper.jar"

# Exec Gradle
exec "$JAVACMD" -jar "$WRAPPER_JAR" "$@"
