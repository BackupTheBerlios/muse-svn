#! /bin/sh

#   Copyright (c) 2001-2003 The Apache Software Foundation.  All rights
#   reserved.

# load system-wide ant configuration
if [ -f "/etc/ant.conf" ] ; then 
  . /etc/ant.conf
fi

# load user ant configuration
if [ -f "$HOME/.antrc" ] ; then 
  . "$HOME/.antrc"
fi

# OS specific support.  $var _must_ be set to either true or false.
cygwin=false;
darwin=false;
case "`uname`" in
  CYGWIN*) cygwin=true ;;
  Darwin*) darwin=true
           if [ -z "$JAVA_HOME" ] ; then
             JAVA_HOME=/System/Library/Frameworks/JavaVM.framework/Home   
           fi
           ;;
esac

if [ -z "$JAVACMD" ] ; then 
  if [ -n "$JAVA_HOME"  ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then 
      # IBM's JDK on AIX uses strange locations for the executables
      JAVACMD="$JAVA_HOME/jre/sh/java"
    else
      JAVACMD="$JAVA_HOME/bin/java"
    fi
  else
    JAVACMD=`which java 2> /dev/null `
    if [ -z "$JAVACMD" ] ; then 
        JAVACMD=java
    fi
  fi
fi
 
$JAVACMD -classpath build/ant.jar:build/optional.jar:build/junit.jar org.apache.tools.ant.Main $@
