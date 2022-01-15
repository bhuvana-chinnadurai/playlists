#
# Scala and sbt Dockerfile
#
# https://github.com/hseeberger/scala-sbt
#

# Pull base image
FROM openjdk:8

ENV SCALA_VERSION 2.13.6
ENV SBT_VERSION 1.5.5


RUN curl -L -o sbt-$SBT_VERSION.zip https://github.com/sbt/sbt/releases/download/v$SBT_VERSION/sbt-$SBT_VERSION.zip
RUN unzip sbt-$SBT_VERSION.zip -d ops

# Define working directory
WORKDIR /playlists

ADD . /playlists

CMD /ops/sbt/bin/sbt run

