#!/bin/bash

mkdir -p target/surefire-reports/
cp -r src/test/resources/junit/* target/surefire-reports/

mkdir -p target/site/jacoco/
cp -r src/test/resources/jacoco/* target/site/jacoco/

mkdir -p target/pit-reports/
cp -r src/test/resources/pit/* target/pit-reports/

cp -r src/test/resources/checkstyle/* target/
cp -r src/test/resources/pmd/* target/
cp -r src/test/resources/findbugs/* target/
