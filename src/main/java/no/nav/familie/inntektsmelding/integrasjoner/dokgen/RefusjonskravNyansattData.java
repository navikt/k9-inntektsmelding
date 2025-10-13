package no.nav.familie.inntektsmelding.integrasjoner.dokgen;

import java.util.List;

import no.nav.familie.inntektsmelding.koder.Ytelsetype;

public record RefusjonskravNyansattData(String navnSøker,
                                        String personnummer,
                                        Ytelsetype ytelsetype,
                                        String arbeidsgiverIdent,
                                        String arbeidsgiverNavn,
                                        Kontaktperson kontaktperson,
                                        String startDato,
                                        String opprettetTidspunkt,
                                        List<RefusjonsendringPeriode> refusjonsendringer,
                                        int antallRefusjonsperioder) {


    public RefusjonskravNyansattData anonymiser() {
        return new RefusjonskravNyansattData(
            navnSøker,
            personnummer.substring(0, 4) + "** *****",
            ytelsetype,
            arbeidsgiverIdent.substring(0, 4) + "** *****",
            arbeidsgiverNavn,
            kontaktperson,
            startDato,
            opprettetTidspunkt,
            refusjonsendringer,
            antallRefusjonsperioder
        );
    }

}

