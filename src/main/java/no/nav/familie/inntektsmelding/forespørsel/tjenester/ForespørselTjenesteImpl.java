package no.nav.familie.inntektsmelding.forespørsel.tjenester;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselRepository;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.AktørIdDto;
import no.nav.familie.inntektsmelding.typer.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.SaksnummerDto;

@ApplicationScoped
public class ForespørselTjenesteImpl implements ForespørselTjeneste {

    private ForespørselRepository forespørselRepository;

    @Inject
    public ForespørselTjenesteImpl(ForespørselRepository forespørselRepository) {
        this.forespørselRepository = forespørselRepository;
    }

    public ForespørselTjenesteImpl() {
    }

    @Override
    public UUID opprettForespørsel(LocalDate skjæringstidspunkt,
                                   Ytelsetype ytelseType,
                                   AktørIdDto brukerAktørId,
                                   OrganisasjonsnummerDto orgnr,
                                   SaksnummerDto fagsakSaksnummer) {
        return forespørselRepository.lagreForespørsel(skjæringstidspunkt, ytelseType, brukerAktørId.id(), orgnr.orgnr(),
            fagsakSaksnummer.getSaksnr());
    }

    @Override
    public void setOppgaveId(UUID forespørselUUID, String oppgaveId) {
        forespørselRepository.oppdaterOppgaveId(forespørselUUID, oppgaveId);
    }


    @Override
    public void setSakId(UUID forespørselUUID, String sakId) {
        forespørselRepository.oppdaterSakId(forespørselUUID, sakId);

    }

    @Override
    public void ferdigstillSak(String sakId) {
        forespørselRepository.ferdigstillSak(sakId);
    }

    @Override
    public Optional<ForespørselEntitet> finnÅpenForespørsel(LocalDate skjæringstidspunkt,
                                                            Ytelsetype ytelseType,
                                                            AktørIdDto brukerAktørId,
                                                            OrganisasjonsnummerDto orgnr) {
        return forespørselRepository.finnÅpenForespørsel(brukerAktørId.id(), ytelseType, orgnr.orgnr(), skjæringstidspunkt);
    }

    @Override
    public Optional<ForespørselEntitet> finnForespørsel(UUID forespørselUuid) {
        return forespørselRepository.hentForespørsel(forespørselUuid);
    }

}
