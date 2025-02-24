package no.nav.familie.inntektsmelding.forespørsel.tjenester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.familie.inntektsmelding.database.JpaExtension;
import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselRepository;
import no.nav.familie.inntektsmelding.forespørsel.rest.OppdaterForespørselDto;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.task.GjenåpneForespørselTask;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.task.OpprettForespørselTask;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.task.SettForespørselTilUtgåttTask;
import no.nav.familie.inntektsmelding.forvaltning.rest.InntektsmeldingForespørselDto;
import no.nav.familie.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjon;
import no.nav.familie.inntektsmelding.integrasjoner.organisasjon.OrganisasjonTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.koder.ForespørselStatus;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.dto.ForespørselAksjon;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.SaksnummerDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskGruppe;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.felles.prosesstask.api.TaskType;
import no.nav.vedtak.felles.testutilities.db.EntityManagerAwareTest;

@ExtendWith({JpaExtension.class, MockitoExtension.class})
class ForespørselBehandlingTjenesteTest extends EntityManagerAwareTest {

    private static final String BRREG_ORGNUMMER = "974760673";
    private static final String AKTØR_ID = "1234567891234";
    private static final String SAK_ID = "1";
    private static final String OPPGAVE_ID = "2";
    private static final String SAK_ID_2 = "3";
    private static final String OPPGAVE_ID_2 = "4";
    private static final String SAKSNUMMMER = "FAGSAK_SAKEN";
    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now().minusYears(1);
    private static final LocalDate FØRSTE_UTTAKSDATO = LocalDate.now().minusYears(1).plusDays(1);
    private static final Ytelsetype YTELSETYPE = Ytelsetype.PLEIEPENGER_SYKT_BARN;

    @Mock
    private ArbeidsgiverNotifikasjon arbeidsgiverNotifikasjon;
    @Mock
    private PersonTjeneste personTjeneste;
    @Mock
    private ProsessTaskTjeneste prosessTaskTjeneste;
    @Mock
    private OrganisasjonTjeneste organisasjonTjeneste;

    private ForespørselRepository forespørselRepository;
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;

    @BeforeEach
    void setUp() {
        this.forespørselRepository = new ForespørselRepository(getEntityManager());
        this.forespørselBehandlingTjeneste = new ForespørselBehandlingTjeneste(new ForespørselTjeneste(forespørselRepository),
            arbeidsgiverNotifikasjon,
            personTjeneste,
            prosessTaskTjeneste,
            organisasjonTjeneste);
    }

    @Test
    void skal_opprette_opprette_arbeidsgiverinitiert_forespørsel_uten_oppgave() {
        var aktørIdent = new AktørIdEntitet(AKTØR_ID);
        mockInfoForOpprettelse(AKTØR_ID, YTELSETYPE, BRREG_ORGNUMMER, SAK_ID, OPPGAVE_ID);
        when(personTjeneste.hentPersonInfoFraAktørId(any(), any())).thenReturn(new PersonInfo("12345678910", "test", "test", new PersonIdent("12345678910"), aktørIdent, LocalDate.now(), null));
        when(arbeidsgiverNotifikasjon.opprettSak(any(), any(), any(), any(), any())).thenReturn(SAK_ID);

        var saksnummerDto = new SaksnummerDto(SAKSNUMMMER);

        var uuid = forespørselBehandlingTjeneste.opprettForespørselForArbeidsgiverInitiertIm(YTELSETYPE,
            new AktørIdEntitet(AKTØR_ID),
            saksnummerDto,
            new OrganisasjonsnummerDto(BRREG_ORGNUMMER),
            SKJÆRINGSTIDSPUNKT,
            FØRSTE_UTTAKSDATO);

        var lagret = forespørselRepository.hentForespørsel(uuid).orElseThrow();

        clearHibernateCache();
        assertThat(lagret.getStatus()).isEqualTo(ForespørselStatus.UNDER_BEHANDLING);
        assertThat(lagret.getOppgaveId()).isEmpty();
        assertThat(lagret.getFørsteUttaksdato().orElse(null)).isEqualTo(FØRSTE_UTTAKSDATO);
    }

