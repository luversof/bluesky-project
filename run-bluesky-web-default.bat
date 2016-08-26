cd bluesky-parent
call mvn clean install -Dmaven.test.skip=true -Dspring.profiles.active=opdev
call mvn -pl bluesky-web/bluesky-web-default spring-boot:run -Dspring.profiles.active=opdev