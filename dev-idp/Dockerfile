FROM openjdk:8-jre

RUN mkdir /opt/eidas-connector
ADD target/tomcat /opt/eidas-connector/tomcat/
ADD target/shibboleth /opt/eidas-connector/shibboleth/
ADD credentials /opt/eidas-connector/credentials

# Redirecting log directories
RUN rm -rf /opt/eidas-connector/tomcat/logs && ln -s /var/log/eidas-connector/ /opt/eidas-connector/tomcat/logs
RUN rm -rf /opt/eidas-connector/shibboleth/logs && ln -s /var/log/eidas-connector/ /opt/eidas-connector/shibboleth/logs
RUN chmod a+x /opt/eidas-connector/tomcat/bin/*.sh

EXPOSE 8443
EXPOSE 8009

VOLUME /etc/eidas-connector
VOLUME /var/log/eidas-connector
CMD mkdir -p /var/log/eidas-connector/ && /opt/eidas-connector/tomcat/bin/dockerStart.sh eidas-connector
