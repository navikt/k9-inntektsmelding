package no.nav.familie.inntektsmelding.integrasjoner.dokgen;

import java.math.BigDecimal;
import java.util.List;

import no.nav.familie.inntektsmelding.koder.Ytelsetype;

public record InntektsmeldingPdfRequest(String avsenderSystem,
                                        String navnSøker,
                                        String personnummer,
                                        Ytelsetype ytelsetype,
                                        String arbeidsgiverIdent,
                                        String arbeidsgiverNavn,
                                        Kontaktperson kontaktperson,
                                        String startDato,
                                        BigDecimal månedInntekt,
                                        String opprettetTidspunkt,
                                        List<RefusjonsendringPeriode> refusjonsendringer,
                                        List<NaturalYtelse> naturalytelser,
                                        boolean ingenBortfaltNaturalytelse,
                                        boolean ingenGjenopptattNaturalytelse,
                                        List<Endringsarsak> endringsarsaker,
                                        int antallRefusjonsperioder) {

    public InntektsmeldingPdfRequest anonymiser() {
        return new InntektsmeldingPdfRequest(
            avsenderSystem,
            navnSøker,
            personnummer.substring(0, 4) + "** *****",
            ytelsetype,
            arbeidsgiverIdent.substring(0, 4) + "** *****",
            arbeidsgiverNavn,
            kontaktperson,
            startDato,
            månedInntekt,
            opprettetTidspunkt,
            refusjonsendringer,
            naturalytelser,
            ingenBortfaltNaturalytelse,
            ingenGjenopptattNaturalytelse,
            endringsarsaker,
            antallRefusjonsperioder
        );
    }
}
