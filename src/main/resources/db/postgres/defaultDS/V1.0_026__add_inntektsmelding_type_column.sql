-- Add inntektsmelding_type column with default value 'ORDINÆR'
ALTER TABLE INNTEKTSMELDING ADD COLUMN inntektsmelding_type VARCHAR(255) DEFAULT 'ORDINÆR';

-- Make the column NOT NULL (all existing rows will already have 'ORDINÆR' due to default)
ALTER TABLE INNTEKTSMELDING ALTER COLUMN inntektsmelding_type SET NOT NULL;

-- Add index for query performance on inntektsmelding_type
CREATE INDEX idx_inntektsmelding_type ON INNTEKTSMELDING(inntektsmelding_type);

COMMENT ON COLUMN INNTEKTSMELDING.inntektsmelding_type IS 'Hva slags type inntektsmelding det er';
