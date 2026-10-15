package dev.martianzoo.tfm.cardgenerator

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.pets.ast.Requirement

/** Infers a preferred tag from immediate benefits, independently of printed requirements. */
internal class WantedTagTypes(private val classes: RawClassIndex) {
  private val wanted = linkedSetOf<ClassName>()
  private val perUses = mutableListOf<Pair<Instruction.Per, InstructionGroup?>>()

  internal fun forBenefit(instructions: List<InstructionTree>): ClassName? {
    instructions.forEach { inspect(it) }
    if (wanted.size <= 1) return wanted.singleOrNull()
    // Gyropolis: equal sibling gains per Earth and per Venus. Choose Earth by convention.
    val group = perUses.firstOrNull()?.second ?: return null
    if (perUses.size != wanted.size || perUses.any { it.second !== group }) return null
    if (perUses.map { it.first.inner }.distinct().size != 1) return null
    if (perUses.any { it.first.metric !is Metric.Count }) return null
    return wanted.minBy(ClassName::toString)
  }

  private fun inspect(node: PetNode, group: InstructionGroup? = null) {
    when (node) {
      is InstructionGroup -> node.instructions.forEach { inspect(it, node) }
      is Instruction.Per -> {
        if (node.inner is Instruction.Gain) {
          val tags = preferredTypes(node.metric)
          if (tags.isNotEmpty()) {
            wanted += tags
            perUses += node to group
          }
        }
      }
      is Instruction.Gated -> {
        Requirement.split(node.gate).filterIsInstance<Requirement.Min>().forEach {
          wanted += preferredTypes(it.metric)
        }
        inspect(node.inner, group)
      }
      else -> node.childNodes().forEach { inspect(it, group) }
    }
  }

  private fun preferredTypes(metric: Metric): Set<ClassName> =
      when (metric) {
        is Metric.Count -> tagTypes(metric.expression).toSet()
        is Metric.Scaled -> preferredTypes(metric.inner)
        is Metric.Max -> preferredTypes(metric.inner) + preferredTypes(metric.maximum)
        // Any member of this union contributes equally. Choose a stable concrete member.
        is Metric.Or ->
            metric.metrics
                .flatMap { tagTypes(it.expression) }
                .minByOrNull(ClassName::toString)
                ?.let(::setOf)
                .orEmpty()
        else -> emptySet()
      }

  private fun tagTypes(expression: Expression): List<ClassName> {
    if (!classes.isSubtypeOf(expression.className, cn("Tag"))) return emptyList()
    if (expression.refinement != null) return emptyList()
    if (expression.arguments.any { !it.simple || it.className != cn("Anyone") }) return emptyList()
    return classes.concreteSubtypes(expression.className)
  }
}