    @Test
    void skal_ferdigstille_forespørsel() {
        var forespørselUuid = forespørselRepository.lagreForespørsel(SKJÆRINGSTIDSPUNKT, YTELSETYPE, AKTØR_ID, BRREG_ORGNUMMER, SAKSNUMMMER,
            SKJÆRINGSTIDSPUNKT);
        forespørselRepository.oppdaterArbeidsgiverNotifikasjonSakId(forespørselUuid, SAK_ID);

        forespørselBehandlingTjeneste.ferdigstillForespørsel(forespørselUuid,
            new AktørIdEntitet(AKTØR_ID),
            new OrganisasjonsnummerDto(BRREG_ORGNUMMER),
            SKJÆRINGSTIDSPUNKT,
            LukkeÅrsak.EKSTERN_INNSENDING);

        clearHibernateCache();

        var lagret = forespørselRepository.hentForespørsel(forespørselUuid);
        assertThat(lagret.map( ForespørselEntitet::getStatus)).isEqualTo(Optional.of(ForespørselStatus.FERDIG));
    }

    @Test
    void skal_ferdigstille_forespørsel_ulik_stp_og_startdato() {
        var forespørselUuid = forespørselRepository.lagreForespørsel(SKJÆRINGSTIDSPUNKT, YTELSETYPE, AKTØR_ID, BRREG_ORGNUMMER, SAKSNUMMMER,
            FØRSTE_UTTAKSDATO);
        forespørselRepository.oppdaterArbeidsgiverNotifikasjonSakId(forespørselUuid, SAK_ID);

        forespørselBehandlingTjeneste.ferdigstillForespørsel(forespørselUuid,
            new AktørIdEntitet(AKTØR_ID),
            new OrganisasjonsnummerDto(BRREG_ORGNUMMER),
            FØRSTE_UTTAKSDATO,
            LukkeÅrsak.EKSTERN_INNSENDING);

        clearHibernateCache();

        var lagret = forespørselRepository.hentForespørsel(forespørselUuid);
        assertThat(lagret.map( ForespørselEntitet::getStatus)).isEqualTo(Optional.of(ForespørselStatus.FERDIG));
    }

    @Test
    void skal_sette_alle_forespørspørsler_for_sak_til_ferdig() {
        var forespørselUuid = forespørselRepository.lagreForespørsel(SKJÆRINGSTIDSPUNKT, YTELSETYPE, AKTØR_ID, BRREG_ORGNUMMER, SAKSNUMMMER,
            FØRSTE_UTTAKSDATO);
        forespørselRepository.oppdaterArbeidsgiverNotifikasjonSakId(forespørselUuid, SAK_ID);
        var forespørselUuid2 = forespørselRepository.lagreForespørsel(SKJÆRINGSTIDSPUNKT.plusDays(2),
            YTELSETYPE,
            AKTØR_ID,
            BRREG_ORGNUMMER,
            SAKSNUMMMER,
            FØRSTE_UTTAKSDATO.plusDays(1));
        forespørselRepository.oppdaterArbeidsgiverNotifikasjonSakId(forespørselUuid2, "2");

        forespørselBehandlingTjeneste.lukkForespørsel(new SaksnummerDto(SAKSNUMMMER), new OrganisasjonsnummerDto(BRREG_ORGNUMMER), null);

        clearHibernateCache();

        var lagret = forespørselRepository.hentForespørsel(forespørselUuid);
        assertThat(lagret.map( ForespørselEntitet::getStatus)).isEqualTo(Optional.of(ForespørselStatus.FERDIG));
        var lagret2 = forespørselRepository.hentForespørsel(forespørselUuid2);
        assertThat(lagret2.map( ForespørselEntitet::getStatus)).isEqualTo(Optional.of(ForespørselStatus.FERDIG));
    }

