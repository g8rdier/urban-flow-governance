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

# Setzt den user
USER ${USERNAME}

# Standard Versionen die installiert werden sollen
ARG JAVA_VERSION=17.0.18-tem
ARG GRAILS_VERSION=7.0.10
ARG GROOVY_VERSION=4.0.31
ARG TOMCAT_VERSION=9.0.116

# Startet eine "login" shell. Den folgenden Kommandos stehen also die
# Umgebungsvariablen zur Verfuegung
SHELL ["/bin/bash", "-lc"]

# Installiert alle benoetigten Pakete mit SDKMAN
RUN curl -s "https://get.sdkman.io" | bash \
    && source "$SDKMAN_DIR/bin/sdkman-init.sh" \
    && sdk install java "$JAVA_VERSION" \
    && sdk install groovy "$GROOVY_VERSION" \
    && sdk install grails "$GRAILS_VERSION" \
    && sdk install tomcat "$TOMCAT_VERSION" \
    && sdk flush archives \
    && sdk flush temp
