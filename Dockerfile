# Declare the runtime JDK image arg
ARG RUNTIME_JDK_IMAGE=eclipse-temurin:25-alpine
# Set the base-image for build stage
FROM maven:3-eclipse-temurin-25-alpine AS build
# Set up working directory
RUN mkdir -p /usr/app
COPY . /usr/app
WORKDIR /usr/app
# Build the application
RUN --mount=type=cache,target=/root/.m2 ./mvnw clean package -DskipTests

# Set the runtime JDK image for run stage
FROM ${RUNTIME_JDK_IMAGE} AS run
# Declare the runtime JDK image version arg
ARG RUNTIME_JDK_VERSION=25
# Copy the artifact from run-stage
RUN mkdir -p /usr/app
COPY --from=build /usr/app/target /usr/app/target
WORKDIR /usr/app
# Get the JDK version from run JDK args
RUN echo ${RUNTIME_JDK_VERSION} > jdk.version
# Build the application specific JRE
RUN jdeps --ignore-missing-deps -q \
    --recursive \
    --multi-release $(cat jdk.version) \
    --print-module-deps \
    --class-path 'target/dependencies/*' \
    target/*.jar > modules.info
# Add 'jdk.management' module for JDK-specific management interfaces for the JVM while building application specific JRE
RUN jlink --add-modules jdk.management,$(cat modules.info) \
    --no-header-files \
    --no-man-pages \
    --output /app-jre

# Set the base-image for final stage
FROM alpine:latest
# Install curl for health checks
RUN apk --no-cache add curl
# Set JAVA_HOME using application specific JRE from run-stage
ENV JAVA_HOME=/usr/lib/java/jre
ENV PATH=$JAVA_HOME/bin:$PATH
COPY --from=run /app-jre $JAVA_HOME
# Copy the artifact from run-stage
RUN mkdir -p /usr/webapp
COPY --from=run /usr/app/target/*.jar /usr/webapp/webapp-service.jar
WORKDIR /usr/webapp
# Define environment variables for java-options and application-arguments
ENV JAVA_OPTS=""
ENV APP_ARGS=""
# Build the application start-up script
RUN echo 'java ${JAVA_OPTS} -jar webapp-service.jar ${APP_ARGS}' > ./start-app.sh
# Set a non-root user: Add a system group 'appgroup' and a system user 'appuser' in this group
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
RUN chown -R appuser:appgroup /usr/webapp
RUN chmod +x /usr/webapp/start-app.sh
USER appuser
# Expose the application port
EXPOSE 8080
# Run using start-up script
CMD ["sh", "-c", "./start-app.sh"]
