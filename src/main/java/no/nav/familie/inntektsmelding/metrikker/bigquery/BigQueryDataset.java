package no.nav.familie.inntektsmelding.metrikker.bigquery;

/**
 * Sørg for at datasetName samsvarer med bigQueryDatasets.name i NAIS config.
 */
public enum BigQueryDataset {
    K9_INNTEKTSMELDING_STATISTIKK_DATASET("k9_inntektsmelding_statistikk_dataset");

    private final String datasetNavn;

    BigQueryDataset(String datasetNavn) {
        this.datasetNavn = datasetNavn;
    }

    public String getDatasetNavn() {
        return datasetNavn;
    }
}
