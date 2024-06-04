package no.nav.familie.inntektsmelding.imdialog.tjenester;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.imdialog.tjenester.InntektsmeldingMapper;
import no.nav.familie.inntektsmelding.typer.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselTjeneste;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingRepository;
import no.nav.familie.inntektsmelding.imdialog.rest.SendInntektsmeldingRequestDto;
import no.nav.familie.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjon;

@ApplicationScoped
public class InntektsmeldingDialogTjeneste {
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;

public class InntektsmeldingDialogTjeneste {
    private ArbeidsgiverNotifikasjon arbeidsgiverNotifikasjon;
    private ForespørselTjeneste forespørselTjeneste;
    private InntektsmeldingRepository inntektsmeldingRepository;

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
        var entitet = InntektsmeldingMapper.mapTilEntitet(sendInntektsmeldingRequestDto);
        inntektsmeldingRepository.lagreInntektsmelding(entitet);

        forespørselBehandlingTjeneste.ferdigstillForespørsel(foresporselUuid, aktorId, orgnummer, body.startdato());
        // TODO: lagre i database og opprette prosesstask for å lagre i joark
    }
}
