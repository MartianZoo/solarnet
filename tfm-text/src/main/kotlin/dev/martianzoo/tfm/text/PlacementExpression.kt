package dev.martianzoo.tfm.text

import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.types.Dependency.Key

internal data class PlacementExpression(
    val owner: Expression?,
    val sites: List<Expression>,
    val unknownDependencies: Set<Key>,
)
