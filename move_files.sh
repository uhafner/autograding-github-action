#!/bin/bash

mv /resources/ /target/
mkdir -p /target/surefire-reports/
mv /target/TEST-* /target/surefire-reports/
mkdir -p /target/site/jacoco/
mv /target/jacoco.xml /target/site/jacoco/
mkdir -p /target/pit-reports/
mv /target/mutations-few.xml /target/pit-reports/
