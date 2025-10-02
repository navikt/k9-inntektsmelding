package no.nav.familie.inntektsmelding.imdialog.modell;

import java.util.List;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import no.nav.familie.inntektsmelding.koder.Ytelsetype;
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

    public InntektsmeldingEntitet hentInntektsmelding(long inntektsmeldingId) {
        return entityManager.find(InntektsmeldingEntitet.class, inntektsmeldingId);
    }

    public List<InntektsmeldingEntitet> hentInntektsmeldingerForÅr(AktørIdEntitet aktørId, String arbeidsgiverIdent, int år, Ytelsetype ytelsetype) {
        var query = entityManager.createQuery(
                "FROM InntektsmeldingEntitet where aktørId = :brukerAktørId and ytelsetype = :ytelsetype and arbeidsgiverIdent = :arbeidsgiverIdent and EXTRACT(YEAR FROM startDato) = :år order by opprettetTidspunkt desc",
                InntektsmeldingEntitet.class)
            .setParameter("brukerAktørId", aktørId)
            .setParameter("arbeidsgiverIdent", arbeidsgiverIdent)
            .setParameter("ytelsetype", ytelsetype)
            .setParameter("år", år);

        return query.getResultList();
    }

    public long tellAntallInntektsmeldinger() {
        var query = entityManager.createQuery("SELECT COUNT(i) FROM InntektsmeldingEntitet i", Long.class);
        return query.getSingleResult();
    }
}
