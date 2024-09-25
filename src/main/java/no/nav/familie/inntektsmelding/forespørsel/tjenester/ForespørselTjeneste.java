package no.nav.familie.inntektsmelding.forespørsel.tjenester;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselRepository;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.SaksnummerDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;

@ApplicationScoped
public class ForespørselTjeneste {

    private ForespørselRepository forespørselRepository;

    @Inject
    public ForespørselTjeneste(ForespørselRepository forespørselRepository) {
        this.forespørselRepository = forespørselRepository;
    }

    public ForespørselTjeneste() {
    }

    public UUID opprettForespørsel(LocalDate skjæringstidspunkt,
                                   Ytelsetype ytelseType,
                                   AktørIdEntitet brukerAktørId,
                                   OrganisasjonsnummerDto orgnr,
                                   SaksnummerDto fagsakSaksnummer) {
        return forespørselRepository.lagreForespørsel(skjæringstidspunkt, ytelseType, brukerAktørId.getAktørId(), orgnr.orgnr(),
            fagsakSaksnummer.saksnr());
    }

    public void setOppgaveId(UUID forespørselUUID, String oppgaveId) {
        forespørselRepository.oppdaterOppgaveId(forespørselUUID, oppgaveId);
    }


    public void setSakId(UUID forespørselUUID, String sakId) {
        forespørselRepository.oppdaterSakId(forespørselUUID, sakId);

    }

    public void ferdigstillForespørsel(String sakId) {
        forespørselRepository.ferdigstillForespørsel(sakId);
    }

    public void settSakTilUtgått(String sakId) {
        forespørselRepository.settSakTilUtgått(sakId);
    }

    public Optional<ForespørselEntitet> finnÅpenForespørsel(LocalDate skjæringstidspunkt,
                                                            Ytelsetype ytelseType,
                                                            AktørIdEntitet brukerAktørId,
                                                            OrganisasjonsnummerDto orgnr) {
        return forespørselRepository.finnÅpenForespørsel(brukerAktørId, ytelseType, orgnr.orgnr(), skjæringstidspunkt);
    }

    public List<ForespørselEntitet> finnÅpneForespørslerForFagsak(SaksnummerDto fagsakSaksnummer) {
        return forespørselRepository.finnÅpenForespørsel(fagsakSaksnummer);
    }

    public Optional<ForespørselEntitet> finnForespørsel(UUID forespørselUuid) {
        return forespørselRepository.hentForespørsel(forespørselUuid);
    }

    public List<ForespørselEntitet> finnForespørslerForSak(SaksnummerDto saksnummer) {
        return forespørselRepository.hentForespørsler(saksnummer);
    }

}
