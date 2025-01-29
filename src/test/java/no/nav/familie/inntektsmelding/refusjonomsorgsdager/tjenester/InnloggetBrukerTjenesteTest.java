package no.nav.familie.inntektsmelding.refusjonomsorgsdager.tjenester;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.familie.inntektsmelding.integrasjoner.organisasjon.Organisasjon;
import no.nav.familie.inntektsmelding.integrasjoner.organisasjon.OrganisasjonTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.pip.AltinnTilgangTjeneste;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.vedtak.exception.ManglerTilgangException;

@ExtendWith(MockitoExtension.class)
class InnloggetBrukerTjenesteTest {
    @Mock
    private PersonTjeneste personTjenesteMock;

    @Mock
    private AltinnTilgangTjeneste altinnTilgangTjenesteMock;

    @Mock
    private OrganisasjonTjeneste organisasjonTjenesteMock;

    private InnloggetBrukerTjeneste innloggetBrukerTjeneste;

    @BeforeEach
    void setUp() {
        innloggetBrukerTjeneste = new InnloggetBrukerTjeneste(personTjenesteMock, altinnTilgangTjenesteMock, organisasjonTjenesteMock);
    }

    @Test
    void hent_innlogget_bruker_skal_returnere_innlogget_bruker() {
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

        var organisasjon = new Organisasjon(
            "organisasjonsnavn",
            "123456789"
        );

        when(personTjenesteMock.hentInnloggetPerson(ytelseType)).thenReturn(innloggetPerson);
        when(organisasjonTjenesteMock.finnOrganisasjon("123456789")).thenReturn(organisasjon);
        when(altinnTilgangTjenesteMock.manglerTilgangTilBedriften("123456789")).thenReturn(false);

        var innloggetBruker = innloggetBrukerTjeneste.hentInnloggetBruker(ytelseType, "123456789");

        assertEquals("fornavn", innloggetBruker.fornavn());
        assertEquals("mellomnavn", innloggetBruker.mellomnavn());
        assertEquals("etternavn", innloggetBruker.etternavn());
        assertEquals("81549300", innloggetBruker.telefon());
        assertEquals("organisasjonsnavn", innloggetBruker.organisasjonsnavn());
        assertEquals("123456789", innloggetBruker.organisasjonsnummer());
        verify(personTjenesteMock).hentInnloggetPerson(ytelseType);
    }

    @Test
    void hent_innlogget_bruker_skal_kaste_exception_om_PersonTjeneste_returnerer_null() {
        var ytelseType = Ytelsetype.OMSORGSPENGER;
        when(personTjenesteMock.hentInnloggetPerson(ytelseType)).thenReturn(null);

        assertThrows(IllegalStateException.class, () -> innloggetBrukerTjeneste.hentInnloggetBruker(ytelseType, "123456789"));
    }

    @Test
    void hent_innlogget_bruker_skal_kaste_exception_om_organisasjonen_ikke_finnes() {
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
        when(organisasjonTjenesteMock.finnOrganisasjon("123456789")).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> innloggetBrukerTjeneste.hentInnloggetBruker(ytelseType, "123456789"));
    }

    @Test
    void hent_innlogget_bruker_skal_kaste_exception_om_bruker_ikke_har_tilgang_til_organisasjonen() {
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
        var organisasjon = new Organisasjon(
            "organisasjonsnavn",
            "123456789"
        );

        when(personTjenesteMock.hentInnloggetPerson(ytelseType)).thenReturn(innloggetPerson);
        when(organisasjonTjenesteMock.finnOrganisasjon("123456789")).thenReturn(organisasjon);
        when(altinnTilgangTjenesteMock.manglerTilgangTilBedriften("123456789")).thenReturn(true);

        assertThrows(ManglerTilgangException.class, () -> innloggetBrukerTjeneste.hentInnloggetBruker(ytelseType, "123456789"));
    }

}
