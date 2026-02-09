FROM openjdk:17.0.1-jdk-slim

RUN useradd -ms /bin/bash appuser

COPY hub-services-0.0.1-SNAPSHOT.jar /opt/
EXPOSE 3013

RUN chown -R appuser:appuser /opt
USER appuser
WORKDIR /opt

CMD ["/bin/bash", "-c", "java -XX:+PrintFlagsFinal $JAVA_OPTIONS -XX:+UnlockExperimentalVMOptions -jar /opt/hub-services-0.0.1-SNAPSHOT.jar"]

