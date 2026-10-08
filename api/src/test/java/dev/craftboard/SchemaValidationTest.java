package dev.craftboard;

import org.junit.jupiter.api.Test;

class SchemaValidationTest extends IntegrationTest {

    @Test
    void migrationsMatchEntities() {
        // ddl-auto=validate fails the context if a migration and an entity disagree.
    }
}
