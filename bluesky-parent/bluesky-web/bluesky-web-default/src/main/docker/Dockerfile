FROM java:8
VOLUME /tmp
ADD bluesky-web-default-0.0.1-SNAPSHOT.jar bluesky-web-default.jar
RUN bash -c 'touch /bluesky-web-default.jar'
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/bluesky-web-default.jar"]