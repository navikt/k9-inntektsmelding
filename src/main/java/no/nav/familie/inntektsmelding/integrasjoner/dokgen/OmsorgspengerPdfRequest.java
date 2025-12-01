package no.nav.familie.inntektsmelding.integrasjoner.dokgen;

import java.math.BigDecimal;
import java.util.List;

public record OmsorgspengerPdfRequest(
    String avsenderSystem,
    String navnSøker,
    String personnummer,
    String arbeidsgiverIdent,
    String arbeidsgiverNavn,
    Kontaktperson kontaktperson,
    BigDecimal månedInntekt,
    String opprettetTidspunkt,
    List<Endringsarsak> endringsarsaker,
    List<FraværsPeriode> fraværsperioder,
    String harUtbetaltLønn) {

    public OmsorgspengerPdfRequest anonymiser() {
        return new OmsorgspengerPdfRequest(
            avsenderSystem,
            navnSøker,
            personnummer.substring(0, 4) + "** *****",
            arbeidsgiverIdent.substring(0, 4) + "** *****",
            arbeidsgiverNavn,
            kontaktperson,
            månedInntekt,
            opprettetTidspunkt,
            endringsarsaker,
            fraværsperioder,
            harUtbetaltLønn
        );
    }
}
