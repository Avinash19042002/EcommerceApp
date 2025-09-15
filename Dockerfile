# -------- Stage 1: Build --------
FROM maven:3.8.4-openjdk-17-slim AS build

WORKDIR /app

# Copy wrapper & app code
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
COPY src ./src

RUN chmod +x mvnw && ./mvnw clean package -DskipTests

# -------- Stage 2: Run --------
FROM openjdk:17-jdk-slim

WORKDIR /app

# Copy the built jar from the build stage
COPY --from=build /app/target/EcommerceApp-0.0.1-SNAPSHOT.jar app.jar

# Make sure it is readable
RUN chmod +r app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
