FROM java:8
COPY --from=build bluesky-web/bluesky-web-default/target/bluesky-web-default-0.0.1-SNAPSHOT.jar bluesky-web-default.jar
EXPOSE 8082
RUN bash -c 'touch bluesky-web-default.jar'
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","bluesky-web-default.jar"]