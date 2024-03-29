package dev.martianzoo.engine

import dev.martianzoo.api.GameReader
import dev.martianzoo.api.Type
import dev.martianzoo.api.TypeInfo
import dev.martianzoo.engine.Component.Companion.toComponent
import dev.martianzoo.engine.Engine.GameScoped
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Metric.Count
import dev.martianzoo.pets.ast.Metric.Plus
import dev.martianzoo.pets.ast.Metric.Scaled
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.ast.Requirement.And
import dev.martianzoo.pets.ast.Requirement.Counting
import dev.martianzoo.pets.ast.Requirement.Exact
import dev.martianzoo.pets.ast.Requirement.Max
import dev.martianzoo.pets.ast.Requirement.Min
import dev.martianzoo.pets.ast.Requirement.Or
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.tfm.api.TfmAuthority
import dev.martianzoo.types.MClassTable
import javax.inject.Inject
import kotlin.math.min

@GameScoped
internal class GameReaderImpl
@Inject
constructor(
    private val classes: MClassTable,
    private val components: ComponentGraph,
    internal val transformers: Transformers,
) : GameReader, TypeInfo {

  override val authority: TfmAuthority = classes.authority as TfmAuthority

  override fun resolve(expression: Expression) = classes.resolve(expression)

  // Next 3 are for TypeInfo interface

  override fun isAbstract(e: Expression) = resolve(e).abstract

  override fun ensureNarrows(wide: Expression, narrow: Expression) =
      resolve(narrow).ensureNarrows(resolve(wide), this)

  override fun has(requirement: Requirement): Boolean =
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
        is Or -> requirement.requirements.any { has(it) }
        is And -> requirement.requirements.all { has(it) }
        is Requirement.Transform -> error("should have been transformed by now: $requirement")
      }

  override fun count(metric: Metric): Int =
      when (metric) {
        is Count -> components.count(resolve(metric.expression), this)
        is Scaled -> count(metric.inner) / metric.unit
        is Metric.Max -> min(count(metric.inner), metric.maximum)
        is Plus -> metric.metrics.sumOf(::count)
        is Metric.Transform -> error("should have been transformed by now: $metric")
      }

  override fun count(type: Type) = components.count(classes.resolve(type), this)

  override fun containsAny(type: Type) = components.containsAny(classes.resolve(type), this)

  override fun countComponent(concreteType: Type) =
      components.countComponent(concreteType.toComponent(this))

  override fun getComponents(type: Type) =
      components.getAll(classes.resolve(type), this).map { it.type }
}
