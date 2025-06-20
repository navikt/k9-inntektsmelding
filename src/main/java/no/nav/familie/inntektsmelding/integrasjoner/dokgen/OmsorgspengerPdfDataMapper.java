package no.nav.familie.inntektsmelding.integrasjoner.dokgen;

import static no.nav.familie.inntektsmelding.integrasjoner.dokgen.InntektsmeldingPdfData.formaterDatoForLister;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

import no.nav.familie.inntektsmelding.imdialog.modell.EndringsårsakEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.OmsorgspengerEntitet;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonInfo;

public class OmsorgspengerPdfDataMapper {

    private OmsorgspengerPdfDataMapper() {
        throw new IllegalStateException("InntektsmeldingPdfDataMapper: Utility class");
    }

    public static OmsorgspengerPdfData mapOmsorgspengerData(InntektsmeldingEntitet inntektsmelding,
                                                            String arbeidsgiverNavn,
                                                            PersonInfo personInfo,
                                                            String arbeidsgvierIdent) {
        if (inntektsmelding.getOmsorgspenger() == null) {
            throw new IllegalStateException("InntektsmeldingEntitet mangler omsorgspenger data");
        }

        var startdato = inntektsmelding.getStartDato();
        var imDokumentdataBuilder = new OmsorgspengerPdfData.Builder()
            .medNavn(personInfo.mapNavn())
            .medPersonnummer(personInfo.fødselsnummer().getIdent())
            .medArbeidsgiverIdent(arbeidsgvierIdent)
            .medArbeidsgiverNavn(arbeidsgiverNavn)
            .medAvsenderSystem("NAV_NO")
            .medOpprettetTidspunkt(inntektsmelding.getOpprettetTidspunkt())
            .medMånedInntekt(inntektsmelding.getMånedInntekt())
            .medKontaktperson(mapKontaktperson(inntektsmelding))
            .medEndringsårsaker(mapEndringsårsaker(inntektsmelding.getEndringsårsaker()))
            .medOmsorgspenger(mapOmsorgspenger(inntektsmelding.getOmsorgspenger()))
            .medÅrForRefusjon(BigDecimal.valueOf(startdato.getYear()));

        return imDokumentdataBuilder.build();
    }

    private static Omsorgspenger mapOmsorgspenger(OmsorgspengerEntitet omsorgspenger) {
        var datoFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        var fraværsPerioder = omsorgspenger.getFraværsPerioder()
            .stream()
            .map(fp -> new Omsorgspenger.FraværsPeriode(fp.getPeriode().getFom().format(datoFormat), fp.getPeriode().getTom().format(datoFormat)))
            .toList();

        var delvisFraværsPerioder = omsorgspenger.getDelvisFraværsPerioder()
            .stream()
            .filter(dfp -> dfp.getTimer().compareTo(BigDecimal.ZERO) > 0)
            .map(dfp -> new Omsorgspenger.DelvisFraværsPeriode(dfp.getDato().format(datoFormat), dfp.getTimer()))
            .toList();

        var trukketFraværsPerioder = omsorgspenger.getDelvisFraværsPerioder()
            .stream()
            .filter(dfp -> dfp.getTimer().compareTo(BigDecimal.ZERO) == 0)
            .map(dfp -> new Omsorgspenger.TrukketFraværsPeriode(dfp.getDato().format(datoFormat)))
            .toList();

        return new Omsorgspenger(omsorgspenger.isHarUtbetaltPliktigeDager(), fraværsPerioder, delvisFraværsPerioder, trukketFraværsPerioder);
    }

    private static List<Endringsarsak> mapEndringsårsaker(List<EndringsårsakEntitet> endringsårsaker) {
        return endringsårsaker.stream()
            .map( endringsårsakEntitet -> new Endringsarsak(endringsårsakEntitet.getÅrsak().getBeskrivelse(), formaterDatoForLister(endringsårsakEntitet.getFom().orElse(null)), formaterDatoForLister(endringsårsakEntitet.getTom().orElse(null)),
                formaterDatoForLister(endringsårsakEntitet.getBleKjentFom().orElse(null))))
            .toList();
    }

    private static Kontaktperson mapKontaktperson(InntektsmeldingEntitet inntektsmelding) {
        return new Kontaktperson(inntektsmelding.getKontaktperson().getNavn(), inntektsmelding.getKontaktperson().getTelefonnummer());
    }
}
