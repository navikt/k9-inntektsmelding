-- Remove the default value so future inserts must explicitly provide a value
ALTER TABLE INNTEKTSMELDING ALTER COLUMN inntektsmelding_type DROP DEFAULT;
