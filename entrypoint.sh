#!/bin/sh

CONFIG=$1
TOKEN=$2
DEBUG=$3

if [ ! -z $DEBUG ]; then
  DEBUG="-d"
fi

if [ ! -z $CONFIG ]; then
  CONFIG="-c "$CONFIG
fi

if [ ! -z $TOKEN ]; then
  TOKEN="-t "$TOKEN
fi

echo $DEBUG
echo $CONFIG
echo $TOKEN

exit

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
java -jar /jars/github-actions-autograding.jar $DEBUG $CONFIG $TOKEN

