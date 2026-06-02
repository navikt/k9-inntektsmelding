package no.nav.familie.inntektsmelding.imapi.forespørsel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.koder.ForespørselType;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.dto.ArbeidsgiverDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;

@ExtendWith(MockitoExtension.class)
class ForespørselApiTjenesteTest {
    @Mock
    private PersonTjeneste personTjeneste;
    @Mock
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;

    private ForespørselApiTjeneste forespørselApiTjeneste;

    @BeforeEach
    void setUp() {
        this.forespørselApiTjeneste = new ForespørselApiTjeneste(personTjeneste, forespørselBehandlingTjeneste);
    }

    @Test
    void skal_returnere_forespørsel_med_fnr() {
        var aktørId = new AktørIdEntitet("1234567890123");
        var fnr = new PersonIdent("11111111111");
        var orgnr = "999999999";
        var forespørsel = ForespørselEntitet.builder()
            .medOrganisasjonsnummer(orgnr)
            .medSkjæringstidspunkt(LocalDate.now())
            .medAktørId(aktørId)
            .medYtelseType(Ytelsetype.PLEIEPENGER_SYKT_BARN)
            .medForespørselType(ForespørselType.BESTILT_AV_FAGSYSTEM)
            .build();
        var forespørselUuid = UUID.randomUUID();
        when(forespørselBehandlingTjeneste.hentForespørsel(forespørselUuid)).thenReturn(Optional.of(forespørsel));
        when(personTjeneste.finnPersonIdentForAktørId(aktørId)).thenReturn(fnr);

        var dto = forespørselApiTjeneste.hentForesørselDto(forespørselUuid);

        assertThat(dto).isPresent();
        assertThat(dto.get().fødselsnummer().fnr()).isEqualTo(fnr.getIdent());
        assertThat(dto.get().orgnummer().orgnr()).isEqualTo(orgnr);
    }

    @Test
    void skal_filtrere_forespørsler_på_orgnr() {
        var aktørId = new AktørIdEntitet("1234567890123");
        var fnr = new PersonIdent("11111111111");
        var orgnr = "999999999";
        var arbeidsgiverDto = new ArbeidsgiverDto(orgnr);
        var forespørsel = ForespørselEntitet.builder()
            .medOrganisasjonsnummer(orgnr)
            .medSkjæringstidspunkt(LocalDate.now())
            .medAktørId(aktørId)
            .medYtelseType(Ytelsetype.PLEIEPENGER_SYKT_BARN)
            .medForespørselType(ForespørselType.BESTILT_AV_FAGSYSTEM)
            .build();
        when(forespørselBehandlingTjeneste.hentForespørsler(arbeidsgiverDto, null, null, null, null, null))
            .thenReturn(List.of(forespørsel));
        when(personTjeneste.finnPersonIdentForAktørIdBolk(Set.of(aktørId)))
            .thenReturn(Map.of(aktørId, fnr));

        var dtoer = forespørselApiTjeneste.hentForespørslerDto(arbeidsgiverDto, null, null, null, null, null);

        assertThat(dtoer).hasSize(1);
        assertThat(dtoer.getFirst().fødselsnummer().fnr()).isEqualTo(fnr.getIdent());
        assertThat(dtoer.getFirst().orgnummer().orgnr()).isEqualTo(orgnr);
    }
}
