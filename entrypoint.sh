#!/bin/sh

CONFIG=$1
DEBUG=$2
TOKEN=$3

if [ ! -z $DEBUG ]; then
  INPUTS="-d"
fi

if [ ! -z $CONFIG ]; then
  INPUTS=$INPUTS" -c "
  INPUTS=$INPUTS$CONFIG
fi

if [ ! -z $TOKEN ]; then
  INPUTS=$INPUTS" -t "
  INPUTS=$INPUTS$TOKEN
fi

# JUnit, CMD, Checkstyle, FindBugs
mvn -ntp -V -e clean verify -Dmaven.test.failure.ignore -Dgpg.skip
# Build with maven (pit)
mvn -ntp org.pitest:pitest-maven:mutationCoverage
# Build with maven (jacoco)
mvn -ntp -V -U -e jacoco:prepare-agent test jacoco:report -Dmaven.test.failure.ignore

#if [ ! -d "/target" ]; then
#  mv /resources/ /target/
#  mkdir -p /target/surefire-reports/
#  mv /target/TEST-* /target/surefire-reports/
#  mkdir -p /target/site/jacoco/
#  mv /target/jacoco.xml /target/site/jacoco/
#  mkdir -p /target/pit-reports/
#  mv /target/mutations-few.xml /target/pit-reports/
#fi

# Get report
java -jar /jars/github-actions-autograding.jar $INPUTS

