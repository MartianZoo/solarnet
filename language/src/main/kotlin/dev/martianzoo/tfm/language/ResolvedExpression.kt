package dev.martianzoo.tfm.language

import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.types.Dependency.Key
import dev.martianzoo.types.Dependency.TypeDependency
import dev.martianzoo.types.DependencySet.DependencyPath
import dev.martianzoo.types.Type

/** A Type together with the dependency keys explicitly supplied by its authored expression. */
internal data class ResolvedExpression(
    val type: Type,
    val authoredDependencies: Map<Key, Expression>,
) {
  internal fun authored(key: Key): Expression? = authoredDependencies[key]

  internal fun dependency(key: Key): Type? {
    if (key !in type.dependencies.keys) return null
    return (type.dependencies.at(DependencyPath(listOf(key))) as? TypeDependency)?.boundType
  }
}
