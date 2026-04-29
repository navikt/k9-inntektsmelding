package no.nav.familie.inntektsmelding.integrasjoner.altinn;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.enterprise.context.Dependent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.metrikker.MetrikkerTjeneste;

@Dependent
public class AltinnTilgangTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(AltinnTilgangTjeneste.class);

    private final ArbeidsgiverAltinnTilgangerKlient altinnKlient;

    public AltinnTilgangTjeneste() {
        this(ArbeidsgiverAltinnTilgangerKlient.instance());
    }

    public AltinnTilgangTjeneste(ArbeidsgiverAltinnTilgangerKlient altinnKlient) {
        this.altinnKlient = altinnKlient;
    }

    public boolean harTilgangTilBedriften(String orgnr) {
        Map<String, List<String>> altinnRessurserBrukerHarTilgangTilPerOrgnr = altinnKlient.hentTilganger().orgNrTilTilganger();

        if (altinnRessurserBrukerHarTilgangTilPerOrgnr == null || altinnRessurserBrukerHarTilgangTilPerOrgnr.isEmpty()
            || !altinnRessurserBrukerHarTilgangTilPerOrgnr.containsKey(orgnr)) {
            return false;
        }

        List<String> brukersTilgangerForOrgnr = altinnRessurserBrukerHarTilgangTilPerOrgnr.get(orgnr);

        boolean harTilgangGjennomAltinn2 = brukersTilgangerForOrgnr.contains(AltinnRessurser.ALTINN_TO_TJENESTE);
        boolean harTilgangGjennomAltinn3 = brukersTilgangerForOrgnr.contains(AltinnRessurser.ALTINN_TRE_RESSURS);

        if (harTilgangGjennomAltinn2 != harTilgangGjennomAltinn3) { // hvis tilgang er ulikt mellom Altinn 2 og Altinn 3, logg for avstemming.
            LOG.warn("ALTINN: Tilgangsbeslutninger er ulike for bruker! Altinn 2: {}, Altinn 3: {}.",
                harTilgangGjennomAltinn2,
                harTilgangGjennomAltinn3);
        }
        MetrikkerTjeneste.loggAltinnTilgangBedrift(harTilgangGjennomAltinn2 == harTilgangGjennomAltinn3);

        if (harTilgangGjennomAltinn3) {
            return true;
        }

        return harTilgangGjennomAltinn2;
    }

    public boolean manglerTilgangTilBedriften(String orgNr) {
        return !harTilgangTilBedriften(orgNr);
    }

    public List<String> hentBedrifterArbeidsgiverHarTilgangTil() {
        var organisasjoner = altinnKlient.hentTilganger().hierarki();
        var underenheter = finnUnderenheter(organisasjoner);

        var orgNrMedGittTilgangIAltinn2 = hentOrgNrMedTilgangAltinn2(underenheter);
        var orgNrMedGittTilgangIAltinn3 = hentOrgNrMedTilgangAltinn3(underenheter);

        var erLik = orgNrMedGittTilgangIAltinn2.equals(orgNrMedGittTilgangIAltinn3);
        if (!erLik) {
            LOG.info("ALTINN: Uoverensstemmelse i lister over bedrifter bruker har tilgang til mellom Altinn 2 og Altinn 3.");
        } else {
            LOG.info("ALTINN: Hentet like mange bedrifter fra altinn 2 som altinn 3.");
        }
        MetrikkerTjeneste.loggAltinnHentBedrifter(erLik);

        if (!orgNrMedGittTilgangIAltinn3.containsAll(orgNrMedGittTilgangIAltinn2)) {
            orgNrMedGittTilgangIAltinn3.addAll(orgNrMedGittTilgangIAltinn2); // vi legger på det som mangler fra Altinn 2.
        }
        return orgNrMedGittTilgangIAltinn3.stream().toList();
    }

    private static Set<String> hentOrgNrMedTilgangAltinn2(List<ArbeidsgiverAltinnTilgangerKlient.ArbeidsgiverAltinnTilgangerResponse.Organisasjon> underenheter) {
        var resultat = new HashSet<String>();
        for (var org : underenheter) {
            var tilganger = org.altinn2Tilganger() != null ? org.altinn2Tilganger() : List.<String>of();
            if (tilganger.contains(AltinnRessurser.ALTINN_TO_TJENESTE)) {
                resultat.add(org.orgnr());
            }
        }
        return resultat;
    }

    private static Set<String> hentOrgNrMedTilgangAltinn3(List<ArbeidsgiverAltinnTilgangerKlient.ArbeidsgiverAltinnTilgangerResponse.Organisasjon> underenheter) {
        var resultat = new HashSet<String>();
        for (var org : underenheter) {
            var tilganger = org.altinn3Tilganger() != null ? org.altinn3Tilganger() : List.<String>of();
            if (tilganger.contains(AltinnRessurser.ALTINN_TRE_RESSURS)) {
                resultat.add(org.orgnr());
            }
        }
        return resultat;
    }

    private static List<ArbeidsgiverAltinnTilgangerKlient.ArbeidsgiverAltinnTilgangerResponse.Organisasjon> finnUnderenheter(
        List<ArbeidsgiverAltinnTilgangerKlient.ArbeidsgiverAltinnTilgangerResponse.Organisasjon> organisasjoner) {
        var underenheter = new ArrayList<ArbeidsgiverAltinnTilgangerKlient.ArbeidsgiverAltinnTilgangerResponse.Organisasjon>();
        if (organisasjoner == null) {
            return underenheter;
        }
        for (var organisasjon : organisasjoner) {
            if (organisasjon.underenheter() == null || organisasjon.underenheter().isEmpty()) {
                underenheter.add(organisasjon);
            } else {
                underenheter.addAll(finnUnderenheter(organisasjon.underenheter()));
            }
        }
        return underenheter;
    }
}
