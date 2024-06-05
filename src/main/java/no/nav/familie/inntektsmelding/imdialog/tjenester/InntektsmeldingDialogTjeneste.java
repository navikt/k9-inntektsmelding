package no.nav.familie.inntektsmelding.imdialog.tjenester;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingRepository;
import no.nav.familie.inntektsmelding.imdialog.rest.SendInntektsmeldingRequestDto;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;

@ApplicationScoped
public class InntektsmeldingDialogTjeneste {
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;
    private InntektsmeldingRepository inntektsmeldingRepository;

    InntektsmeldingDialogTjeneste() {
    }

    @Inject
    public InntektsmeldingDialogTjeneste(ForespørselBehandlingTjeneste forespørselBehandlingTjeneste,
                                         InntektsmeldingRepository inntektsmeldingRepository) {
        this.forespørselBehandlingTjeneste = forespørselBehandlingTjeneste;
        this.inntektsmeldingRepository = inntektsmeldingRepository;
    }

    public void mottaInntektsmelding(SendInntektsmeldingRequestDto mottattInntektsmeldingDto) {
        var foresporselUuid = UUID.fromString(mottattInntektsmeldingDto.foresporselUuid());
        var aktorId = mottattInntektsmeldingDto.aktorId();
        var orgnummer = new OrganisasjonsnummerDto(mottattInntektsmeldingDto.arbeidsgiverIdent().ident());
        var entitet = InntektsmeldingMapper.mapTilEntitet(mottattInntektsmeldingDto);
        inntektsmeldingRepository.lagreInntektsmelding(entitet);

        forespørselBehandlingTjeneste.ferdigstillForespørsel(foresporselUuid, aktorId, orgnummer, mottattInntektsmeldingDto.startdato());
        // TODO: lagre i database og opprette prosesstask for å lagre i joark
    }
}
