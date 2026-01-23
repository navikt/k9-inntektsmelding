FROM ghcr.io/navikt/sif-baseimages/java-25:2026.01.22.0755Z

LABEL org.opencontainers.image.source=https://github.com/navikt/k9-inntektsmelding

COPY target/classes/logback*.xml conf/
COPY target/lib/*.jar lib/
COPY target/app.jar .
