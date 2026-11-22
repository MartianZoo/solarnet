package dev.martianzoo.engine

import dev.martianzoo.pets.PetElaborator
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.types.ClassTable
import dev.martianzoo.pets.types.Type
import dev.martianzoo.state.toComponent

/**
 * Returns the effects the engine would install for a component of this exact [type], including
 * owner-sensitive trigger matching. Abstract or uninhabited types yield an empty list.
 *
 * Reads static type facts from [classTable] without accessing a live [World]. [type] must be
 * understood by [classTable]. Invalid types and effect-elaboration failures are reported to the
 * caller.
 */
public fun compiledComponentEffects(classTable: ClassTable, type: Type): List<Effect> =
    if (type.abstract || !classTable.isInhabited(type)) emptyList()
    else LiveEffect.compile(type.toComponent(), PetElaborator(classTable)).map(LiveEffect::effect)
