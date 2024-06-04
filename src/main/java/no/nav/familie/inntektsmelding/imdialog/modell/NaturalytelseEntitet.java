package no.nav.familie.inntektsmelding.imdialog.modell;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import no.nav.familie.inntektsmelding.koder.Naturalytelsetype;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity(name = "NaturalytelseEntitet")
@Table(name = "NATURALYTELSE")
public class NaturalytelseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_NATURALYTELSE")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "inntektsmelding_id", nullable = false, updatable = false)
    private InntektsmeldingEntitet inntektsmelding;

    @Column(name = "fom", nullable = false)
    private LocalDate fom;

    @Column(name = "tom", nullable = false)
    private LocalDate tom;

    @Column(name = "type", nullable = false)
    private Naturalytelsetype type;

    @Column(name = "beloep", nullable = false)
    private BigDecimal beløp;

    @Column(name = "erBortfalt", nullable = false)
    private Boolean erBortfalt;

    public NaturalytelseEntitet() {
        // Hibernate
    }
}
