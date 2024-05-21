package no.nav.familie.inntektsmelding.forepørsel.rest;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.AktørId;
import no.nav.familie.inntektsmelding.typer.FagsakSaksnummer;
import no.nav.familie.inntektsmelding.typer.Organisasjonsnummer;


public record OpprettForespørselRequest(@NotNull AktørId aktørId, @NotNull Organisasjonsnummer orgnummer, @NotNull LocalDate skjæringstidspunkt,
                                        @NotNull Ytelsetype ytelsetype, @NotNull FagsakSaksnummer saksnummer) {
}
