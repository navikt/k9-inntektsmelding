package no.nav.familie.inntektsmelding.forespørsel.tjenester;

import no.nav.familie.inntektsmelding.koder.Ytelsetype;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ForespørselTeksterTest {

    @Test
    void lagSaksTittel() {
        String saksTittel = ForespørselTekster.lagSaksTittel("OLA NORDMANN", LocalDate.of(2021, 02, 1));
        assertEquals("Inntektsmelding for Ola Nordmann (01.02.21)", saksTittel);
    }

    @Test
    void lagOppgaveTekst() {
        String oppgaveTekst = ForespørselTekster.lagOppgaveTekst("OLA NORDMANN", Ytelsetype.OPPLÆRINGSPENGER);
        assertEquals("Send inntektsmelding for Ola Nordmann som har søkt om opplæringspenger", oppgaveTekst);
    }
}
