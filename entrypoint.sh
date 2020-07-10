#!/bin/sh

echo $CONFIG
echo $TOKEN
echo $DEBUG

# JUnit, CMD, Checkstyle, FindBugs
mvn -ntp -V -e clean verify -Dmaven.test.failure.ignore -Dgpg.skip
# Build with maven (pit)
mvn -ntp org.pitest:pitest-maven:mutationCoverage
# Build with maven (jacoco)
mvn -ntp -V -U -e jacoco:prepare-agent test jacoco:report -Dmaven.test.failure.ignore

# Get report
java -jar /jars/github-actions-autograding.jar "$CONFIG" $TOKEN $DEBUG

