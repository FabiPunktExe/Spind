FROM eclipse-temurin:25.0.1_8-jdk AS builder
COPY . /app
WORKDIR /app
RUN apt update
RUN apt install libatomic1
RUN chmod +x ./gradlew
RUN ./gradlew composeApp:wasmJsBrowserDistribution

FROM nginx:latest
LABEL authors="Fabi.exe"
COPY --from=builder /app/composeApp/nginx.conf /etc/nginx/nginx.conf
COPY --from=builder /app/composeApp/build/dist/wasmJs/productionExecutable /app
EXPOSE 80