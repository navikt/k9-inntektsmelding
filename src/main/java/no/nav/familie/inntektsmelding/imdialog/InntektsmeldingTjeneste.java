package no.nav.familie.inntektsmelding.imdialog;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.familie.inntektsmelding.database.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.database.tjenester.ForespørselTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjon;

@ApplicationScoped
public class InntektsmeldingTjeneste {
    private ArbeidsgiverNotifikasjon arbeidsgiverNotifikasjon;
    private ForespørselTjeneste forespørselTjeneste;

    public InntektsmeldingTjeneste() {
    }

    @Inject
    public InntektsmeldingTjeneste(ArbeidsgiverNotifikasjon arbeidsgiverNotifikasjon, ForespørselTjeneste forespørselTjeneste) {
        this.arbeidsgiverNotifikasjon = arbeidsgiverNotifikasjon;
        this.forespørselTjeneste = forespørselTjeneste;
    }

    public void mottaInntektsmelding(SendInntektsmeldingRequestDto sendInntektsmeldingRequestDto) {
        //Todo lagre i database og opprette prosesstask for å lagre i joark
        var forespørsel = forespørselTjeneste.finnForespørsel(UUID.fromString(sendInntektsmeldingRequestDto.foresporselUuid()))
            .orElseThrow(() -> new IllegalStateException("Finnes ikke forespørsel for inntektsmelding, ugyldig tilstand"));
        valider(forespørsel, sendInntektsmeldingRequestDto);
        arbeidsgiverNotifikasjon.lukkOppgave(forespørsel.getOppgaveId(), LocalDateTime.now());
    }

    private void valider(ForespørselEntitet forespørsel, SendInntektsmeldingRequestDto sendInntektsmeldingRequestDto) {
        if (!forespørsel.getBrukerAktørId().equals(sendInntektsmeldingRequestDto.aktorId().aktørId())) {
            throw new IllegalStateException("AktørId for bruker var ikke like");
        }
        if (!forespørsel.getOrganisasjonsnummer().equals(sendInntektsmeldingRequestDto.arbeidsgiverIdent())) {
            throw new IllegalStateException("Organisasjonsnummer var ikke like");
        }
        if (!forespørsel.getSkjæringstidspunkt().equals(sendInntektsmeldingRequestDto.startdato())) {
            throw new IllegalStateException("Startdato var ikke like");
        }
    }
}
