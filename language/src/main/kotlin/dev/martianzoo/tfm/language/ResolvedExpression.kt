package dev.martianzoo.tfm.language

import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.types.Dependency.Key
import dev.martianzoo.types.Dependency.TypeDependency
import dev.martianzoo.types.DependencySet.DependencyPath
import dev.martianzoo.types.Type

/** A card-context Type plus keyed source dependencies needed after contextual normalization. */
internal data class ResolvedExpression(
    val type: Type,
    val selectedKeys: Set<Key>,
    val sourceDependencies: Map<Key, Expression>,
) {
  internal fun sourceDependency(key: Key): Expression? = sourceDependencies[key]

  internal fun hasOnlySourceDependency(key: Key, expression: Expression): Boolean =
      sourceDependencies.size == 1 && sourceDependency(key) == expression

  internal fun dependency(key: Key): Type? {
    if (key !in type.dependencies.keys) return null
    return (type.dependencies.at(DependencyPath(listOf(key))) as? TypeDependency)?.boundType
  }

  internal fun selectedDependency(key: Key): Type? = dependency(key).takeIf { key in selectedKeys }
}
