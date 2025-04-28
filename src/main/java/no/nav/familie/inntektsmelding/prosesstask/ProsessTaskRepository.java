package no.nav.familie.inntektsmelding.prosesstask;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@ApplicationScoped
public class ProsessTaskRepository {
    private EntityManager entityManager;

    public ProsessTaskRepository() {
        // CDI
    }

    @Inject
    public ProsessTaskRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public long tellAntallFeilendeProsessTasker() {
        return entityManager.createQuery("""
                SELECT COUNT(p)
                FROM ProsessTaskEntitet p
                WHERE (
                    p.status = 'FEILET'
                    OR ((p.status IN ('KLAR', 'VETO')) AND p.opprettetTid < :tidspunkt_i_dag_tidlig AND (p.nesteKjøringEtter IS NULL OR p.nesteKjøringEtter < :tidspunkt_nå))
                    OR (p.status IN ('VENTER_SVAR', 'SUSPENDERT') AND p.opprettetTid < :tidspunkt_i_dag_tidlig)
                )
                """, Long.class)
            .setParameter("tidspunkt_i_dag_tidlig", LocalDateTime.now().truncatedTo(ChronoUnit.DAYS))
            .setParameter("tidspunkt_nå", LocalDateTime.now())
            .getSingleResult();
    }
}
