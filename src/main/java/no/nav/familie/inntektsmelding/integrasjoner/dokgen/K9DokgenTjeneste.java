package no.nav.familie.inntektsmelding.integrasjoner.dokgen;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.integrasjoner.organisasjon.OrganisasjonTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.koder.InntektsmeldingType;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.OrganisasjonsnummerValidator;
import no.nav.vedtak.exception.TekniskException;

@ApplicationScoped
public class K9DokgenTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(K9DokgenTjeneste.class);
    private DokgenKlient pdfgenKlient;
    private PersonTjeneste personTjeneste;
    private OrganisasjonTjeneste organisasjonTjeneste;

    K9DokgenTjeneste() {
        //CDI
    }

    @Inject
    public K9DokgenTjeneste(DokgenKlientSelector dokgenKlientSelector, PersonTjeneste personTjeneste, OrganisasjonTjeneste organisasjonTjeneste) {
        this.pdfgenKlient = dokgenKlientSelector.getDokgenKlient();
        this.personTjeneste = personTjeneste;
        this.organisasjonTjeneste = organisasjonTjeneste;
    }

    public byte[] mapDataOgGenererPdf(InntektsmeldingEntitet inntektsmelding) {
        PersonInfo personInfo;
        String arbeidsgiverNavn;
        var arbeidsgvierIdent = inntektsmelding.getArbeidsgiverIdent();
        var inntektsmeldingsid = inntektsmelding.getId() != null ? inntektsmelding.getId().intValue() : 1;

        personInfo = personTjeneste.hentPersonInfoFraAktørId(inntektsmelding.getAktørId());
        arbeidsgiverNavn = finnArbeidsgiverNavn(arbeidsgvierIdent);

        if (inntektsmelding.getYtelsetype() == Ytelsetype.OMSORGSPENGER) {
            if (inntektsmelding.getMånedRefusjon() != null) {
                // lag pdf for refusjonskrav omsorgspenger
                var omsorgspengerRefusjonPdfRequest = OmsorgspengerRefusjonPdfRequestMapper.map(inntektsmelding, arbeidsgiverNavn, personInfo, arbeidsgvierIdent);
                return genererPdfForOmsorgspengerRefusjon(omsorgspengerRefusjonPdfRequest, inntektsmeldingsid);
            } else {
                // lag pdf for inntektsmelding omsorgspenger
                var omsorgspengerInntektsmeldingPdfRequest = OmsorgspengerInntektsmeldingPdfRequestMapper.map(inntektsmelding, arbeidsgiverNavn, personInfo, arbeidsgvierIdent);
                return genererPdfForOmsorgspengerInntektsmelding(omsorgspengerInntektsmeldingPdfRequest, inntektsmeldingsid);
            }
        }

        if (inntektsmelding.getInntektsmeldingType() == InntektsmeldingType.ARBEIDSGIVERINITIERT_NYANSATT) {
            var refusjonskravNyansattData = RefusjonskravNyansattPdfDataMapper.mapRefusjonskravNyansattData(inntektsmelding, personInfo, arbeidsgiverNavn, arbeidsgvierIdent);
            return genererPdfForRefusjonskravNyansatt(refusjonskravNyansattData, inntektsmeldingsid);
        }

        var imDokumentdata = InntektsmeldingPdfDataMapper.map(inntektsmelding, arbeidsgiverNavn, personInfo, arbeidsgvierIdent);
        return genererPdfForInntektsmelding(imDokumentdata, inntektsmeldingsid);
    }

    private byte[] genererPdfForOmsorgspengerRefusjon(OmsorgspengerRefusjonPdfRequest omsorgspengerRefusjonPdfRequest, int inntektsmeldingId) {
        try {
            byte[] pdf = pdfgenKlient.genererPdfOmsorgspengerRefusjon(omsorgspengerRefusjonPdfRequest);
            LOG.info("Pdf av refusjonskrav omsorgspenger med id {} ble generert.", inntektsmeldingId);
            return pdf;
        } catch (Exception e) {
            var anonymPdfRequest = omsorgspengerRefusjonPdfRequest.anonymiser();
            throw new TekniskException("K9INNTEKTSMELDING_1",
                String.format("Klarte ikke å generere pdf for refusjonskrav omsorgspenger med id %s", inntektsmeldingId), e);
        }
    }

    private byte[] genererPdfForOmsorgspengerInntektsmelding(OmsorgspengerInntektsmeldingPdfRequest omsorgspengerInntektsmeldingPdfRequest, int inntektsmeldingId) {
        try {
            byte[] pdf = pdfgenKlient.genererPdfOmsorgspengerInntektsmelding(omsorgspengerInntektsmeldingPdfRequest);
            LOG.info("Pdf av inntektsmelding omsorgspenger med id {} ble generert.", inntektsmeldingId);
            return pdf;
        } catch (Exception e) {
            var anonymPdfRequest = omsorgspengerInntektsmeldingPdfRequest.anonymiser();
            throw new TekniskException("K9INNTEKTSMELDING_1",
                String.format("Klarte ikke å generere pdf for inntektsmelding omsorgspenger med id %s", inntektsmeldingId), e);
        }
    }

    private byte[] genererPdfForInntektsmelding(InntektsmeldingPdfRequest inntektsmeldingPdfRequest, int inntektsmeldingId) {
        try {
            byte[] pdf = pdfgenKlient.genererPdfInntektsmelding(inntektsmeldingPdfRequest);
            LOG.info("Pdf av inntektsmelding med id {} ble generert.", inntektsmeldingId);
            return pdf;
        } catch (Exception e) {
            var anonymPdfRequest = inntektsmeldingPdfRequest.anonymiser();
            throw new TekniskException("K9INNTEKTSMELDING_1",
                String.format("Klarte ikke å generere pdf for inntektsmelding med id %s", inntektsmeldingId), e);
        }
    }

    private byte[] genererPdfForRefusjonskravNyansatt(RefusjonskravNyansattData refusjonskravNyansattPdfData, int inntektsmeldingId) {
        try {
            byte[] pdf = pdfgenKlient.genererPdfRefusjonskravNyansatt(refusjonskravNyansattPdfData);
            LOG.info("Pdf av refusjonskrav for nyansatt med id {} ble generert.", inntektsmeldingId);
            return pdf;
        } catch (Exception e) {
            var anonymPdfRequest = refusjonskravNyansattPdfData.anonymiser();
            throw new TekniskException("K9INNTEKTSMELDING_1",
                String.format("Klarte ikke å generere pdf for refusjonskrav for nyansatt med id %s", inntektsmeldingId), e);
        }
    }

    private String finnArbeidsgiverNavn(String arbeidsgvierIdent) {
        String arbeidsgiverNavn;
        if (!OrganisasjonsnummerValidator.erGyldig(arbeidsgvierIdent)) {
            var personIdent = new PersonIdent(arbeidsgvierIdent);
            arbeidsgiverNavn = personTjeneste.hentPersonFraIdent(personIdent).mapNavn();
        } else {
            arbeidsgiverNavn = organisasjonTjeneste.finnOrganisasjon(arbeidsgvierIdent).navn();
        }
        return arbeidsgiverNavn;
    }
}
