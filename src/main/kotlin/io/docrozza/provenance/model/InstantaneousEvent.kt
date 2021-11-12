package io.docrozza.provenance.model

import java.time.Instant

/**
 * Represents a provenance object that occurred at some instant of time
 */
interface InstantaneousEvent : ProvenanceObject {
    val atTime: Instant?
}