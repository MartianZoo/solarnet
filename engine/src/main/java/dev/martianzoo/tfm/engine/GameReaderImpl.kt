package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.Authority
import dev.martianzoo.tfm.api.GameReader
import dev.martianzoo.tfm.api.Type
import dev.martianzoo.tfm.api.TypeInfo
import dev.martianzoo.tfm.engine.ComponentGraph.Component.Companion.toComponent
import dev.martianzoo.tfm.pets.Parsing
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Metric
import dev.martianzoo.tfm.pets.ast.Metric.Count
import dev.martianzoo.tfm.pets.ast.Metric.Plus
import dev.martianzoo.tfm.pets.ast.Metric.Scaled
import dev.martianzoo.tfm.pets.ast.PetElement
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.Requirement.And
import dev.martianzoo.tfm.pets.ast.Requirement.Counting
import dev.martianzoo.tfm.pets.ast.Requirement.Exact
import dev.martianzoo.tfm.pets.ast.Requirement.Max
import dev.martianzoo.tfm.pets.ast.Requirement.Min
import dev.martianzoo.tfm.pets.ast.Requirement.Or
import dev.martianzoo.tfm.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.tfm.types.MClassTable
import dev.martianzoo.tfm.types.Transformers
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlin.reflect.KClass

@Singleton
internal class GameReaderImpl
@Inject
constructor(
    private val table: MClassTable,
    private val components: ComponentGraph,
    internal val transformers: Transformers,
) : GameReader, TypeInfo {

  override val authority: Authority by table::authority

  override fun resolve(expression: Expression) = table.resolve(expression)

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
        is Scaled -> count(metric.metric) / metric.unit
        is Metric.Max -> min(count(metric.metric), metric.maximum)
        is Plus -> metric.metrics.sumOf(::count)
        is Metric.Transform -> error("should have been transformed by now: $metric")
      }

  override fun count(type: Type) = components.count(table.resolve(type), this)

  override fun countComponent(concreteType: Type) =
      components.countComponent(concreteType.toComponent(this))

  override fun getComponents(type: Type) =
      components.getAll(table.resolve(type), this).map { it.mtype }

  override fun <P : PetElement> parse2(type: KClass<P>, text: String): P =
      preprocess(Parsing.parse(type, text))

  override fun <P : PetElement> preprocess(node: P) =
      transformers.standardPreprocess().transform(node)
}
