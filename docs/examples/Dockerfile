FROM openjdk:21-jdk-slim

LABEL org.opencontainers.image.source=https://github.com/swedenconnect/eidas-connector
LABEL org.opencontainers.image.description="Swedish eIDAS Connector"
LABEL org.opencontainers.image.licenses=Apache-2.0

ADD target/eidas-connector-*.jar /eidas-connector.jar

ENV JAVA_OPTS="--add-opens java.base/java.lang=ALL-UNNAMED -Djava.net.preferIPv4Stack=true -Dorg.apache.xml.security.ignoreLineBreaks=true"

ENTRYPOINT exec java $JAVA_OPTS -jar /eidas-connector.jar

EXPOSE 8443 8444
