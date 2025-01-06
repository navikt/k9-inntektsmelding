package no.nav.familie.inntektsmelding.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import no.nav.foreldrepenger.konfig.Environment;
import no.nav.vedtak.felles.testutilities.db.EntityManagerAwareExtension;

public class JpaExtension extends EntityManagerAwareExtension {
    private static final Logger LOG = LoggerFactory.getLogger(JpaExtension.class);
    private static final String TEST_DB_CONTAINER = Environment.current().getProperty("testcontainer.test.db", String.class, "postgres:17-alpine");
    private static final PostgreSQLContainer TEST_DATABASE;

    static {
        try {
            TEST_DATABASE = new PostgreSQLContainer<>(DockerImageName.parse(TEST_DB_CONTAINER))
                .withReuse(true);
            TEST_DATABASE.start();
            TestDatabaseInit.settOppDatasourceOgMigrer(TEST_DATABASE.getJdbcUrl(), TEST_DATABASE.getUsername(), TEST_DATABASE.getPassword());
        } catch (Exception e) {
            LOG.error("FEIL I TESTCONTAINER!",e);
            throw new RuntimeException(e);
        }
    }
}
