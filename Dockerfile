FROM ghcr.io/navikt/sif-baseimages/java-25:2025.10.21.1249Z

LABEL org.opencontainers.image.source=https://github.com/navikt/k9-inntektsmelding

COPY target/classes/logback*.xml conf/
COPY target/lib/*.jar lib/
COPY target/app.jar .
