package no.nav.familie.inntektsmelding.imdialog;

import java.time.LocalDateTime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.transaction.Transactional;
import no.nav.familie.inntektsmelding.database.tjenester.ForespørselTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjon;

@ApplicationScoped
public class InntektsmeldingTjeneste {
    private ArbeidsgiverNotifikasjon arbeidsgiverNotifikasjon;
    private ForespørselTjeneste forespørselTjeneste;

    public InntektsmeldingTjeneste() {
    }

    @Inject
    public InntektsmeldingTjeneste(ArbeidsgiverNotifikasjon arbeidsgiverNotifikasjon,
                                   ForespørselTjeneste forespørselTjeneste) {
        this.arbeidsgiverNotifikasjon = arbeidsgiverNotifikasjon;
        this.forespørselTjeneste = forespørselTjeneste;
    }

    public void mottaInntektsmelding(SendInntektsmeldingRequestDto sendInntektsmeldingRequestDto) {
        //Todo lagre i database og opprette prosesstask for å lagre i joark
        var forespørsel = forespørselTjeneste.finnForespørsel(sendInntektsmeldingRequestDto.aktorId().aktørId(), sendInntektsmeldingRequestDto.arbeidsgiverIdent(), sendInntektsmeldingRequestDto.startdato());

        forespørsel.map(forespørselEntitet -> arbeidsgiverNotifikasjon.lukkOppgave(forespørselEntitet.getOppgaveId(), LocalDateTime.now()))
            .orElseThrow(() -> new IllegalStateException("Finnes ikke forespørsel for inntektsmelding, ugyldig tilstand"));
    }
}