    @Test
    void skal_sette_alle_forespørspørsler_for_sak_til_utgått() {
        var forespørselUuid = forespørselRepository.lagreForespørsel(SKJÆRINGSTIDSPUNKT, YTELSETYPE, AKTØR_ID, BRREG_ORGNUMMER, SAKSNUMMMER,
            FØRSTE_UTTAKSDATO);
        forespørselRepository.oppdaterArbeidsgiverNotifikasjonSakId(forespørselUuid, SAK_ID);
        var forespørselUuid2 = forespørselRepository.lagreForespørsel(SKJÆRINGSTIDSPUNKT.plusDays(2),
            YTELSETYPE,
            AKTØR_ID,
            BRREG_ORGNUMMER,
            SAKSNUMMMER,
            FØRSTE_UTTAKSDATO);
        forespørselRepository.oppdaterArbeidsgiverNotifikasjonSakId(forespørselUuid2, "2");

        forespørselBehandlingTjeneste.settForespørselTilUtgått(new SaksnummerDto(SAKSNUMMMER), null, null);

        clearHibernateCache();

        var lagret = forespørselRepository.hentForespørsel(forespørselUuid);
        assertThat(lagret.map( ForespørselEntitet::getStatus)).isEqualTo(Optional.of(ForespørselStatus.UTGÅTT));
        var lagret2 = forespørselRepository.hentForespørsel(forespørselUuid2);
        assertThat(lagret2.map( ForespørselEntitet::getStatus)).isEqualTo(Optional.of(ForespørselStatus.UTGÅTT));
    }

    @Test
    void skal_lukke_forespørsel_for_sak_med_gitt_stp() {
        var forespørselUuid = forespørselRepository.lagreForespørsel(SKJÆRINGSTIDSPUNKT, YTELSETYPE, AKTØR_ID, BRREG_ORGNUMMER, SAKSNUMMMER,
            SKJÆRINGSTIDSPUNKT);
        forespørselRepository.oppdaterArbeidsgiverNotifikasjonSakId(forespørselUuid, SAK_ID);

        var forespørselUuid2 = forespørselRepository.lagreForespørsel(SKJÆRINGSTIDSPUNKT.plusDays(2),
            YTELSETYPE,
            AKTØR_ID,
            BRREG_ORGNUMMER,
            SAKSNUMMMER,
            SKJÆRINGSTIDSPUNKT);
        forespørselRepository.oppdaterArbeidsgiverNotifikasjonSakId(forespørselUuid, "2");

        forespørselBehandlingTjeneste.lukkForespørsel(new SaksnummerDto(SAKSNUMMMER),
            new OrganisasjonsnummerDto(BRREG_ORGNUMMER),
            SKJÆRINGSTIDSPUNKT);

        clearHibernateCache();

        var lagret = forespørselRepository.hentForespørsel(forespørselUuid);
        assertThat(lagret.map( ForespørselEntitet::getStatus)).isEqualTo(Optional.of(ForespørselStatus.FERDIG));
        var lagret2 = forespørselRepository.hentForespørsel(forespørselUuid2);
        assertThat(lagret2.map( ForespørselEntitet::getStatus)).isEqualTo(Optional.of(ForespørselStatus.UNDER_BEHANDLING));
    }

    @Test
    void skal_ikke_opprette_ny_forespørsel_dersom_det_eksisterer_en_for_samme_stp() {
        var forespørselUuid = forespørselRepository.lagreForespørsel(SKJÆRINGSTIDSPUNKT, YTELSETYPE, AKTØR_ID, BRREG_ORGNUMMER, SAKSNUMMMER,
            SKJÆRINGSTIDSPUNKT);
        forespørselRepository.oppdaterArbeidsgiverNotifikasjonSakId(forespørselUuid, SAK_ID);

        var forespørsler = List.of(new OppdaterForespørselDto(SKJÆRINGSTIDSPUNKT, new OrganisasjonsnummerDto(BRREG_ORGNUMMER), ForespørselAksjon.OPPRETT));
        forespørselBehandlingTjeneste.oppdaterForespørsler(YTELSETYPE, new AktørIdEntitet(AKTØR_ID), forespørsler, new SaksnummerDto(SAKSNUMMMER));

        verifyNoInteractions(prosessTaskTjeneste);
    }

