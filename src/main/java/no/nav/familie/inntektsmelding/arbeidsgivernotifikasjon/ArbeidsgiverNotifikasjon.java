package no.nav.familie.inntektsmelding.arbeidsgivernotifikasjon;

import java.net.URI;
import java.time.LocalDateTime;

public interface ArbeidsgiverNotifikasjon {
    String opprettNyOppgave(String eksternId, String tekst, URI lenke, Merkelapp merkelapp, String virksomhetsnummer, LocalDateTime tidspunkt);
}
