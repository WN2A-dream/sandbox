FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY src/WebServer.java src/
RUN javac -d bin src/WebServer.java

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/bin bin
CMD ["java", "-cp", "bin", "WebServer"]
