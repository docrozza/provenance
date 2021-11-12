package io.docrozza.provenance.rdf

import com.complexible.stardog.Stardog
import com.complexible.stardog.api.Connection
import com.complexible.stardog.api.admin.AdminConnectionConfiguration
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import java.time.ZonedDateTime
import java.util.function.Supplier
import org.junit.jupiter.api.AfterEach as after
import org.junit.jupiter.api.BeforeEach as before

abstract class ProvenanceTest {

    @Suppress("unused")
    companion object {

        const val PROV = "http://www.w3.org/ns/prov#"
        const val DCTERMS = "http://purl.org/dc/terms/"
        const val FOAF = "http://xmlns.com/foaf/0.1/"
        const val EX = "http://www.example.org#"

        const val PREFIXES = "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n" +
                "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n" +
                "@prefix owl: <http://www.w3.org/2002/07/owl#> .\n" +
                "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n" +
                "@prefix prov: <$PROV> .\n" +
                "@prefix dcterms: <$DCTERMS> .\n" +
                "@prefix foaf: <$FOAF> .\n" +
                "@prefix : <$EX> ."

        private val DB = ProvenanceTest::class.simpleName
        private const val ADMIN = "admin"

        private lateinit var stardog: Stardog

        @BeforeAll
        @JvmStatic internal fun setup() {
            stardog = Stardog.builder().create()
        }

        @AfterAll
        @JvmStatic internal fun teardown() {
            stardog.shutdown()
        }
    }

    protected lateinit var connectionFactory: Supplier<Connection>

    @before fun setup() {
        val db = AdminConnectionConfiguration.toEmbeddedServer()
            .credentials(ADMIN, ADMIN)
            .connect()
            .use {
                if (it.list().contains(DB)) {
                    it.drop(DB)
                }

                it.newDatabase(DB).create()
            }

        connectionFactory = Supplier { db.connect() }
    }

    @after fun teardown() {
        AdminConnectionConfiguration.toEmbeddedServer()
            .credentials(ADMIN, ADMIN)
            .connect()
            .use {
                if (it.list().contains(DB)) {
                    it.drop(DB)
                }
            }
    }

    protected fun datetime(text: String) = ZonedDateTime.parse(text).toInstant()
}