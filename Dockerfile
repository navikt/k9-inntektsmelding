FROM gcr.io/distroless/java21-debian12:nonroot

LABEL org.opencontainers.image.source=https://github.com/navikt/k9-inntektsmelding
# Healtcheck lokalt/test
COPY --from=busybox:stable-musl /bin/wget /usr/bin/wget

# Working dir for RUN, CMD, ENTRYPOINT, COPY and ADD (required because of nonroot user cannot run commands in root)
WORKDIR /app

COPY target/classes/logback*.xml conf/
COPY target/lib/*.jar lib/
COPY target/app.jar .

ENV TZ=Europe/Oslo
ENV JAVA_OPTS="-Djava.security.egd=file:/dev/urandom \
    -Dlogback.configurationFile=conf/logback.xml"

CMD ["app.jar"]
