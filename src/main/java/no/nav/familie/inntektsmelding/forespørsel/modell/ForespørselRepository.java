package no.nav.familie.inntektsmelding.forespørsel.modell;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.koder.ForespørselStatus;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.dto.SaksnummerDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;

@Dependent
public class ForespørselRepository {

    private EntityManager entityManager;
    private static final Logger LOG = LoggerFactory.getLogger(ForespørselRepository.class);

    public ForespørselRepository() {
    }

    @Inject
    public ForespørselRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }


    public UUID lagreForespørsel(LocalDate skjæringstidspunkt, Ytelsetype ytelsetype, String aktørId, String orgnummer, String saksnummer,
                                 LocalDate førsteUttaksdato) {
        var forespørselEntitet = new ForespørselEntitet(orgnummer,
            skjæringstidspunkt,
            new AktørIdEntitet(aktørId),
            ytelsetype,
            saksnummer,
            førsteUttaksdato);
        LOG.info("ForespørselRepository: lagrer forespørsel entitet: {}", forespørselEntitet);
        entityManager.persist(forespørselEntitet);
        entityManager.flush();
        return forespørselEntitet.getUuid();
    }

    public void oppdaterOppgaveId(UUID forespørselUUID, String oppgaveId) {
        var forespørselOpt = hentForespørsel(forespørselUUID);
        if (forespørselOpt.isPresent()) {
            var forespørsel = forespørselOpt.get();
            forespørsel.setOppgaveId(oppgaveId);
            entityManager.persist(forespørsel);
            entityManager.flush();
        }
    }

    public void oppdaterArbeidsgiverNotifikasjonSakId(UUID forespørselUUID, String arbeidsgiverNotifikasjonSakId) {
        var forespørselOpt = hentForespørsel(forespørselUUID);
        if (forespørselOpt.isPresent()) {
            var forespørsel = forespørselOpt.get();
            forespørsel.setArbeidsgiverNotifikasjonSakId(arbeidsgiverNotifikasjonSakId);
            entityManager.persist(forespørsel);
            entityManager.flush();
        }
    }

    public Optional<ForespørselEntitet> hentForespørsel(UUID uuid) {
        var query = entityManager.createQuery("FROM ForespørselEntitet where uuid = :foresporselUUID", ForespørselEntitet.class)
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

    public void ferdigstillForespørsel(String arbeidsgiverNotifikasjonSakId) {
        var query = entityManager.createQuery("FROM ForespørselEntitet where sakId = :SAK_ID", ForespørselEntitet.class)
            .setParameter("SAK_ID", arbeidsgiverNotifikasjonSakId);
        var resultList = query.getResultList();

        resultList.forEach(f -> {
            f.setStatus(ForespørselStatus.FERDIG);
            entityManager.persist(f);
        });
        entityManager.flush();
    }

    public void settForespørselTilUtgått(String arbeidsgiverNotifikasjonSakId) {
        var query = entityManager.createQuery("FROM ForespørselEntitet where sakId = :SAK_ID", ForespørselEntitet.class)
            .setParameter("SAK_ID", arbeidsgiverNotifikasjonSakId);
        var resultList = query.getResultList();

        resultList.forEach(f -> {
            f.setStatus(ForespørselStatus.UTGÅTT);
            entityManager.persist(f);
        });
        entityManager.flush();
    }


    public List<ForespørselEntitet> hentForespørsler(SaksnummerDto saksnummer) {
        var query = entityManager.createQuery("FROM ForespørselEntitet f where saksnummer = :saksnummer", ForespørselEntitet.class)
            .setParameter("saksnummer", saksnummer.saksnr());
        return query.getResultList();
    }

    public List<ForespørselEntitet> finnÅpenForespørsel(SaksnummerDto saksnummer) {
        var query = entityManager.createQuery("FROM ForespørselEntitet where status=:status " + "and saksnummer=:saksnummer",
                ForespørselEntitet.class)
            .setParameter("saksnummer", saksnummer.saksnr())
            .setParameter("status", ForespørselStatus.UNDER_BEHANDLING);
        return query.getResultList();
    }

    public List<ForespørselEntitet> finnForespørslerForAktørId(AktørIdEntitet aktørId, Ytelsetype ytelsetype) {
        var query = entityManager.createQuery("FROM ForespørselEntitet where aktørId=:aktørId "
                    + "and status !=:utgått and ytelseType=:ytelseType",
                ForespørselEntitet.class)
            .setParameter("aktørId", aktørId)
            .setParameter("utgått", ForespørselStatus.UTGÅTT)
            .setParameter("ytelseType", ytelsetype);
        return query.getResultList();
    }

    public List<ForespørselEntitet> finnForespørslerUnderBehandling(AktørIdEntitet aktørId, Ytelsetype ytelsetype, String orgnr) {
        var query = entityManager.createQuery("FROM ForespørselEntitet where aktørId=:aktørId "
                    + "and status=:underBehandling and ytelseType=:ytelseType and organisasjonsnummer=:orgnr",
                ForespørselEntitet.class)
            .setParameter("aktørId", aktørId)
            .setParameter("underBehandling", ForespørselStatus.UNDER_BEHANDLING)
            .setParameter("ytelseType", ytelsetype)
            .setParameter("orgnr", orgnr);
        return query.getResultList();
    }

    public List<ForespørselEntitet> finnAlleForespørsler(AktørIdEntitet aktørId, Ytelsetype ytelsetype, String orgnr) {
        var query = entityManager.createQuery("FROM ForespørselEntitet where aktørId=:aktørId "
                    + "and ytelseType=:ytelseType and organisasjonsnummer=:orgnr",
                ForespørselEntitet.class)
            .setParameter("aktørId", aktørId)
            .setParameter("ytelseType", ytelsetype)
            .setParameter("orgnr", orgnr);
        return query.getResultList();
    }
}
