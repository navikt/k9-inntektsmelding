package no.nav.familie.inntektsmelding.server.tilgangsstyring;

import java.util.Set;
import java.util.stream.Collectors;

import no.nav.sif.abac.kontrakt.abac.resultat.IkkeTilgangÅrsak;

public final class SifAbacPdpUtil {

    private SifAbacPdpUtil() {
        // Utility class
    }

    public static String hentBegrunnelse(Set<IkkeTilgangÅrsak> årsaker) {
        return årsaker.stream()
            .map(årsak -> switch (årsak) {
                case HAR_IKKE_TILGANG_TIL_KODE6_PERSON -> "Ikke tilgang til kode6 person";
                case HAR_IKKE_TILGANG_TIL_KODE7_PERSON -> "Ikke tilgang til kode7 person";
                case HAR_IKKE_TILGANG_TIL_EGEN_ANSATT -> "Ikke tilgang til egen ansatt";
                case HAR_IKKE_TILGANG_TIL_APPLIKASJONEN -> "Ikke tilgang til applikasjonen";
                case ER_IKKE_VEILEDER_ELLER_SAKSBEHANDLER -> "Ikke veileder eller saksbehandler";
                case ER_IKKE_SAKSBEHANDLER -> "Ikke saksbehandler";
                case ER_IKKE_BESLUTTER -> "Ikke beslutter";
                case ER_IKKE_OVERSTYRER -> "Ikke overstyrer";
                case ER_IKKE_DRIFTER -> "Ikke drifter";
                default -> "Ikke tilgang";
            })
            .collect(Collectors.joining("\n"));
    }
}

