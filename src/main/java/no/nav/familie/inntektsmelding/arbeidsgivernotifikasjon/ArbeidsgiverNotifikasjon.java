package no.nav.familie.inntektsmelding.arbeidsgivernotifikasjon;

import java.net.URI;

public interface ArbeidsgiverNotifikasjon {
    String opprettOppgave(String tekst, URI lenke, Merkelapp merkelapp, String virksomhetsnummer);
}
