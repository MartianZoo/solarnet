package dev.martianzoo.pets.types

import dev.martianzoo.pets.types.Dependency.TypeDependency
import dev.martianzoo.pets.types.DependencySet.DependencyPath

/**
 * Compiled existence conditions for unrefined class bases. Conditions use only positive AND/OR
 * dependencies and selected-class inputs. Their greatest fixed point preserves mutually supporting
 * Class<X> representatives without constructing the concrete types witnessing every answer.
 *
 * Each frozen universe compiles these conditions once, including its game-local classes. Inclusion
 * changes only reevaluate the conditions; they never reconstruct the witnessing types.
 */
internal class ClassInhabitance
private constructor(
    private val nodes: List<Node>,
    private val indices: Map<GroundType, Int>,
) {
  private class Node(
      val type: GroundType,
      val alternatives: Boolean,
      val dependencies: IntArray,
  )

  private val dependents: Array<IntArray> = run {
    val incoming = Array(nodes.size) { mutableListOf<Int>() }
    nodes.forEachIndexed { index, node ->
      node.dependencies.forEach { incoming[it].add(index) }
    }
    Array(nodes.size) { incoming[it].toIntArray() }
  }

  fun evaluate(included: Set<Class>): Set<Class> {
    val possible = BooleanArray(nodes.size) { true }
    val remaining = IntArray(nodes.size) { nodes[it].dependencies.size }
    val removed = ArrayDeque<Int>()
    nodes.forEachIndexed { index, node ->
      if (
          (!node.type.rootClass.abstract && node.type.rootClass !in included) ||
              (node.alternatives && remaining[index] == 0)
      ) {
        possible[index] = false
        removed.addLast(index)
      }
    }
    while (removed.isNotEmpty()) {
      val missing = removed.removeFirst()
      dependents[missing].forEach { dependent ->
        if (possible[dependent]) {
          remaining[dependent]--
          if (!nodes[dependent].alternatives || remaining[dependent] == 0) {
            possible[dependent] = false
            removed.addLast(dependent)
          }
        }
      }
    }
    return included.filterTo(linkedSetOf()) {
      !it.abstract && possible[indices.getValue(it.baseType)]
    }
  }

  companion object {
    fun create(table: ClassTable): ClassInhabitance = Compiler(table).build()
  }

  private class Compiler(private val table: ClassTable) {
    private val nodes = mutableListOf<Node>()
    private val indices = mutableMapOf<GroundType, Int>()

    fun build(): ClassInhabitance {
      table.allKnownClasses().filterNot(Class::abstract).forEach { compile(it.baseType) }
      return ClassInhabitance(nodes.toList(), indices.toMap())
    }

    private fun compile(type: GroundType): Int {
      indices[type]?.let {
        return it
      }
      check(type.refinement == null) { "class-base existence requires unrefined bounds: $type" }
      val index = nodes.size
      indices[type] = index
      // Reserve the index before visiting children: Class<X> conditions can refer back to X.
      nodes.add(Node(type, false, intArrayOf()))
      val choice =
          if (type.representedClass == null && !type.rootClass.abstract)
              type.rootClass.sharedInhabitanceChoice(type)
          else null
      val alternative = type.representedClass != null || type.rootClass.abstract || choice != null
      val dependencies =
          if (alternative) alternatives(type, choice)
          else type.dependencies.typeDependencies().asSequence().map { it.boundType }
      val children = dependencies.map(::compile).distinct().toList().toIntArray()
      nodes[index] = Node(type, alternative, children)
      return index
    }

    private fun alternatives(
        type: GroundType,
        choice: DependencyPath?,
    ): Sequence<GroundType> {
      if (choice != null) {
        val dependency = type.dependencies.at(choice) as TypeDependency
        // Bind the shared value before treating the remaining dependency witnesses independently.
        return table.allStructuralConcreteSubtypes(dependency.boundType).map { selected ->
          type.rootClass
              .withAllDependencies(
                  type.dependencies.replaceAt(choice, dependency.copy(boundType = selected))
              )
              .also { bound ->
                // Binding a shared choice must progress; a self-supporting OR here would be
                // unsound.
                check(bound != type) { "unbound shared choice in $bound" }
              }
        }
      }
      val root = type.representedClass ?: type.rootClass
      val candidates = table.allStructuralSubclasses(root).asSequence()
      return candidates.filterNot(Class::abstract).mapNotNull { candidate ->
        if (type.representedClass != null) candidate.baseType
        else table.glb(type, candidate.baseType)
      }
    }
  }
}
