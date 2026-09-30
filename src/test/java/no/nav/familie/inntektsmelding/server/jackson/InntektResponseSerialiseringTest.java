package no.nav.familie.inntektsmelding.server.jackson;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import no.nav.k9.inntektsmelding.imapi.inntekt.InntektResponse;

class InntektResponseSerialiseringTest {

    private final ObjectMapper objectMapper = new JacksonJsonConfig().getContext(InntektResponse.class);

    @Test
    void skal_beholde_måned_med_null_beløp_i_serialisert_json() throws Exception {
        // Simulerer at inntektskomponenten ikke har rapportert beløp for en måned (null-verdi),
        // slik InntektApiTjeneste#mapTilDto bevisst legger dette inn i map-et.
        Map<YearMonth, BigDecimal> inntektPerMåned = new LinkedHashMap<>();
        inntektPerMåned.put(YearMonth.of(2025, 1), null);
        inntektPerMåned.put(YearMonth.of(2025, 2), BigDecimal.valueOf(30000));
        var dto = new InntektResponse(inntektPerMåned, BigDecimal.valueOf(30000));

        var json = objectMapper.writeValueAsString(dto);

        // Verifiserer den faktiske serialiserte JSON-en, ikke bare det in-memory objektet,
        // siden global NON_EMPTY-inclusion ellers ville fjernet null-verdien fra map-et.
        assertThat(json).contains("\"2025-01\":null");
        assertThat(json).contains("\"2025-02\":30000");
    }
}

