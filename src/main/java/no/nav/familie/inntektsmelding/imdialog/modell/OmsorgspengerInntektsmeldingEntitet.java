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

@Entity(name = "OmsorgspengerInntektsmeldingEntitet")
@Table(name = "OMSORGSPENGER_INNTEKTSMELDING")
public class OmsorgspengerInntektsmeldingEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "GLOBAL_PK_SEQ_GENERATOR")
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "inntektsmelding_id", nullable = false, updatable = false)
    private InntektsmeldingEntitet inntektsmelding;

    @Column(name = "har_utbetalt_pliktige_dager", nullable = false)
    private boolean harUtbetaltPliktigeDager;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "omsorgspenger")
    private List<FraværsPeriodeInntektsmeldingEntitet> fraværsPerioder = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "omsorgspenger")
    private List<DelvisFraværsDagInntektsmeldingEntitet> delvisFraværsDager = new ArrayList<>();


    public OmsorgspengerInntektsmeldingEntitet() {
        // Hibernate
    }

    public OmsorgspengerInntektsmeldingEntitet(boolean harUtbetaltPliktigeDager) {
        this.harUtbetaltPliktigeDager = harUtbetaltPliktigeDager;
    }

    public boolean isHarUtbetaltPliktigeDager() {
        return harUtbetaltPliktigeDager;
    }

    public List<FraværsPeriodeInntektsmeldingEntitet> getFraværsPerioder() {
        return fraværsPerioder;
    }

    public List<DelvisFraværsDagInntektsmeldingEntitet> getDelvisFraværsDager() {
        return delvisFraværsDager;
    }

    public void setInntektsmelding(InntektsmeldingEntitet inntektsmelding) {
        this.inntektsmelding = inntektsmelding;
    }

    private void leggTilFraværsPeriode(FraværsPeriodeInntektsmeldingEntitet fraværsPeriode) {
        fraværsPeriode.setOmsorgspenger(this);
        fraværsPerioder.add(fraværsPeriode);
    }

    private void leggTilDelvisFraværsDag(DelvisFraværsDagInntektsmeldingEntitet delvisFraværsDag) {
        delvisFraværsDag.setOmsorgspenger(this);
        delvisFraværsDager.add(delvisFraværsDag);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final OmsorgspengerInntektsmeldingEntitet kladd = new OmsorgspengerInntektsmeldingEntitet();

        public Builder medHarUtbetaltPliktigeDager(boolean harUtbetaltPliktigeDager) {
            kladd.harUtbetaltPliktigeDager = harUtbetaltPliktigeDager;
            return this;
        }

        public Builder medFraværsPerioder(List<FraværsPeriodeInntektsmeldingEntitet> fraværsPerioder) {
            if (fraværsPerioder != null) {
                fraværsPerioder.forEach(kladd::leggTilFraværsPeriode);
            }
            return this;
        }

        public Builder medDelvisFraværsDager(List<DelvisFraværsDagInntektsmeldingEntitet> delvisFraværsDager) {
            if (delvisFraværsDager != null) {
                delvisFraværsDager.forEach(kladd::leggTilDelvisFraværsDag);
            }
            return this;
        }

        public OmsorgspengerInntektsmeldingEntitet build() {
            return kladd;
        }
    }

    @Override
    public String toString() {
        return "OmsorgspengerInntektsmeldingEntitet{" +
            "harUtbetaltPliktigeDager=" + harUtbetaltPliktigeDager +
            ", fraværsPerioder=" + fraværsPerioder +
            ", delvisFraværsDager=" + delvisFraværsDager +
            '}';
    }
}
