package no.nav.familie.inntektsmelding.pip;


import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.enterprise.context.Dependent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.integrasjoner.altinn.AltinnRessurser;
import no.nav.familie.inntektsmelding.integrasjoner.altinn.ArbeidsgiverAltinnTilgangerKlient;
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

        // TODO: Må ryddes opp etter Altinn 3 ressurs overgang i prod.
        var orgNrBrukerHarTilgangTilPerRessurs = altinnKlient.hentTilganger().tilgangTilOrgNr();

        var orgNrMedGittTilgangIAltinn2 = hentOrgNrMedGittTilgang(orgNrBrukerHarTilgangTilPerRessurs, AltinnRessurser.ALTINN_TO_TJENESTE);
        var orgNrMedGittTilgangIAltinn3 = hentOrgNrMedGittTilgang(orgNrBrukerHarTilgangTilPerRessurs, AltinnRessurser.ALTINN_TRE_RESSURS);

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

    private static Set<String> hentOrgNrMedGittTilgang(Map<String, List<String>> orgNrBrukerHarTilgangTilPerRessurs, String ressurs) {
        return new HashSet<>(orgNrBrukerHarTilgangTilPerRessurs.getOrDefault(ressurs, List.of()));
    }
}
