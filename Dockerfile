FROM maven:3.6.3-jdk-8-openj9

COPY . /
ADD entrypoint.sh /entrypoint.sh
ADD action.yml /action.yml

ENTRYPOINT ["sh", "/entrypoint.sh"]