    @Test
    void skal_opprette_forespørsel_dersom_det_ikke_eksisterer_en_for_stp() {
        mockInfoForOpprettelse(AKTØR_ID, YTELSETYPE, BRREG_ORGNUMMER, SAK_ID, OPPGAVE_ID);

        var forespørsler = List.of(new OppdaterForespørselDto(SKJÆRINGSTIDSPUNKT, new OrganisasjonsnummerDto(BRREG_ORGNUMMER), ForespørselAksjon.OPPRETT));
        forespørselBehandlingTjeneste.oppdaterForespørsler(YTELSETYPE, new AktørIdEntitet(AKTØR_ID), forespørsler, new SaksnummerDto(SAKSNUMMMER));

        var captor = ArgumentCaptor.forClass(ProsessTaskGruppe.class);
        verify(prosessTaskTjeneste).lagre(captor.capture());
        var taskGruppe = captor.getValue();
        assertThat(taskGruppe.getTasks()).hasSize(1);
        var taskdata = taskGruppe.getTasks().getFirst().task();
        assertThat(taskdata.taskType()).isEqualTo(TaskType.forProsessTask(OpprettForespørselTask.class));
        assertThat(taskdata.getPropertyValue(OpprettForespørselTask.YTELSETYPE)).isEqualTo(YTELSETYPE.toString());
        assertThat(taskdata.getSaksnummer()).isEqualTo(SAKSNUMMMER);
        assertThat(taskdata.getAktørId()).isEqualTo(AKTØR_ID);
        assertThat(taskdata.getPropertyValue(OpprettForespørselTask.ORGNR)).isEqualTo(BRREG_ORGNUMMER);
        assertThat(taskdata.getPropertyValue(OpprettForespørselTask.STP)).isEqualTo(SKJÆRINGSTIDSPUNKT.toString());
    }

    @Test
    void skal_opprette_ny_forespørsel_og_beholde_gammel_dersom_vi_ber_om_et_nytt_stp() {
        mockInfoForOpprettelse(AKTØR_ID, YTELSETYPE, BRREG_ORGNUMMER, SAK_ID, OPPGAVE_ID);

        var forespørselUuid = forespørselRepository.lagreForespørsel(SKJÆRINGSTIDSPUNKT, YTELSETYPE, AKTØR_ID, BRREG_ORGNUMMER, SAKSNUMMMER,
            SKJÆRINGSTIDSPUNKT);
        forespørselRepository.oppdaterArbeidsgiverNotifikasjonSakId(forespørselUuid, SAK_ID);

        var forespørsler = List.of(new OppdaterForespørselDto(SKJÆRINGSTIDSPUNKT, new OrganisasjonsnummerDto(BRREG_ORGNUMMER), ForespørselAksjon.OPPRETT),
            new OppdaterForespørselDto(SKJÆRINGSTIDSPUNKT.plusDays(10), new OrganisasjonsnummerDto(BRREG_ORGNUMMER), ForespørselAksjon.OPPRETT));
        forespørselBehandlingTjeneste.oppdaterForespørsler(YTELSETYPE, new AktørIdEntitet(AKTØR_ID), forespørsler, new SaksnummerDto(SAKSNUMMMER));

        var captor = ArgumentCaptor.forClass(ProsessTaskGruppe.class);
        verify(prosessTaskTjeneste).lagre(captor.capture());
        var taskGruppe = captor.getValue();
        assertThat(taskGruppe.getTasks()).hasSize(1);
        var taskdata1 = taskGruppe.getTasks().getFirst().task();
        assertThat(taskdata1.taskType()).isEqualTo(TaskType.forProsessTask(OpprettForespørselTask.class));
    }

