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

FROM alpine:latest AS base
WORKDIR /staging
# Prepare the staging environment with repositories AND keys
RUN mkdir -p /staging/etc/apk/keys && \
    cp -r /etc/apk/repositories /staging/etc/apk/ && \
    cp /etc/apk/keys/* /staging/etc/apk/keys/
# Now run apk add with the keys in place
RUN apk add --initdb --root /staging --no-cache \
    alpine-baselayout \
    ca-certificates \
    zlib \
    musl \
    libcrypto3 \
    libssl3
# Setup user (Standard Alpine shells use /sbin/nologin for system users)
RUN echo "appuser:x:1000:1000:appuser:/home/appuser:/sbin/nologin" >> /staging/etc/passwd && \
    echo "appuser:x:1000:" >> /staging/etc/group && \
    mkdir -p /staging/home/appuser && \
    chown -R 1000:1000 /staging/home/appuser
# Clean up the package manager metadata to save space
RUN rm -rf /staging/etc/apk /staging/lib/apk /staging/var/cache/apk

# Set the scratch for final stage
FROM scratch
WORKDIR /usr/webapp
# Copy everything from the staging directory to the root of the scratch image
COPY --from=base /staging /
# Set JAVA_HOME using application specific JRE from build-stage
ENV JAVA_HOME=/usr/lib/java/jre
ENV PATH=$JAVA_HOME/bin:$PATH
COPY --from=run /app-jre $JAVA_HOME
# Copy the artifact from build-stage
COPY --from=run /usr/app/target/*.jar /usr/webapp/webapp-service.jar
# Define environment variables for java-options and application-arguments
ENV JAVA_OPTS=""
ENV APP_ARGS=""
# Build the application start-up script
RUN echo 'java ${JAVA_OPTS} -jar webapp-service.jar ${APP_ARGS}' > ./start-app.sh
RUN chown -R 1000:1000 /usr/webapp
RUN chmod +x /usr/webapp/start-app.sh
# Set the non-root user
USER 1000
# Expose the application port
EXPOSE 8080
# Run via entrypoint
ENTRYPOINT ["sh", "-c", "./start-app.sh"]
