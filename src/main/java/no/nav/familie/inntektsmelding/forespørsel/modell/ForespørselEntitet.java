package no.nav.familie.inntektsmelding.forespørsel.modell;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import no.nav.familie.inntektsmelding.koder.SakStatus;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;

@Entity(name = "ForespørselEntitet")
@Table(name = "FORESPOERSEL")
public class ForespørselEntitet {

    @Id
    @SequenceGenerator(name = "SEQ_FORESPOERSEL", sequenceName = "SEQ_FORESPOERSEL")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_FORESPOERSEL")
    private Long id;

    @Column(name = "uuid", nullable = false, updatable = false)
    private UUID uuid;

    @Column(name = "sak_id")
    private String sakId;

    @Enumerated(EnumType.STRING)
    @Column(name = "sak_status", nullable = false, updatable = false)
    private SakStatus sakStatus = SakStatus.UNDER_BEHANDLING;

    @Column(name = "oppgave_id")
    private String oppgaveId;

    @Column(name = "orgnr", nullable = false, updatable = false)
    private String organisasjonsnummer;

    @Column(name = "skjaeringstidspunkt", nullable = false, updatable = false)
    private LocalDate skjæringstidspunkt;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "aktørId", column = @Column(name = "bruker_aktoer_id", nullable = false, updatable = false)))
    private AktørIdEntitet aktørId;

    @Enumerated(EnumType.STRING)
    @Column(name = "ytelse_type", nullable = false, updatable = false)
    private Ytelsetype ytelseType;

    @Column(name = "fagsystem_saksnummer", nullable = false, updatable = false)
    private String fagsystemSaksnummer;

    @Column(name = "opprettet_tid", nullable = false, updatable = false)
    private final LocalDateTime opprettetTidspunkt = LocalDateTime.now();

    @Column(name = "endret_tid")
    private LocalDateTime endretTidspunkt;

    public ForespørselEntitet(String organisasjonsnummer,
                              LocalDate skjæringstidspunkt,
                              AktørIdEntitet aktørId,
                              Ytelsetype ytelseType,
                              String fagsystemSaksnummer) {
        this.uuid = UUID.randomUUID();
        this.organisasjonsnummer = organisasjonsnummer;
        this.skjæringstidspunkt = skjæringstidspunkt;
        this.aktørId = aktørId;
        this.ytelseType = ytelseType;
        this.fagsystemSaksnummer = fagsystemSaksnummer;
    }

    public ForespørselEntitet() {
    }

    @PreUpdate
    protected void onUpdate() {
        endretTidspunkt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getSakId() {
        return sakId;
    }

    void setSakId(String sakId) {
        this.sakId = sakId;
    }

    public SakStatus getSakStatus() {
        return sakStatus;
    }

    public void setSakStatus(SakStatus sakStatus) {
        this.sakStatus = sakStatus;
    }

    public String getOppgaveId() {
        return oppgaveId;
    }

    void setOppgaveId(String oppgaveId) {
        this.oppgaveId = oppgaveId;
    }

    public String getOrganisasjonsnummer() {
        return organisasjonsnummer;
    }

    public LocalDate getSkjæringstidspunkt() {
        return skjæringstidspunkt;
    }

    public AktørIdEntitet getAktørId() {
        return aktørId;
    }

    public Ytelsetype getYtelseType() {
        return ytelseType;
    }

    public String getFagsystemSaksnummer() {
        return fagsystemSaksnummer;
    }

    @Override
    public String toString() {
        return "ForespørselEntitet{" + "id=" + id + ", uuid=" + uuid + ", sakId=" + sakId + ", organisasjonsnummer=" + organisasjonsnummer
            + ", skjæringstidspunkt=" + skjæringstidspunkt + ", aktørId=" + aktørId + ", ytelseType=" + ytelseType + ", fagsystemSaksnummer="
            + fagsystemSaksnummer + '}';
    }
}
