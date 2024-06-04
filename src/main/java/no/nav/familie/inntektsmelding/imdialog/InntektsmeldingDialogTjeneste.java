package no.nav.familie.inntektsmelding.imdialog;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.typer.OrganisasjonsnummerDto;

@ApplicationScoped
public class InntektsmeldingDialogTjeneste {
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;


    InntektsmeldingDialogTjeneste() {
    }

    @Inject
    public InntektsmeldingDialogTjeneste(ForespørselBehandlingTjeneste forespørselBehandlingTjeneste) {
        this.forespørselBehandlingTjeneste = forespørselBehandlingTjeneste;
    }

    public void mottaInntektsmelding(SendInntektsmeldingRequestDto body) {
        var foresporselUuid = UUID.fromString(body.foresporselUuid());
        var aktorId = body.aktorId();
        var orgnummer = new OrganisasjonsnummerDto(body.arbeidsgiverIdent().ident());

        forespørselBehandlingTjeneste.ferdigstillForespørsel(foresporselUuid, aktorId, orgnummer, body.startdato());
        // TODO: lagre i database og opprette prosesstask for å lagre i joark
    }
}
