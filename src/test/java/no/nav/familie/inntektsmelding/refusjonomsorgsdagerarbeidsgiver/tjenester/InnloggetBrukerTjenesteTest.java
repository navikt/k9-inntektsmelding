package no.nav.familie.inntektsmelding.refusjonomsorgsdagerarbeidsgiver.tjenester;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;

@ExtendWith(MockitoExtension.class)
class InnloggetBrukerTjenesteTest {
    @Mock
    private PersonTjeneste personTjenesteMock;

    private InnloggetBrukerTjeneste innloggetBrukerTjeneste;

    @BeforeEach
    void setUp() {
        innloggetBrukerTjeneste = new InnloggetBrukerTjeneste(personTjenesteMock);
    }

    @Test
    void hent_innlogget_bruker_skal_returnere_innlogget_bruker() {
        // Arrange
        var ytelseType = Ytelsetype.OMSORGSPENGER;
        var innloggetPerson = new PersonInfo(
            "fornavn",
            "mellomnavn",
            "etternavn",
            PersonIdent.fra("11839798115"),
            AktørIdEntitet.dummy(),
            LocalDate.of(1997, 11, 23),
            "81549300"
        );

        when(personTjenesteMock.hentInnloggetPerson(ytelseType)).thenReturn(innloggetPerson);

        var innloggetBruker = innloggetBrukerTjeneste.hentInnloggetBruker(ytelseType);

        assertEquals("fornavn", innloggetBruker.fornavn());
        assertEquals("mellomnavn", innloggetBruker.mellomnavn());
        assertEquals("etternavn", innloggetBruker.etternavn());
        assertEquals("81549300", innloggetBruker.telefon());
        verify(personTjenesteMock).hentInnloggetPerson(ytelseType);
    }

    @Test
    void hent_innlogget_bruker_skal_returnere_tom_dto_om_PersonTjeneste_returnerer_null() {
        var ytelseType = Ytelsetype.OMSORGSPENGER;
        when(personTjenesteMock.hentInnloggetPerson(ytelseType)).thenReturn(null);

        var innloggetBruker = innloggetBrukerTjeneste.hentInnloggetBruker(ytelseType);

        assertNull(innloggetBruker.fornavn());
        assertNull(innloggetBruker.mellomnavn());
        assertNull(innloggetBruker.etternavn());
        assertNull(innloggetBruker.telefon());
        verify(personTjenesteMock).hentInnloggetPerson(ytelseType);
    }

}
