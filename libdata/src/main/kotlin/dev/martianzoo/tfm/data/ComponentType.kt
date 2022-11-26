package dev.martianzoo.tfm.data

import dev.martianzoo.tfm.petaform.api.Action
import dev.martianzoo.tfm.petaform.api.Effect
import dev.martianzoo.tfm.petaform.api.Expression
import dev.martianzoo.tfm.petaform.api.Instruction

data class ComponentType(
    val backing: RawComponentType,
    val supertypes: Set<Expression>,
    val dependencies: List<Expression>,
    val immediate: Instruction?,
    val actions: Set<Action>,
    val effects: Set<Effect>)
