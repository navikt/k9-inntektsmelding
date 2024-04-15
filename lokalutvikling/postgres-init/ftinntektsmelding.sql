CREATE DATABASE ftinntektsmelding_unit;
CREATE USER ftinntektsmelding_unit WITH PASSWORD 'ftinntektsmelding_unit';
GRANT ALL ON DATABASE ftinntektsmelding_unit TO ftinntektsmelding_unit;
ALTER DATABASE ftinntektsmelding_unit SET timezone TO 'Europe/Oslo';
ALTER DATABASE ftinntektsmelding_unit OWNER TO ftinntektsmelding_unit;

CREATE DATABASE ftinntektsmelding;
CREATE USER ftinntektsmelding WITH PASSWORD 'ftinntektsmelding';
GRANT ALL ON DATABASE ftinntektsmelding TO ftinntektsmelding;
ALTER DATABASE ftinntektsmelding SET timezone TO 'Europe/Oslo';
ALTER DATABASE ftinntektsmelding OWNER TO ftinntektsmelding;
