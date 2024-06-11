package no.nav.familie.inntektsmelding.imdialog.modell;

import java.time.LocalDate;
import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;

@Dependent
public class InntektsmeldingRepository {

    private EntityManager entityManager;

    public InntektsmeldingRepository() {
        // CDI
    }

    @Inject
    public InntektsmeldingRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Long lagreInntektsmelding(InntektsmeldingEntitet inntektsmeldingEntitet) {
        entityManager.persist(inntektsmeldingEntitet);
        entityManager.flush();
        return inntektsmeldingEntitet.getId();
    }

    public Optional<InntektsmeldingEntitet> hentSisteInntektsmelding(AktørIdEntitet aktørId, String arbeidsgiverIdent, LocalDate startDato) {
        var query = entityManager.createQuery("FROM InntektsmeldingEntitet where aktørId = :brukerAktørId and arbeidsgiverIdent = :arbeidsgiverIdent and startDato = :startDato order by opprettetTidspunkt desc", InntektsmeldingEntitet.class)
            .setParameter("brukerAktørId", aktørId)
            .setParameter("arbeidsgiverIdent", arbeidsgiverIdent)
            .setParameter("startDato", startDato)
            .setMaxResults(1);

        return query.getResultStream().findFirst();
    }

    public InntektsmeldingEntitet hentSisteInntektsmelding(int inntektsmeldingId) {
        var query = entityManager.createQuery("FROM InntektsmeldingEntitet where id = :id", InntektsmeldingEntitet.class)
            .setParameter("id", inntektsmeldingId);
        return query.getSingleResult();

    }
}
