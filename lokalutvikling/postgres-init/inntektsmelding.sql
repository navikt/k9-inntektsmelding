CREATE DATABASE inntektsmelding_unit;
CREATE USER inntektsmelding_unit WITH PASSWORD 'inntektsmelding_unit';
GRANT ALL ON DATABASE inntektsmelding_unit TO inntektsmelding_unit;
ALTER DATABASE inntektsmelding_unit SET timezone TO 'Europe/Oslo';
ALTER DATABASE inntektsmelding_unit OWNER TO inntektsmelding_unit;

CREATE DATABASE inntektsmelding;
CREATE USER inntektsmelding WITH PASSWORD 'inntektsmelding';
GRANT ALL ON DATABASE inntektsmelding TO inntektsmelding;
ALTER DATABASE inntektsmelding SET timezone TO 'Europe/Oslo';
ALTER DATABASE inntektsmelding OWNER TO inntektsmelding;
