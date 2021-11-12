package io.docrozza.provenance.rdf

import com.complexible.stardog.api.Connection
import com.stardog.stark.Literal
import com.stardog.stark.Values
import com.stardog.stark.vocabs.XSD
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.function.Supplier

object StardogUtils {

    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    fun String.iri(localName: String) = Values.iri(this, localName)

    fun <R> Supplier<Connection>.use(block: (Connection) -> R) : R {
        return get().use {
            try {
                it.begin()
                val result = block(it)
                it.commit()
                result
            } catch (error: Exception) {
                it.rollback()
                throw error
            }
        }
    }

    fun toLiteral(instant: Instant, zone: ZoneId = ZoneOffset.UTC) : Literal {
        val zoned = instant.atZone(zone)
        return Values.literal(formatter.format(zoned.toLocalDateTime()) + zoned.offset.toString(), XSD.DATETIME)
    }

    fun toInstant(literal: Literal, zone: ZoneId = ZoneOffset.UTC) : Instant {
        return when (val datatype = literal.datatype().iri()) {
            XSD.DATE -> {
                val parseDate = parseDate(literal.label())
                parseDate.first.atStartOfDay(parseDate.second ?: zone).toInstant()
            }
            XSD.DATETIME -> {
                val parseDate = parseDateTime(literal.label())
                parseDate.first.atZone(parseDate.second ?: zone).toInstant()
            }
            else -> error("Literal is not a temporal type: $datatype")
        }
    }

    private fun parseDate(text: String) : Pair<LocalDate, ZoneOffset?> {
        val nonWS = removeFrom(text)
        return when {
            isZoned(nonWS, 10) -> {
                val dt = OffsetDateTime.parse(nonWS, DateTimeFormatter.ISO_OFFSET_DATE)
                Pair(dt.toLocalDate(), dt.offset)
            }
            else -> Pair(LocalDate.parse(nonWS), null)
        }
    }

    private fun parseDateTime(text: String) : Pair<LocalDateTime, ZoneOffset?> {
        val nonWS = removeFrom(text)
        return when {
            isZoned(nonWS, 16) -> {
                val dt = OffsetDateTime.parse(nonWS)
                Pair(dt.toLocalDateTime(), dt.offset)
            }
            else -> Pair(LocalDateTime.parse(nonWS), null)
        }
    }

    private fun isZoned(text: String, offset: Int) : Boolean {
        return text.endsWith("Z") || text.indexOf('+', offset) > 0 || text.indexOf('-', offset) > 0
    }

    private fun removeFrom(text: String): String {
        val len = text.length
        var start = 0

        while (start < len && isWhiteSpace(text[start])) {
            start++
        }

        var end = len - 1
        while (end > start && isWhiteSpace(text[end])) {
            end--
        }

        return if (start == 0 && end == len - 1) text else text.substring(start, end + 1)
    }

    private fun isWhiteSpace(ch: Char): Boolean {
        return if (ch.code > 0x20) false else ch.code == 0x9 || ch.code == 0xA || ch.code == 0xD || ch.code == 0x20
    }
}