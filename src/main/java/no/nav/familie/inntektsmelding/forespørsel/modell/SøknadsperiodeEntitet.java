package no.nav.familie.inntektsmelding.forespørsel.modell;

import java.util.Objects;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import no.nav.familie.inntektsmelding.typer.entitet.IntervallEntitet;

@Entity(name = "SøknadsperiodeEntitet")
@Table(name = "SOKNADSPERIODE")
public class SøknadsperiodeEntitet {

    @Id
    @SequenceGenerator(name = "SEQ_SOKNADSPERIODE", sequenceName = "SEQ_SOKNADSPERIODE")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SOKNADSPERIODE")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "forespoersel_id", nullable = false, updatable = false)
    private ForespørselEntitet forespørsel;

    @Embedded
    private IntervallEntitet periode;

    public SøknadsperiodeEntitet() {
        // Hibernate
    }

    public SøknadsperiodeEntitet(IntervallEntitet periode) {
        this.periode = periode;
    }

    void setForespørsel(ForespørselEntitet forespørsel) {
        this.forespørsel = forespørsel;
    }

    public IntervallEntitet getPeriode() {
        return periode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        SøknadsperiodeEntitet that = (SøknadsperiodeEntitet) o;
        return Objects.equals(periode, that.periode);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(periode);
    }

    @Override
    public String toString() {
        return "SøknadsperiodeEntitet{" +
            "periode=" + periode +
            '}';
    }
}
