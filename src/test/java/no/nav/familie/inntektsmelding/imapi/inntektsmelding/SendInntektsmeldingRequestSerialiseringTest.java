package no.nav.familie.inntektsmelding.imapi.inntektsmelding;

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
import no.nav.k9.inntektsmelding.felles.AvsenderSystemDto;
import no.nav.k9.inntektsmelding.felles.BortfaltNaturalytelseDto;
import no.nav.k9.inntektsmelding.felles.EndringsårsakDto;
import no.nav.k9.inntektsmelding.felles.EndringsårsakerDto;
import no.nav.k9.inntektsmelding.felles.FødselsnummerDto;
import no.nav.k9.inntektsmelding.felles.KontaktpersonDto;
import no.nav.k9.inntektsmelding.felles.NaturalytelsetypeDto;
import no.nav.k9.inntektsmelding.felles.OrganisasjonsnummerDto;
import no.nav.k9.inntektsmelding.felles.RefusjonDto;
import no.nav.k9.inntektsmelding.felles.YtelseTypeDto;
import no.nav.k9.inntektsmelding.imapi.inntektsmelding.SendInntektsmeldingRequest;

class SendInntektsmeldingRequestSerialiseringTest {

    private static final UUID FORESPORSEL_UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
    private static final String FNR = "11111111111";
    private static final String ORGNR = "974760673";

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new JacksonJsonConfig().getContext(null);
    }

    // --- Deserialiseringstester: fra kjent JSON til objekt ---

    @Test
    void skal_deserialisere_minimal_request_fra_json() throws Exception {
        var json = """
            {
              "foresporselUuid": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
              "fødselsnummer": {"fnr": "11111111111"},
              "organisasjonsnummer": {"orgnr": "974760673"},
              "startdato": "2024-03-01",
              "ytelseType": "PLEIEPENGER_SYKT_BARN",
              "kontaktperson": {"navn": "Kari Nordmann", "telefonnummer": "99887766"},
              "inntekt": 60000
            }
            """;

        var request = objectMapper.readValue(json, SendInntektsmeldingRequest.class);

        assertThat(request.foresporselUuid()).isEqualTo(FORESPORSEL_UUID);
        assertThat(request.fødselsnummer().fnr()).isEqualTo(FNR);
        assertThat(request.organisasjonsnummer().orgnr()).isEqualTo(ORGNR);
        assertThat(request.startdato()).isEqualTo(LocalDate.of(2024, 3, 1));
        assertThat(request.ytelseType()).isEqualTo(YtelseTypeDto.PLEIEPENGER_SYKT_BARN);
        assertThat(request.inntekt()).isEqualByComparingTo("60000");
        assertThat(request.refusjon()).isNull();
        assertThat(request.avsenderSystem()).isNull();
    }

    @Test
    void skal_deserialisere_request_med_refusjon_og_naturalytelse() throws Exception {
        var json = """
            {
              "foresporselUuid": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
              "fødselsnummer": {"fnr": "11111111111"},
              "organisasjonsnummer": {"orgnr": "974760673"},
              "startdato": "2024-03-01",
              "ytelseType": "OMSORGSPENGER",
              "kontaktperson": {"navn": "Ola", "telefonnummer": "11223344"},
              "inntekt": 45000,
              "refusjon": [{"fom": "2024-03-01", "beløp": 10000}],
              "bortfaltNaturalytelsePerioder": [
                {"fom": "2024-01-01", "naturalytelsetype": "BIL", "beløp": 3000}
              ],
              "endringAvInntektÅrsaker": [
                {"årsak": "BONUS", "fom": "2024-01-01"}
              ],
              "avsenderSystem": {"systemNavn": "ALTINN", "systemVersjon": "2.0"}
            }
            """;

        var request = objectMapper.readValue(json, SendInntektsmeldingRequest.class);

        assertThat(request.refusjon()).hasSize(1);
        assertThat(request.refusjon().getFirst().fom()).isEqualTo(LocalDate.of(2024, 3, 1));
        assertThat(request.refusjon().getFirst().beløp()).isEqualByComparingTo("10000");
        assertThat(request.bortfaltNaturalytelsePerioder().getFirst().naturalytelsetype())
            .isEqualTo(NaturalytelsetypeDto.BIL);
        assertThat(request.bortfaltNaturalytelsePerioder().getFirst().tom()).isNull();
        assertThat(request.endringAvInntektÅrsaker().getFirst().årsak()).isEqualTo(EndringsårsakDto.BONUS);
        assertThat(request.avsenderSystem().systemNavn()).isEqualTo("ALTINN");
    }

    @Test
    void skal_deserialisere_request_med_tomme_lister() throws Exception {
        var json = """
            {
              "foresporselUuid": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
              "fødselsnummer": {"fnr": "11111111111"},
              "organisasjonsnummer": {"orgnr": "974760673"},
              "startdato": "2024-03-01",
              "ytelseType": "PLEIEPENGER_SYKT_BARN",
              "kontaktperson": {"navn": "Test", "telefonnummer": "99999999"},
              "inntekt": 50000,
              "refusjon": [],
              "bortfaltNaturalytelsePerioder": [],
              "endringAvInntektÅrsaker": []
            }
            """;

        var request = objectMapper.readValue(json, SendInntektsmeldingRequest.class);

        assertThat(request.refusjon()).isEmpty();
        assertThat(request.bortfaltNaturalytelsePerioder()).isEmpty();
        assertThat(request.endringAvInntektÅrsaker()).isEmpty();
    }

    // --- Serialiseringstester: fra objekt til kjent JSON-form ---

    @Test
    void fødselsnummer_skal_serialiseres_som_objekt_med_fnr_felt() throws Exception {
        var node = serialiser(lagFullRequest());

        assertThat(node.get("fødselsnummer").isObject()).isTrue();
        assertThat(node.get("fødselsnummer").get("fnr").asText()).isEqualTo(FNR);
    }

    @Test
    void organisasjonsnummer_skal_serialiseres_som_objekt_med_orgnr_felt() throws Exception {
        var node = serialiser(lagFullRequest());

        assertThat(node.get("organisasjonsnummer").isObject()).isTrue();
        assertThat(node.get("organisasjonsnummer").get("orgnr").asText()).isEqualTo(ORGNR);
    }

    @Test
    void startdato_skal_serialiseres_som_iso_dato() throws Exception {
        var node = serialiser(lagFullRequest());

        assertThat(node.get("startdato").asText()).isEqualTo("2024-03-01");
    }

    @Test
    void tomme_lister_skal_inkluderes_i_json() throws Exception {
        var node = serialiser(lagRequestMedTommeLister());

        assertThat(node.get("refusjon").isArray()).isTrue();
        assertThat(node.get("refusjon")).isEmpty();
        assertThat(node.get("bortfaltNaturalytelsePerioder").isArray()).isTrue();
        assertThat(node.get("endringAvInntektÅrsaker").isArray()).isTrue();
    }

    @Test
    void null_felt_skal_utelates_fra_json() throws Exception {
        var node = serialiser(lagRequestMedTommeLister()); // avsenderSystem er null

        assertThat(node.has("avsenderSystem")).isFalse();
    }

    // --- Hjelpemetoder ---

    private JsonNode serialiser(Object obj) throws Exception {
        return objectMapper.readTree(objectMapper.writeValueAsString(obj));
    }

    private SendInntektsmeldingRequest lagFullRequest() {
        return new SendInntektsmeldingRequest(
            FORESPORSEL_UUID,
            new FødselsnummerDto(FNR),
            new OrganisasjonsnummerDto(ORGNR),
            LocalDate.of(2024, 3, 1),
            YtelseTypeDto.PLEIEPENGER_SYKT_BARN,
            new KontaktpersonDto("Kari Nordmann", "99887766"),
            new BigDecimal("60000"),
            List.of(new RefusjonDto(LocalDate.of(2024, 3, 1), new BigDecimal("60000"))),
            List.of(), List.of(),
            new AvsenderSystemDto("NAV_NO", "1.0")
        );
    }

    private SendInntektsmeldingRequest lagRequestMedTommeLister() {
        return new SendInntektsmeldingRequest(
            FORESPORSEL_UUID,
            new FødselsnummerDto(FNR),
            new OrganisasjonsnummerDto(ORGNR),
            LocalDate.now(),
            YtelseTypeDto.OPPLÆRINGSPENGER,
            new KontaktpersonDto("Ola Nordmann", "11223344"),
            new BigDecimal("40000"),
            List.of(), List.of(), List.of(),
            null
        );
    }
}
