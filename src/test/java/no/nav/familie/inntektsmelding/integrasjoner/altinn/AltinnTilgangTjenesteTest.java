package no.nav.familie.inntektsmelding.integrasjoner.altinn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AltinnTilgangTjenesteTest {

    private static final String TEST_ORGNR = "999999999";
    private static final String ANNET_ORGNR = "000000000";

    @Mock
    ArbeidsgiverAltinnTilgangerKlient altinnKlient;

    AltinnTilgangTjeneste tjeneste;

    @BeforeEach
    void setUp() {
        tjeneste = new AltinnTilgangTjeneste(altinnKlient);
    }

    // --- harTilgangTilBedriften ---

    @Test
    void harTilgangTilBedriften__har_tilgang_via_altinn2_og_altinn3() {
        when(altinnKlient.hentTilganger()).thenReturn(lagOrgNrTilTilgangResponse(TEST_ORGNR, AltinnRessurser.ALTINN_TO_TJENESTE, AltinnRessurser.ALTINN_TRE_RESSURS));
        assertThat(tjeneste.harTilgangTilBedriften(TEST_ORGNR)).isTrue();
    }

    @Test
    void harTilgangTilBedriften__har_tilgang_kun_via_altinn2() {
        when(altinnKlient.hentTilganger()).thenReturn(lagOrgNrTilTilgangResponse(TEST_ORGNR, AltinnRessurser.ALTINN_TO_TJENESTE));
        assertThat(tjeneste.harTilgangTilBedriften(TEST_ORGNR)).isTrue();
    }

    @Test
    void harTilgangTilBedriften__har_tilgang_kun_via_altinn3() {
        when(altinnKlient.hentTilganger()).thenReturn(lagOrgNrTilTilgangResponse(TEST_ORGNR, AltinnRessurser.ALTINN_TRE_RESSURS));
        assertThat(tjeneste.harTilgangTilBedriften(TEST_ORGNR)).isTrue();
    }

    @Test
    void harTilgangTilBedriften__har_ikke_tilgang_feil_orgnr() {
        when(altinnKlient.hentTilganger()).thenReturn(lagOrgNrTilTilgangResponse(TEST_ORGNR, AltinnRessurser.ALTINN_TO_TJENESTE, AltinnRessurser.ALTINN_TRE_RESSURS));
        assertThat(tjeneste.harTilgangTilBedriften(ANNET_ORGNR)).isFalse();
    }

    @Test
    void harTilgangTilBedriften__har_ikke_tilgang_ingen_relevant_ressurs() {
        when(altinnKlient.hentTilganger()).thenReturn(lagOrgNrTilTilgangResponse(TEST_ORGNR, "annen_ressurs"));
        assertThat(tjeneste.harTilgangTilBedriften(TEST_ORGNR)).isFalse();
    }

    @Test
    void manglerTilgangTilBedriften__returnerer_true_naar_ingen_tilgang() {
        when(altinnKlient.hentTilganger()).thenReturn(lagOrgNrTilTilgangResponse(TEST_ORGNR, "annen_ressurs"));
        assertThat(tjeneste.manglerTilgangTilBedriften(TEST_ORGNR)).isTrue();
    }

    // --- hentBedrifterArbeidsgiverHarTilgangTil ---

    @Test
    void hentBedrifter__har_tilgang_via_altinn2_tjeneste() {
        when(altinnKlient.hentTilganger()).thenReturn(lagTilgangTilOrgNrResponse(AltinnRessurser.ALTINN_TO_TJENESTE, TEST_ORGNR));
        assertThat(tjeneste.hentBedrifterArbeidsgiverHarTilgangTil()).isNotEmpty().contains(TEST_ORGNR);
    }

    @Test
    void hentBedrifter__har_tilgang_via_altinn3_ressurs() {
        when(altinnKlient.hentTilganger()).thenReturn(lagTilgangTilOrgNrResponse(AltinnRessurser.ALTINN_TRE_RESSURS, TEST_ORGNR));
        assertThat(tjeneste.hentBedrifterArbeidsgiverHarTilgangTil()).isNotEmpty().contains(TEST_ORGNR);
    }

    @Test
    void hentBedrifter__ingen_tilgang_med_annen_ressurs() {
        when(altinnKlient.hentTilganger()).thenReturn(lagTilgangTilOrgNrResponse("annen_ressurs", TEST_ORGNR));
        assertThat(tjeneste.hentBedrifterArbeidsgiverHarTilgangTil()).isEmpty();
    }

    @Test
    void hentBedrifter__begge_altinn_versjoner_gir_ingen_duplikater() {
        when(altinnKlient.hentTilganger()).thenReturn(
            new ArbeidsgiverAltinnTilgangerKlient.ArbeidsgiverAltinnTilgangerResponse(
                false, List.of(), null,
                Map.of(
                    AltinnRessurser.ALTINN_TO_TJENESTE, List.of(TEST_ORGNR),
                    AltinnRessurser.ALTINN_TRE_RESSURS, List.of(TEST_ORGNR)
                )
            )
        );
        var bedrifter = tjeneste.hentBedrifterArbeidsgiverHarTilgangTil();
        assertThat(bedrifter).containsExactly(TEST_ORGNR);
    }

    @Test
    void hentBedrifter__altinn2_har_ekstra_orgnr_som_legges_til() {
        when(altinnKlient.hentTilganger()).thenReturn(
            new ArbeidsgiverAltinnTilgangerKlient.ArbeidsgiverAltinnTilgangerResponse(
                false, List.of(), null,
                Map.of(
                    AltinnRessurser.ALTINN_TO_TJENESTE, List.of(TEST_ORGNR, ANNET_ORGNR),
                    AltinnRessurser.ALTINN_TRE_RESSURS, List.of(TEST_ORGNR)
                )
            )
        );
        var bedrifter = tjeneste.hentBedrifterArbeidsgiverHarTilgangTil();
        assertThat(bedrifter).containsExactlyInAnyOrder(TEST_ORGNR, ANNET_ORGNR);
    }

    // --- Hjelpemetoder ---

    private ArbeidsgiverAltinnTilgangerKlient.ArbeidsgiverAltinnTilgangerResponse lagOrgNrTilTilgangResponse(String orgnr, String... tilganger) {
        return new ArbeidsgiverAltinnTilgangerKlient.ArbeidsgiverAltinnTilgangerResponse(false, List.of(), Map.of(orgnr, List.of(tilganger)), null);
    }

    private ArbeidsgiverAltinnTilgangerKlient.ArbeidsgiverAltinnTilgangerResponse lagTilgangTilOrgNrResponse(String tilgang, String... orgnre) {
        return new ArbeidsgiverAltinnTilgangerKlient.ArbeidsgiverAltinnTilgangerResponse(false, List.of(), null, Map.of(tilgang, List.of(orgnre)));
    }
}

