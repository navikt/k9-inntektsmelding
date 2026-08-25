package no.nav.familie.inntektsmelding.imapi.inntektsmelding;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import no.nav.familie.inntektsmelding.server.jackson.JacksonJsonConfig;
import no.nav.k9.inntektsmelding.felles.AvsenderSystemDto;
import no.nav.k9.inntektsmelding.felles.FødselsnummerDto;
import no.nav.k9.inntektsmelding.felles.KontaktpersonDto;
import no.nav.k9.inntektsmelding.felles.OmsorgspengerDto;
import no.nav.k9.inntektsmelding.felles.OrganisasjonsnummerDto;
import no.nav.k9.inntektsmelding.felles.PeriodeDto;
import no.nav.k9.inntektsmelding.imapi.inntektsmelding.SendRefusjonOmsorgspengerRequest;

class SendRefusjonOmsorgspengerRequestSerialiseringTest {

    private static final String FNR = "22222222222";
    private static final String ORGNR = "974760673";

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new JacksonJsonConfig().getContext(null);
    }

    // --- Deserialiseringstester: fra kjent JSON til objekt ---

    @Test
    void skal_deserialisere_request_med_fraværHeleDager() throws Exception {
        var json = """
            {
              "fødselsnummer": {"fnr": "22222222222"},
              "organisasjonsnummer": {"orgnr": "974760673"},
              "startdato": "2024-01-01",
              "kontaktperson": {"navn": "Leder", "telefonnummer": "55443322"},
              "inntekt": 55000,
              "endringAvInntektÅrsaker": [],
              "avsenderSystem": {"systemNavn": "NAV_NO", "systemVersjon": "1.0"},
              "omsorgspenger": {
                "harUtbetaltPliktigeDager": true,
                "fraværHeleDager": [
                  {"fom": "2024-01-08", "tom": "2024-01-12"},
                  {"fom": "2024-01-15", "tom": "2024-01-19"}
                ]
              }
            }
            """;

        var request = objectMapper.readValue(json, SendRefusjonOmsorgspengerRequest.class);

        assertThat(request.fødselsnummer().fnr()).isEqualTo(FNR);
        assertThat(request.omsorgspenger().harUtbetaltPliktigeDager()).isTrue();
        assertThat(request.omsorgspenger().fraværHeleDager()).hasSize(2);
        assertThat(request.omsorgspenger().fraværHeleDager().getFirst().fom())
            .isEqualTo(LocalDate.of(2024, 1, 8));
        assertThat(request.omsorgspenger().fraværDelerAvDagen()).isNull();
        assertThat(request.omsorgspenger().trukketPerioder()).isNull();
    }

    @Test
    void skal_deserialisere_request_med_fraværDelerAvDagen() throws Exception {
        var json = """
            {
              "fødselsnummer": {"fnr": "22222222222"},
              "organisasjonsnummer": {"orgnr": "974760673"},
              "startdato": "2024-02-01",
              "kontaktperson": {"navn": "Leder", "telefonnummer": "55443322"},
              "inntekt": 55000,
              "endringAvInntektÅrsaker": [],
              "avsenderSystem": {"systemNavn": "NAV_NO", "systemVersjon": "1.0"},
              "omsorgspenger": {
                "harUtbetaltPliktigeDager": true,
                "fraværDelerAvDagen": [
                  {"dato": "2024-02-05", "timer": 3.5}
                ]
              }
            }
            """;

        var request = objectMapper.readValue(json, SendRefusjonOmsorgspengerRequest.class);

        assertThat(request.omsorgspenger().fraværDelerAvDagen()).hasSize(1);
        assertThat(request.omsorgspenger().fraværDelerAvDagen().getFirst().dato())
            .isEqualTo(LocalDate.of(2024, 2, 5));
        assertThat(request.omsorgspenger().fraværDelerAvDagen().getFirst().timer())
            .isEqualByComparingTo("3.5");
    }

    @Test
    void skal_deserialisere_request_med_tomme_lister_i_omsorgspenger() throws Exception {
        var json = """
            {
              "fødselsnummer": {"fnr": "22222222222"},
              "organisasjonsnummer": {"orgnr": "974760673"},
              "startdato": "2024-01-01",
              "kontaktperson": {"navn": "Leder", "telefonnummer": "55443322"},
              "inntekt": 55000,
              "endringAvInntektÅrsaker": [],
              "avsenderSystem": {"systemNavn": "NAV_NO", "systemVersjon": "1.0"},
              "omsorgspenger": {
                "harUtbetaltPliktigeDager": false,
                "fraværHeleDager": [],
                "fraværDelerAvDagen": [],
                "trukketPerioder": []
              }
            }
            """;

        var request = objectMapper.readValue(json, SendRefusjonOmsorgspengerRequest.class);

        assertThat(request.omsorgspenger().fraværHeleDager()).isEmpty();
        assertThat(request.omsorgspenger().fraværDelerAvDagen()).isEmpty();
        assertThat(request.omsorgspenger().trukketPerioder()).isEmpty();
    }

    // --- Serialiseringstester: fra objekt til kjent JSON-form ---

    @Test
    void null_trukketPerioder_og_fraværDelerAvDagen_skal_utelates_fra_json() throws Exception {
        var omsorgspenger = new OmsorgspengerDto(
            true,
            List.of(new PeriodeDto(LocalDate.of(2024, 1, 2), LocalDate.of(2024, 1, 3))),
            null,
            null
        );
        var node = serialiser(lagRequestMedOmsorgspenger(omsorgspenger)).get("omsorgspenger");

        assertThat(node.has("trukketPerioder")).isFalse();
        assertThat(node.has("fraværDelerAvDagen")).isFalse();
    }

    @Test
    void tomme_lister_i_omsorgspenger_inkluderes_i_json() throws Exception {
        var omsorgspenger = new OmsorgspengerDto(false, List.of(), List.of(), List.of());
        var node = serialiser(lagRequestMedOmsorgspenger(omsorgspenger)).get("omsorgspenger");

        assertThat(node.get("fraværHeleDager").isArray()).isTrue();
        assertThat(node.get("fraværHeleDager")).isEmpty();
        assertThat(node.get("fraværDelerAvDagen").isArray()).isTrue();
        assertThat(node.get("trukketPerioder").isArray()).isTrue();
    }

    @Test
    void perioder_skal_serialiseres_med_iso_datoer() throws Exception {
        var omsorgspenger = new OmsorgspengerDto(
            true,
            List.of(new PeriodeDto(LocalDate.of(2024, 1, 8), LocalDate.of(2024, 1, 12))),
            null, null
        );
        var node = serialiser(lagRequestMedOmsorgspenger(omsorgspenger))
            .get("omsorgspenger").get("fraværHeleDager").get(0);

        assertThat(node.get("fom").asText()).isEqualTo("2024-01-08");
        assertThat(node.get("tom").asText()).isEqualTo("2024-01-12");
    }

    // --- Hjelpemetoder ---

    private JsonNode serialiser(Object obj) throws Exception {
        return objectMapper.readTree(objectMapper.writeValueAsString(obj));
    }

    private SendRefusjonOmsorgspengerRequest lagRequestMedOmsorgspenger(OmsorgspengerDto omsorgspenger) {
        return new SendRefusjonOmsorgspengerRequest(
            new FødselsnummerDto(FNR),
            new OrganisasjonsnummerDto(ORGNR),
            LocalDate.of(2024, 1, 1),
            new KontaktpersonDto("Leder Navnesen", "55443322"),
            new BigDecimal("55000"),
            List.of(),
            new AvsenderSystemDto("NAV_NO", "1.0"),
            omsorgspenger
        );
    }
}
