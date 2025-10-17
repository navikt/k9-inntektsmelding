package no.nav.familie.inntektsmelding.integrasjoner.dokgen;

import java.time.LocalDate;
import java.util.List;

import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.utils.FormatUtils;
import no.nav.familie.inntektsmelding.utils.mapper.PdfDataMapperUtil;

public class RefusjonskravNyansattPdfDataMapper {

    private RefusjonskravNyansattPdfDataMapper() {
        throw new IllegalStateException("RefusjonskravNyansattPdfDataMapper: Utility class");
    }

    public static RefusjonskravNyansattData mapRefusjonskravNyansattData(InntektsmeldingEntitet inntektsmelding,
                                                                         PersonInfo personInfo,
                                                                         String arbeidsgiverNavn,
                                                                         String arbeidsgiverIdent) {
        String avsenderSystem = "NAV_NO";
        String navnSøker = personInfo.mapNavn();
        String personnummer = FormatUtils.formaterPersonnummer(personInfo.fødselsnummer().getIdent());
        Ytelsetype ytelsetype = inntektsmelding.getYtelsetype();
        Kontaktperson kontaktperson = PdfDataMapperUtil.mapKontaktperson(inntektsmelding);
        LocalDate startDato = inntektsmelding.getStartDato();
        String opprettetTidspunkt = FormatUtils.formaterDatoOgTidNorsk(inntektsmelding.getOpprettetTidspunkt());

        LocalDate opphørsdato = inntektsmelding.getOpphørsdatoRefusjon() != null ? inntektsmelding.getOpphørsdatoRefusjon() : null;
        List<RefusjonsendringPeriode> refusjonsendringer = PdfDataMapperUtil.mapRefusjonsendringPerioder(inntektsmelding.getRefusjonsendringer(),
            opphørsdato,
            inntektsmelding.getMånedRefusjon(),
            startDato);
        int antallRefusjonsperioder = refusjonsendringer.size();

        return new RefusjonskravNyansattData(
            avsenderSystem,
            navnSøker,
            personnummer,
            ytelsetype,
            arbeidsgiverIdent,
            arbeidsgiverNavn,
            kontaktperson,
            FormatUtils.formaterDatoMedNavnPåUkedag(startDato),
            opprettetTidspunkt,
            refusjonsendringer,
            antallRefusjonsperioder
        );
    }
}