    @Test
    void skal_opprette_ny_forespørsel_og_markere_gammel_som_utgått_dersom_vi_erstatter_stp() {
        mockInfoForOpprettelse(AKTØR_ID, YTELSETYPE, BRREG_ORGNUMMER, SAK_ID, OPPGAVE_ID);

        var forespørselUuid = forespørselRepository.lagreForespørsel(SKJÆRINGSTIDSPUNKT, YTELSETYPE, AKTØR_ID, BRREG_ORGNUMMER, SAKSNUMMMER,
            SKJÆRINGSTIDSPUNKT);
        forespørselRepository.oppdaterArbeidsgiverNotifikasjonSakId(forespørselUuid, SAK_ID);

        var forespørsler = List.of(new OppdaterForespørselDto(SKJÆRINGSTIDSPUNKT.plusDays(10), new OrganisasjonsnummerDto(BRREG_ORGNUMMER), ForespørselAksjon.OPPRETT));

        mockInfoForOpprettelse(AKTØR_ID, YTELSETYPE, BRREG_ORGNUMMER, SAK_ID_2, OPPGAVE_ID_2);
        forespørselBehandlingTjeneste.oppdaterForespørsler(YTELSETYPE, new AktørIdEntitet(AKTØR_ID), forespørsler, new SaksnummerDto(SAKSNUMMMER));

        var captor = ArgumentCaptor.forClass(ProsessTaskGruppe.class);
        verify(prosessTaskTjeneste).lagre(captor.capture());
        var taskGruppe = captor.getValue();
        assertThat(taskGruppe.getTasks()).hasSize(2);
        var taskdata1 = taskGruppe.getTasks().get(0).task();
        assertThat(taskdata1.taskType()).isEqualTo(TaskType.forProsessTask(OpprettForespørselTask.class));
        var taskdata2 = taskGruppe.getTasks().get(1).task();
        assertThat(taskdata2.taskType()).isEqualTo(TaskType.forProsessTask(SettForespørselTilUtgåttTask.class));
        assertThat(taskdata2.getPropertyValue(SettForespørselTilUtgåttTask.FORESPØRSEL_UUID)).isEqualTo(forespørselUuid.toString());
    }

    @Test
    void skal_sperre_forespørsel_for_endringer() {
        mockInfoForOpprettelse(AKTØR_ID, YTELSETYPE, BRREG_ORGNUMMER, SAK_ID, OPPGAVE_ID);

        var forespørselUuid = forespørselRepository.lagreForespørsel(SKJÆRINGSTIDSPUNKT, YTELSETYPE, AKTØR_ID, BRREG_ORGNUMMER, SAKSNUMMMER,
            SKJÆRINGSTIDSPUNKT);
        forespørselRepository.oppdaterArbeidsgiverNotifikasjonSakId(forespørselUuid, SAK_ID);
        forespørselRepository.ferdigstillForespørsel(SAK_ID);

        var forespørsler = List.of(new OppdaterForespørselDto(SKJÆRINGSTIDSPUNKT, new OrganisasjonsnummerDto(BRREG_ORGNUMMER), ForespørselAksjon.UTGÅTT));
        forespørselBehandlingTjeneste.oppdaterForespørsler(YTELSETYPE, new AktørIdEntitet(AKTØR_ID), forespørsler, new SaksnummerDto(SAKSNUMMMER));

        var captor = ArgumentCaptor.forClass(ProsessTaskGruppe.class);
        verify(prosessTaskTjeneste).lagre(captor.capture());
        var taskGruppe = captor.getValue();
        assertThat(taskGruppe.getTasks()).hasSize(1);
        var taskdata = taskGruppe.getTasks().getFirst().task();
        assertThat(taskdata.taskType()).isEqualTo(TaskType.forProsessTask(SettForespørselTilUtgåttTask.class));
        assertThat(taskdata.getPropertyValue(SettForespørselTilUtgåttTask.FORESPØRSEL_UUID)).isEqualTo(forespørselUuid.toString());
    }

