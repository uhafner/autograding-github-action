FROM maven:3.6.3-jdk-8-openj9

COPY default.conf /default.conf
COPY src/test/resources/* /resources/
ADD entrypoint.sh /entrypoint.sh

ENTRYPOINT ["sh", "/entrypoint.sh"]