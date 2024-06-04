package no.nav.familie.inntektsmelding.imdialog.modell;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity(name = "InntektsmeldingEntitet")
@Table(name = "INNTEKTSMELDING")
public class InntektsmeldingEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_INNTEKTSMELDING")
    private Long id;

    @Embedded
    private AktørIdEntitet aktørId;

    @Enumerated(EnumType.STRING)
    @Column(name = "ytelse_type")
    private Ytelsetype ytelsetype;

    @Column(name = "arbeidsgiver_ident")
    private String arbeidsgiverIdent;

    @OneToOne(cascade = CascadeType.ALL, mappedBy = "inntektsmelding")
    private KontaktpersonEntitet kontaktperson;

    @Column(name = "start_dato")
    private LocalDate startDato;

    @Column(name = "maaned_inntekt")
    private BigDecimal månedInntekt;

    @Column(name = "opprettet_tid", nullable = false, updatable = false)
    private LocalDateTime opprettetTidspunkt = LocalDateTime.now();

    @OneToMany(cascade = CascadeType.ALL, mappedBy ="inntektsmelding")
    private List<RefusjonPeriodeEntitet> refusjonsPeriode= new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy ="inntektsmelding")
    private List<NaturalytelseEntitet> naturalYtelse= new ArrayList<>();

    public InntektsmeldingEntitet() {
        // Hibernate
    }

    public AktørIdEntitet getAktørId() {
        return aktørId;
    }

    public Ytelsetype getYtelsetype() {
        return ytelsetype;
    }

    public String getArbeidsgiverIdent() {
        return arbeidsgiverIdent;
    }

    public LocalDate getStartDato() {
        return startDato;
    }

    public BigDecimal getMånedInntekt() {
        return månedInntekt;
    }

    public List<RefusjonPeriodeEntitet> getRefusjonsPeriode() {
        return refusjonsPeriode;
    }

    public List<NaturalytelseEntitet> getNaturalYtelse() {
        return naturalYtelse;
    }

    public KontaktpersonEntitet getKontaktperson() {
        return kontaktperson;
    }

    public LocalDateTime getOpprettetTidspunkt() {
        return opprettetTidspunkt;
    }

    public static class InntektsmeldingEntitetBuilder {
        private InntektsmeldingEntitet kladd = new InntektsmeldingEntitet();

        public InntektsmeldingEntitetBuilder() {

        }

        public InntektsmeldingEntitetBuilder medAktørId(AktørIdEntitet aktørId) {
            kladd.aktørId = aktørId;
            return this;
        }

        public InntektsmeldingEntitetBuilder medYtelsetype(Ytelsetype ytelsetype) {
            kladd.ytelsetype = ytelsetype;
            return this;
        }

        public InntektsmeldingEntitetBuilder medArbeidsgiverIdent(String arbeidsgiverIdent) {
            kladd.arbeidsgiverIdent = arbeidsgiverIdent;
            return this;
        }

        public InntektsmeldingEntitetBuilder medKontaktperson(KontaktpersonEntitet kontaktpersonEntitet) {
            kladd.kontaktperson = kontaktpersonEntitet;
            return this;
        }

        public InntektsmeldingEntitetBuilder medStartDato(LocalDate startDato) {
            kladd.startDato = startDato;
            return this;
        }

        public InntektsmeldingEntitetBuilder medMånedInntekt(BigDecimal månedInntekt) {
            kladd.månedInntekt = månedInntekt;
            return this;
        }

        public InntektsmeldingEntitetBuilder medRefusjonsPeriode(List<RefusjonPeriodeEntitet> refusjonsPeriode) {
            kladd.refusjonsPeriode = refusjonsPeriode;
            return this;
        }

        public InntektsmeldingEntitetBuilder medNaturalYtelse(List<NaturalytelseEntitet> naturalYtelse) {
            kladd.naturalYtelse = naturalYtelse;
            return this;
        }

        public InntektsmeldingEntitet build() {
            return kladd;
        }

    }
}
