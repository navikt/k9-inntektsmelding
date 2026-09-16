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
    private static final LocalDateTime DIALOGPORTEN_PRODSETTINGSDATO = LocalDateTime.parse(ENV.getProperty("dialogporten.prodsettingsdato", "2026-09-08T13:23:51.192"));

    private DialogportenKlient dialogportenKlient;
    private ForespørselTjeneste forespørselTjeneste;
    private PersonTjeneste personTjeneste;

    DialogportenTjeneste() {
        // CDI
    }

    @Inject
    public DialogportenTjeneste(DialogportenKlient dialogportenKlient,
                                ForespørselTjeneste forespørselTjeneste,
                                PersonTjeneste personTjeneste) {
        this.dialogportenKlient = dialogportenKlient;
        this.forespørselTjeneste = forespørselTjeneste;
        this.personTjeneste = personTjeneste;
    }

    public void opprettForespørselDialogporten(UUID forespørselUuid,
                                               ArbeidsgiverDto arbeidsgiver,
                                               AktørIdEntitet aktørId,
                                               Ytelsetype ytelsetype,
                                               LocalDate førsteUttaksdato) {
        String saksTittelDialog = lagSaksTittelForDialogporten(aktørId);
        String dialogPortenUuid = dialogportenKlient.opprettDialog(forespørselUuid, arbeidsgiver, saksTittelDialog, førsteUttaksdato, ytelsetype);

        String vasketDialogUuid = dialogPortenUuid.replace("\"", "");
        LOG.info("Mottok UUID {} fra dialogporten", vasketDialogUuid);
        forespørselTjeneste.setDialogportenUuid(forespørselUuid, UUID.fromString(vasketDialogUuid));
    }

    public void ferdigstillDialog(ForespørselEntitet forespørsel,
                                  Optional<UUID> inntektsmeldingUuid,
                                  LukkeÅrsak lukkeÅrsak) {
        if (forespørsel.getDialogportenUuid().isEmpty()) {
            throw new IllegalStateException("Forespørsel med uuid " + forespørsel.getUuid() + " har ikke dialogportenUuid satt");
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
        if (forespørsel.getDialogportenUuid().isEmpty()) {
            throw new IllegalStateException("Forespørsel med uuid " + forespørsel.getUuid() + " har ikke dialogportenUuid satt");
        }
        dialogportenKlient.oppdaterDialogMedEndretInntektsmelding(
            forespørsel.getDialogportenUuid().get(),
            new ArbeidsgiverDto(forespørsel.getOrganisasjonsnummer()),
            inntektsmeldingUuid
        );
    }

    public void settDialogTilUtgått(ForespørselEntitet forespørsel) {
        if (forespørsel.getDialogportenUuid().isEmpty()) {
            throw new IllegalStateException("Forespørsel med uuid " + forespørsel.getUuid() + " har ikke dialogportenUuid satt");
        }
        String saksTittel = lagSaksTittelForDialogporten(forespørsel.getAktørId());
        dialogportenKlient.settDialogTilUtgått(forespørsel.getDialogportenUuid().get(), saksTittel);
    }

    public void sendMeldingOmAvvistInntektsmelding(ForespørselEntitet forespørsel,
                                                   String avvistTekst) {
        if (forespørsel.getDialogportenUuid().isEmpty()) {
            throw new IllegalStateException("Forespørsel med uuid " + forespørsel.getUuid() + " har ikke dialogportenUuid satt");
        }
        dialogportenKlient.sendMeldingOmAvvistInntektsmelding(forespørsel, avvistTekst);
    }

    public boolean erOpprettetFørProdsetting(ForespørselEntitet forespørsel) {
        return forespørsel.getOpprettetTidspunkt().isBefore(DIALOGPORTEN_PRODSETTINGSDATO);
    }

    private String lagSaksTittelForDialogporten(AktørIdEntitet aktørId) {
        var person = personTjeneste.hentPersonInfoFraAktørId(aktørId);
        return ForespørselTekster.lagSaksTittelInntektsmelding(person.mapFulltNavn(), person.fødselsdato());
    }
}
