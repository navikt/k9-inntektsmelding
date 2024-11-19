package no.nav.familie.inntektsmelding.forespørsel.tjenester.task;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;

import jakarta.inject.Inject;

import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskGruppe;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskLifecycleObserver;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskStatus;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskVeto;
import no.nav.vedtak.felles.prosesstask.impl.ProsessTaskRepository;

@ApplicationScoped
public class HåndterRekkefølgeAvForespørselTasks implements ProsessTaskLifecycleObserver {

    private static final Logger log = LoggerFactory.getLogger(HåndterRekkefølgeAvForespørselTasks.class);

    private ProsessTaskRepository prosessTaskRepository;

    HåndterRekkefølgeAvForespørselTasks() {
        //CDI
    }

    @Inject
    public HåndterRekkefølgeAvForespørselTasks(ProsessTaskRepository prosessTaskRepository) {
        this.prosessTaskRepository = prosessTaskRepository;
    }

    @Override
    public ProsessTaskVeto vetoKjøring(ProsessTaskData prosessTaskData) {

        if (prosessTaskData.getTaskType().equals(OpprettForespørselTask.TASKTYPE)) {
            String fagsakSaksnummer = prosessTaskData.getPropertyValue(OpprettForespørselTask.FAGSAK_SAKSNUMMER);

            if (fagsakSaksnummer == null || fagsakSaksnummer.isBlank()) {
                throw new IllegalArgumentException("Task av type " + OpprettForespørselTask.TASKTYPE + " mangler " + OpprettForespørselTask.FAGSAK_SAKSNUMMER);
            }

            //TODO bytt til en mer spesifikk query når vi er over på k9-prosesstask
            Optional<ProsessTaskData> blokkerendeTask = prosessTaskRepository.finnAlle(List.of(ProsessTaskStatus.KLAR))
                .stream()
                .filter(task -> List.of(OpprettForespørselTask.TASKTYPE, SettForespørselTilUtgåttTask.TASKTYPE).contains(task.getTaskType()))
                .filter(task -> Objects.equals(task.getPropertyValue(OpprettForespørselTask.FAGSAK_SAKSNUMMER), fagsakSaksnummer))
                .filter(task -> !Objects.equals(task.getGruppe(), prosessTaskData.getGruppe()))
                .filter(task -> task.getOpprettetTid().isBefore(prosessTaskData.getOpprettetTid()))
                .max(Comparator.comparing(ProsessTaskData::getOpprettetTid));

            if (blokkerendeTask.isPresent()) {
                log.info("Vetoer kjøring av prosesstask[{}] av type[{}] for fagsak [{}], er blokkert av prosesstask[{}] for samme fagsak.",
                    prosessTaskData.getId(), prosessTaskData.getTaskType(), fagsakSaksnummer, blokkerendeTask.get().getId());

                return new ProsessTaskVeto(true, prosessTaskData.getId(), blokkerendeTask.get().getId(),
                    "Må vente på annen task for samme fagsak som ble opprettet før denne.");
            }
        }

        return new ProsessTaskVeto(false, prosessTaskData.getId());
    }

    @Override
    public void opprettetProsessTaskGruppe(ProsessTaskGruppe sammensattTask) {

    }
}
