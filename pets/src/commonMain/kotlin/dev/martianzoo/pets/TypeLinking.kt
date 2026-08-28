package dev.martianzoo.pets

import dev.martianzoo.pets.api.SystemClasses.THIS
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction.Transmute
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.pets.ast.Requirement

/** Finds authored expressions which must narrow together. */
internal object TypeLinking {
  fun sourcesAcrossRegions(root: PetNode): Set<Expression> =
      sourcesAcross(root.immediateChildren(), isAbstract = { true })

  /** Finds repeated proper subexpressions on opposite sides of a transmutation. */
  fun atomicSources(
      transmute: Transmute,
      isAbstract: (Expression) -> Boolean,
  ): Set<Expression> =
      sourcesAcross(
          listOf(transmute.gaining, transmute.removing),
          isAbstract,
          includeRegionRoots = false,
      )

  /** Returns the nodes corresponding to occurrences of [source] in two parallel trees. */
  fun bindings(wide: PetNode, narrow: PetNode, source: Expression): List<Expression> = buildList {
    fun collect(wideNode: PetNode, narrowNode: PetNode) {
      if (wideNode == source) {
        (narrowNode as? Expression)?.let(::add)
        return
      }
      wideNode.immediateChildren().zip(narrowNode.immediateChildren()).forEach { (wide, narrow) ->
        collect(wide, narrow)
      }
    }
    collect(wide, narrow)
  }

  private fun sourcesAcross(
      regions: List<PetNode>,
      isAbstract: (Expression) -> Boolean,
      includeRegionRoots: Boolean = true,
  ): Set<Expression> {
    data class Occurrence(
        val expression: Expression,
        val region: Int,
        val ancestors: Set<Expression>,
        val inRequirement: Boolean,
        val directlyCounted: Boolean,
    )

    val occurrences = buildList {
      fun collect(
          node: PetNode,
          region: Int,
          ancestors: Set<Expression>,
          inRequirement: Boolean,
          directlyCounted: Boolean,
          regionRoot: Boolean,
      ) {
        val expression = node as? Expression
        val nextAncestors = expression?.let { ancestors + it } ?: ancestors
        if (expression != null && (includeRegionRoots || !regionRoot)) {
          add(Occurrence(expression, region, ancestors, inRequirement, directlyCounted))
        }
        node.immediateChildren().forEach { child ->
          collect(
              child,
              region,
              nextAncestors,
              inRequirement || node is Requirement,
              node is Metric.Count && child is Expression,
              false,
          )
        }
      }
      regions.forEachIndexed { index, region ->
        collect(region, index, emptySet(), false, false, true)
      }
    }
    val occurrencesByExpression =
        occurrences.filterNot(Occurrence::directlyCounted).groupBy(Occurrence::expression)
    val candidates =
        occurrencesByExpression
            .filter { expression ->
              val (source, found) = expression
              source.className != THIS &&
                  isAbstract(source) &&
                  found.map(Occurrence::region).distinct().size >= 2 &&
                  !found.all(Occurrence::inRequirement)
            }
            .toList()
            .sortedBy { (_, found) -> found.minOf { it.ancestors.size } }

    val result = linkedSetOf<Expression>()
    for ((expression, found) in candidates) {
      if (!found.all { occurrence -> occurrence.ancestors.any(result::contains) }) {
        result += expression
      }
    }
    return result
  }
}
