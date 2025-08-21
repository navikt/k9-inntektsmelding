package no.nav.familie.inntektsmelding.forespørsel.tjenester.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.ObjectMapper;

import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.dto.PeriodeDto;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;

class OppdaterForespørselTaskTest {

    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;
    private OppdaterForespørselTask task;
    private UUID forespørselUuid;
    private List<PeriodeDto> etterspurtePerioder;
    private static final ObjectMapper OBJECT_MAPPER = DefaultJsonMapper.getObjectMapper();

    @BeforeEach
    void setUp() {
        forespørselBehandlingTjeneste = Mockito.mock(ForespørselBehandlingTjeneste.class);
        task = new OppdaterForespørselTask(forespørselBehandlingTjeneste);
        forespørselUuid = UUID.randomUUID();

        // Oppretter test perioder
        etterspurtePerioder = List.of(
            new PeriodeDto(LocalDate.now(), LocalDate.now().plusDays(10)),
            new PeriodeDto(LocalDate.now().plusDays(20), LocalDate.now().plusDays(30))
        );
    }

    @Test
    void skal_oppdatere_forespørsel_med_nye_perioder() throws Exception {
        // Arrange
        var taskdata = OppdaterForespørselTask.lagOppdaterTaskData(forespørselUuid, Ytelsetype.OMSORGSPENGER, etterspurtePerioder);

        // Act
        task.doTask(taskdata);

        // Assert
        verify(forespørselBehandlingTjeneste).oppdaterForespørselMedNyeEtterspurtePerioder(forespørselUuid, etterspurtePerioder);
    }

    @Test
    void skal_kaste_feil_når_ytelsetype_ikke_er_omsorgspenger() {
        // Arrange
        var taskdata = OppdaterForespørselTask.lagOppdaterTaskData(forespørselUuid, Ytelsetype.PLEIEPENGER_SYKT_BARN, etterspurtePerioder);

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> task.doTask(taskdata));
        assertEquals("Støtter kun oppdatering av forespørsel for OMSORGSPENGER, fikk: PLEIEPENGER_SYKT_BARN", exception.getMessage());
        verifyNoInteractions(forespørselBehandlingTjeneste);
    }

    @Test
    void skal_kaste_feil_når_etterspurte_perioder_er_null() throws Exception {
        // Arrange
        var taskdata = OppdaterForespørselTask.lagOppdaterTaskData(forespørselUuid, Ytelsetype.OMSORGSPENGER, null);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> task.doTask(taskdata));
        assertEquals("Kunne ikke deserialisere etterspurtePerioder", exception.getMessage());
        verifyNoInteractions(forespørselBehandlingTjeneste);
    }

    @Test
    void skal_lage_riktig_taskdata() throws Exception {
        // Act
        var taskdata = OppdaterForespørselTask.lagOppdaterTaskData(
            forespørselUuid,
            Ytelsetype.OMSORGSPENGER,
            etterspurtePerioder
        );

        // Assert
        assertEquals(forespørselUuid.toString(), taskdata.getPropertyValue(OppdaterForespørselTask.FORESPØRSEL_UUID));
        assertEquals(Ytelsetype.OMSORGSPENGER.name(), taskdata.getPropertyValue(OppdaterForespørselTask.YTELSETYPE));

        // Verifiser at payload inneholder riktige perioder
        List<PeriodeDto> deserialisertePerioder = OBJECT_MAPPER.readValue(
            taskdata.getPayloadAsString(),
            OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, PeriodeDto.class)
        );
        assertEquals(etterspurtePerioder.size(), deserialisertePerioder.size());
        assertEquals(etterspurtePerioder.get(0).fom(), deserialisertePerioder.get(0).fom());
        assertEquals(etterspurtePerioder.get(0).tom(), deserialisertePerioder.get(0).tom());
    }
}
