FROM openjdk:17
VOLUME /tmp
EXPOSE 8081
ARG JAR_FILE=target/Users-0.0.1-SNAPSHOT.jar
ENTRYPOINT ["java","-jar","/Users-0.0.1-SNAPSHOT.jar"]