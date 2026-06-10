package no.nav.familie.inntektsmelding.server.tilgangsstyring;

import java.util.Set;
import java.util.stream.Collectors;

import no.nav.k9.felles.sikkerhet.abac.ÅrsakIkkeTilgang;

public final class SifAbacPdpUtil {

    private SifAbacPdpUtil() {
        // Utility class
    }

    public static String hentBegrunnelse(Set<ÅrsakIkkeTilgang> årsaker) {
        return årsaker.stream()
            .map(årsak -> switch (årsak) {
                case HAR_IKKE_TILGANG_TIL_KODE6_PERSON -> "Ikke tilgang til kode6 person";
                case HAR_IKKE_TILGANG_TIL_KODE7_PERSON -> "Ikke tilgang til kode7 person";
                case HAR_IKKE_TILGANG_TIL_EGEN_ANSATT -> "Ikke tilgang til egen ansatt";
                case HAR_IKKE_TILGANG_TIL_APPLIKASJONEN -> "Ikke tilgang til applikasjonen";
                case HAR_IKKE_TILGANG_TIL_HISTORISK_SAK -> "Ikke tilgang til historisk sak";
                default -> "Ikke tilgang";
            })
            .collect(Collectors.joining("\n"));
    }
}

