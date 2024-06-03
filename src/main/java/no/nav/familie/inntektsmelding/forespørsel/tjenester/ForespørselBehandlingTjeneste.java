package no.nav.familie.inntektsmelding.forespørsel.tjenester;

import java.time.LocalDate;
import java.util.UUID;

import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.AktørIdDto;
import no.nav.familie.inntektsmelding.typer.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.SaksnummerDto;

public interface ForespørselBehandlingTjeneste {

    void håndterInnkommendeForespørsel(LocalDate skjæringstidspunkt,
                                       Ytelsetype ytelsetype,
                                       AktørIdDto aktørId,
                                       OrganisasjonsnummerDto organisasjonsnummer,
                                       SaksnummerDto fagsakSaksnummer);

    void ferdigstillForespørsel(UUID foresporselUuid, AktørIdDto aktorId, OrganisasjonsnummerDto organisasjonsnummerDto, LocalDate startdato);

}
