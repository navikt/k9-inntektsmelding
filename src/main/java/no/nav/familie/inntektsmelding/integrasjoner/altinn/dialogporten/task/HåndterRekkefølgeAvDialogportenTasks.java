package no.nav.familie.inntektsmelding.integrasjoner.altinn.dialogporten.task;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskGruppe;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskLifecycleObserver;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskStatus;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskVeto;
import no.nav.vedtak.felles.prosesstask.api.TaskType;
import no.nav.vedtak.felles.prosesstask.impl.ProsessTaskRepository;

@ApplicationScoped
public class HåndterRekkefølgeAvDialogportenTasks implements ProsessTaskLifecycleObserver {
    private static final Logger LOG = LoggerFactory.getLogger(HåndterRekkefølgeAvDialogportenTasks.class);
    public static final String FORESPØRSEL_UUID = "forespoerselUuid";

    private static final Set<TaskType> DIALOGPORTEN_TASKER = Set.of(
        TaskType.forProsessTask(FerdigstillForespørselDialogTask.class),
        TaskType.forProsessTask(OppdaterDialogMedEndretInntektsmeldingTask.class),
        TaskType.forProsessTask(OpprettForespørselDialogportenTask.class),
        TaskType.forProsessTask(SendMeldingOmAvvistInntektsmeldingTask.class),
        TaskType.forProsessTask(SettDialogTilUtgåttTask.class));

    private ProsessTaskRepository prosessTaskRepository;

    HåndterRekkefølgeAvDialogportenTasks() {
        //CDI
    }

    @Inject
    public HåndterRekkefølgeAvDialogportenTasks(ProsessTaskRepository prosessTaskRepository) {
        this.prosessTaskRepository = prosessTaskRepository;
    }

    @Override
    public ProsessTaskVeto vetoKjøring(ProsessTaskData prosessTaskData) {

        if (DIALOGPORTEN_TASKER.contains(prosessTaskData.taskType())) {
            UUID forespørselUuid = hentForespørselUuid(prosessTaskData);

            //TODO bytt til en mer spesifikk query når vi er over på k9-prosesstask
            Optional<ProsessTaskData> dialogportenTask = prosessTaskRepository.finnAlle(List.of(ProsessTaskStatus.KLAR))
                .stream()
                .filter(task -> DIALOGPORTEN_TASKER.contains(task.taskType()))
                .filter(task -> !Objects.equals(task.getId(), prosessTaskData.getId()))
                .filter(task -> Objects.equals(hentForespørselUuid(task), forespørselUuid))
                .filter(task -> !Objects.equals(task.getGruppe(), prosessTaskData.getGruppe()))
                .filter(task -> task.getOpprettetTid().isBefore(prosessTaskData.getOpprettetTid()))
                .max(Comparator.comparing(ProsessTaskData::getOpprettetTid));

            if (dialogportenTask.isPresent()) {
                LOG.info("Vetoer kjøring av prosesstask[{}] av {} for forespørsel [{}], er blokkert av prosesstask[{}] for samme forespørsel.",
                    prosessTaskData.getId(), prosessTaskData.taskType(), forespørselUuid, dialogportenTask.get().getId());

                return new ProsessTaskVeto(true, prosessTaskData.getId(), dialogportenTask.get().getId(),
                    "Må vente på annen task for samme forespørsel som ble opprettet før denne.");
            }
        }

        return new ProsessTaskVeto(false, prosessTaskData.getId());
    }

    @Override
    public void opprettetProsessTaskGruppe(ProsessTaskGruppe sammensattTask) {

    }

    private static UUID hentForespørselUuid(ProsessTaskData task) {
        String verdi = task.getPropertyValue(FORESPØRSEL_UUID);
        if (verdi == null) {
            throw new IllegalArgumentException(
                "Task[%d] av type %s mangler forespørselUuid".formatted(task.getId(), task.taskType().value()));
        }
        try {
            return UUID.fromString(verdi);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "Task[%d] av type %s har ugyldig forespørselUuid: %s".formatted(task.getId(), task.taskType().value(), verdi), e);
        }
    }
}
