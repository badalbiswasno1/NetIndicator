#!/bin/sh
APP_HOME=$( cd "${APP_HOME:-./}" > /dev/null && pwd -P ) || exit
exec java -Xmx64m -Xms64m -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
