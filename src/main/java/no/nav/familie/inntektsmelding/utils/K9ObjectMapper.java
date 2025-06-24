package no.nav.familie.inntektsmelding.utils;

import java.util.TimeZone;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class K9ObjectMapper {
    private final ObjectMapper objectMapper;

    public K9ObjectMapper() {
        this.objectMapper = new ObjectMapper()
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule())
            .setTimeZone(TimeZone.getTimeZone("Europe/Oslo"));;
    }
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
