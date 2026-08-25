package no.nav.familie.inntektsmelding.forespørsel.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import no.nav.familie.inntektsmelding.koder.ForespørselStatus;
import no.nav.familie.inntektsmelding.server.jackson.JacksonJsonConfig;
import no.nav.familie.inntektsmelding.typer.dto.AktørIdDto;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.PeriodeDto;
import no.nav.familie.inntektsmelding.typer.dto.YtelseTypeDto;

class ForespørselResponseSerialiseringTest {

    private static final UUID FORESPØRSEL_UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
    private static final String AKTØR_ID = "1234567890123";
    private static final String ORGNR = "974760673";

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new JacksonJsonConfig().getContext(null);
    }

    // --- Deserialiseringstester: fra kjent JSON til objekt ---

    @Test
    void skal_deserialisere_response_uten_etterspurte_perioder() throws Exception {
        var json = """
            {
              "uuid": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
              "organisasjonsnummer": {"orgnr": "974760673"},
              "skjæringstidspunkt": "2024-01-15",
              "brukerAktørId": {"id": "1234567890123"},
              "ytelseType": "PLEIEPENGER_SYKT_BARN",
              "status": "UNDER_BEHANDLING",
              "etterspurtePerioder": []
            }
            """;

        var response = objectMapper.readValue(json, ForespørselResponse.class);

        assertThat(response.uuid()).isEqualTo(FORESPØRSEL_UUID);
        assertThat(response.organisasjonsnummer().orgnr()).isEqualTo(ORGNR);
        assertThat(response.skjæringstidspunkt()).isEqualTo(LocalDate.of(2024, 1, 15));
        assertThat(response.brukerAktørId().id()).isEqualTo(AKTØR_ID);
        assertThat(response.ytelseType()).isEqualTo(YtelseTypeDto.PLEIEPENGER_SYKT_BARN);
        assertThat(response.status()).isEqualTo(ForespørselStatus.UNDER_BEHANDLING);
        assertThat(response.etterspurtePerioder()).isEmpty();
    }

    @Test
    void skal_deserialisere_response_med_etterspurte_perioder() throws Exception {
        var json = """
            {
              "uuid": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
              "organisasjonsnummer": {"orgnr": "974760673"},
              "skjæringstidspunkt": "2024-01-01",
              "brukerAktørId": {"id": "1234567890123"},
              "ytelseType": "OMSORGSPENGER",
              "status": "FERDIG",
              "etterspurtePerioder": [
                {"fom": "2024-01-01", "tom": "2024-01-31"},
                {"fom": "2024-03-01", "tom": "2024-03-31"}
              ]
            }
            """;

        var response = objectMapper.readValue(json, ForespørselResponse.class);

        assertThat(response.ytelseType()).isEqualTo(YtelseTypeDto.OMSORGSPENGER);
        assertThat(response.status()).isEqualTo(ForespørselStatus.FERDIG);
        assertThat(response.etterspurtePerioder()).hasSize(2);
        assertThat(response.etterspurtePerioder().getFirst().fom()).isEqualTo(LocalDate.of(2024, 1, 1));
        assertThat(response.etterspurtePerioder().getLast().tom()).isEqualTo(LocalDate.of(2024, 3, 31));
    }

    @Test
    void skal_deserialisere_alle_forespørsel_statuser() throws Exception {
        for (var status : ForespørselStatus.values()) {
            var json = """
                {
                  "uuid": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                  "organisasjonsnummer": {"orgnr": "974760673"},
                  "skjæringstidspunkt": "2024-01-01",
                  "brukerAktørId": {"id": "1234567890123"},
                  "ytelseType": "PLEIEPENGER_SYKT_BARN",
                  "status": "%s",
                  "etterspurtePerioder": []
                }
                """.formatted(status.name());

            var response = objectMapper.readValue(json, ForespørselResponse.class);
            assertThat(response.status()).isEqualTo(status);
        }
    }

    // --- Serialiseringstester: fra objekt til kjent JSON-form ---

    @Test
    void tom_etterspurtePerioder_liste_skal_inkluderes_som_tom_array_i_json() throws Exception {
        var node = serialiser(lagResponse(List.of()));

        assertThat(node.get("etterspurtePerioder").isArray()).isTrue();
        assertThat(node.get("etterspurtePerioder")).isEmpty();
    }

    @Test
    void etterspurte_perioder_serialiseres_med_iso_datoer() throws Exception {
        var perioder = List.of(new PeriodeDto(LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 29)));
        var node = serialiser(lagResponse(perioder));

        var periodeNode = node.get("etterspurtePerioder").get(0);
        assertThat(periodeNode.get("fom").asText()).isEqualTo("2024-02-01");
        assertThat(periodeNode.get("tom").asText()).isEqualTo("2024-02-29");
    }

    @Test
    void skjæringstidspunkt_skal_serialiseres_som_iso_dato() throws Exception {
        var node = serialiser(lagResponse(List.of()));

        assertThat(node.get("skjæringstidspunkt").asText()).isEqualTo("2024-01-15");
    }

    @Test
    void null_etterspurtePerioder_skal_utelates_fra_json() throws Exception {
        var response = new ForespørselResponse(
            FORESPØRSEL_UUID,
            new OrganisasjonsnummerDto(ORGNR),
            LocalDate.of(2024, 1, 15),
            new AktørIdDto(AKTØR_ID),
            YtelseTypeDto.PLEIEPENGER_SYKT_BARN,
            ForespørselStatus.UNDER_BEHANDLING,
            null
        );

        var node = serialiser(response);

        assertThat(node.has("etterspurtePerioder")).isFalse();
    }

    // --- Hjelpemetoder ---

    private JsonNode serialiser(Object obj) throws Exception {
        return objectMapper.readTree(objectMapper.writeValueAsString(obj));
    }

    private ForespørselResponse lagResponse(List<PeriodeDto> etterspurtePerioder) {
        return new ForespørselResponse(
            FORESPØRSEL_UUID,
            new OrganisasjonsnummerDto(ORGNR),
            LocalDate.of(2024, 1, 15),
            new AktørIdDto(AKTØR_ID),
            YtelseTypeDto.PLEIEPENGER_SYKT_BARN,
            ForespørselStatus.UNDER_BEHANDLING,
            etterspurtePerioder
        );
    }
}

