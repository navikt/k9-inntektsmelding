package no.nav.familie.inntektsmelding.imdialog.tjenester;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselTjeneste;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingRepository;
import no.nav.familie.inntektsmelding.imdialog.rest.SendInntektsmeldingRequestDto;
import no.nav.familie.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjon;

@ApplicationScoped
public class InntektsmeldingDialogTjeneste {
    private ArbeidsgiverNotifikasjon arbeidsgiverNotifikasjon;
    private ForespørselTjeneste forespørselTjeneste;
    private InntektsmeldingRepository inntektsmeldingRepository;

    InntektsmeldingDialogTjeneste() {
    }

    @Inject
    public InntektsmeldingDialogTjeneste(ArbeidsgiverNotifikasjon arbeidsgiverNotifikasjon, ForespørselTjeneste forespørselTjeneste, InntektsmeldingRepository inntektsmeldingRepository) {
        this.arbeidsgiverNotifikasjon = arbeidsgiverNotifikasjon;
        this.forespørselTjeneste = forespørselTjeneste;
        this.inntektsmeldingRepository = inntektsmeldingRepository;
    }

    public void mottaInntektsmelding(SendInntektsmeldingRequestDto sendInntektsmeldingRequestDto) {
        //Todo lagre i database og opprette prosesstask for å lagre i joark


        var forespørsel = forespørselTjeneste.finnForespørsel(UUID.fromString(sendInntektsmeldingRequestDto.foresporselUuid()))
            .orElseThrow(() -> new IllegalStateException("Finnes ikke forespørsel for inntektsmelding, ugyldig tilstand"));
        valider(forespørsel, sendInntektsmeldingRequestDto);
        var entitet = InntektsmeldingMapper.mapTilEntitet(sendInntektsmeldingRequestDto);
        inntektsmeldingRepository.lagreInntektsmelding(entitet);
        arbeidsgiverNotifikasjon.lukkOppgave(forespørsel.getOppgaveId(), OffsetDateTime.now());
        ferdigstillSak(forespørsel);
    }

    private void ferdigstillSak(ForespørselEntitet forespørsel) {
        arbeidsgiverNotifikasjon.ferdigstillSak(forespørsel.getSakId()); // Oppdaterer status i arbeidsgiver-notifikasjon
        forespørselTjeneste.ferdigstillSak(forespørsel.getSakId()); // Oppdaterer status i forespørsel
    }

    private void valider(ForespørselEntitet forespørsel, SendInntektsmeldingRequestDto sendInntektsmeldingRequestDto) {
        if (!forespørsel.getBrukerAktørId().equals(sendInntektsmeldingRequestDto.aktorId().id())) {
            throw new IllegalStateException("AktørId for bruker var ikke like");
        }
        if (!forespørsel.getOrganisasjonsnummer().equals(sendInntektsmeldingRequestDto.arbeidsgiverIdent().ident())) {
            throw new IllegalStateException("Organisasjonsnummer var ikke like");
        }
        if (!forespørsel.getSkjæringstidspunkt().equals(sendInntektsmeldingRequestDto.startdato())) {
            throw new IllegalStateException("Startdato var ikke like");
        }
    }
}
