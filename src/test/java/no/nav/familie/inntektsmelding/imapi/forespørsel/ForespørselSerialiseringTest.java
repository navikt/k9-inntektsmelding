package no.nav.familie.inntektsmelding.imapi.forespørsel;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import no.nav.familie.inntektsmelding.server.jackson.JacksonJsonConfig;
import no.nav.k9.inntektsmelding.felles.ForespørselStatusDto;
import no.nav.k9.inntektsmelding.felles.FødselsnummerDto;
import no.nav.k9.inntektsmelding.felles.OrganisasjonsnummerDto;
import no.nav.k9.inntektsmelding.felles.PeriodeDto;
import no.nav.k9.inntektsmelding.felles.YtelseTypeDto;
import no.nav.k9.inntektsmelding.imapi.forespørsel.ForespørselDto;
import no.nav.k9.inntektsmelding.imapi.forespørsel.HentForespørselerRequest;
import no.nav.k9.inntektsmelding.imapi.forespørsel.HentForespørslerResponse;

class ForespørselSerialiseringTest {

    private static final UUID FS_UUID = UUID.fromString("d4e5f6a7-b8c9-0123-def0-456789012345");
    private static final String FNR = "44444444444";
    private static final String ORGNR = "974760673";

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new JacksonJsonConfig().getContext(null);
    }

    // --- Deserialiseringstester: fra kjent JSON til objekt ---

    @Test
    void skal_deserialisere_forespørselDto_fra_json() throws Exception {
        var json = """
            {
              "loepenr": 7,
              "forespørselUuid": "d4e5f6a7-b8c9-0123-def0-456789012345",
              "orgnummer": {"orgnr": "974760673"},
              "fødselsnummer": {"fnr": "44444444444"},
              "skjæringstidspunkt": "2024-01-15",
              "ytelseType": "PLEIEPENGER_SYKT_BARN",
              "status": "UNDER_BEHANDLING",
              "etterspurtePerioder": [],
              "opprettetTid": "2024-01-10T08:00:00"
            }
            """;

        var dto = objectMapper.readValue(json, ForespørselDto.class);

        assertThat(dto.loepenr()).isEqualTo(7L);
        assertThat(dto.forespørselUuid()).isEqualTo(FS_UUID);
        assertThat(dto.orgnummer().orgnr()).isEqualTo(ORGNR);
        assertThat(dto.fødselsnummer().fnr()).isEqualTo(FNR);
        assertThat(dto.skjæringstidspunkt()).isEqualTo(LocalDate.of(2024, 1, 15));
        assertThat(dto.status()).isEqualTo(ForespørselStatusDto.UNDER_BEHANDLING);
        assertThat(dto.etterspurtePerioder()).isEmpty();
        assertThat(dto.opprettetTid()).isEqualTo(LocalDateTime.of(2024, 1, 10, 8, 0, 0));
    }

    @Test
    void skal_deserialisere_forespørselDto_med_perioder() throws Exception {
        var json = """
            {
              "loepenr": 1,
              "forespørselUuid": "d4e5f6a7-b8c9-0123-def0-456789012345",
              "orgnummer": {"orgnr": "974760673"},
              "fødselsnummer": {"fnr": "44444444444"},
              "skjæringstidspunkt": "2024-01-01",
              "ytelseType": "OMSORGSPENGER",
              "status": "FERDIG",
              "etterspurtePerioder": [
                {"fom": "2024-01-01", "tom": "2024-01-31"},
                {"fom": "2024-03-01", "tom": "2024-03-31"}
              ],
              "opprettetTid": "2024-01-10T08:00:00"
            }
            """;

        var dto = objectMapper.readValue(json, ForespørselDto.class);

        assertThat(dto.etterspurtePerioder()).hasSize(2);
        assertThat(dto.etterspurtePerioder().getFirst().fom()).isEqualTo(LocalDate.of(2024, 1, 1));
        assertThat(dto.status()).isEqualTo(ForespørselStatusDto.FERDIG);
    }

    @Test
    void skal_deserialisere_hentForespørselerRequest_med_kun_orgnr() throws Exception {
        var json = """
            {
              "orgnr": {"orgnr": "974760673"}
            }
            """;

        var request = objectMapper.readValue(json, HentForespørselerRequest.class);

        assertThat(request.orgnr().orgnr()).isEqualTo(ORGNR);
        assertThat(request.fnr()).isNull();
        assertThat(request.status()).isNull();
        assertThat(request.ytelseType()).isNull();
    }

    @Test
    void skal_deserialisere_hentForespørselerRequest_med_alle_felt() throws Exception {
        var json = """
            {
              "orgnr": {"orgnr": "974760673"},
              "fnr": {"fnr": "44444444444"},
              "status": "FERDIG",
              "ytelseType": "OMSORGSPENGER",
              "fom": "2024-01-01",
              "tom": "2024-12-31",
              "loepenr": 100
            }
            """;

        var request = objectMapper.readValue(json, HentForespørselerRequest.class);

        assertThat(request.fnr().fnr()).isEqualTo(FNR);
        assertThat(request.status()).isEqualTo(ForespørselStatusDto.FERDIG);
        assertThat(request.ytelseType()).isEqualTo(YtelseTypeDto.OMSORGSPENGER);
        assertThat(request.fom()).isEqualTo(LocalDate.of(2024, 1, 1));
        assertThat(request.loepenr()).isEqualTo(100L);
    }

    // --- Serialiseringstester: fra objekt til kjent JSON-form ---

    @Test
    void opprettetTid_skal_serialiseres_som_iso_datetime() throws Exception {
        var node = serialiser(lagForespørselDto(List.of()));

        assertThat(node.get("opprettetTid").asText()).isEqualTo("2024-01-10T08:00:00");
    }

    @Test
    void tom_etterspurtePerioder_liste_skal_inkluderes_i_json() throws Exception {
        var node = serialiser(lagForespørselDto(List.of()));

        assertThat(node.get("etterspurtePerioder").isArray()).isTrue();
        assertThat(node.get("etterspurtePerioder")).isEmpty();
    }

    @Test
    void null_felt_i_hentForespørselerRequest_utelates_fra_json() throws Exception {
        var request = new HentForespørselerRequest(
            new OrganisasjonsnummerDto(ORGNR),
            null, null, null, null, null, null
        );

        var node = serialiser(request);

        assertThat(node.has("fnr")).isFalse();
        assertThat(node.has("status")).isFalse();
        assertThat(node.has("ytelseType")).isFalse();
        assertThat(node.has("fom")).isFalse();
        assertThat(node.has("tom")).isFalse();
        assertThat(node.has("loepenr")).isFalse();
    }

    @Test
    void hentForespørslerResponse_med_tom_liste_serialiseres_korrekt() throws Exception {
        var node = serialiser(new HentForespørslerResponse(List.of()));

        assertThat(node.get("forespørsler").isArray()).isTrue();
        assertThat(node.get("forespørsler")).isEmpty();
    }

    // --- Hjelpemetoder ---

    private JsonNode serialiser(Object obj) throws Exception {
        return objectMapper.readTree(objectMapper.writeValueAsString(obj));
    }

    private ForespørselDto lagForespørselDto(List<PeriodeDto> perioder) {
        return new ForespørselDto(
            1L, FS_UUID,
            new OrganisasjonsnummerDto(ORGNR),
            new FødselsnummerDto(FNR),
            LocalDate.of(2024, 1, 15),
            YtelseTypeDto.PLEIEPENGER_SYKT_BARN,
            ForespørselStatusDto.UNDER_BEHANDLING,
            perioder,
            LocalDateTime.of(2024, 1, 10, 8, 0, 0)
        );
    }
}
