# ---------- BUILD STAGE ----------
#FROM maven:3.9.9-eclipse-temurin-17 AS build
#WORKDIR /build

# EJB bauen
#COPY v2metatool1 ./v2metatool1
#WORKDIR /build/v2metatool1
#RUN mvn clean install -DskipTests

# testapp kopieren (direkt aus Build-Kontext)
# Webapp bauen
#COPY testapp /build/testapp   
#WORKDIR /build/testapp                 
#RUN mvn clean package -DskipTests


# ---------- RUNTIME STAGE ----------
FROM payara/server-full:7.2025.2

USER root

ADD https://jdbc.postgresql.org/download/postgresql-42.7.3.jar \
    /opt/payara/appserver/glassfish/domains/domain1/lib/

RUN chmod 644 /opt/payara/appserver/glassfish/domains/domain1/lib/postgresql-42.7.3.jar

# postboot commands kopieren
COPY projektdocker/post-boot-commands.asadmin \
     /opt/payara/config/post-boot-commands.asadmin

RUN chown payara:payara /opt/payara/config/post-boot-commands.asadmin

USER payara

EXPOSE 8080 4848



