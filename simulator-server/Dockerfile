# syntax=docker/dockerfile:1

FROM gradle:7.4-alpine AS build
COPY --chown=gradle:gradle . /wittgenstein-simulator
WORKDIR /wittgenstein-simulator
RUN gradle clean shadowJar

FROM azul/zulu-openjdk:15.0.6 AS run
WORKDIR /wittgenstein-simulator

COPY --from=build /wittgenstein-simulator/wserver/build/libs/wittgenstein-ws.jar wittgenstein-ws.jar

ENTRYPOINT ["java", "-Xms6000m", "-Xmx12048m", "-classpath", "wittgenstein-ws.jar", "net.consensys.wittgenstein.server.ws.WServer"]