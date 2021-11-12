package io.docrozza.provenance.model

/**
 * Represents a reference to some provenance object or the object itself
 *
 * Useful for capturing objects in sequence that can be defined inline or externally to the collection
 */
interface RefOrValue<V : ProvenanceObject> : Identifiable {
    /**
     * Indicates that this object is a reference to provenance object defined in the model elsewhere
     */
    val isRef : Boolean

    /**
     * Handle to the provenance object
     */
    fun about() : V
}