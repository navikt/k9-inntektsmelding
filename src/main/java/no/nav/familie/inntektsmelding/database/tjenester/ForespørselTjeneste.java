package no.nav.familie.inntektsmelding.database.tjenester;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import no.nav.familie.inntektsmelding.database.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.AktørId;
import no.nav.familie.inntektsmelding.typer.FagsakSaksnummer;
import no.nav.familie.inntektsmelding.typer.Organisasjonsnummer;


public interface ForespørselTjeneste {

    UUID opprettForespørsel(LocalDate skjæringstidspunkt,
                            Ytelsetype ytelseType,
                            AktørId brukerAktørId,
                            Organisasjonsnummer orgnr,
                            FagsakSaksnummer fagsakSaksnummer);

    void setOppgaveId(UUID forespørselUUID, String oppgaveId);
    void setSakId(UUID forespørselUUID, String sakId);

    Optional<ForespørselEntitet> finnForespørsel(String aktørId, String arbeidsgiverIdent, LocalDate startdato);
}
