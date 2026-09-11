package no.nav.familie.inntektsmelding.integrasjoner.altinn.dialogporten;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselTekster;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.dto.ArbeidsgiverDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;

@ApplicationScoped
public class DialogportenTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(DialogportenTjeneste.class);

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

    private String lagSaksTittelForDialogporten(AktørIdEntitet aktørId) {
        var person = personTjeneste.hentPersonInfoFraAktørId(aktørId);
        return ForespørselTekster.lagSaksTittelInntektsmelding(person.mapFulltNavn(), person.fødselsdato());
    }
}
