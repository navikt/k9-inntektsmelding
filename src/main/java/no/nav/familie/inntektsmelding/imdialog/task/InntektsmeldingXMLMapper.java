package no.nav.familie.inntektsmelding.imdialog.task;

import jakarta.xml.bind.JAXBElement;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.OrganisasjonsnummerValidator;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.seres.xsd.nav.inntektsmelding_m._20181211.Arbeidsforhold;
import no.seres.xsd.nav.inntektsmelding_m._20181211.Arbeidsgiver;
import no.seres.xsd.nav.inntektsmelding_m._20181211.ArbeidsgiverPrivat;
import no.seres.xsd.nav.inntektsmelding_m._20181211.Inntekt;
import no.seres.xsd.nav.inntektsmelding_m._20181211.InntektsmeldingM;
import no.seres.xsd.nav.inntektsmelding_m._20181211.Kontaktinformasjon;
import no.seres.xsd.nav.inntektsmelding_m._20181211.ObjectFactory;
import no.seres.xsd.nav.inntektsmelding_m._20181211.Refusjon;
import no.seres.xsd.nav.inntektsmelding_m._20181211.Skjemainnhold;

import java.util.Map;

public class InntektsmeldingXMLMapper {

    public static InntektsmeldingM map(InntektsmeldingEntitet inntektsmelding, Map<AktørIdEntitet, PersonIdent> aktørIdFnrMap) {
        var of = new ObjectFactory();
        Skjemainnhold skjemainnhold = new Skjemainnhold();

        if (OrganisasjonsnummerValidator.erGyldig(inntektsmelding.getArbeidsgiverIdent())) {
            var arbeidsgiver = new Arbeidsgiver();
            arbeidsgiver.setVirksomhetsnummer(inntektsmelding.getArbeidsgiverIdent());
            arbeidsgiver.setKontaktinformasjon(lagKontaktperson(inntektsmelding));
            var agOrg = of.createSkjemainnholdArbeidsgiver(arbeidsgiver);
            skjemainnhold.setArbeidsgiver(agOrg);
        } else if (inntektsmelding.getArbeidsgiverIdent().length() == 13) {
            var arbeidsgiver = new ArbeidsgiverPrivat();
            var identArbeidsgiver = aktørIdFnrMap.get(new AktørIdEntitet(inntektsmelding.getArbeidsgiverIdent()));
            arbeidsgiver.setArbeidsgiverFnr(identArbeidsgiver.getIdent());
            arbeidsgiver.setKontaktinformasjon(lagKontaktperson(inntektsmelding));
            var agPriv = of.createSkjemainnholdArbeidsgiverPrivat(arbeidsgiver);
            skjemainnhold.setArbeidsgiverPrivat(agPriv);
        }
        skjemainnhold.setArbeidsforhold(lagArbeidsforholdXml(inntektsmelding, of));
        skjemainnhold.setArbeidstakerFnr(aktørIdFnrMap.get(inntektsmelding.getAktørId()).getIdent());

        // TODO skal denne være Ny eller Endring?
        skjemainnhold.setAarsakTilInnsending("Ny");
        // FIXME
        skjemainnhold.setAvsendersystem(null);
        // TODO Finn ut hva kodene her skal være
        skjemainnhold.setYtelse(finnYtelseTekst(inntektsmelding.getYtelsetype()));

        skjemainnhold.setRefusjon(lagRefusjonXml(inntektsmelding, of));
        var imXml = new InntektsmeldingM();
        imXml.setSkjemainnhold(skjemainnhold);
        return imXml;
    }

    private static JAXBElement<Refusjon> lagRefusjonXml(InntektsmeldingEntitet inntektsmeldingEntitet, ObjectFactory of) {
        var refusjon = new Refusjon();
        return null;
    }

    private static String finnYtelseTekst(Ytelsetype ytelsetype) {
        return switch (ytelsetype) {
            case FORELDREPENGER -> "Foreldrepenger";
            case SVANGERSKAPSPENGER -> "Svangerskapspenger";
            case OPPLÆRINGSPENGER -> "Opplæringspenger";
            case OMSORGSPENGER -> "Omsorgspenger";
            case PLEIEPENGER_SYKT_BARN, PLEIEPENGER_NÆRSTÅENDE -> throw new IllegalStateException("Ukjent kode ved map til XML " + ytelsetype);
        };
    }

    private static JAXBElement<Arbeidsforhold> lagArbeidsforholdXml(InntektsmeldingEntitet inntektsmeldingEntitet, ObjectFactory of) {
        var arbeidsforhold = new Arbeidsforhold();

        // Inntekt
        var inntektBeløp = of.createInntektBeloep(inntektsmeldingEntitet.getMånedInntekt());
        var inntekt = new Inntekt();
        inntekt.setBeloep(inntektBeløp);
        // TODO sett endringsårsak
        var inntektSkjemaVerdi = of.createArbeidsforholdBeregnetInntekt(inntekt);
        arbeidsforhold.setBeregnetInntekt(inntektSkjemaVerdi);

        // Startdato
        // TODO blir det rett med createSkjemainnholdStartdatoForeldrepengeperiode her?
        arbeidsforhold.setFoersteFravaersdag(of.createSkjemainnholdStartdatoForeldrepengeperiode(inntektsmeldingEntitet.getStartDato()));
        return of.createSkjemainnholdArbeidsforhold(arbeidsforhold);
    }

    private static Kontaktinformasjon lagKontaktperson(InntektsmeldingEntitet inntektsmelding) {
        var kontaktPerson = inntektsmelding.getKontaktperson();
        var ki = new Kontaktinformasjon();
        ki.setTelefonnummer(kontaktPerson.getTelefonnummer());
        ki.setKontaktinformasjonNavn(kontaktPerson.getNavn());
        return ki;
    }

}
