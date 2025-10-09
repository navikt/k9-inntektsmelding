package no.nav.familie.inntektsmelding.forespørsel.modell;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.koder.ForespørselStatus;
import no.nav.familie.inntektsmelding.koder.ForespørselType;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.dto.PeriodeDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;

@SequenceGenerator(name = "GLOBAL_PK_SEQ_GENERATOR", sequenceName = "SEQ_GLOBAL_PK")
@Entity(name = "ForespørselEntitet")
@Table(name = "FORESPOERSEL")
public class ForespørselEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "GLOBAL_PK_SEQ_GENERATOR")
    private Long id;

    @Column(name = "uuid", nullable = false, updatable = false)
    private UUID uuid;

    @Column(name = "sak_id")
    private String sakId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ForespørselStatus status = ForespørselStatus.UNDER_BEHANDLING;

    @Enumerated(EnumType.STRING)
    @Column(name = "forespoersel_type", nullable = false, updatable = false)
    private ForespørselType forespørselType;

    @Column(name = "oppgave_id")
    private String oppgaveId;

    @Column(name = "orgnr", nullable = false, updatable = false)
    private String organisasjonsnummer;

    @Column(name = "skjaeringstidspunkt", nullable = false, updatable = false)
    private LocalDate skjæringstidspunkt;

    @Column(name = "forste_uttaksdato", updatable = false)
    private LocalDate førsteUttaksdato;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "aktørId", column = @Column(name = "bruker_aktoer_id", nullable = false, updatable = false)))
    private AktørIdEntitet aktørId;

    @Enumerated(EnumType.STRING)
    @Column(name = "ytelse_type", nullable = false, updatable = false)
    private Ytelsetype ytelseType;

    @Column(name = "saksnummer", updatable = false)
    private String saksnummer;

    @Column(name = "opprettet_tid", nullable = false, updatable = false)
    private final LocalDateTime opprettetTidspunkt = LocalDateTime.now();

    @Column(name = "endret_tid")
    private LocalDateTime endretTidspunkt;

    @OneToMany(mappedBy = "forespørsel", fetch = FetchType.LAZY)
    private List<InntektsmeldingEntitet> inntektsmeldinger;

    @OneToMany(mappedBy = "forespørsel", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EtterspurtPeriodeEntitet> etterspurtePerioder = new ArrayList<>();

    ForespørselEntitet() {
        this.uuid = UUID.randomUUID();
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

    public String getArbeidsgiverNotifikasjonSakId() {
        return sakId;
    }

    void setArbeidsgiverNotifikasjonSakId(String arbeidsgiverNotifikasjonSakId) {
        this.sakId = arbeidsgiverNotifikasjonSakId;
    }

    public ForespørselStatus getStatus() {
        return status;
    }

    public void setStatus(ForespørselStatus sakStatus) {
        this.status = sakStatus;
    }

    public ForespørselType getForespørselType() {
        return forespørselType;
    }

    public void setForespørselType(ForespørselType forespørselType) {
        this.forespørselType = forespørselType;
    }

    public Optional<String> getOppgaveId() {
        return Optional.ofNullable(oppgaveId);
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

    public Optional<String> getSaksnummer() {
        return Optional.ofNullable(saksnummer);
    }

    public LocalDateTime getOpprettetTidspunkt() {
        return opprettetTidspunkt;
    }

    public Optional<LocalDate> getFørsteUttaksdato() {
        return Optional.ofNullable(førsteUttaksdato);
    }

    public List<InntektsmeldingEntitet> getInntektsmeldinger() {
        if (inntektsmeldinger == null) {
            return List.of();
        }
        return inntektsmeldinger.stream()
            .sorted(Comparator.comparing(InntektsmeldingEntitet::getOpprettetTidspunkt).reversed())
            .toList();
    }

    public List<PeriodeDto> getEtterspurtePerioder() {
        return etterspurtePerioder.stream()
            .map(etterspurtPeriode -> new PeriodeDto(etterspurtPeriode.getFom(), etterspurtPeriode.getTom()))
            .toList();
    }

    public void setEtterspurtePerioder(List<PeriodeDto> etterspurtePerioder) {
        if (etterspurtePerioder == null) {
            throw new IllegalArgumentException("Etterspurte perioder kan ikke være null");
        }
        this.etterspurtePerioder.clear();
        etterspurtePerioder.forEach(this::leggTilEtterspurtPeriode);
    }

    private void leggTilEtterspurtPeriode(PeriodeDto etterspurtPeriode) {
        if (etterspurtePerioderInneholderNyPeriode(etterspurtPeriode)){
            // Dette burde ikke skje, validering skal være gjort i OppdaterForespørselDto
            throw new IllegalArgumentException("Etterspurt periode " + etterspurtPeriode + " finnes allerede i listen over etterspurte perioder: " + etterspurtePerioder);
        }
        etterspurtePerioder.add(new EtterspurtPeriodeEntitet(this, etterspurtPeriode));
    }

    private boolean etterspurtePerioderInneholderNyPeriode(PeriodeDto etterspurtPeriode) {
        return etterspurtePerioder.stream()
            .anyMatch(eksisterendePeriode -> eksisterendePeriode.getFom().equals(etterspurtPeriode.fom()) &&
                                             eksisterendePeriode.getTom().equals(etterspurtPeriode.tom()));
    }

    @Override
    public String toString() {
        return "ForespørselEntitet{" +
            "id=" + id +
            ", uuid=" + uuid +
            ", sakId=" + sakId +
            ", organisasjonsnummer=" + maskerId(organisasjonsnummer) +
            ", skjæringstidspunkt=" + skjæringstidspunkt +
            ", aktørId=" + maskerId(aktørId.getAktørId()) +
            ", ytelseType=" + ytelseType +
            ", saksnummer=" + saksnummer +
            ", status=" + status +
            ", forespørselType=" + forespørselType +
            ", etterspurtePerioder=" + etterspurtePerioder +
            '}';
    }

    private String maskerId(String id) {
        if (id == null) {
            return "";
        }
        var length = id.length();
        if (length <= 4) {
            return "*".repeat(length);
        }
        return "*".repeat(length - 4) + id.substring(length - 4);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final ForespørselEntitet kladd = new ForespørselEntitet();

        public Builder medOrganisasjonsnummer(String organisasjonsnummer) {
            kladd.organisasjonsnummer = organisasjonsnummer;
            return this;
        }

        public Builder medSkjæringstidspunkt(LocalDate skjæringstidspunkt) {
            kladd.skjæringstidspunkt = skjæringstidspunkt;
            return this;
        }

        public Builder medAktørId(AktørIdEntitet aktørId) {
            kladd.aktørId = aktørId;
            return this;
        }

        public Builder medYtelseType(Ytelsetype ytelseType) {
            kladd.ytelseType = ytelseType;
            return this;
        }

        public Builder medSaksnummer(String saksnummer) {
            kladd.saksnummer = saksnummer;
            return this;
        }

        public Builder medForespørselType(ForespørselType forespørselType) {
            kladd.forespørselType = forespørselType;
            return this;
        }

        public Builder medFørsteUttaksdato(LocalDate førsteUttaksdato) {
            kladd.førsteUttaksdato = førsteUttaksdato;
            return this;
        }

        public Builder medEtterspurtePerioder(List<PeriodeDto> etterspurtePerioder) {
            if (etterspurtePerioder != null && !etterspurtePerioder.isEmpty()) {
                etterspurtePerioder.forEach(kladd::leggTilEtterspurtPeriode);
            }
            return this;
        }


        public ForespørselEntitet build() {
            if (kladd.organisasjonsnummer == null || kladd.skjæringstidspunkt == null || kladd.aktørId == null || kladd.ytelseType == null) {
                throw new IllegalArgumentException("Mangler obligatoriske felt(er) for å bygge ForespørselEntitet");
            }

            return kladd;
        }
    }
}
