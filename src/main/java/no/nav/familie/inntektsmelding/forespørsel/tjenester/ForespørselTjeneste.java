package no.nav.familie.inntektsmelding.forespørsel.tjenester;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.AktørIdDto;
import no.nav.familie.inntektsmelding.typer.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.SaksnummerDto;


public interface ForespørselTjeneste {

    UUID opprettForespørsel(LocalDate skjæringstidspunkt,
                            Ytelsetype ytelseType,
                            AktørIdDto brukerAktørId,
                            OrganisasjonsnummerDto orgnr,
                            SaksnummerDto fagsakSaksnummer);

    void setOppgaveId(UUID forespørselUUID, String oppgaveId);

    void setSakId(UUID forespørselUUID, String sakId);

    void ferdigstillSak(String sakId);

    Optional<ForespørselEntitet> finnÅpenForespørsel(LocalDate skjæringstidspunkt,
                                                     Ytelsetype ytelseType,
                                                     AktørIdDto brukerAktørId,
                                                     OrganisasjonsnummerDto orgnr);

    Optional<ForespørselEntitet> finnForespørsel(UUID forespørselUuid);
}
