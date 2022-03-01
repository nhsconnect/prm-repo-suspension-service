FROM openjdk:11.0.7-jdk-buster
RUN mkdir /home/spring
RUN groupadd --gid 102 spring
RUN useradd --home-dir /home/spring --uid 1000 --gid 102 --shell /bin/bash spring
RUN usermod -a -G spring spring
USER spring
COPY build/libs/*.jar app.jar
COPY run-application.sh ./

ARG UTILS_VERSION
RUN test -n "$UTILS_VERSION"
COPY utils/$UTILS_VERSION/run-with-redaction.sh ./utils/
COPY utils/$UTILS_VERSION/redactor              ./utils/

ENTRYPOINT ["./utils/run-with-redaction.sh", "./run-application.sh"]
