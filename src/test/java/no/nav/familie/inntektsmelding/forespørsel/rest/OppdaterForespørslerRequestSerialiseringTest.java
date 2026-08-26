package no.nav.familie.inntektsmelding.forespørsel.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import no.nav.familie.inntektsmelding.server.jackson.JacksonJsonConfig;
import no.nav.familie.inntektsmelding.typer.dto.AktørIdDto;
import no.nav.familie.inntektsmelding.typer.dto.ForespørselAksjon;
import no.nav.familie.inntektsmelding.typer.dto.OppdaterForespørselDto;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.PeriodeDto;
import no.nav.familie.inntektsmelding.typer.dto.SaksnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.YtelseTypeDto;

class OppdaterForespørslerRequestSerialiseringTest {

    private static final String AKTØR_ID = "1234567890123";
    private static final String ORGNR = "974760673";
    private static final String SAKSNR = "SAK123";

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new JacksonJsonConfig().getContext(null);
    }

    // --- Deserialiseringstester: fra kjent JSON til objekt ---

    @Test
    void skal_deserialisere_request_med_pleiepenger() throws Exception {
        var json = """
            {
              "aktørId": "1234567890123",
              "forespørsler": [
                {
                  "skjæringstidspunkt": "2024-01-01",
                  "orgnr": "974760673",
                  "aksjon": "OPPRETT"
                }
              ],
              "ytelsetype": "PLEIEPENGER_SYKT_BARN",
              "saksnummer": "SAK123"
            }
            """;

        var request = objectMapper.readValue(json, OppdaterForespørslerRequest.class);

        assertThat(request.aktørId().id()).isEqualTo(AKTØR_ID);
        assertThat(request.forespørsler()).hasSize(1);
        assertThat(request.forespørsler().getFirst().skjæringstidspunkt()).isEqualTo(LocalDate.of(2024, 1, 1));
        assertThat(request.forespørsler().getFirst().orgnr().orgnr()).isEqualTo(ORGNR);
        assertThat(request.forespørsler().getFirst().aksjon()).isEqualTo(ForespørselAksjon.OPPRETT);
        assertThat(request.forespørsler().getFirst().etterspurtePerioder()).isNull();
        assertThat(request.ytelsetype()).isEqualTo(YtelseTypeDto.PLEIEPENGER_SYKT_BARN);
        assertThat(request.saksnummer().saksnr()).isEqualTo(SAKSNR);
    }

    @Test
    void skal_deserialisere_request_med_omsorgspenger_og_etterspurte_perioder() throws Exception {
        var json = """
            {
              "aktørId": "1234567890123",
              "forespørsler": [
                {
                  "skjæringstidspunkt": "2024-01-01",
                  "orgnr": "974760673",
                  "aksjon": "OPPRETT",
                  "etterspurtePerioder": [
                    {"fom": "2024-01-01", "tom": "2024-01-31"},
                    {"fom": "2024-03-01", "tom": "2024-03-31"}
                  ]
                }
              ],
              "ytelsetype": "OMSORGSPENGER",
              "saksnummer": "SAK123"
            }
            """;

        var request = objectMapper.readValue(json, OppdaterForespørslerRequest.class);

        assertThat(request.ytelsetype()).isEqualTo(YtelseTypeDto.OMSORGSPENGER);
        assertThat(request.forespørsler().getFirst().etterspurtePerioder()).hasSize(2);
        assertThat(request.forespørsler().getFirst().etterspurtePerioder().getFirst().fom())
            .isEqualTo(LocalDate.of(2024, 1, 1));
        assertThat(request.forespørsler().getFirst().etterspurtePerioder().getLast().tom())
            .isEqualTo(LocalDate.of(2024, 3, 31));
    }

    @Test
    void skal_deserialisere_request_med_flere_forespørsler_og_ulike_aksjoner() throws Exception {
        var json = """
            {
              "aktørId": "1234567890123",
              "forespørsler": [
                {"skjæringstidspunkt": "2024-01-01", "orgnr": "974760673", "aksjon": "OPPRETT"},
                {"skjæringstidspunkt": "2023-06-01", "orgnr": "974760673", "aksjon": "UTGÅTT"},
                {"skjæringstidspunkt": "2023-01-01", "orgnr": "974760673", "aksjon": "BEHOLD"}
              ],
              "ytelsetype": "PLEIEPENGER_SYKT_BARN",
              "saksnummer": "SAK123"
            }
            """;

        var request = objectMapper.readValue(json, OppdaterForespørslerRequest.class);

        assertThat(request.forespørsler()).hasSize(3);
        assertThat(request.forespørsler().get(1).aksjon()).isEqualTo(ForespørselAksjon.UTGÅTT);
        assertThat(request.forespørsler().get(2).aksjon()).isEqualTo(ForespørselAksjon.BEHOLD);
    }

    // --- Serialiseringstester: fra objekt til kjent JSON-form ---

    @Test
    void aktørId_og_orgnr_og_saksnummer_skal_serialiseres_som_plain_string() throws Exception {
        var forespørsel = new OppdaterForespørselDto(
            LocalDate.of(2024, 1, 1),
            new OrganisasjonsnummerDto(ORGNR),
            ForespørselAksjon.OPPRETT
        );
        var request = new OppdaterForespørslerRequest(
            new AktørIdDto(AKTØR_ID),
            List.of(forespørsel),
            YtelseTypeDto.PLEIEPENGER_SYKT_BARN,
            new SaksnummerDto(SAKSNR)
        );

        var node = serialiser(request);

        assertThat(node.get("aktørId").isTextual()).isTrue();
        assertThat(node.get("aktørId").asText()).isEqualTo(AKTØR_ID);
        assertThat(node.get("saksnummer").isTextual()).isTrue();
        assertThat(node.get("saksnummer").asText()).isEqualTo(SAKSNR);
        assertThat(node.get("forespørsler").get(0).get("orgnr").isTextual()).isTrue();
        assertThat(node.get("forespørsler").get(0).get("orgnr").asText()).isEqualTo(ORGNR);
    }

    @Test
    void tom_forespørsler_liste_skal_inkluderes_som_tom_array_i_json() throws Exception {
        var request = new OppdaterForespørslerRequest(
            new AktørIdDto(AKTØR_ID),
            List.of(),
            YtelseTypeDto.PLEIEPENGER_SYKT_BARN,
            new SaksnummerDto(SAKSNR)
        );

        var node = serialiser(request);

        assertThat(node.get("forespørsler").isArray()).isTrue();
        assertThat(node.get("forespørsler")).isEmpty();
    }

    @Test
    void null_etterspurtePerioder_skal_utelates_fra_json() throws Exception {
        var forespørsel = new OppdaterForespørselDto(
            LocalDate.of(2024, 1, 1),
            new OrganisasjonsnummerDto(ORGNR),
            ForespørselAksjon.OPPRETT,
            null
        );
        var request = new OppdaterForespørslerRequest(
            new AktørIdDto(AKTØR_ID),
            List.of(forespørsel),
            YtelseTypeDto.PLEIEPENGER_SYKT_BARN,
            new SaksnummerDto(SAKSNR)
        );

        var forespørselNode = serialiser(request).get("forespørsler").get(0);

        assertThat(forespørselNode.has("etterspurtePerioder")).isFalse();
    }

    @Test
    void tom_etterspurtePerioder_liste_skal_inkluderes_som_tom_array_i_json() throws Exception {
        var forespørsel = new OppdaterForespørselDto(
            LocalDate.of(2024, 1, 1),
            new OrganisasjonsnummerDto(ORGNR),
            ForespørselAksjon.OPPRETT,
            List.of()
        );
        var request = new OppdaterForespørslerRequest(
            new AktørIdDto(AKTØR_ID),
            List.of(forespørsel),
            YtelseTypeDto.PLEIEPENGER_SYKT_BARN,
            new SaksnummerDto(SAKSNR)
        );

        var forespørselNode = serialiser(request).get("forespørsler").get(0);

        assertThat(forespørselNode.get("etterspurtePerioder").isArray()).isTrue();
        assertThat(forespørselNode.get("etterspurtePerioder")).isEmpty();
    }

    @Test
    void skjæringstidspunkt_skal_serialiseres_som_iso_dato() throws Exception {
        var forespørsel = new OppdaterForespørselDto(
            LocalDate.of(2024, 6, 15),
            new OrganisasjonsnummerDto(ORGNR),
            ForespørselAksjon.OPPRETT
        );
        var request = new OppdaterForespørslerRequest(
            new AktørIdDto(AKTØR_ID),
            List.of(forespørsel),
            YtelseTypeDto.PLEIEPENGER_SYKT_BARN,
            new SaksnummerDto(SAKSNR)
        );

        var forespørselNode = serialiser(request).get("forespørsler").get(0);

        assertThat(forespørselNode.get("skjæringstidspunkt").asText()).isEqualTo("2024-06-15");
    }

    @Test
    void etterspurte_perioder_serialiseres_med_iso_datoer() throws Exception {
        var perioder = List.of(new PeriodeDto(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31)));
        var forespørsel = new OppdaterForespørselDto(
            LocalDate.of(2024, 1, 1),
            new OrganisasjonsnummerDto(ORGNR),
            ForespørselAksjon.OPPRETT,
            perioder
        );
        var request = new OppdaterForespørslerRequest(
            new AktørIdDto(AKTØR_ID),
            List.of(forespørsel),
            YtelseTypeDto.OMSORGSPENGER,
            new SaksnummerDto(SAKSNR)
        );

        var periodeNode = serialiser(request).get("forespørsler").get(0).get("etterspurtePerioder").get(0);

        assertThat(periodeNode.get("fom").asText()).isEqualTo("2024-01-01");
        assertThat(periodeNode.get("tom").asText()).isEqualTo("2024-01-31");
    }

    // --- Hjelpemetoder ---

    private JsonNode serialiser(Object obj) throws Exception {
        return objectMapper.readTree(objectMapper.writeValueAsString(obj));
    }
}

