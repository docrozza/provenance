package io.docrozza.provenance.model

import io.docrozza.provenance.*
import java.time.Instant

/**
 * A provenance activity
 */
interface Activity : Locatable {
    override val kind: Kind get() = Kind.Activity
    val generated: EntityOrRef?
    val wasAssociatedWith: Sequence<AgentOrAssociation>
    val used: Sequence<EntityOrUsage>
    val invalidated: Sequence<EntityOrRef>
    val wasInformedBy: Sequence<ActivityOrCommunication>
    val wasStartedBy: EntityOrStart?
    val startedAtTime: Instant?
    val wasEndedBy: EntityOrEnd?
    val endedAtTime: Instant?
}