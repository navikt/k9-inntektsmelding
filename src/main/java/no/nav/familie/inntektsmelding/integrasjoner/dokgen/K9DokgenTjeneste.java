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
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.OrganisasjonsnummerValidator;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;

@ApplicationScoped
public class K9DokgenTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(K9DokgenTjeneste.class);
    private static final Logger SECURE_LOG = LoggerFactory.getLogger("secureLogger");
    private K9DokgenKlient k9DokgenKlient;
    private PersonTjeneste personTjeneste;
    private OrganisasjonTjeneste organisasjonTjeneste;

    K9DokgenTjeneste() {
        //CDI
    }

    @Inject
    public K9DokgenTjeneste(K9DokgenKlient k9DokgenKlient, PersonTjeneste personTjeneste, OrganisasjonTjeneste organisasjonTjeneste) {
        this.k9DokgenKlient = k9DokgenKlient;
        this.personTjeneste = personTjeneste;
        this.organisasjonTjeneste = organisasjonTjeneste;
    }

    public byte[] mapDataOgGenererPdf(InntektsmeldingEntitet inntektsmelding) {
        PersonInfo personInfo;
        String arbeidsgiverNavn;
        var arbeidsgvierIdent = inntektsmelding.getArbeidsgiverIdent();
        var inntektsmeldingsid = inntektsmelding.getId() != null ? inntektsmelding.getId().intValue() : 1;

        personInfo = personTjeneste.hentPersonInfoFraAktørId(inntektsmelding.getAktørId(), inntektsmelding.getYtelsetype());
        arbeidsgiverNavn = finnArbeidsgiverNavn(inntektsmelding, arbeidsgvierIdent);

        if (inntektsmelding.getYtelsetype() == Ytelsetype.OMSORGSPENGER) {
            var omsorgspengerRefusjonPdfData = OmsorgspengerRefusjonPdfDataMapper.mapOmsorgspengerRefusjonData(inntektsmelding, arbeidsgiverNavn, personInfo, arbeidsgvierIdent);
            return genererPdfForOmsorgspengerRefusjon(omsorgspengerRefusjonPdfData, inntektsmeldingsid);
        }

        var imDokumentdata = InntektsmeldingPdfDataMapper.mapInntektsmeldingData(inntektsmelding, arbeidsgiverNavn, personInfo, arbeidsgvierIdent);
        return genererPdf(imDokumentdata, inntektsmeldingsid);
    }

    private byte[] genererPdfForOmsorgspengerRefusjon(OmsorgspengerRefusjonPdfData imDokumentData, int inntektsmeldingId) {
        try {
            byte[] pdf = k9DokgenKlient.genererPdfOmsorgspengerRefusjon(imDokumentData);
            LOG.info("Pdf av refusjonskrav omsorgspenger med id {} ble generert.", inntektsmeldingId);
            return pdf;
        } catch (Exception e) {
            imDokumentData.anonymiser();
            SECURE_LOG.warn("Klarte ikke å generere pdf av refusjonskrav omsorgspenger: {}", DefaultJsonMapper.toJson(imDokumentData));
            throw new TekniskException("K9INNTEKTSMELDING_1",
                String.format("Klarte ikke å generere pdf for refusjonskrav omsorgspenger med id %s", inntektsmeldingId), e);
        }
    }

    private byte[] genererPdf(InntektsmeldingPdfData imDokumentData, int inntektsmeldingId) {
        try {
            byte[] pdf = k9DokgenKlient.genererPdf(imDokumentData);
            LOG.info("Pdf av inntektsmelding med id {} ble generert.", inntektsmeldingId);
            return pdf;
        } catch (Exception e) {
            imDokumentData.anonymiser();
            SECURE_LOG.warn("Klarte ikke å generere pdf av inntektsmelding: {}", DefaultJsonMapper.toJson(imDokumentData));
            throw new TekniskException("K9INNTEKTSMELDING_1",
                String.format("Klarte ikke å generere pdf for inntektsmelding med id %s", inntektsmeldingId), e);
        }
    }

    private String finnArbeidsgiverNavn(InntektsmeldingEntitet inntektsmelding, String arbeidsgvierIdent) {
        String arbeidsgiverNavn;
        if (!OrganisasjonsnummerValidator.erGyldig(arbeidsgvierIdent)) {
            var personIdent = new PersonIdent(arbeidsgvierIdent);
            arbeidsgiverNavn = personTjeneste.hentPersonFraIdent(personIdent, inntektsmelding.getYtelsetype()).mapNavn();
        } else {
            arbeidsgiverNavn = organisasjonTjeneste.finnOrganisasjon(arbeidsgvierIdent).navn();
        }
        return arbeidsgiverNavn;
    }
}
