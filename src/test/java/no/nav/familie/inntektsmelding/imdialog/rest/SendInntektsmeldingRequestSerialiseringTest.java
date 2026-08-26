package no.nav.familie.inntektsmelding.imdialog.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
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

class SendInntektsmeldingRequestSerialiseringTest {

    private static final UUID FORESPORSEL_UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
    private static final String AKTØR_ID = "1234567890123";
    private static final String ORGNR = "974760673";

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new JacksonJsonConfig().getContext(null);
    }

    // --- Deserialiseringstester: fra kjent JSON til objekt ---

    @Test
    void skal_deserialisere_minimal_request() throws Exception {
        var json = """
            {
              "foresporselUuid": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
              "aktorId": "1234567890123",
              "ytelse": "PLEIEPENGER_SYKT_BARN",
              "arbeidsgiverIdent": "974760673",
              "kontaktperson": {"navn": "Kari Nordmann", "telefonnummer": "99887766"},
              "startdato": "2024-03-01",
              "inntekt": 60000,
              "refusjon": [],
              "bortfaltNaturalytelsePerioder": [],
              "endringAvInntektÅrsaker": []
            }
            """;

        var request = objectMapper.readValue(json, SendInntektsmeldingRequest.class);

        assertThat(request.foresporselUuid()).isEqualTo(FORESPORSEL_UUID);
        assertThat(request.aktorId().id()).isEqualTo(AKTØR_ID);
        assertThat(request.ytelse()).isEqualTo(YtelseTypeDto.PLEIEPENGER_SYKT_BARN);
        assertThat(request.arbeidsgiverIdent().ident()).isEqualTo(ORGNR);
        assertThat(request.kontaktperson().navn()).isEqualTo("Kari Nordmann");
        assertThat(request.startdato()).isEqualTo(LocalDate.of(2024, 3, 1));
        assertThat(request.inntekt()).isEqualByComparingTo("60000");
        assertThat(request.refusjon()).isEmpty();
        assertThat(request.bortfaltNaturalytelsePerioder()).isEmpty();
        assertThat(request.endringAvInntektÅrsaker()).isEmpty();
        assertThat(request.omsorgspenger()).isNull();
    }

    @Test
    void skal_deserialisere_request_med_refusjon_og_naturalytelse() throws Exception {
        var json = """
            {
              "foresporselUuid": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
              "aktorId": "1234567890123",
              "ytelse": "OMSORGSPENGER",
              "arbeidsgiverIdent": "974760673",
              "kontaktperson": {"navn": "Ola Nordmann", "telefonnummer": "11223344"},
              "startdato": "2024-01-01",
              "inntekt": 45000,
              "refusjon": [
                {"fom": "2024-01-01", "beløp": 10000}
              ],
              "bortfaltNaturalytelsePerioder": [
                {"fom": "2024-01-01", "naturalytelsetype": "BIL", "beløp": 3000}
              ],
              "endringAvInntektÅrsaker": [
                {"årsak": "BONUS", "fom": "2024-01-01"}
              ]
            }
            """;

        var request = objectMapper.readValue(json, SendInntektsmeldingRequest.class);

        assertThat(request.ytelse()).isEqualTo(YtelseTypeDto.OMSORGSPENGER);
        assertThat(request.refusjon()).hasSize(1);
        assertThat(request.refusjon().getFirst().fom()).isEqualTo(LocalDate.of(2024, 1, 1));
        assertThat(request.refusjon().getFirst().beløp()).isEqualByComparingTo("10000");
        assertThat(request.bortfaltNaturalytelsePerioder()).hasSize(1);
        assertThat(request.bortfaltNaturalytelsePerioder().getFirst().naturalytelsetype()).isEqualTo(NaturalytelsetypeDto.BIL);
        assertThat(request.bortfaltNaturalytelsePerioder().getFirst().tom()).isNull();
        assertThat(request.endringAvInntektÅrsaker()).hasSize(1);
        assertThat(request.endringAvInntektÅrsaker().getFirst().årsak()).isEqualTo(EndringsårsakDto.BONUS);
    }

    @Test
    void skal_deserialisere_request_med_omsorgspenger() throws Exception {
        var json = """
            {
              "foresporselUuid": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
              "aktorId": "1234567890123",
              "ytelse": "OMSORGSPENGER",
              "arbeidsgiverIdent": "974760673",
              "kontaktperson": {"navn": "Kontakt", "telefonnummer": "55443322"},
              "startdato": "2024-01-01",
              "inntekt": 55000,
              "refusjon": [],
              "bortfaltNaturalytelsePerioder": [],
              "endringAvInntektÅrsaker": [],
              "omsorgspenger": {
                "harUtbetaltPliktigeDager": true,
                "fraværHeleDager": [
                  {"fom": "2024-01-08", "tom": "2024-01-12"}
                ]
              }
            }
            """;

        var request = objectMapper.readValue(json, SendInntektsmeldingRequest.class);

        assertThat(request.omsorgspenger()).isNotNull();
        assertThat(request.omsorgspenger().harUtbetaltPliktigeDager()).isTrue();
        assertThat(request.omsorgspenger().fraværHeleDager()).hasSize(1);
        assertThat(request.omsorgspenger().fraværHeleDager().getFirst().fom()).isEqualTo(LocalDate.of(2024, 1, 8));
    }

    // --- Serialiseringstester: fra objekt til kjent JSON-form ---

    @Test
    void aktorId_og_arbeidsgiverIdent_skal_serialiseres_som_plain_string() throws Exception {
        var node = serialiser(lagRequestMedTommeLister());

        assertThat(node.get("aktorId").isTextual()).isTrue();
        assertThat(node.get("aktorId").asText()).isEqualTo(AKTØR_ID);
        assertThat(node.get("arbeidsgiverIdent").isTextual()).isTrue();
        assertThat(node.get("arbeidsgiverIdent").asText()).isEqualTo(ORGNR);
    }

    @Test
    void tomme_lister_skal_inkluderes_som_tom_array_i_json() throws Exception {
        var node = serialiser(lagRequestMedTommeLister());

        assertThat(node.get("refusjon").isArray()).isTrue();
        assertThat(node.get("refusjon")).isEmpty();
        assertThat(node.get("bortfaltNaturalytelsePerioder").isArray()).isTrue();
        assertThat(node.get("bortfaltNaturalytelsePerioder")).isEmpty();
        assertThat(node.get("endringAvInntektÅrsaker").isArray()).isTrue();
        assertThat(node.get("endringAvInntektÅrsaker")).isEmpty();
    }

    @Test
    void null_omsorgspenger_skal_utelates_fra_json() throws Exception {
        var node = serialiser(lagRequestMedTommeLister());

        assertThat(node.has("omsorgspenger")).isFalse();
    }

    @Test
    void startdato_skal_serialiseres_som_iso_dato() throws Exception {
        var node = serialiser(lagRequestMedTommeLister());

        assertThat(node.get("startdato").asText()).isEqualTo("2024-03-01");
    }

    @Test
    void refusjon_og_naturalytelse_serialiseres_med_iso_datoer() throws Exception {
        var request = new SendInntektsmeldingRequest(
            FORESPORSEL_UUID,
            new AktørIdDto(AKTØR_ID),
            YtelseTypeDto.PLEIEPENGER_SYKT_BARN,
            new ArbeidsgiverDto(ORGNR),
            new KontaktpersonDto("Kari Nordmann", "99887766"),
            LocalDate.of(2024, 3, 1),
            new BigDecimal("60000"),
            List.of(new RefusjonDto(LocalDate.of(2024, 3, 1), new BigDecimal("60000"))),
            List.of(new BortfaltNaturalytelseDto(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 2, 29), NaturalytelsetypeDto.BIL, new BigDecimal("3000"))),
            List.of(new EndringsårsakerDto(EndringsårsakDto.BONUS, LocalDate.of(2024, 1, 1), null, null)),
            null
        );

        var node = serialiser(request);

        assertThat(node.get("refusjon").get(0).get("fom").asText()).isEqualTo("2024-03-01");
        assertThat(node.get("bortfaltNaturalytelsePerioder").get(0).get("fom").asText()).isEqualTo("2024-01-01");
        assertThat(node.get("bortfaltNaturalytelsePerioder").get(0).get("tom").asText()).isEqualTo("2024-02-29");
        assertThat(node.get("endringAvInntektÅrsaker").get(0).get("årsak").asText()).isEqualTo("BONUS");
    }

    @Test
    void null_tom_i_bortfaltNaturalytelse_skal_utelates_fra_json() throws Exception {
        var request = new SendInntektsmeldingRequest(
            FORESPORSEL_UUID,
            new AktørIdDto(AKTØR_ID),
            YtelseTypeDto.PLEIEPENGER_SYKT_BARN,
            new ArbeidsgiverDto(ORGNR),
            new KontaktpersonDto("Kari", "12345678"),
            LocalDate.of(2024, 3, 1),
            new BigDecimal("60000"),
            List.of(),
            List.of(new BortfaltNaturalytelseDto(LocalDate.of(2024, 1, 1), null, NaturalytelsetypeDto.BIL, new BigDecimal("3000"))),
            List.of(),
            null
        );

        var naturalytelseNode = serialiser(request).get("bortfaltNaturalytelsePerioder").get(0);

        assertThat(naturalytelseNode.has("tom")).isFalse();
    }

    // --- Hjelpemetoder ---

    private JsonNode serialiser(Object obj) throws Exception {
        return objectMapper.readTree(objectMapper.writeValueAsString(obj));
    }

    private SendInntektsmeldingRequest lagRequestMedTommeLister() {
        return new SendInntektsmeldingRequest(
            FORESPORSEL_UUID,
            new AktørIdDto(AKTØR_ID),
            YtelseTypeDto.PLEIEPENGER_SYKT_BARN,
            new ArbeidsgiverDto(ORGNR),
            new KontaktpersonDto("Kari Nordmann", "99887766"),
            LocalDate.of(2024, 3, 1),
            new BigDecimal("60000"),
            List.of(),
            List.of(),
            List.of(),
            null
        );
    }
}

