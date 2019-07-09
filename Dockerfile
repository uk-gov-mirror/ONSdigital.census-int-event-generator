FROM openjdk:11-jre-slim

ARG JAR_FILE=event-generator*.jar
RUN apt-get update
RUN apt-get -yq clean
RUN groupadd -g 982 eventgenerator && \
    useradd -r -u 982 -g eventgenerator eventgenerator
USER eventgenerator
COPY target/$JAR_FILE /opt/eventgenerator.jar

ENTRYPOINT [ "java", "-jar", "/opt/eventgenerator.jar" ]
