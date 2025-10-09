package no.nav.familie.inntektsmelding.forespørsel.tjenester.task;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselMapper;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.koder.ForespørselType;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.dto.ForespørselAksjon;
import no.nav.familie.inntektsmelding.typer.dto.OppdaterForespørselDto;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.PeriodeDto;
import no.nav.familie.inntektsmelding.typer.dto.SaksnummerDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;

class OpprettForespørselTaskTest {

    private final Ytelsetype ytelsetype = Ytelsetype.PLEIEPENGER_SYKT_BARN;
    private final AktørIdEntitet aktørId = new AktørIdEntitet("1111111111111");
    private final SaksnummerDto saksnummer = new SaksnummerDto("456");
    private final OrganisasjonsnummerDto organisasjon = new OrganisasjonsnummerDto("789");
    private final LocalDate skjæringstidspunkt = LocalDate.now();

    private final ForespørselBehandlingTjeneste forespørselBehandlingTjeneste = Mockito.mock(ForespørselBehandlingTjeneste.class);

    @Test
    void skal_opprette_forespørsel_dersom_det_ikke_eksisterer_en_for_stp() {
        var oppdaterForespørselDto = new OppdaterForespørselDto(skjæringstidspunkt, organisasjon, ForespørselAksjon.OPPRETT, null);
        var task = new OpprettForespørselTask(forespørselBehandlingTjeneste);
        var taskdata = OpprettForespørselTask.lagOpprettForespørselTaskData(ytelsetype, aktørId, saksnummer, oppdaterForespørselDto);

        task.doTask(taskdata);

        verify(forespørselBehandlingTjeneste).opprettForespørsel(ytelsetype, aktørId, saksnummer, organisasjon, skjæringstidspunkt, null, null);
    }

    @Test
    void skal_ikke_opprette_ny_forespørsel_dersom_det_eksisterer_en_for_samme_stp() {
        var oppdaterForespørselDto = new OppdaterForespørselDto(skjæringstidspunkt, organisasjon, ForespørselAksjon.OPPRETT, null);
        var task = new OpprettForespørselTask(forespørselBehandlingTjeneste);
        var taskdata = OpprettForespørselTask.lagOpprettForespørselTaskData(ytelsetype, aktørId, saksnummer, oppdaterForespørselDto);

        when(forespørselBehandlingTjeneste.hentForespørslerForFagsak(saksnummer, organisasjon, skjæringstidspunkt))
            .thenReturn(List.of(ForespørselMapper.mapForespørsel(organisasjon.orgnr(), skjæringstidspunkt, aktørId.getAktørId(), ytelsetype, saksnummer.saksnr(), ForespørselType.BESTILT_AV_FAGSYSTEM, null, null
            )));

        task.doTask(taskdata);

        verify(forespørselBehandlingTjeneste, times(0)).opprettForespørsel(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void skal_opprette_forespørsel_med_etterspurte_perioder_dersom_det_ikke_eksisterer_en_for_stp() {
        List<PeriodeDto> etterspurtePerioder = List.of(new PeriodeDto(LocalDate.now(), LocalDate.now().plusDays(10)));
        var oppdaterForespørselDto = new OppdaterForespørselDto(skjæringstidspunkt, organisasjon, ForespørselAksjon.OPPRETT, etterspurtePerioder);
        var task = new OpprettForespørselTask(forespørselBehandlingTjeneste);
        var taskdata = OpprettForespørselTask.lagOpprettForespørselTaskData(Ytelsetype.OMSORGSPENGER, aktørId, saksnummer, oppdaterForespørselDto);

        task.doTask(taskdata);

        verify(forespørselBehandlingTjeneste).opprettForespørsel(Ytelsetype.OMSORGSPENGER, aktørId, saksnummer, organisasjon, skjæringstidspunkt, null, etterspurtePerioder);
    }
}
