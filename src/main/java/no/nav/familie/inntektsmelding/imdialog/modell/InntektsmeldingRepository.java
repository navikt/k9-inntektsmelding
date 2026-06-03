package no.nav.familie.inntektsmelding.imdialog.modell;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import no.nav.familie.inntektsmelding.koder.ForespørselType;
import no.nav.familie.inntektsmelding.koder.InntektsmeldingType;
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

    public Optional<InntektsmeldingEntitet> hentInntektsmeldingForUuid(UUID uuid) {
        var query = entityManager.createQuery(
                "FROM InntektsmeldingEntitet where uuid = :uuid",
                InntektsmeldingEntitet.class)
            .setParameter("uuid", uuid);
        return query.getResultStream().findFirst();
    }

    public List<InntektsmeldingEntitet> hentInntektsmeldingerFraFilter(String orgnr,
                                                                       AktørIdEntitet aktørId,
                                                                       Ytelsetype ytelsetype,
                                                                       LocalDate fom,
                                                                       LocalDate tom) {
        var jpql = new StringBuilder("FROM InntektsmeldingEntitet where arbeidsgiverIdent = :orgnr");
        if (aktørId != null) {
            jpql.append(" and aktørId = :aktørId");
        }
        if (ytelsetype != null) {
            jpql.append(" and ytelsetype = :ytelsetype");
        }
        if (fom != null) {
            jpql.append(" and startDato >= :fom");
        }
        if (tom != null) {
            jpql.append(" and startDato <= :tom");
        }
        jpql.append(" order by opprettetTidspunkt desc");

        var query = entityManager.createQuery(jpql.toString(), InntektsmeldingEntitet.class)
            .setParameter("orgnr", orgnr);
        if (aktørId != null) {
            query.setParameter("aktørId", aktørId);
        }
        if (ytelsetype != null) {
            query.setParameter("ytelsetype", ytelsetype);
        }
        if (fom != null) {
            query.setParameter("fom", fom);
        }
        if (tom != null) {
            query.setParameter("tom", tom);
        }
        return query.getResultList();
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

    public long tellAntallOmsorgspengerRefusjonInntektsmeldinger() {
        var query = entityManager.createQuery(
                "SELECT COUNT(i) FROM InntektsmeldingEntitet i WHERE i.inntektsmeldingType = :type", Long.class)
            .setParameter("type", InntektsmeldingType.OMSORGSPENGER_REFUSJON);
        return query.getSingleResult();
    }

    public long tellAntallUtledetOmsorgspengerRefusjonInntektsmeldinger() {
        var query = entityManager.createQuery(
                "SELECT COUNT(i) FROM InntektsmeldingEntitet i WHERE i.ytelsetype = :ytelsetype AND i.månedRefusjon IS NOT NULL", Long.class)
            .setParameter("ytelsetype", Ytelsetype.OMSORGSPENGER);
        return query.getSingleResult();
    }

    public long tellAntallUtledetOmsorgspengerRefusjonInntektsmeldingerFraForespørsel() {
        var query = entityManager.createQuery(
                "SELECT COUNT(i) FROM InntektsmeldingEntitet i WHERE i.forespørsel.forespørselType = :forespørselType", Long.class)
            .setParameter("forespørselType", ForespørselType.OMSORGSPENGER_REFUSJON);
        return query.getSingleResult();
    }
}
