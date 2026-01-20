FROM eclipse-temurin:25.0.1_8-jdk-alpine AS builder
COPY .. /app
WORKDIR /app
RUN chmod +x ./gradlew
RUN ./gradlew installDist

FROM eclipse-temurin:25.0.1_8-jre-alpine
LABEL authors="Fabi.exe"
COPY --from=builder /app/server/build/install/server /app
VOLUME /data
WORKDIR /data
ENTRYPOINT ["/app/bin/server"]