    @Test
    void skal_gjenåpne_utgått_forespørsel() {
        mockInfoForOpprettelse(AKTØR_ID, YTELSETYPE, BRREG_ORGNUMMER, SAK_ID, OPPGAVE_ID);

        var forespørselUuid = forespørselRepository.lagreForespørsel(SKJÆRINGSTIDSPUNKT, YTELSETYPE, AKTØR_ID, BRREG_ORGNUMMER, SAKSNUMMMER,
            SKJÆRINGSTIDSPUNKT);
        forespørselRepository.oppdaterArbeidsgiverNotifikasjonSakId(forespørselUuid, SAK_ID);
        forespørselRepository.settForespørselTilUtgått(SAK_ID);

        var forespørsler = List.of(new OppdaterForespørselDto(SKJÆRINGSTIDSPUNKT, new OrganisasjonsnummerDto(BRREG_ORGNUMMER), ForespørselAksjon.GJENOPPRETT));
        forespørselBehandlingTjeneste.oppdaterForespørsler(YTELSETYPE, new AktørIdEntitet(AKTØR_ID), forespørsler, new SaksnummerDto(SAKSNUMMMER));

        var captor = ArgumentCaptor.forClass(ProsessTaskGruppe.class);
        verify(prosessTaskTjeneste).lagre(captor.capture());
        var taskGruppe = captor.getValue();
        assertThat(taskGruppe.getTasks()).hasSize(1);
        var taskdata = taskGruppe.getTasks().getFirst().task();
        assertThat(taskdata.taskType()).isEqualTo(TaskType.forProsessTask(GjenåpneForespørselTask.class));
        assertThat(taskdata.getPropertyValue(GjenåpneForespørselTask.FORESPØRSEL_UUID)).isEqualTo(forespørselUuid.toString());
    }

    @Test
    void skal_slette_oppgave_gitt_saksnummer_og_orgnr() {
        var forespørselUuid = forespørselRepository.lagreForespørsel(SKJÆRINGSTIDSPUNKT, YTELSETYPE, AKTØR_ID, BRREG_ORGNUMMER, SAKSNUMMMER,
            SKJÆRINGSTIDSPUNKT);
        forespørselRepository.oppdaterArbeidsgiverNotifikasjonSakId(forespørselUuid, SAK_ID);

        when(arbeidsgiverNotifikasjon.slettSak(SAK_ID)).thenReturn(SAK_ID);

        forespørselBehandlingTjeneste.slettForespørsel(new SaksnummerDto(SAKSNUMMMER), new OrganisasjonsnummerDto(BRREG_ORGNUMMER), null);

        clearHibernateCache();

        var lagret = forespørselRepository.hentForespørsel(forespørselUuid);
        assertThat(lagret.map( ForespørselEntitet::getStatus)).isEqualTo(Optional.of(ForespørselStatus.UTGÅTT));
        verify(arbeidsgiverNotifikasjon, Mockito.times(1)).slettSak(SAK_ID);
    }

    @Test
    void skal_slette_oppgave_gitt_saksnummer() {
        var forespørselUuid = forespørselRepository.lagreForespørsel(SKJÆRINGSTIDSPUNKT, YTELSETYPE, AKTØR_ID, BRREG_ORGNUMMER, SAKSNUMMMER,
            SKJÆRINGSTIDSPUNKT);
        forespørselRepository.oppdaterArbeidsgiverNotifikasjonSakId(forespørselUuid, SAK_ID);

        when(arbeidsgiverNotifikasjon.slettSak(SAK_ID)).thenReturn(SAK_ID);

        forespørselBehandlingTjeneste.slettForespørsel(new SaksnummerDto(SAKSNUMMMER), null, null);

        clearHibernateCache();

        var lagret = forespørselRepository.hentForespørsel(forespørselUuid);
        assertThat(lagret.map( ForespørselEntitet::getStatus)).isEqualTo(Optional.of(ForespørselStatus.UTGÅTT));
        verify(arbeidsgiverNotifikasjon, Mockito.times(1)).slettSak(SAK_ID);
    }

