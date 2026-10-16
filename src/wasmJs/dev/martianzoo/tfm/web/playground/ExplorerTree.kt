package dev.martianzoo.tfm.web.playground

import dev.martianzoo.pets.Parsing
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.SystemClasses.CLASS
import dev.martianzoo.pets.api.SystemClasses.COMPONENT
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.types.Class as PetsClass
import dev.martianzoo.pets.types.ClassTable
import dev.martianzoo.pets.types.Dependency
import dev.martianzoo.pets.types.Dependency.Key
import dev.martianzoo.pets.types.DependencySet.DependencyPath
import dev.martianzoo.pets.types.GroundType

/** The present components viewed through Pets types, with no separate hierarchy or count model. */
internal class ExplorerTree(
    private val table: ClassTable,
    specimen: Specimen,
    includeNonReal: Boolean,
) {
  internal data class Entry(val type: GroundType, val count: Int, val nonReal: Boolean)

  internal sealed interface Line {
    val depth: Int

    data class Group(
        override val depth: Int,
        val id: List<GroundType>,
        val type: GroundType,
        val count: Int,
        val pivot: List<Int>?,
    ) : Line

    data class Exact(override val depth: Int, val entry: Entry) : Line
  }

  internal val root: GroundType = table.getClass(COMPONENT).baseType

  private val entries =
      specimen.components
          .asSequence()
          .filter { it.count > 0 && (includeNonReal || !it.nonReal) }
          .map {
            Entry(table.resolve(Parsing.parse<Expression>(it.type)), it.count, it.nonReal)
          }
          .toList()

  private val visibleClasses = table.allClasses().filterNot(table::isProbableAspect).toSet()

  /** Bypass aspect categories without losing the concrete kinds that inherit them. */
  private val visibleSubclasses: Map<PetsClass, List<PetsClass>> =
      visibleClasses
          .flatMap { child ->
            val parents = child.allSuperclasses().filter { it != child && it in visibleClasses }
            parents
                .filter { parent ->
                  parents.none { it != parent && it.isSubtypeOf(parent) }
                }
                .map { parent -> parent to child }
          }
          .groupBy({ it.first }, { it.second })

  /** Each row counts its own query; rows may overlap when classes have several parents. */
  internal fun lines(pivotFor: (List<GroundType>) -> List<Int>?): List<Line> = buildList {
    fun visit(id: List<GroundType>, members: List<Entry>) {
      val type = id.last()
      val depth = id.size - 1
      val pivot = pivotFor(id)
      add(Line.Group(depth, id, type, members.sumOf(Entry::count), pivot))
      if (pivot == null) return
      val target = target(type, pivot) ?: return
      val path = target.keys
      members
          .filter { member -> boundClass(member.type, path) == target.type.rootClass }
          .sortedBy { it.type.expressionFull.toString() }
          .forEach { add(Line.Exact(depth + 1, it)) }
      for (child in
          visibleSubclasses[target.type.rootClass].orEmpty().sortedBy { it.className.toString() }) {
        val narrowed = narrow(type, pivot, child) ?: continue
        if (narrowed == type) continue
        val matching = members.filter { it.type.isSubtypeOf(narrowed) }
        if (matching.isNotEmpty()) visit(id + narrowed, matching)
      }
    }
    visit(listOf(root), entries)
  }

  /** The type and stable dependency keys named by an argument position in the full expression. */
  private data class Target(val type: GroundType, val keys: List<Key>)

  private fun target(type: GroundType, path: List<Int>): Target? {
    var current = type
    val keys = mutableListOf<Key>()
    for (index in path) {
      val open =
          current.rootClass.dependencies.keys.filter {
            current.rootClass.dependencies.get(it).abstract
          }
      val key = open.getOrNull(index) ?: return null
      keys += key
      val dependency = current.dependencies.get(key)
      current =
          when (dependency) {
            is Dependency.TypeDependency -> dependency.boundType
            else -> table.getClass(dependency.className).baseType
          }
    }
    return Target(current, keys)
  }

  private fun boundClass(type: GroundType, path: List<Key>): PetsClass {
    if (path.isEmpty()) return type.rootClass
    val dependency = type.dependencies.at(DependencyPath(path))
    return when (dependency) {
      is Dependency.TypeDependency -> dependency.boundType.rootClass
      else -> table.getClass(dependency.className)
    }
  }

  private fun narrow(type: GroundType, path: List<Int>, child: PetsClass): GroundType? {
    if (path.isEmpty()) return table.glb(type, child.baseType)
    val full = type.expressionFull
    val old = expressionAt(full, path) ?: return null
    val parent = expressionAt(full, path.dropLast(1)) ?: return null
    val replacement =
        if (parent.className == CLASS) {
          Expression(child.className)
        } else {
          table.glb(table.resolve(old), child.baseType)?.expressionFull ?: return null
        }
    return try {
      table.resolve(full.replaceAt(path, replacement))
    } catch (_: ExpressionException) {
      null
    }
  }

  private fun expressionAt(expression: Expression, path: List<Int>): Expression? {
    var current = expression
    for (index in path) current = current.arguments.getOrNull(index) ?: return null
    return current
  }

  private fun Expression.replaceAt(path: List<Int>, replacement: Expression): Expression {
    if (path.isEmpty()) return replacement
    val index = path.first()
    return copy(
        arguments =
            arguments.mapIndexed { i, argument ->
              if (i == index) argument.replaceAt(path.drop(1), replacement) else argument
            }
    )
  }
}
