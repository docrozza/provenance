package io.docrozza.provenance.model

import com.stardog.stark.Resource

/**
 * Represents a referenceable provenance object
 */
sealed class Referencable<V : ProvenanceObject, I : Influence<V>> : RefOrValue<V> {
    /**
     * Whether the underlying provenance object has been qualified
     */
    val isQualified : Boolean get() = this is Qualification<*, *>
    /**
     * Whether the underlying provenance object is a reference
     */
    override val isRef : Boolean get() = this is Ref<*, *>
    /**
     * Returns the qualification, if possible or an error if not qualified
     */
    abstract fun qualification() : I
    /**
     * Returns a reference to the provenance object
    */
    abstract fun toRef() : RefOrValue<V>

    /**
     * An unqualified reference to a provenance object
     */
    data class Ref<V : ProvenanceObject, I : Influence<V>>(override val id: Resource, override val kind: Kind) : Referencable<V, I>() {
        override fun about() : V {
            error("This is a reference to an object and not the object itself")
        }

        override fun qualification(): I {
            error("This is a reference to an object and not a qualified version of it")
        }

        override fun toRef(): RefOrValue<V> {
            return this
        }
    }

    /**
     * An unqualified provenance object
     */
    data class Value<V : ProvenanceObject, I : Influence<V>>(private val wrapped: V) : Referencable<V, I>() {

        override val id = wrapped.id
        override val kind = wrapped.kind

        override fun about() : V {
            return wrapped
        }

        override fun qualification(): I {
            error("This is a simple value and not a qualified version of it")
        }

        override fun toRef(): RefOrValue<V> {
            return Ref(id, kind)
        }
    }

    /**
     * An qualified provenance object
     */
    data class Qualification<V : ProvenanceObject, I : Influence<V>>(private val wrapped: I) : Referencable<V, I>() {

        override val id = wrapped.id
        override val kind = wrapped.kind

        override fun about() : V {
            val v = wrapped.influencer()
            if (v.isRef) {
                error("This is a reference to an object and not the object itself")
            } else {
                return v.about()
            }
        }

        override fun qualification(): I {
            return wrapped
        }

        override fun toRef(): RefOrValue<V> {
            val v = wrapped.influencer()
            return Ref(v.id, v.kind)
        }
    }
}