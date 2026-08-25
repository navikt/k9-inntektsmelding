package no.nav.familie.inntektsmelding.imapi.inntektsmelding;

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
import no.nav.k9.inntektsmelding.felles.AvsenderSystemDto;
import no.nav.k9.inntektsmelding.felles.FødselsnummerDto;
import no.nav.k9.inntektsmelding.felles.InntektsmeldingStatusDto;
import no.nav.k9.inntektsmelding.felles.KontaktpersonDto;
import no.nav.k9.inntektsmelding.felles.OrganisasjonsnummerDto;
import no.nav.k9.inntektsmelding.felles.YtelseTypeDto;
import no.nav.k9.inntektsmelding.imapi.inntektsmelding.HentInntektsmeldingerRequest;
import no.nav.k9.inntektsmelding.imapi.inntektsmelding.HentInntektsmeldingerResponse;
import no.nav.k9.inntektsmelding.imapi.inntektsmelding.InntektsmeldingDto;

class InntektsmeldingDtoSerialiseringTest {

    private static final UUID IM_UUID = UUID.fromString("b2c3d4e5-f6a7-8901-bcde-f23456789012");
    private static final UUID FS_UUID = UUID.fromString("c3d4e5f6-a7b8-9012-cdef-345678901234");
    private static final String FNR = "33333333333";
    private static final String ORGNR = "974760673";

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new JacksonJsonConfig().getContext(null);
    }

    // --- Deserialiseringstester: fra kjent JSON til objekt ---

    @Test
    void skal_deserialisere_inntektsmeldingDto_fra_json() throws Exception {
        var json = """
            {
              "loepenr": 42,
              "inntektsmeldingUuid": "b2c3d4e5-f6a7-8901-bcde-f23456789012",
              "forespørselUuid": "c3d4e5f6-a7b8-9012-cdef-345678901234",
              "fnr": {"fnr": "33333333333"},
              "ytelseType": "PLEIEPENGER_SYKT_BARN",
              "arbeidsgiver": {"orgnr": "974760673"},
              "kontaktperson": {"navn": "Kontakt Person", "telefonnummer": "99999999"},
              "startdato": "2024-03-01",
              "inntekt": 50000,
              "innsendtTidspunkt": "2024-03-01T12:00:00",
              "avsenderSystem": {"systemNavn": "NAV_NO", "systemVersjon": "1.0"},
              "refusjonsendringer": [],
              "bortfaltNaturalytelsePerioder": [],
              "endringAvInntektÅrsaker": [],
              "status": "GODKJENT"
            }
            """;

        var dto = objectMapper.readValue(json, InntektsmeldingDto.class);

        assertThat(dto.loepenr()).isEqualTo(42L);
        assertThat(dto.inntektsmeldingUuid()).isEqualTo(IM_UUID);
        assertThat(dto.forespørselUuid()).isEqualTo(FS_UUID);
        assertThat(dto.fnr().fnr()).isEqualTo(FNR);
        assertThat(dto.arbeidsgiver().orgnr()).isEqualTo(ORGNR);
        assertThat(dto.startdato()).isEqualTo(LocalDate.of(2024, 3, 1));
        assertThat(dto.innsendtTidspunkt()).isEqualTo(LocalDateTime.of(2024, 3, 1, 12, 0, 0));
        assertThat(dto.refusjonPrMnd()).isNull();
        assertThat(dto.opphørsdatoRefusjon()).isNull();
        assertThat(dto.omsorgspenger()).isNull();
        assertThat(dto.status()).isEqualTo(InntektsmeldingStatusDto.GODKJENT);
    }

    @Test
    void skal_deserialisere_hentInntektsmeldingerRequest_med_kun_orgnr() throws Exception {
        var json = """
            {
              "orgnr": {"orgnr": "974760673"}
            }
            """;

        var request = objectMapper.readValue(json, HentInntektsmeldingerRequest.class);

        assertThat(request.orgnr().orgnr()).isEqualTo(ORGNR);
        assertThat(request.fnr()).isNull();
        assertThat(request.ytelseType()).isNull();
        assertThat(request.forespørselUuid()).isNull();
    }

    @Test
    void skal_deserialisere_hentInntektsmeldingerRequest_med_alle_felt() throws Exception {
        var json = """
            {
              "orgnr": {"orgnr": "974760673"},
              "fnr": {"fnr": "33333333333"},
              "ytelseType": "OMSORGSPENGER",
              "forespørselUuid": "c3d4e5f6-a7b8-9012-cdef-345678901234",
              "fom": "2024-01-01",
              "tom": "2024-12-31",
              "loepenr": 42,
              "status": "GODKJENT"
            }
            """;

        var request = objectMapper.readValue(json, HentInntektsmeldingerRequest.class);

        assertThat(request.fnr().fnr()).isEqualTo(FNR);
        assertThat(request.ytelseType()).isEqualTo(YtelseTypeDto.OMSORGSPENGER);
        assertThat(request.fom()).isEqualTo(LocalDate.of(2024, 1, 1));
        assertThat(request.loepenr()).isEqualTo(42L);
        assertThat(request.status()).isEqualTo(InntektsmeldingStatusDto.GODKJENT);
    }

    // --- Serialiseringstester: fra objekt til kjent JSON-form ---

    @Test
    void innsendtTidspunkt_skal_serialiseres_som_iso_datetime() throws Exception {
        var node = serialiser(lagInntektsmeldingDto(LocalDateTime.of(2024, 3, 15, 10, 30, 0)));

        assertThat(node.get("innsendtTidspunkt").asText()).isEqualTo("2024-03-15T10:30:00");
    }

    @Test
    void null_valgfrie_felt_skal_utelates_fra_json() throws Exception {
        var node = serialiser(lagInntektsmeldingDto(LocalDateTime.of(2024, 3, 1, 12, 0, 0)));

        assertThat(node.has("refusjonPrMnd")).isFalse();
        assertThat(node.has("opphørsdatoRefusjon")).isFalse();
        assertThat(node.has("omsorgspenger")).isFalse();
    }

    @Test
    void tomme_lister_skal_inkluderes_i_json() throws Exception {
        var node = serialiser(lagInntektsmeldingDto(LocalDateTime.now()));

        assertThat(node.get("refusjonsendringer").isArray()).isTrue();
        assertThat(node.get("refusjonsendringer")).isEmpty();
        assertThat(node.get("bortfaltNaturalytelsePerioder").isArray()).isTrue();
        assertThat(node.get("endringAvInntektÅrsaker").isArray()).isTrue();
    }

    @Test
    void hentInntektsmeldingerResponse_med_tom_liste_serialiseres_korrekt() throws Exception {
        var node = serialiser(new HentInntektsmeldingerResponse(List.of()));

        assertThat(node.get("inntektsmeldinger").isArray()).isTrue();
        assertThat(node.get("inntektsmeldinger")).isEmpty();
    }

    // --- Hjelpemetoder ---

    private JsonNode serialiser(Object obj) throws Exception {
        return objectMapper.readTree(objectMapper.writeValueAsString(obj));
    }

    private InntektsmeldingDto lagInntektsmeldingDto(LocalDateTime innsendtTidspunkt) {
        return new InntektsmeldingDto(
            1L, IM_UUID, FS_UUID,
            new FødselsnummerDto(FNR),
            YtelseTypeDto.PLEIEPENGER_SYKT_BARN,
            new OrganisasjonsnummerDto(ORGNR),
            new KontaktpersonDto("Kontakt Person", "99999999"),
            LocalDate.of(2024, 3, 1),
            new BigDecimal("50000"),
            innsendtTidspunkt,
            null, null,
            new AvsenderSystemDto("NAV_NO", "1.0"),
            List.of(), List.of(), List.of(),
            InntektsmeldingStatusDto.GODKJENT,
            null
        );
    }
}
