package no.nav.familie.inntektsmelding.imdialog.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import no.nav.familie.inntektsmelding.server.jackson.JacksonJsonConfig;
import no.nav.familie.inntektsmelding.typer.dto.AktørIdDto;
import no.nav.familie.inntektsmelding.typer.dto.ArbeidsgiverDto;
import no.nav.familie.inntektsmelding.typer.dto.BortfaltNaturalytelseDto;
import no.nav.familie.inntektsmelding.typer.dto.EndringsårsakDto;
import no.nav.familie.inntektsmelding.typer.dto.EndringsårsakerDto;
import no.nav.familie.inntektsmelding.typer.dto.KontaktpersonDto;
import no.nav.familie.inntektsmelding.typer.dto.NaturalytelsetypeDto;
import no.nav.familie.inntektsmelding.typer.dto.RefusjonDto;
import no.nav.familie.inntektsmelding.typer.dto.YtelseTypeDto;

class InntektsmeldingResponseDtoSerialiseringTest {

    private static final UUID FORESPORSEL_UUID = UUID.fromString("b2c3d4e5-f6a7-8901-bcde-f23456789012");
    private static final String AKTØR_ID = "1234567890123";
    private static final String ORGNR = "974760673";

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new JacksonJsonConfig().getContext(null);
    }

    // --- Deserialiseringstester: fra kjent JSON til objekt ---

    @Test
    void skal_deserialisere_response_uten_valgfrie_felt() throws Exception {
        var json = """
            {
              "id": 42,
              "foresporselUuid": "b2c3d4e5-f6a7-8901-bcde-f23456789012",
              "aktorId": {"id": "1234567890123"},
              "ytelse": "PLEIEPENGER_SYKT_BARN",
              "arbeidsgiverIdent": {"ident": "974760673"},
              "kontaktperson": {"navn": "Kari Nordmann", "telefonnummer": "99887766"},
              "startdato": "2024-03-01",
              "inntekt": 60000,
              "opprettetTidspunkt": "2024-03-01T12:00:00",
              "refusjon": [],
              "bortfaltNaturalytelsePerioder": [],
              "endringAvInntektÅrsaker": []
            }
            """;

        var dto = objectMapper.readValue(json, InntektsmeldingResponseDto.class);

        assertThat(dto.id()).isEqualTo(42L);
        assertThat(dto.foresporselUuid()).isEqualTo(FORESPORSEL_UUID);
        assertThat(dto.aktorId().id()).isEqualTo(AKTØR_ID);
        assertThat(dto.ytelse()).isEqualTo(YtelseTypeDto.PLEIEPENGER_SYKT_BARN);
        assertThat(dto.arbeidsgiverIdent().ident()).isEqualTo(ORGNR);
        assertThat(dto.kontaktperson().navn()).isEqualTo("Kari Nordmann");
        assertThat(dto.startdato()).isEqualTo(LocalDate.of(2024, 3, 1));
        assertThat(dto.inntekt()).isEqualByComparingTo("60000");
        assertThat(dto.opprettetTidspunkt()).isEqualTo(LocalDateTime.of(2024, 3, 1, 12, 0, 0));
        assertThat(dto.refusjon()).isEmpty();
        assertThat(dto.bortfaltNaturalytelsePerioder()).isEmpty();
        assertThat(dto.endringAvInntektÅrsaker()).isEmpty();
        assertThat(dto.omsorgspenger()).isNull();
    }

    @Test
    void skal_deserialisere_response_med_refusjon_og_naturalytelse() throws Exception {
        var json = """
            {
              "id": 1,
              "foresporselUuid": "b2c3d4e5-f6a7-8901-bcde-f23456789012",
              "aktorId": {"id": "1234567890123"},
              "ytelse": "OMSORGSPENGER",
              "arbeidsgiverIdent": {"ident": "974760673"},
              "kontaktperson": {"navn": "Ola", "telefonnummer": "11223344"},
              "startdato": "2024-01-01",
              "inntekt": 45000,
              "opprettetTidspunkt": "2024-01-15T08:30:00",
              "refusjon": [
                {"fom": "2024-01-01", "beløp": 10000}
              ],
              "bortfaltNaturalytelsePerioder": [
                {"fom": "2024-01-01", "naturalytelsetype": "BIL", "beløp": 3000}
              ],
              "endringAvInntektÅrsaker": [
                {"årsak": "TARIFFENDRING", "fom": "2024-01-01"}
              ]
            }
            """;

        var dto = objectMapper.readValue(json, InntektsmeldingResponseDto.class);

        assertThat(dto.refusjon()).hasSize(1);
        assertThat(dto.refusjon().getFirst().fom()).isEqualTo(LocalDate.of(2024, 1, 1));
        assertThat(dto.refusjon().getFirst().beløp()).isEqualByComparingTo("10000");
        assertThat(dto.bortfaltNaturalytelsePerioder()).hasSize(1);
        assertThat(dto.bortfaltNaturalytelsePerioder().getFirst().naturalytelsetype()).isEqualTo(NaturalytelsetypeDto.BIL);
        assertThat(dto.endringAvInntektÅrsaker()).hasSize(1);
        assertThat(dto.endringAvInntektÅrsaker().getFirst().årsak()).isEqualTo(EndringsårsakDto.TARIFFENDRING);
    }

    @Test
    void skal_deserialisere_opprettetTidspunkt_som_iso_datetime() throws Exception {
        var json = """
            {
              "id": 1,
              "foresporselUuid": "b2c3d4e5-f6a7-8901-bcde-f23456789012",
              "aktorId": {"id": "1234567890123"},
              "ytelse": "PLEIEPENGER_SYKT_BARN",
              "arbeidsgiverIdent": {"ident": "974760673"},
              "kontaktperson": {"navn": "Kari", "telefonnummer": "99887766"},
              "startdato": "2024-03-01",
              "inntekt": 60000,
              "opprettetTidspunkt": "2024-06-15T10:30:45",
              "refusjon": [],
              "bortfaltNaturalytelsePerioder": [],
              "endringAvInntektÅrsaker": []
            }
            """;

        var dto = objectMapper.readValue(json, InntektsmeldingResponseDto.class);

        assertThat(dto.opprettetTidspunkt()).isEqualTo(LocalDateTime.of(2024, 6, 15, 10, 30, 45));
    }

    // --- Serialiseringstester: fra objekt til kjent JSON-form ---

    @Test
    void tomme_lister_skal_inkluderes_som_tom_array_i_json() throws Exception {
        var node = serialiser(lagResponseMedTommeLister());

        assertThat(node.get("refusjon").isArray()).isTrue();
        assertThat(node.get("refusjon")).isEmpty();
        assertThat(node.get("bortfaltNaturalytelsePerioder").isArray()).isTrue();
        assertThat(node.get("bortfaltNaturalytelsePerioder")).isEmpty();
        assertThat(node.get("endringAvInntektÅrsaker").isArray()).isTrue();
        assertThat(node.get("endringAvInntektÅrsaker")).isEmpty();
    }

    @Test
    void null_omsorgspenger_skal_utelates_fra_json() throws Exception {
        var node = serialiser(lagResponseMedTommeLister());

        assertThat(node.has("omsorgspenger")).isFalse();
    }

    @Test
    void opprettetTidspunkt_skal_serialiseres_som_iso_datetime() throws Exception {
        var node = serialiser(lagResponseMedTommeLister());

        assertThat(node.get("opprettetTidspunkt").asText()).isEqualTo("2024-03-01T12:00:00");
    }

    @Test
    void startdato_skal_serialiseres_som_iso_dato() throws Exception {
        var node = serialiser(lagResponseMedTommeLister());

        assertThat(node.get("startdato").asText()).isEqualTo("2024-03-01");
    }

    @Test
    void refusjon_med_data_serialiseres_med_iso_dato() throws Exception {
        var dto = new InntektsmeldingResponseDto(
            1L,
            FORESPORSEL_UUID,
            new AktørIdDto(AKTØR_ID),
            YtelseTypeDto.PLEIEPENGER_SYKT_BARN,
            new ArbeidsgiverDto(ORGNR),
            new KontaktpersonDto("Kari Nordmann", "99887766"),
            LocalDate.of(2024, 3, 1),
            new BigDecimal("60000"),
            LocalDateTime.of(2024, 3, 1, 12, 0, 0),
            List.of(new RefusjonDto(LocalDate.of(2024, 3, 1), new BigDecimal("60000"))),
            List.of(new BortfaltNaturalytelseDto(LocalDate.of(2024, 1, 1), null, NaturalytelsetypeDto.BIL, new BigDecimal("3000"))),
            List.of(new EndringsårsakerDto(EndringsårsakDto.BONUS, LocalDate.of(2024, 1, 1), null, null)),
            null
        );

        var node = serialiser(dto);

        assertThat(node.get("refusjon").get(0).get("fom").asText()).isEqualTo("2024-03-01");
        assertThat(node.get("bortfaltNaturalytelsePerioder").get(0).has("tom")).isFalse();
        assertThat(node.get("endringAvInntektÅrsaker").get(0).get("årsak").asText()).isEqualTo("BONUS");
    }

    // --- Hjelpemetoder ---

    private JsonNode serialiser(Object obj) throws Exception {
        return objectMapper.readTree(objectMapper.writeValueAsString(obj));
    }

    private InntektsmeldingResponseDto lagResponseMedTommeLister() {
        return new InntektsmeldingResponseDto(
            1L,
            FORESPORSEL_UUID,
            new AktørIdDto(AKTØR_ID),
            YtelseTypeDto.PLEIEPENGER_SYKT_BARN,
            new ArbeidsgiverDto(ORGNR),
            new KontaktpersonDto("Kari Nordmann", "99887766"),
            LocalDate.of(2024, 3, 1),
            new BigDecimal("60000"),
            LocalDateTime.of(2024, 3, 1, 12, 0, 0),
            List.of(),
            List.of(),
            List.of(),
            null
        );
    }
}

