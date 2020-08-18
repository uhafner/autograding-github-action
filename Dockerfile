FROM maven:3.6.3-openjdk-8

WORKDIR /
COPY default.conf /default.conf
ADD entrypoint.sh /entrypoint.sh
COPY pom.xml /pom.xml
COPY src /src/
RUN mvn package
RUN rm -rf /src/

ENTRYPOINT ["sh", "/entrypoint.sh"]