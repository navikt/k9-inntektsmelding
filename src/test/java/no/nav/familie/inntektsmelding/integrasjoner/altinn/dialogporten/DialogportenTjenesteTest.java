package no.nav.familie.inntektsmelding.integrasjoner.altinn.dialogporten;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselMapper;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselTjeneste;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.LukkeÅrsak;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.koder.ForespørselType;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;

class DialogportenTjenesteTest {

    private static final String AKTØR_ID = "1234567890134";

    private final DialogportenKlient klient = Mockito.mock(DialogportenKlient.class);
    private final ForespørselTjeneste forespørselTjeneste = Mockito.mock(ForespørselTjeneste.class);
    private final PersonTjeneste personTjeneste = Mockito.mock(PersonTjeneste.class);

    private final ForespørselEntitet forespørselUtenDialog = ForespørselMapper.mapForespørsel("974760673", LocalDate.now(), AKTØR_ID,
        Ytelsetype.PLEIEPENGER_SYKT_BARN, "saksnummer", ForespørselType.BESTILT_AV_FAGSYSTEM, LocalDate.now(), null);

    @Test
    void skal_hoppe_over_kall_når_dialog_mangler_og_manglende_dialog_ignoreres() {
        var tjeneste = new DialogportenTjeneste(klient, forespørselTjeneste, personTjeneste, true);

        tjeneste.ferdigstillDialog(forespørselUtenDialog, Optional.empty(), LukkeÅrsak.ORDINÆR_INNSENDING);
        tjeneste.oppdaterDialogMedEndretInntektsmelding(forespørselUtenDialog, Optional.empty());
        tjeneste.settDialogTilUtgått(forespørselUtenDialog);
        tjeneste.sendMeldingOmAvvistInntektsmelding(forespørselUtenDialog, "avvist");

        verifyNoInteractions(klient);
    }

    @Test
    void skal_feile_når_dialog_mangler_og_manglende_dialog_ikke_ignoreres() {
        var tjeneste = new DialogportenTjeneste(klient, forespørselTjeneste, personTjeneste, false);

        assertThatThrownBy(() -> tjeneste.ferdigstillDialog(forespørselUtenDialog, Optional.empty(), LukkeÅrsak.ORDINÆR_INNSENDING))
            .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> tjeneste.oppdaterDialogMedEndretInntektsmelding(forespørselUtenDialog, Optional.empty()))
            .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> tjeneste.settDialogTilUtgått(forespørselUtenDialog))
            .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> tjeneste.sendMeldingOmAvvistInntektsmelding(forespørselUtenDialog, "avvist"))
            .isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(klient);
    }

    @Test
    void skal_kalle_dialogporten_når_dialog_finnes() {
        var tjeneste = new DialogportenTjeneste(klient, forespørselTjeneste, personTjeneste, true);
        var forespørselMedDialog = Mockito.mock(ForespørselEntitet.class);
        when(forespørselMedDialog.getDialogportenUuid()).thenReturn(Optional.of(UUID.randomUUID()));
        var aktørId = new AktørIdEntitet(AKTØR_ID);
        when(forespørselMedDialog.getAktørId()).thenReturn(aktørId);
        when(personTjeneste.hentPersonInfoFraAktørId(any())).thenReturn(
            new PersonInfo("Fornavn", null, "Etternavn", new PersonIdent("12345678910"), aktørId, LocalDate.now(), null, null));

        tjeneste.settDialogTilUtgått(forespørselMedDialog);

        verify(klient).settDialogTilUtgått(any(), any());
    }
}
