# BUILD STAGE 1
# ---------------------------------------------------
FROM hseeberger/scala-sbt:8u181_2.12.8_1.2.8 as builder
MAINTAINER "Robert Lyons" <nextdude@epiphanous.io>

RUN useradd -c "App User" -m -s /usr/sbin/nologin -U app

ADD . /home/app/build
RUN chown -R app:app /home/app/build

USER app
WORKDIR /home/app/build

RUN sbt assembly

# ---------------------------------------------------
# BUILD STAGE 2 - copy build artifacts into fresh runtime image
# ---------------------------------------------------
FROM openjdk:8-jre-alpine as runtime

RUN apk add --no-cache bash util-linux coreutils binutils findutils grep
RUN addgroup app
RUN adduser -g /home/app -g "App User" -s /sbin/nologin -D -G app app

COPY --from=builder /home/app/build/target/scala-2.12/sse-ingest-*.jar /home/app/app.jar
RUN chown -R app:app /home/app/app.jar

USER app
WORKDIR /home/app

ENTRYPOINT ["java"]
CMD ["-jar", "app.jar"]

