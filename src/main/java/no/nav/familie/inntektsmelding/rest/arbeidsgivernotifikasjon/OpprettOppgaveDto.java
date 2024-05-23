package no.nav.familie.inntektsmelding.rest.arbeidsgivernotifikasjon;

import java.time.LocalDate;
import java.util.List;

import no.nav.familie.inntektsmelding.koder.Ytelsetype;

public record OpprettOppgaveDto(String organisasjonsnummer, Ytelsetype ytelse, List<String> arbeidsforholdId, String saksnummer,
                                LocalDate skjæringstidpunkt, String brukerID, LocalDate frist) {
}
