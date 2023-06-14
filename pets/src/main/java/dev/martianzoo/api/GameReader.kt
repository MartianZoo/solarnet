package dev.martianzoo.api

import dev.martianzoo.tfm.api.TfmAuthority
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Metric
import dev.martianzoo.tfm.pets.ast.PetElement
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.util.Multiset
import kotlin.reflect.KClass

/** A readable view of the state of a game in progress. */
interface GameReader : TypeInfo {
  /** The initial configuration for the game. */
  val authority: TfmAuthority

  /** Returns the type represented by the (fully-prepared) [expression]. */
  fun resolve(expression: Expression): Type

  /** Determines whether the (fully-prepared) [requirement] is met in the current game state. */
  override fun has(requirement: Requirement): Boolean

  /** Evaluates the (fully-prepared) [metric] in the current game state. */
  fun count(metric: Metric): Int

  /** Returns the number of component instances having type [type] in the current game state. */
  fun count(type: Type): Int

  fun containsAny(type: Type): Boolean

  /** Returns the number of instances of [concreteType] in the current game state. */
  fun countComponent(concreteType: Type): Int

  /** Returns the types of all concrete components in the current game state. */
  fun getComponents(type: Type): Multiset<out Type>

  // TODO
  fun <P : PetElement> parseInternal(type: KClass<P>, text: String): P

  fun <P : PetElement> preprocess(node: P): P

  companion object {
    inline fun <reified P : PetElement> GameReader.parse(text: String): P =
        parseInternal(P::class, text)
  }
}
