FROM node:24-alpine AS web
WORKDIR /web
COPY web/package.json web/package-lock.json ./
RUN npm ci
COPY web/ ./
RUN npm run build

FROM maven:3.9-eclipse-temurin-25 AS api
WORKDIR /api
COPY api/pom.xml ./
RUN mvn -q dependency:go-offline
COPY api/src ./src
COPY --from=web /web/dist ./src/main/resources/static
RUN mvn -q package -DskipTests

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=api /api/target/tickboard-api-*.jar tickboard.jar
ENV TICKBOARD_SERVER_DIR=/minecraft
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "tickboard.jar"]
