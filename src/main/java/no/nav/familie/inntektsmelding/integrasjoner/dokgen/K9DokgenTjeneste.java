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

        personInfo = personTjeneste.hentPersonInfoFraAktørId(inntektsmelding.getAktørId());
        arbeidsgiverNavn = finnArbeidsgiverNavn(arbeidsgvierIdent);

        if (inntektsmelding.getYtelsetype() == Ytelsetype.OMSORGSPENGER) {
            if (inntektsmelding.getMånedRefusjon() != null) {
                // lag pdf for refusjonskrav omsorgspenger
                var omsorgspengerRefusjonPdfData = OmsorgspengerRefusjonPdfDataMapper.mapOmsorgspengerRefusjonData(inntektsmelding, arbeidsgiverNavn, personInfo, arbeidsgvierIdent);
                return genererPdfForOmsorgspengerRefusjon(omsorgspengerRefusjonPdfData, inntektsmeldingsid);
            } else {
                // lag pdf for inntektsmelding omsorgspenger
                var omsorgspengerInntektsmeldingPdfData = OmsorgspengerInntektsmeldingPdfDataMapper.mapOmsorgspengerInntektsmeldingData(inntektsmelding, arbeidsgiverNavn, personInfo, arbeidsgvierIdent);
                return genererPdfForOmsorgspengerInntektsmelding(omsorgspengerInntektsmeldingPdfData, inntektsmeldingsid);
            }
        }

        var imDokumentdata = InntektsmeldingPdfDataMapper.mapInntektsmeldingData(inntektsmelding, arbeidsgiverNavn, personInfo, arbeidsgvierIdent);
        return genererPdfForInntektsmelding(imDokumentdata, inntektsmeldingsid);
    }

    private byte[] genererPdfForOmsorgspengerRefusjon(OmsorgspengerRefusjonPdfData omsorgspengerRefusjonPdfData, int inntektsmeldingId) {
        try {
            byte[] pdf = k9DokgenKlient.genererPdfOmsorgspengerRefusjon(omsorgspengerRefusjonPdfData);
            LOG.info("Pdf av refusjonskrav omsorgspenger med id {} ble generert.", inntektsmeldingId);
            return pdf;
        } catch (Exception e) {
            omsorgspengerRefusjonPdfData.anonymiser();
            SECURE_LOG.warn("Klarte ikke å generere pdf av refusjonskrav omsorgspenger: {}", DefaultJsonMapper.toJson(omsorgspengerRefusjonPdfData));
            throw new TekniskException("K9INNTEKTSMELDING_1",
                String.format("Klarte ikke å generere pdf for refusjonskrav omsorgspenger med id %s", inntektsmeldingId), e);
        }
    }

    private byte[] genererPdfForOmsorgspengerInntektsmelding(OmsorgspengerInntektsmeldingPdfData omsorgspengerInntektsmeldingPdfData, int inntektsmeldingId) {
        try {
            byte[] pdf = k9DokgenKlient.genererPdfOmsorgspengerInntektsmelding(omsorgspengerInntektsmeldingPdfData);
            LOG.info("Pdf av inntektsmelding omsorgspenger med id {} ble generert.", inntektsmeldingId);
            return pdf;
        } catch (Exception e) {
            omsorgspengerInntektsmeldingPdfData.anonymiser();
            SECURE_LOG.warn("Klarte ikke å generere pdf av inntektsmelding omsorgspenger: {}", DefaultJsonMapper.toJson(omsorgspengerInntektsmeldingPdfData));
            throw new TekniskException("K9INNTEKTSMELDING_1",
                String.format("Klarte ikke å generere pdf for inntektsmelding omsorgspenger med id %s", inntektsmeldingId), e);
        }
    }

    private byte[] genererPdfForInntektsmelding(InntektsmeldingPdfData inntektsmeldingPdfData, int inntektsmeldingId) {
        try {
            byte[] pdf = k9DokgenKlient.genererPdfInntektsmelding(inntektsmeldingPdfData);
            LOG.info("Pdf av inntektsmelding med id {} ble generert.", inntektsmeldingId);
            return pdf;
        } catch (Exception e) {
            inntektsmeldingPdfData.anonymiser();
            SECURE_LOG.warn("Klarte ikke å generere pdf av inntektsmelding: {}", DefaultJsonMapper.toJson(inntektsmeldingPdfData));
            throw new TekniskException("K9INNTEKTSMELDING_1",
                String.format("Klarte ikke å generere pdf for inntektsmelding med id %s", inntektsmeldingId), e);
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
