FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
ENV MAVEN_OPTS="-Dhttps.protocols=TLSv1.2 -Dmaven.wagon.http.retryHandler.count=5 -Dmaven.wagon.httpconnectionManager.ttlSeconds=120"
COPY . .
RUN for i in 1 2 3; do ./mvnw -B -ntp -DskipTests package && exit 0; echo "Retrying Maven build ($i)..."; sleep 5; done; exit 1

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8086
ENTRYPOINT ["java","-jar","/app/app.jar"]
