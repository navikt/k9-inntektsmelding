package no.nav.familie.inntektsmelding.integrasjoner.altinn.dialogporten;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselTekster;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselTjeneste;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.LukkeÅrsak;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.dto.ArbeidsgiverDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.foreldrepenger.konfig.Environment;

@ApplicationScoped
public class DialogportenTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(DialogportenTjeneste.class);
    private static final Environment ENV = Environment.current();
    private static final LocalDateTime DIALOGPORTEN_PRODSETTING_TIDSPUNKT = LocalDateTime.parse(ENV.getProperty("dialogporten.prodsetting.tidspunkt", "2026-09-08T13:23:51.192"));

    private DialogportenKlient dialogportenKlient;
    private ForespørselTjeneste forespørselTjeneste;
    private PersonTjeneste personTjeneste;
    private boolean ignorerManglendeDialog;

    DialogportenTjeneste() {
        // CDI
    }

    @Inject
    public DialogportenTjeneste(DialogportenKlient dialogportenKlient,
                                ForespørselTjeneste forespørselTjeneste,
                                PersonTjeneste personTjeneste) {
        this(dialogportenKlient, forespørselTjeneste, personTjeneste, ENV.getProperty("dialogporten.ignorer.ukjent.aktoer", boolean.class, false));
    }

    DialogportenTjeneste(DialogportenKlient dialogportenKlient,
                         ForespørselTjeneste forespørselTjeneste,
                         PersonTjeneste personTjeneste,
                         boolean ignorerManglendeDialog) {
        this.dialogportenKlient = dialogportenKlient;
        this.forespørselTjeneste = forespørselTjeneste;
        this.personTjeneste = personTjeneste;
        this.ignorerManglendeDialog = ignorerManglendeDialog;
    }

    public void opprettForespørselDialogporten(UUID forespørselUuid,
                                               ArbeidsgiverDto arbeidsgiver,
                                               AktørIdEntitet aktørId,
                                               Ytelsetype ytelsetype,
                                               LocalDate førsteUttaksdato) {
        String saksTittelDialog = lagSaksTittelForDialogporten(aktørId);
        Optional<String> dialogPortenUuid = dialogportenKlient.opprettDialog(forespørselUuid, arbeidsgiver, saksTittelDialog, førsteUttaksdato, ytelsetype);

        if (dialogPortenUuid.isEmpty()) {
            LOG.warn("Kun håndterte feil vil gi en tom optional for forespørsel med uuid: {}, dialogportenUuid: {}", forespørselUuid, dialogPortenUuid);
            return;
        }

        String vasketDialogUuid = dialogPortenUuid.get().replace("\"", "");
        LOG.info("Mottok UUID {} fra dialogporten", vasketDialogUuid);
        forespørselTjeneste.setDialogportenUuid(forespørselUuid, UUID.fromString(vasketDialogUuid));
    }

    public void ferdigstillDialog(ForespørselEntitet forespørsel,
                                  Optional<UUID> inntektsmeldingUuid,
                                  LukkeÅrsak lukkeÅrsak) {
        if (!harDialogportenUuid(forespørsel)) {
            return;
        }

        String sakstittel = lagSaksTittelForDialogporten(forespørsel.getAktørId());

        dialogportenKlient.ferdigstillDialog(
            forespørsel.getDialogportenUuid().get(),
            new ArbeidsgiverDto(forespørsel.getOrganisasjonsnummer()),
            sakstittel,
            forespørsel.getYtelseType(),
            forespørsel.getSkjæringstidspunkt(),
            inntektsmeldingUuid,
            lukkeÅrsak);
    }

    public void oppdaterDialogMedEndretInntektsmelding(ForespørselEntitet forespørsel,
                                                       Optional<UUID> inntektsmeldingUuid) {
        if (!harDialogportenUuid(forespørsel)) {
            return;
        }
        dialogportenKlient.oppdaterDialogMedEndretInntektsmelding(
            forespørsel.getDialogportenUuid().get(),
            new ArbeidsgiverDto(forespørsel.getOrganisasjonsnummer()),
            inntektsmeldingUuid
        );
    }

    public void settDialogTilUtgått(ForespørselEntitet forespørsel) {
        if (!harDialogportenUuid(forespørsel)) {
            return;
        }
        String saksTittel = lagSaksTittelForDialogporten(forespørsel.getAktørId());
        dialogportenKlient.settDialogTilUtgått(forespørsel.getDialogportenUuid().get(), saksTittel);
    }

    public void sendMeldingOmAvvistInntektsmelding(ForespørselEntitet forespørsel,
                                                   String avvistTekst) {
        if (!harDialogportenUuid(forespørsel)) {
            return;
        }
        dialogportenKlient.sendMeldingOmAvvistInntektsmelding(forespørsel, avvistTekst);
    }

    private boolean harDialogportenUuid(ForespørselEntitet forespørsel) {
        if (forespørsel.getDialogportenUuid().isPresent()) {
            return true;
        }
        if (ignorerManglendeDialog) {
            LOG.info("Forespørsel med uuid {} har ikke dialogportenUuid, sannsynligvis pga. ukjent aktør i dev. Hopper over kall til dialogporten.", forespørsel.getUuid());
            return false;
        }
        throw new IllegalStateException("Forespørsel med uuid " + forespørsel.getUuid() + " har ikke dialogportenUuid satt");
    }

    public boolean erOpprettetFørProdsetting(ForespørselEntitet forespørsel) {
        return forespørsel.getOpprettetTidspunkt().isBefore(DIALOGPORTEN_PRODSETTING_TIDSPUNKT);
    }

    private String lagSaksTittelForDialogporten(AktørIdEntitet aktørId) {
        var person = personTjeneste.hentPersonInfoFraAktørId(aktørId);
        return ForespørselTekster.lagSaksTittelInntektsmelding(person.mapFulltNavn(), person.fødselsdato());
    }
}
