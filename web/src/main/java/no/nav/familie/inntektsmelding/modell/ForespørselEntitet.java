package no.nav.familie.inntektsmelding.modell;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity(name = "ForespørselEntitet")
@Table(name = "FORESPOERSEL")
public class ForespørselEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_FORESPOERSEL")
    private Long id;

    @Column(name = "uuid", nullable = false, updatable=false)
    private UUID uuid;

    @Column(name = "sak_id")
    private String sakId;

    @Column(name = "oppgave_id")
    private String oppgaveId;

    @Column(name = "orgnr", nullable = false, updatable=false)
    private String organisasjonsnummer;

    @Column(name = "skjaeringstidspunkt", nullable = false, updatable=false)
    private LocalDate skjæringstidspunkt;

    @Column(name = "bruker_aktoer_id", nullable = false, updatable=false)
    private String brukerAktørId;

    @Column(name = "ytelse_type", nullable = false, updatable=false)
    private String ytelseType; // TODO: Map til enum

    @Column(name = "fagsystem_saksnummer", nullable = false, updatable=false)
    private String fagsystemSaksnummer;

    @Column(name = "opprettet_av", nullable = false, updatable=false)
    protected String opprettetAv;

    @Column(name = "opprettet_tid", nullable = false, updatable=false)
    private LocalDateTime opprettetTidspunkt;

    @Column(name = "endret_av")
    private String endretAv;

    @Column(name = "endret_tid")
    private LocalDateTime endretTidspunkt;

    public ForespørselEntitet(String organisasjonsnummer,
                              LocalDate skjæringstidspunkt,
                              String brukerAktørId,
                              String ytelseType,
                              String fagsystemSaksnummer) {
        this.organisasjonsnummer = organisasjonsnummer;
        this.skjæringstidspunkt = skjæringstidspunkt;
        this.brukerAktørId = brukerAktørId;
        this.ytelseType = ytelseType;
        this.fagsystemSaksnummer = fagsystemSaksnummer;
    }

    public ForespørselEntitet() {
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

    public String getOppgaveId() {
        return oppgaveId;
    }

    public String getOrganisasjonsnummer() {
        return organisasjonsnummer;
    }

    public LocalDate getSkjæringstidspunkt() {
        return skjæringstidspunkt;
    }

    public String getBrukerAktørId() {
        return brukerAktørId;
    }

    public String getYtelseType() {
        return ytelseType;
    }

    public String getFagsystemSaksnummer() {
        return fagsystemSaksnummer;
    }
}
