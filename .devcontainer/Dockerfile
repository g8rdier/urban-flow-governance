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
       openjdk-21-jdk \
    && rm -rf /var/lib/apt/lists/*

# Ermoeglicht passwordless sudo mit dem ubuntu Nutzer
RUN echo "ubuntu ALL=(ALL) NOPASSWD:ALL" > /etc/sudoers.d/ubuntu && \
    chmod 0440 /etc/sudoers.d/ubuntu

# Setzt den user
USER ${USERNAME}

# Grails wird direkt vom GitHub-Repository installiert
# Nutzt eine stabile, bekannte Version statt dynamisches Fetching
ARG GRAILS_VERSION=6.1.1
RUN cd /home/${USERNAME} \
    && curl -L "https://github.com/grails/grails-core/releases/download/v${GRAILS_VERSION}/grails-${GRAILS_VERSION}.zip" -o grails.zip \
    && unzip -q grails.zip \
    && rm grails.zip \
    && ln -s grails-${GRAILS_VERSION} grails

ENV PATH="/home/${USERNAME}/grails/bin:${PATH}"
ENV JAVA_HOME="/usr/lib/jvm/java-21-openjdk-arm64"
