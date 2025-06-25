package no.nav.familie.inntektsmelding.integrasjoner.joark;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.integrasjoner.organisasjon.OrganisasjonTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.koder.Behandlingtema;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.vedtak.felles.integrasjon.dokarkiv.DokArkiv;
import no.nav.vedtak.felles.integrasjon.dokarkiv.dto.AvsenderMottaker;
import no.nav.vedtak.felles.integrasjon.dokarkiv.dto.Bruker;
import no.nav.vedtak.felles.integrasjon.dokarkiv.dto.DokumentInfoOpprett;
import no.nav.vedtak.felles.integrasjon.dokarkiv.dto.Dokumentvariant;
import no.nav.vedtak.felles.integrasjon.dokarkiv.dto.OpprettJournalpostRequest;
import no.nav.vedtak.felles.integrasjon.dokarkiv.dto.Sak;

@ApplicationScoped
public class JoarkTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(JoarkTjeneste.class);
    // Ved maskinell journalføring skal enhet være satt til 9999. Se https://confluence.adeo.no/display/BOA/opprettJournalpost
    private static final String JOURNALFØRENDE_ENHET = "9999";
    private static final String JOURNALFØRING_TITTEL = "Inntektsmelding";
    // TODO Denne bør nok synkes med avsendersystem i XML
    private static final String KANAL = "NAV_NO";
    // TODO Dette er brevkode for altinn skjema. Trenger vi egen?
    private static final String BREVKODE_IM = "4936";
    private static final String TEMA_K9 = "OMS";

    private DokArkiv joarkKlient;
    private OrganisasjonTjeneste organisasjonTjeneste;
    private PersonTjeneste personTjeneste;

    JoarkTjeneste() {
        // CDI proxy
    }

    @Inject
    public JoarkTjeneste(JoarkKlient joarkKlient, OrganisasjonTjeneste organisasjonTjeneste, PersonTjeneste personTjeneste) {
        this.joarkKlient = joarkKlient;
        this.organisasjonTjeneste = organisasjonTjeneste;
        this.personTjeneste = personTjeneste;
    }


    public String journalførInntektsmelding(String XMLAvInntektsmelding,
                                            InntektsmeldingEntitet inntektsmelding,
                                            byte[] pdf,
                                            String saksnummer) {
        var request = opprettRequest(XMLAvInntektsmelding, inntektsmelding, pdf, saksnummer);
        try {
            var response = joarkKlient.opprettJournalpost(request, false);
            // Kan nok fjerne loggingen etter en periode i dev, mest for feilsøking i starten.
            LOG.info("Journalført inntektsmelding fikk journalpostId {}", response.journalpostId());
            return response.journalpostId();
        } catch (Exception e) {
            throw new IllegalStateException("Klarte ikke journalføre innteketsmelding " + e);
        }
    }

    private OpprettJournalpostRequest opprettRequest(String xmlAvInntektsmelding,
                                                     InntektsmeldingEntitet inntektsmeldingEntitet,
                                                     byte[] pdf,
                                                     String saksnummer) {
        var erBedrift = inntektsmeldingEntitet.getArbeidsgiverIdent().length() == 9;
        var avsenderMottaker = erBedrift ? lagAvsenderBedrift(inntektsmeldingEntitet) : lagAvsenderPrivatperson(inntektsmeldingEntitet);
        var opprettJournalpostRequestBuilder = OpprettJournalpostRequest.nyInngående()
            .medTittel(JOURNALFØRING_TITTEL)
            .medAvsenderMottaker(avsenderMottaker)
            .medBruker(lagBruker(inntektsmeldingEntitet.getAktørId()))
            .medBehandlingstema(mapBehandlingTema(inntektsmeldingEntitet.getYtelsetype()))
            .medDatoMottatt(inntektsmeldingEntitet.getOpprettetTidspunkt().toLocalDate())
            .medTema(TEMA_K9)
            .medEksternReferanseId(UUID.randomUUID().toString())
            .medJournalfoerendeEnhet(JOURNALFØRENDE_ENHET)
            .medKanal(KANAL)
            .medDokumenter(lagDokumenter(xmlAvInntektsmelding, pdf));

        if (saksnummer != null) {
            opprettJournalpostRequestBuilder.medSak(sak(saksnummer));
        }
        return opprettJournalpostRequestBuilder.build();
    }

    private List<DokumentInfoOpprett> lagDokumenter(String xmlAvInntektsmelding, byte[] pdf) {
        var dokumentXML = new Dokumentvariant(Dokumentvariant.Variantformat.ORIGINAL, Dokumentvariant.Filtype.XML,
            xmlAvInntektsmelding.getBytes(StandardCharsets.UTF_8));

        var dokumentPDF = new Dokumentvariant(Dokumentvariant.Variantformat.ARKIV, Dokumentvariant.Filtype.PDF, pdf);

        var builder = DokumentInfoOpprett.builder()
            .medTittel(JOURNALFØRING_TITTEL)
            .medBrevkode(BREVKODE_IM)
            .leggTilDokumentvariant(dokumentXML)
            .leggTilDokumentvariant(dokumentPDF);

        return Collections.singletonList(builder.build());
    }

    private Sak sak(String saksnummer) {
        return new Sak(saksnummer, Fagsystem.K9SAK.getOffisiellKode(), Sak.Sakstype.FAGSAK);
    }

    private String mapBehandlingTema(Ytelsetype ytelsetype) {
        return switch (ytelsetype) {
            case PLEIEPENGER_SYKT_BARN -> Behandlingtema.PLEIEPENGER_SYKT_BARN.getOffisiellKode();
            case PLEIEPENGER_NÆRSTÅENDE -> Behandlingtema.PLEIEPENGER_LIVETS_SLUTTFASE.getOffisiellKode();
            case OMSORGSPENGER -> Behandlingtema.OMSORGSPENGER.getOffisiellKode();
            case OPPLÆRINGSPENGER -> Behandlingtema.OPPLÆRINGSPENGER.getOffisiellKode();
        };
    }

    private Bruker lagBruker(AktørIdEntitet aktørId) {
        return new Bruker(aktørId.getAktørId(), Bruker.BrukerIdType.AKTOERID);
    }

    private AvsenderMottaker lagAvsenderPrivatperson(InntektsmeldingEntitet inntektsmeldingEntitet) {
        var personInfo = personTjeneste.hentPersonInfoFraAktørId(new AktørIdEntitet(inntektsmeldingEntitet.getArbeidsgiverIdent())
        );
        return new AvsenderMottaker(personInfo.fødselsnummer().getIdent(), AvsenderMottaker.AvsenderMottakerIdType.FNR, personInfo.mapNavn());
    }

    private AvsenderMottaker lagAvsenderBedrift(InntektsmeldingEntitet inntektsmeldingEntitet) {
        var org = organisasjonTjeneste.finnOrganisasjon(inntektsmeldingEntitet.getArbeidsgiverIdent());
        return new AvsenderMottaker(org.orgnr(), AvsenderMottaker.AvsenderMottakerIdType.ORGNR, org.navn());
    }


}
