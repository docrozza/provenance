@file:Suppress("unused")
package io.docrozza.provenance.model

typealias ThingOrRef = RefOrValue<ProvenanceObject>
typealias ActivityOrRef = RefOrValue<Activity>
typealias AgentOrRef = RefOrValue<Agent>
typealias EntityOrRef = RefOrValue<Entity>
typealias LocationOrRef = RefOrValue<Location>
typealias PlanOrRef = RefOrValue<Plan>
typealias RoleOrRef = RefOrValue<Role>
typealias BundleOrRef = RefOrValue<Bundle>
typealias UsageOrRef = RefOrValue<Usage>
typealias GenerationOrRef = RefOrValue<Generation>

typealias ActivityOrGeneration = Referencable<Activity, Generation>
typealias ActivityOrInvalidation = Referencable<Activity, Invalidation>
typealias ActivityOrCommunication = Referencable<Activity, Communication>
typealias AgentOrAttribution = Referencable<Agent, Attribution>
typealias AgentOrAssociation = Referencable<Agent, Association>
typealias AgentOrDelegation = Referencable<Agent, Delegation>
typealias EntityOrDerivation = Referencable<Entity, Derivation>
typealias EntityOrPrimarySource = Referencable<Entity, PrimarySource>
typealias EntityOrQuotation = Referencable<Entity, Quotation>
typealias EntityOrRevision = Referencable<Entity, Revision>
typealias EntityOrStart = Referencable<Entity, Start>
typealias EntityOrEnd = Referencable<Entity, End>
typealias EntityOrUsage = Referencable<Entity, Usage>