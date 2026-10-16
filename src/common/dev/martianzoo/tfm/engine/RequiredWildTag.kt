package dev.martianzoo.tfm.engine

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.types.Type
import dev.martianzoo.state.CustomMetric
import dev.martianzoo.state.GameReader
import dev.martianzoo.state.toComponent

/** Selects concrete assignments; ordinary Tag counts still decide whether a requirement holds. */
internal object RequiredWildTag : CustomMetric() {
  override fun count(game: GameReader, type: Type): Int {
    val (subject, election, requestedTag) = type.typeDependencies.map { it.boundType }
    val player = requireNotNull(election.toComponent().owner).expression
    val subjectType = game.resolve(requireNotNull(subject.representedClass).className.of(player))
    val requirement = subjectType.getRequirementPropertyValue("requirement") ?: return 0
    val tagClass = game.classTable.getClass(cn("Tag"))
    val minima = Requirement.split(requirement).filterIsInstance<Requirement.Min>()
    val needed = minima.flatMap { minimum ->
      val metric = minimum.metric as? Metric.Count ?: return@flatMap emptyList()
      val countedClass = game.classTable.getClass(metric.expression.className)
      if (!countedClass.isSubtypeOf(tagClass)) return@flatMap emptyList()
      val shortfall =
          (minimum.target - game.count(game.resolve(countedClass.className.of(player))))
              .coerceAtLeast(0)
      val concreteTag =
          if (countedClass.className == cn("BioTag")) game.classTable.getClass(cn("PlantTag"))
          else countedClass
      List(shortfall) { concreteTag }
    }
    val unused =
        game
            .getComponents(cn("WildTagElection").of(player))
            .elements
            .filter { game.count(game.resolve(cn("Tag").of(it.expression))) == 0 }
            .sortedBy { it.expression.toString() }
    if (needed.size > unused.size) return 0
    val index = unused.indexOf(election)
    return if (index >= 0 && needed.getOrNull(index) == requestedTag.representedClass) 1 else 0
  }
}
