# syntax=docker/dockerfile:1

# Definiert das Docker Image
FROM ubuntu:24.04

# Verhindert interaktive Fragen bei apt fuer automatische Installation
ENV DEBIAN_FRONTEND=noninteractive

# Dev-Container User wird beim Start an die Host-UID/GID angepasst.
ARG USERNAME=ubuntu

# SDKMAN-Einstellungen fuer Tool-Installationen unter dem non-root User.
ENV SDKMAN_DIR=/home/${USERNAME}/.sdkman

# Installiert updates und Pakete die für SDKMAN benoetigt werden
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
       apache2 \
       ca-certificates \
       curl \
       git \
       gnupg \
       bash \
       unzip \
       postgresql \
       zip \
       sudo \
       nano \
    && rm -rf /var/lib/apt/lists/*

# ubuntu:24.04 already includes the ubuntu user; create it only if absent
RUN id -u ${USERNAME} >/dev/null 2>&1 || useradd -m -s /bin/bash ${USERNAME}

# Ermoeglicht passwordless sudo mit dem ubuntu Nutzer
RUN echo "ubuntu ALL=(ALL) NOPASSWD:ALL" > /etc/sudoers.d/ubuntu && \
    chmod 0440 /etc/sudoers.d/ubuntu

# PostgreSQL Initialisierung: Erstellt das Datenverzeichnis und konfiguriert den Zugriff
RUN mkdir -p /var/run/postgresql && \
    chown -R postgres:postgres /var/run/postgresql

# Initialisiert PostgreSQL Cluster und erstellt Benutzer/Datenbank
RUN sudo /etc/init.d/postgresql start && \
    sleep 2 && \
    sudo -u postgres psql -c "CREATE USER elgreti WITH PASSWORD 'elgreti';" && \
    sudo -u postgres createdb -O elgreti elgreti && \
    sudo /etc/init.d/postgresql stop

# Setzt den user
USER ${USERNAME}

# Standard Versionen die installiert werden sollen
ARG JAVA_VERSION=17.0.18-tem
ARG GRAILS_VERSION=6.2.3
ARG GROOVY_VERSION=4.0.31
ARG TOMCAT_VERSION=9.0.116

# Startet eine "login" shell. Den folgenden Kommandos stehen also die
# Umgebungsvariablen zur Verfuegung
SHELL ["/bin/bash", "-lc"]

# Installiert SDKMAN
RUN curl -s "https://get.sdkman.io" | bash

# Installiert Java mit SDKMAN
RUN source "$SDKMAN_DIR/bin/sdkman-init.sh" && sdk install java "$JAVA_VERSION"

# Installiert Groovy mit SDKMAN
RUN source "$SDKMAN_DIR/bin/sdkman-init.sh" && sdk install groovy "$GROOVY_VERSION"

# Installiert Grails direkt von Apache GitHub (nicht in SDKMAN für ARM verfuegbar)
RUN curl -fL "https://github.com/apache/grails-core/releases/download/v${GRAILS_VERSION}/grails-${GRAILS_VERSION}.zip" \
        -o /tmp/grails.zip \
    && unzip -q /tmp/grails.zip -d /tmp \
    && mkdir -p ${SDKMAN_DIR}/candidates/grails \
    && mv /tmp/grails-${GRAILS_VERSION} ${SDKMAN_DIR}/candidates/grails/current \
    && rm /tmp/grails.zip

# Installiert Tomcat mit SDKMAN
RUN source "$SDKMAN_DIR/bin/sdkman-init.sh" && sdk install tomcat "$TOMCAT_VERSION"

# Cleanup
RUN source "$SDKMAN_DIR/bin/sdkman-init.sh" && sdk flush archives && sdk flush temp
