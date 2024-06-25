package no.nav.familie.inntektsmelding.integrasjoner.dokgen;

import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.integrasjoner.organisasjon.OrganisasjonTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.typer.OrganisasjonsnummerValidator;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;

@ApplicationScoped
public class FpDokgenTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(FpDokgenTjeneste.class);
    private static final Logger SECURE_LOG = LoggerFactory.getLogger("secureLogger");
    private FpDokgenKlient fpDokgenKlient;
    private PersonTjeneste personTjeneste;
    private OrganisasjonTjeneste organisasjonTjeneste;

    FpDokgenTjeneste() {
        //CDI
    }

    @Inject
    public FpDokgenTjeneste(FpDokgenKlient fpDokgenKlient,
                            PersonTjeneste personTjeneste,
                            OrganisasjonTjeneste organisasjonTjeneste) {
        this.fpDokgenKlient = fpDokgenKlient;
        this.personTjeneste = personTjeneste;
        this.organisasjonTjeneste = organisasjonTjeneste;
    }

    public byte[] mapDataOgGenererPdf(InntektsmeldingEntitet inntektsmelding, int inntektsmeldingId) {
        PersonInfo personInfo;
        String arbeidsgiverNavn;
        var arbeidsgvierIdent = inntektsmelding.getArbeidsgiverIdent();

        if (Environment.current().isLocal()) {
            personInfo = new PersonInfo("Test", "Tester", "Testesen", new PersonIdent("16097545298"), inntektsmelding.getAktørId(), LocalDate.now());
            arbeidsgiverNavn = "Arbeidsgvier 1";
        } else {
            personInfo = personTjeneste.hentPersonInfoFraAktørId(inntektsmelding.getAktørId(), inntektsmelding.getYtelsetype());
            arbeidsgiverNavn = finnArbeidsgiverNavn(inntektsmelding, arbeidsgvierIdent);
        }

        var imDokumentdata = InntektsmeldingPdfDataMapper.mapInntektsmeldingData(inntektsmelding, arbeidsgiverNavn, personInfo, arbeidsgvierIdent);

        return genererPdf(imDokumentdata, inntektsmeldingId);
    }

    private byte[] genererPdf(InntektsmeldingPdfData imDokumentData, int inntektsmeldingId) {
        byte[] pdf;
        try {
            pdf = fpDokgenKlient.genererPdf(imDokumentData);
        } catch (Exception e) {
            imDokumentData.anonymiser();
            SECURE_LOG.warn("Klarte ikke å generere pdf av inntektsmelding: {}", DefaultJsonMapper.toJson(imDokumentData));
            throw new TekniskException("FPINNTEKTSMELDING_1",
                String.format("Klarte ikke å generere pdf for inntektsmelding med id %s", inntektsmeldingId), e);
        }
        LOG.info("Pdf av inntektsmelding med id {} ble generert.", inntektsmeldingId);
        return pdf;
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
