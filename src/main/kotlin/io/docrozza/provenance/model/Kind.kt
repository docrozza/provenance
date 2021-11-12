package io.docrozza.provenance.model

import com.stardog.stark.IRI
import io.docrozza.provenance.Provenance

/**
 * The types of provenance entities
 */
enum class Kind {
    Agent,
    Activity,
    Entity,
    Collection,
    Plan,
    Bundle,
    Usage,
    Generation,
    Invalidation,
    Start,
    End,
    Communication,
    Derivation,
    Association,
    Attribution,
    Delegation,
    Influence,
    Quotation,
    Revision,
    PrimarySource,
    Location,
    Role,
    NonProvenance;

    /**
     * Return the corresponding [IRI] for this type
     */
    fun toIRI() : IRI {
        return when (this) {
            Agent -> io.docrozza.provenance.Provenance.Agent
            Activity -> io.docrozza.provenance.Provenance.Activity
            Entity -> io.docrozza.provenance.Provenance.Entity
            Collection -> io.docrozza.provenance.Provenance.Collection
            Plan -> io.docrozza.provenance.Provenance.Plan
            Bundle -> io.docrozza.provenance.Provenance.Bundle
            Usage -> io.docrozza.provenance.Provenance.Usage
            Generation -> io.docrozza.provenance.Provenance.Generation
            Invalidation -> io.docrozza.provenance.Provenance.Invalidation
            Start -> io.docrozza.provenance.Provenance.Start
            End -> io.docrozza.provenance.Provenance.End
            Communication -> io.docrozza.provenance.Provenance.Communication
            Derivation -> io.docrozza.provenance.Provenance.Derivation
            Association -> io.docrozza.provenance.Provenance.Association
            Attribution -> io.docrozza.provenance.Provenance.Attribution
            Delegation -> io.docrozza.provenance.Provenance.Delegation
            Influence -> io.docrozza.provenance.Provenance.Influence
            Quotation -> io.docrozza.provenance.Provenance.Quotation
            Revision -> io.docrozza.provenance.Provenance.Revision
            PrimarySource -> io.docrozza.provenance.Provenance.PrimarySource
            Location -> io.docrozza.provenance.Provenance.Location
            Role -> io.docrozza.provenance.Provenance.Role
            NonProvenance -> error("Non provenance objects must be independently typed")
        }
    }
}