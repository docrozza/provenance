package io.docrozza.provenance.model

import io.docrozza.provenance.*
import java.time.Instant

/**
 * A provenance entity
 */
interface Entity : Locatable {
    override val kind: Kind get() = Kind.Entity
    val generatedAtTime: Instant?
    val invalidatedAtTime: Instant?
    val wasAttributedTo: AgentOrAttribution?
    val wasGeneratedBy: ActivityOrGeneration?
    val wasDerivedFrom: EntityOrDerivation?
    val alternateOf: EntityOrRef?
    val specializationOf: EntityOrRef?
    val hadPrimarySource: EntityOrPrimarySource?
    val wasInvalidatedBy: ActivityOrInvalidation?
    val wasQuotedFrom: EntityOrQuotation?
    val wasRevisionOf: EntityOrRevision?
}