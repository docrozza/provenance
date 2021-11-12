package io.docrozza.provenance.rdf

import com.stardog.stark.Values
import com.stardog.stark.vocabs.XSD
import io.docrozza.provenance.rdf.StardogUtils.iri
import io.docrozza.provenance.rdf.StardogUtils.toInstant
import io.docrozza.provenance.rdf.StardogUtils.toLiteral
import java.time.Instant
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertFails
import org.junit.jupiter.api.Test as test

class StardogUtilsTest {

    @test fun iri() {
        assertEquals(Values.iri("urn:test:thing"), "urn:test:".iri("thing"))
        assertEquals(Values.iri("urn:test:", "thing"), "urn:test:".iri("thing"))
        assertEquals(Values.iri("urn:test:stuff#thing"), "urn:test:stuff#".iri("thing"))
    }

    @test fun instantToRdf() {
        val zone = ZoneId.of("Europe/London")

        var instant = Instant.ofEpochMilli(1583064000000)
        assertEquals("2020-03-01T12:00:00Z", toLiteral(instant).label())
        assertEquals("2020-03-01T12:00:00Z", toLiteral(instant, zone).label())

        instant = Instant.ofEpochMilli(1585738800000)
        assertEquals("2020-04-01T11:00:00Z", toLiteral(instant).label())
        assertEquals("2020-04-01T12:00:00+01:00", toLiteral(instant, zone).label())

        instant = Instant.ofEpochMilli(1585738800042)
        assertEquals("2020-04-01T11:00:00.042Z", toLiteral(instant).label())
        assertEquals("2020-04-01T12:00:00.042+01:00", toLiteral(instant, zone).label())
    }

    @test fun dateToInstant() {
        val zone = ZoneId.of("Europe/London")
        assertEquals(1583020800000, toInstant(Values.literal("2020-03-01", XSD.DATE)).toEpochMilli())
        assertEquals(1585699200000, toInstant(Values.literal("2020-04-01", XSD.DATE)).toEpochMilli())
        assertEquals(1585695600000, toInstant(Values.literal("2020-04-01", XSD.DATE), zone).toEpochMilli())
    }

    @test fun dateTimeToInstant() {
        val zone = ZoneId.of("Europe/London")
        assertEquals(1583064000000, toInstant(Values.literal("2020-03-01T12:00:00.000Z", XSD.DATETIME)).toEpochMilli())
        assertEquals(1585742400000, toInstant(Values.literal("2020-04-01T12:00:00.000Z", XSD.DATETIME)).toEpochMilli())
        assertEquals(1585742400000, toInstant(Values.literal("2020-04-01T12:00:00.000Z", XSD.DATETIME), zone).toEpochMilli())
        assertEquals(1585742400000, toInstant(Values.literal("2020-04-01T13:00:00.000+01:00", XSD.DATETIME)).toEpochMilli())
        assertEquals(1585742400000, toInstant(Values.literal("2020-04-01T13:00:00.000+01:00", XSD.DATETIME), zone).toEpochMilli())
    }

    @test fun invalidToInstant() {
        assertFails { toInstant(Values.literal(true)) }
        assertFails { toInstant(Values.literal("")) }
        assertFails { toInstant(Values.literal(13)) }
    }
}