    @Test
    void skal_finne_siste_opprinnelig_forespørsel_og_før_ny_start_dato_for_aktør() {
        var forespørselUuid = forespørselRepository.lagreForespørsel(SKJÆRINGSTIDSPUNKT,
            Ytelsetype.PLEIEPENGER_SYKT_BARN,
            AKTØR_ID,
            BRREG_ORGNUMMER,
            SAKSNUMMMER,
            FØRSTE_UTTAKSDATO);
        forespørselRepository.oppdaterArbeidsgiverNotifikasjonSakId(forespørselUuid, SAK_ID);
        forespørselRepository.ferdigstillForespørsel(SAK_ID);

        var forespørselUuid2 = forespørselRepository.lagreForespørsel(SKJÆRINGSTIDSPUNKT.plusMonths(4),
            Ytelsetype.PLEIEPENGER_SYKT_BARN,
            AKTØR_ID,
            BRREG_ORGNUMMER,
            "SAKSNUMMMER2",
            FØRSTE_UTTAKSDATO.plusMonths(4));
        forespørselRepository.oppdaterArbeidsgiverNotifikasjonSakId(forespørselUuid2, SAK_ID_2);
        forespørselRepository.ferdigstillForespørsel(SAK_ID_2);

        var datoEtterStartDato = LocalDate.now().plusMonths(1);
        var forespørselUuidEtterStartDato = forespørselRepository.lagreForespørsel(datoEtterStartDato,
            Ytelsetype.PLEIEPENGER_SYKT_BARN,
            AKTØR_ID,
            BRREG_ORGNUMMER,
            "SAKSNUMMMER3",
            datoEtterStartDato);
        forespørselRepository.oppdaterArbeidsgiverNotifikasjonSakId(forespørselUuidEtterStartDato, "3");
        forespørselRepository.ferdigstillForespørsel("3");

        var resultat = forespørselBehandlingTjeneste.finnOpprinneligForespørsel(new AktørIdEntitet(AKTØR_ID),
            Ytelsetype.PLEIEPENGER_SYKT_BARN,
            LocalDate.now());

        clearHibernateCache();

        assertThat(resultat.map(ForespørselEntitet::getUuid)).isEqualTo(Optional.of(forespørselUuid2));
    }

