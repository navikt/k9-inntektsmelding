package no.nav.familie.inntektsmelding.imdialog.modell;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity(name = "OmsorgspengerEntitet")
@Table(name = "OMSORGSPENGER")
public class OmsorgspengerEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "GLOBAL_PK_SEQ_GENERATOR")
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "inntektsmelding_id", nullable = false, updatable = false)
    private InntektsmeldingEntitet inntektsmelding;

    @Column(name = "har_utbetalt_pliktige_dager", nullable = false)
    private boolean harUtbetaltPliktigeDager;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "omsorgspenger")
    private List<FraværsPeriodeEntitet> fraværsPerioder = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "omsorgspenger")
    private List<DelvisFraværsPeriodeEntitet> delvisFraværsPerioder = new ArrayList<>();


    OmsorgspengerEntitet() {
        // Hibernate
    }

    public OmsorgspengerEntitet(boolean harUtbetaltPliktigeDager) {
        this.harUtbetaltPliktigeDager = harUtbetaltPliktigeDager;
    }

    public boolean isHarUtbetaltPliktigeDager() {
        return harUtbetaltPliktigeDager;
    }

    public List<FraværsPeriodeEntitet> getFraværsPerioder() {
        return List.copyOf(fraværsPerioder);
    }

    public List<DelvisFraværsPeriodeEntitet> getDelvisFraværsPerioder() {
        return List.copyOf(delvisFraværsPerioder);
    }

    public void setInntektsmelding(InntektsmeldingEntitet inntektsmelding) {
        this.inntektsmelding = inntektsmelding;
    }

    private void leggTilFraværsPeriode(FraværsPeriodeEntitet fraværsPeriode) {
        fraværsPeriode.setOmsorgspenger(this);
        fraværsPerioder.add(fraværsPeriode);
    }

    private void leggTilDelvisFraværsPeriode(DelvisFraværsPeriodeEntitet delvisFraværsPeriode) {
        delvisFraværsPeriode.setOmsorgspenger(this);
        delvisFraværsPerioder.add(delvisFraværsPeriode);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final OmsorgspengerEntitet kladd = new OmsorgspengerEntitet();

        public Builder medHarUtbetaltPliktigeDager(boolean harUtbetaltPliktigeDager) {
            kladd.harUtbetaltPliktigeDager = harUtbetaltPliktigeDager;
            return this;
        }

        public Builder medFraværsPerioder(List<FraværsPeriodeEntitet> fraværsPerioder) {
            if (fraværsPerioder != null) {
                fraværsPerioder.forEach(kladd::leggTilFraværsPeriode);
            }
            return this;
        }

        public Builder medDelvisFraværsPerioder(List<DelvisFraværsPeriodeEntitet> delvisFraværsPerioder) {
            if (delvisFraværsPerioder != null) {
                delvisFraværsPerioder.forEach(kladd::leggTilDelvisFraværsPeriode);
            }
            return this;
        }

        public OmsorgspengerEntitet build() {
            return kladd;
        }
    }

    @Override
    public String toString() {
        return "OmsorgspengerEntitet{" +
            "harUtbetaltPliktigeDager=" + harUtbetaltPliktigeDager +
            ", fraværsPerioder=" + fraværsPerioder +
            ", delvisFraværsPerioder=" + delvisFraværsPerioder +
            '}';
    }
}
