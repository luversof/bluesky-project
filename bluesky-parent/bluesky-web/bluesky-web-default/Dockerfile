FROM openjdk:10-jdk
VOLUME /tmp
ARG JAR_FILE
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","--add-modules=java.xml.bind","-jar","/app.jar"]