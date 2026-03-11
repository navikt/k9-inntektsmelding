package no.nav.familie.inntektsmelding.integrasjoner.dokgen;

import java.math.BigDecimal;
import java.util.List;

public record OmsorgspengerRefusjonPdfRequest(
    String avsenderSystem,
    String navnSøker,
    String personnummer,
    String arbeidsgiverIdent,
    String arbeidsgiverNavn,
    Kontaktperson kontaktperson,
    BigDecimal månedInntekt,
    String opprettetTidspunkt,
    List<Endringsarsak> endringsarsaker,
    Omsorgspenger omsorgspenger,
    BigDecimal årForRefusjon
) {

    public OmsorgspengerRefusjonPdfRequest anonymiser() {
        return new OmsorgspengerRefusjonPdfRequest(
            avsenderSystem,
            navnSøker,
            personnummer.substring(0, 4) + "** *****",
            arbeidsgiverIdent.substring(0, 4) + "** *****",
            arbeidsgiverNavn,
            kontaktperson,
            månedInntekt,
            opprettetTidspunkt,
            endringsarsaker,
            omsorgspenger,
            årForRefusjon
        );
    }
}
