@echo off
cd /d "%~dp0bluesky-parent\bluesky-authorization-server"
mvn spring-boot:run -Dspring-boot.run.profiles=localdev
