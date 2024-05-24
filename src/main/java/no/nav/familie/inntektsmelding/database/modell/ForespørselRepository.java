package no.nav.familie.inntektsmelding.database.modell;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.FagsakSaksnummer;

@Dependent
public class ForespørselRepository {

    private EntityManager entityManager;

    public ForespørselRepository() {
    }

    @Inject
    public ForespørselRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }


    public UUID lagreForespørsel(LocalDate skjæringstidspunkt, Ytelsetype ytelsetype, String aktørId, String orgnummer, String fagsakSaksnummer) {
        var forespørselEntitet = new ForespørselEntitet(orgnummer, skjæringstidspunkt, aktørId, ytelsetype, fagsakSaksnummer);
        entityManager.persist(forespørselEntitet);
        entityManager.flush();
        return forespørselEntitet.getUuid();
    }

    public void oppdaterOppgaveId(UUID forespørselUUID, String oppgaveId) {
        entityManager.createQuery("UPDATE ForespørselEntitet " + "SET oppgaveId = :oppgaveID " + "where uuid = :foresporselUUID")
            .setParameter("foresporselUUID", forespørselUUID)
            .setParameter("oppgaveID", oppgaveId)
            .executeUpdate();
        entityManager.flush();
    }

    public void oppdaterSakId(UUID forespørselUUID, String sakId) {
        entityManager.createQuery("UPDATE ForespørselEntitet " + "SET sakId = :sakId " + "where uuid = :foresporselUUID")
            .setParameter("foresporselUUID", forespørselUUID)
            .setParameter("sakId", sakId)
            .executeUpdate();
        entityManager.flush();
    }

    public Optional<ForespørselEntitet> hentForespørsel(UUID uuid) {
        var query = entityManager.createQuery("SELECT * FROM ForespørselEntitet where uuid = :foresporselUUID", ForespørselEntitet.class)
            .setParameter("foresporselUUID", uuid);

        var resultList = query.getResultList();
        if (resultList.isEmpty()) {
            return Optional.empty();
        } else if (resultList.size() > 1) {
            throw new IllegalStateException("Forventet å finne kun en forespørsel for oppgitt uuid " + uuid);
        } else {
            return Optional.of(resultList.getFirst());
        }
    }

    public List<ForespørselEntitet> hentForespørsler(FagsakSaksnummer saksnummer) {
        var query = entityManager.createQuery("SELECT f FROM ForespørselEntitet f where fagsystemSaksnummer = :saksnr", ForespørselEntitet.class)
            .setParameter("saksnr", saksnummer.getSaksnr());
        return query.getResultList();
    }


}
