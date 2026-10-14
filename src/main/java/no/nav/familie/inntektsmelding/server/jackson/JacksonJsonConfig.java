package no.nav.familie.inntektsmelding.server.jackson;

import java.util.TimeZone;

import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;

import com.fasterxml.jackson.annotation.JsonInclude;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.json.JsonMapper;

@Provider
public class JacksonJsonConfig implements ContextResolver<JsonMapper> {

    private static final JsonMapper JSON_MAPPER = createJsonMapper();

    // Starter fra Jackson 2-defaults for å bevare eksisterende REST-kontrakt (feltrekkefølge, primitiver, trailing tokens m.m.)
    private static JsonMapper createJsonMapper() {
        return JsonMapper.builderWithJackson2Defaults()
            .defaultTimeZone(TimeZone.getTimeZone("Europe/Oslo"))
            .enable(EnumFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
            .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DateTimeFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_EMPTY))
            .build();
    }

    @Override
    public JsonMapper getContext(Class<?> type) {
        return JSON_MAPPER;
    }

}
