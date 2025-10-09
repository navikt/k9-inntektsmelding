-- Add forespoersel_type column with default value 'BESTILT_AV_FAGSYSTEM'
ALTER TABLE FORESPOERSEL ADD COLUMN forespoersel_type VARCHAR(50) DEFAULT 'BESTILT_AV_FAGSYSTEM';

-- Make the column NOT NULL (all existing rows will already have 'BESTILT_AV_FAGSYSTEM' due to default)
ALTER TABLE FORESPOERSEL ALTER COLUMN forespoersel_type SET NOT NULL;

COMMENT ON COLUMN FORESPOERSEL.forespoersel_type IS 'Hva slags type forespørsel det er';