    @Test
    void skal_returnere_liste_av_inntektsmeldingdto_for_forespørsler() {

        var forespørsel1sak1 = new ForespørselEntitet(BRREG_ORGNUMMER, LocalDate.of(2025, 1, 1), new AktørIdEntitet(AKTØR_ID), Ytelsetype.PLEIEPENGER_SYKT_BARN, SAK_ID, LocalDate.of(2025, 1, 1));
        var forespørsel1sak2 = new ForespørselEntitet(BRREG_ORGNUMMER, LocalDate.of(2025, 2, 1), new AktørIdEntitet(AKTØR_ID), Ytelsetype.PLEIEPENGER_SYKT_BARN, SAK_ID_2 , LocalDate.of(2025, 2, 1));
        var forespørsel2sak1 = new ForespørselEntitet(BRREG_ORGNUMMER, LocalDate.of(2025, 3, 1), new AktørIdEntitet(AKTØR_ID), Ytelsetype.PLEIEPENGER_SYKT_BARN, SAK_ID, LocalDate.of(2025, 3, 1));
        var forespørsel2sak2 = new ForespørselEntitet(BRREG_ORGNUMMER, LocalDate.of(2025, 4, 1), new AktørIdEntitet(AKTØR_ID), Ytelsetype.PLEIEPENGER_SYKT_BARN, SAK_ID_2, LocalDate.of(2025, 4, 1));

        getEntityManager().persist(forespørsel1sak1);
        getEntityManager().persist(forespørsel1sak2);
        getEntityManager().persist(forespørsel2sak1);
        getEntityManager().persist(forespørsel2sak2);
        getEntityManager().flush();

        List<InntektsmeldingForespørselDto> inntektsmeldingForespørselDtos = forespørselBehandlingTjeneste.finnForespørslerForFagsak(new SaksnummerDto(SAK_ID));

        assertThat(inntektsmeldingForespørselDtos).hasSize(2);
        var dto1 = inntektsmeldingForespørselDtos.stream().filter(forespørsel -> forespørsel.skjæringstidspunkt().equals(forespørsel1sak1.getSkjæringstidspunkt())).findAny().get();
        var dto2 = inntektsmeldingForespørselDtos.stream().filter(forespørsel -> forespørsel.skjæringstidspunkt().equals(forespørsel2sak1.getSkjæringstidspunkt())).findAny().get();

        assertThat(dto1.aktørid()).isEqualTo(forespørsel1sak1.getAktørId().getAktørId());
        assertThat(dto1.skjæringstidspunkt()).isEqualTo(forespørsel1sak1.getSkjæringstidspunkt());
        assertThat(dto1.ytelsetype()).isEqualTo(forespørsel1sak1.getYtelseType().toString());
        assertThat(dto1.uuid()).isEqualTo(forespørsel1sak1.getUuid());
        assertThat(dto1.arbeidsgiverident()).isEqualTo(forespørsel1sak1.getOrganisasjonsnummer());

        assertThat(dto2.aktørid()).isEqualTo(forespørsel2sak1.getAktørId().getAktørId());
        assertThat(dto2.skjæringstidspunkt()).isEqualTo(forespørsel2sak1.getSkjæringstidspunkt());
        assertThat(dto2.ytelsetype()).isEqualTo(forespørsel2sak1.getYtelseType().toString());
        assertThat(dto2.uuid()).isEqualTo(forespørsel2sak1.getUuid());
        assertThat(dto2.arbeidsgiverident()).isEqualTo(forespørsel2sak1.getOrganisasjonsnummer());

    }

    private void clearHibernateCache() {
        // Fjerne hibernate cachen før assertions skal evalueres - hibernate ignorerer alle updates som er markert med updatable = false ved skriving mot databasen
        // men objektene i cachen blir oppdatert helt greit likevel.
        // På denne måten evaluerer vi faktisk tilstanden som blir til slutt lagret i databasen.
        getEntityManager().clear();
    }

    private void mockInfoForOpprettelse(String aktørId, Ytelsetype ytelsetype, String brregOrgnummer, String sakId, String oppgaveId) {
        var personInfo = new PersonInfo("Navn",
            null,
            "Navnesen",
            new PersonIdent("01019100000"),
            new AktørIdEntitet(aktørId),
            LocalDate.of(1991, 1, 1).minusYears(30),
            null);
        var sakTittel = ForespørselTekster.lagSaksTittel(personInfo.mapFulltNavn(), personInfo.fødselsdato());

        lenient().when(personTjeneste.hentPersonInfoFraAktørId(new AktørIdEntitet(aktørId), ytelsetype)).thenReturn(personInfo);
        lenient().when(arbeidsgiverNotifikasjon.opprettSak(any(), any(), eq(brregOrgnummer), eq(sakTittel), any())).thenReturn(sakId);
        lenient().when(arbeidsgiverNotifikasjon.opprettOppgave(any(), any(), any(), eq(brregOrgnummer), any(), any(), any(), any()))
            .thenReturn(oppgaveId);
    }
}
