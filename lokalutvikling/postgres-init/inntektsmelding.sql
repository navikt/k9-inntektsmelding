CREATE DATABASE inntektsmelding;
CREATE USER inntektsmelding WITH PASSWORD 'inntektsmelding';
GRANT ALL ON DATABASE inntektsmelding TO inntektsmelding;
ALTER DATABASE inntektsmelding SET timezone TO 'Europe/Oslo';
ALTER DATABASE inntektsmelding OWNER TO inntektsmelding;
