package no.nav.familie.inntektsmelding.server.jackson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

import tools.jackson.core.exc.StreamReadException;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.json.JsonMapper;

class JacksonJsonConfigTest {

    private final JsonMapper mapper = new JacksonJsonConfig().getContext(Object.class);

    enum Type {
        KJENT,
        @JsonEnumDefaultValue UKJENT
    }

    record Dto(String navn, LocalDate dato, LocalDateTime tidspunkt, BigDecimal beløp, Type type, List<String> liste, Optional<String> valgfri) {
    }

    record Primitiv(int antall) {
    }

    record ÆrligDto(String årsak, String ønske) {
    }

    @Test
    void skal_serialisere_datoer_som_iso_og_utelate_tomme_verdier() {
        var dto = new Dto("navn", LocalDate.of(2026, 1, 31), LocalDateTime.of(2026, 1, 31, 12, 30), BigDecimal.TEN, Type.KJENT, List.of(), Optional.empty());

        var json = mapper.writeValueAsString(dto);

        assertThat(json).isEqualTo("{\"navn\":\"navn\",\"dato\":\"2026-01-31\",\"tidspunkt\":\"2026-01-31T12:30:00\",\"beløp\":10,\"type\":\"KJENT\"}");
    }

    @Test
    void skal_ignorere_ukjente_felt_og_bruke_default_for_ukjent_enum() {
        var json = "{\"navn\":\"navn\",\"type\":\"FINNES_IKKE\",\"ukjent\":\"felt\",\"valgfri\":\"verdi\"}";

        var dto = mapper.readValue(json, Dto.class);

        assertThat(dto.navn()).isEqualTo("navn");
        assertThat(dto.type()).isEqualTo(Type.UKJENT);
        assertThat(dto.valgfri()).contains("verdi");
    }

    @Test
    void skal_ikke_feile_paa_null_for_primitiver() {
        assertThat(mapper.readValue("{}", Primitiv.class).antall()).isZero();
    }

    @Test
    void skal_haandtere_felt_som_starter_med_æøå() {
        var dto = new ÆrligDto("årsak", "ønske");

        var json = mapper.writeValueAsString(dto);

        assertThat(json).contains("\"årsak\":\"årsak\"").contains("\"ønske\":\"ønske\"");
        assertThat(mapper.readValue(json, ÆrligDto.class)).isEqualTo(dto);
    }

    @Test
    void skal_kaste_exceptions_som_fanges_av_exception_mapperne() {
        assertThatThrownBy(() -> mapper.readValue("{\"navn\":", Dto.class)).isInstanceOf(StreamReadException.class);
        assertThatThrownBy(() -> mapper.readValue("{\"dato\":\"ikke-en-dato\"}", Dto.class)).isInstanceOf(DatabindException.class);
    }
}
