package no.nav.familie.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon;

import java.net.URI;
import java.time.OffsetDateTime;

public interface ArbeidsgiverNotifikasjon {

    HentetSak hentSak(String grupperingsid, Merkelapp merkelapp);

    String opprettSak(String grupperingsid,
                      Merkelapp merkelapp,
                      String virksomhetsnummer,
                      String saksTittel,
                      URI lenke);

    String opprettOppgave(String eksternId,
                          String grupperingsid,
                          String virksomhetsnummer,
                          String notifikasjonsTekst,
                          URI lenke,
                          Merkelapp merkelapp);

    String lukkOppgave(String id, OffsetDateTime utfoertTidspunkt);
}
