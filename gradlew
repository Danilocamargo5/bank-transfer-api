#!/bin/sh

# Gradle startup script for Unix

APP_NAME="Gradle"
APP_HOME=$(cd "$(dirname "$0")" && pwd)

# Use the maximum available, or set MAX_FD != -1 to use that value.
MAX_FD="maximum"

warn() {
    echo "$*"
}

die() {
    echo
    echo "$*"
    echo
    exit 1
}

# For Darwin, add options to specify how the application appears in the dock
if darwin; then
    GRADLE_OPTS="$GRADLE_OPTS \"-Xdock:name=$APP_NAME\" \"-Xdock:icon=$APP_HOME/media/gradle.icns\""
fi

# Attempt to set APP_HOME
APP_HOME="$(cd "$(dirname "$0")" && pwd)"

# Resolve links
while [ -h "$APP_HOME" ]
do
    ls=$(ls -ld "$APP_HOME")
    link=$(expr "$ls" : '.*-> \(.*\)$')
    if expr "$link" : '/.*' > /dev/null; then
        APP_HOME="$link"
    else
        APP_HOME="$APP_HOME/$(expr "$link" : '.*/\(.*\)')"
    fi
done

CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar:$CLASSPATH"

# Determine the Java command to use to start the JVM.
if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
        # IBM's JDK on AIX uses strange locations for the executables
        JAVACMD="$JAVA_HOME/jre/sh/java"
    else
        JAVACMD="$JAVA_HOME/bin/java"
    fi
else
    JAVACMD="java"
fi

# Increase the maximum file descriptors if we can.
if [ "$darwin" = true ] && [ "$MAX_FD" = "maximum" ] ; then
    MAX_FD=$( ulimit -H -n )
    [ $? -eq 0 ] || MAX_FD=0
fi

# If current process is a cygwin process, convert to proper format, before anything is touched.
if $cygwin ; then
    APP_HOME=$( cygpath --path --mixed "$APP_HOME" )
    CP=$( cygpath --path --windows "$CP" )
    CLASSPATH=$( cygpath --path --windows "$CLASSPATH" )
    JAVACMD=$( cygpath --windows "$JAVACMD" )
    for jar in "$APP_HOME"/gradle/wrapper/*.jar; do
        CLASSPATH="$jar:$CLASSPATH"
    done
fi

GRADLE_OPTS="$GRADLE_OPTS \"-Dorg.gradle.appname=$APP_NAME\" -Xmx1024m"

exec "$JAVACMD" \
    -classpath "$CLASSPATH" \
    org.gradle.wrapper.GradleWrapperMain "$@"
