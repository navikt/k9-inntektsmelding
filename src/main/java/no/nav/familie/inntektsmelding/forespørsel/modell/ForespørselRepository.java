package no.nav.familie.inntektsmelding.forespørsel.modell;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.familie.inntektsmelding.koder.SakStatus;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.AktørIdDto;
import no.nav.familie.inntektsmelding.typer.ArbeidsgiverDto;
import no.nav.familie.inntektsmelding.typer.SaksnummerDto;

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
        var forespørselOpt = hentForespørsel(forespørselUUID);
        if (forespørselOpt.isPresent()) {
            var forespørsel = forespørselOpt.get();
            forespørsel.setOppgaveId(oppgaveId);
            entityManager.persist(forespørsel);
            entityManager.flush();
        }
    }

    public void oppdaterSakId(UUID forespørselUUID, String sakId) {
        var forespørselOpt = hentForespørsel(forespørselUUID);
        if (forespørselOpt.isPresent()) {
            var forespørsel = forespørselOpt.get();
            forespørsel.setSakId(sakId);
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

    public void ferdigstillSak(String sakId) {
        var query = entityManager.createQuery("FROM ForespørselEntitet where sakId = :SAK_ID", ForespørselEntitet.class)
            .setParameter("SAK_ID", sakId);
        var resultList = query.getResultList();

        resultList.forEach(f -> {
            f.setSakStatus(SakStatus.FERDIG);
            entityManager.persist(f);
        });

        entityManager.flush();
    }


    public List<ForespørselEntitet> hentForespørsler(SaksnummerDto saksnummer) {
        var query = entityManager.createQuery("FROM ForespørselEntitet f where fagsystemSaksnummer = :saksnr", ForespørselEntitet.class)
            .setParameter("saksnr", saksnummer.getSaksnr());
        return query.getResultList();
    }


    public Optional<ForespørselEntitet> finnForespørsel(AktørIdDto aktørId, ArbeidsgiverDto arbeidsgiverIdent, LocalDate startdato) {
        var query = entityManager.createQuery("FROM ForespørselEntitet where brukerAktørId = :brukerAktørId and organisasjonsnummer = :arbeidsgiverIdent "
                + "and skjæringstidspunkt = :skjæringstidspunkt", ForespørselEntitet.class)
            .setParameter("brukerAktørId", aktørId.id())
            .setParameter("arbeidsgiverIdent", arbeidsgiverIdent.ident())
            .setParameter("skjæringstidspunkt", startdato);

        var resultList = query.getResultList();
        if (resultList.isEmpty()) {
            return Optional.empty();
        } else if (resultList.size() > 1) {
            var feilmelding = String.format("Forventet å finne kun en forespørsel for gitt aktør %s, arbeidsgiver %s og startdato %s", aktørId, arbeidsgiverIdent, startdato);
            throw new IllegalStateException(feilmelding);
        } else {
            return Optional.of(resultList.getFirst());
        }
    }

    public Optional<ForespørselEntitet> finnÅpenForespørsel(String aktørId, Ytelsetype ytelsetype, String arbeidsgiverIdent, LocalDate startdato) {
        var query = entityManager.createQuery("FROM ForespørselEntitet where sakStatus='UNDER_BEHANDLING' "
                + "and brukerAktørId = :brukerAktørId "
                + "and organisasjonsnummer = :arbeidsgiverIdent "
                + "and skjæringstidspunkt = :skjæringstidspunkt "
                + "and ytelseType = :ytelsetype", ForespørselEntitet.class)
            .setParameter("brukerAktørId", aktørId)
            .setParameter("arbeidsgiverIdent", arbeidsgiverIdent)
            .setParameter("skjæringstidspunkt", startdato)
            .setParameter("ytelsetype", ytelsetype);

        var resultList = query.getResultList();
        if (resultList.isEmpty()) {
            return Optional.empty();
        } else if (resultList.size() > 1) {
            throw new IllegalStateException("Forventet å finne kun en forespørsel for gitt id arbeidsgiver og startdato" + aktørId + arbeidsgiverIdent + startdato);
        } else {
            return Optional.of(resultList.getFirst());
        }
    }
}
