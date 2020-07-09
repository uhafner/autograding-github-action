FROM maven:3.6.3-jdk-8-openj9

COPY default.conf /default.conf
COPY out/artifacts/github_actions_autograding_jar/* /jars/
ADD entrypoint.sh /entrypoint.sh
ADD action.yml /action.yml

ENTRYPOINT ["sh", "/entrypoint.sh"]