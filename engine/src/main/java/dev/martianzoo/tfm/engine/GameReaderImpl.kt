package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.GameReader
import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.api.Type
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Metric
import dev.martianzoo.tfm.pets.ast.Metric.Count
import dev.martianzoo.tfm.pets.ast.Metric.Plus
import dev.martianzoo.tfm.pets.ast.Metric.Scaled
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.Requirement.And
import dev.martianzoo.tfm.pets.ast.Requirement.Counting
import dev.martianzoo.tfm.pets.ast.Requirement.Exact
import dev.martianzoo.tfm.pets.ast.Requirement.Max
import dev.martianzoo.tfm.pets.ast.Requirement.Min
import dev.martianzoo.tfm.pets.ast.Requirement.Or
import dev.martianzoo.tfm.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.tfm.types.MClassLoader
import kotlin.math.min

class GameReaderImpl(
    override val setup: GameSetup,
    val loader: MClassLoader,
    val components: ComponentGraph,
) : GameReader {
  override fun resolve(expression: Expression) = loader.resolve(expression)

  override fun evaluate(requirement: Requirement): Boolean =
      when (requirement) {
        is Counting -> {
          val actual = count(Count(requirement.scaledEx.expression))
          val target = (requirement.scaledEx.scalar as ActualScalar).value
          when (requirement) {
            is Min -> actual >= target
            is Max -> actual <= target
            is Exact -> actual == target
          }
        }

        is Or -> requirement.requirements.any { evaluate(it) }
        is And -> requirement.requirements.all { evaluate(it) }
        is Requirement.Transform -> error("should have been transformed by now: $requirement")
      }

  override fun count(metric: Metric): Int =
      when (metric) {
        is Count -> components.count(resolve(metric.expression))
        is Scaled -> count(metric.metric) / metric.unit
        is Metric.Max -> min(count(metric.metric), metric.maximum)
        is Plus -> metric.metrics.sumOf(::count)
        is Metric.Transform -> error("should have been transformed by now: $metric")
      }

  override fun count(type: Type) = components.count(loader.resolve(type))

  override fun countComponent(concreteType: Type) =
      components.countComponent(Component.ofType(loader.resolve(concreteType)))

  override fun getComponents(type: Type) =
      components.getAll(loader.resolve(type)).map { it.mtype }
}
