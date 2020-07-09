#!/bin/sh

DEBUG=$1
CONFIG=$2
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

clear
ls

# Get report
java -jar /jars/github-actions-autograding.jar $INPUTS

