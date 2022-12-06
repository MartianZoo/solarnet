package dev.martianzoo.tfm.petaform.parser

import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.petaform.api.*
import dev.martianzoo.tfm.petaform.api.Action.Cost
import dev.martianzoo.tfm.petaform.api.Effect.Trigger
import dev.martianzoo.tfm.petaform.api.Instruction.Intensity
import kotlin.random.Random
import kotlin.reflect.KClass

object RandomGenerator {

  val rng = Random.Default
  val registry = mutableMapOf<KClass<out PetaformNode>, () -> PetaformNode>()

  inline fun <reified P : PetaformNode> testRandom(): Boolean {
    val node = random<P>()
    val str = node.toString()
    val trip = PetaformParser.parse<P>(str)
    return trip == node && trip.toString() == str
  }

  init {
    register {
      Expression(
          random(),
          listOfSize(choose(16 to 0, 2 to 1, 1 to 2, 1 to 3)),
          chooseS(12 to { null }, 1 to { random() }))
    }
    register { RootType(choose("Foo", "Bar", "Baz", "Qux", "Abc", "Xyz", "One", "Two", "Six", "Wau")) }

    register { Action(choose(1 to null, 3 to random()), random()) }

    register { random(choose(
        9 to Cost.Spend::class,
        4 to Cost.Or::class,
        2 to Cost.Multi::class,
        3 to Cost.Per::class,
    )) }
    register { Cost.Spend(qe = random()) }
    register { Cost.Per(random(), random()) }
    register { Cost.and(listOfSize(choose(2, 2, 2, 3))) as Cost.Multi }
    register { Cost.or(listOfSize(choose(2, 2, 2, 2, 3, 4))) as Cost.Or }

    register { random(choose(
        9 to Trigger.OnGain::class,
        4 to Trigger.OnRemove::class,
        2 to Trigger.Conditional::class,
    )) }
    register { Trigger.OnGain(random()) }
    register { Trigger.OnRemove(random()) }
    register { Trigger.Conditional(random(), random()) }

    register { Effect(random(), random()) }

    register { random(choose(
      12 to Instruction.Gain::class,
      4 to Instruction.Remove::class,
      2 to Instruction.Transmute::class,
      5 to Instruction.Multi::class,
      3 to Instruction.Per::class,
      4 to Instruction.Or::class,
      2 to Instruction.Then::class,
      2 to Instruction.Gated::class,
    )) }
    register { Instruction.Gain(random(), intensity()) }
    register { Instruction.Remove(random(), intensity()) }
    register { Instruction.Transmute(random(), random(), random<QuantifiedExpression>().scalar, intensity()) }
    register { Instruction.multi(listOfSize(choose(2, 2, 2, 2, 2, 3, 4))) as Instruction.Multi }
    register { Instruction.Per(random(), random()) }
    register { Instruction.or(listOfSize(choose(2, 2, 2, 2, 3))) as Instruction.Or }
    register { Instruction.Then(random(), random()) }
    register { Instruction.Gated(random(), random()) }

    register { random(choose(
        9 to Predicate.Min::class,
        4 to Predicate.Max::class,
        2 to Predicate.Exact::class,
        2 to Predicate.And::class,
        3 to Predicate.Or::class,
    )) }
    register { Predicate.Min(qe=random()) }
    register { Predicate.Max(qe=random()) }
    register { Predicate.Exact(random()) }
    register { Predicate.or(listOfSize(choose(2, 2, 2, 2, 2, 3, 4))) as Predicate.Or }
    register { Predicate.and(listOfSize(choose(2, 2, 2, 2, 2, 3, 4))) as Predicate.And }

    register { QuantifiedExpression(random(), choose(1, 1, 3, 11)) }
  }

  inline fun <reified P : PetaformNode> random() = random(P::class)
  fun <P : PetaformNode> random(type: KClass<out P>): P {
    val function: () -> PetaformNode = registry[type]!!
    while (true) {
      try {
        return function.invoke() as P
      } catch (ignore: Exception) {
      }
    }
  }

  inline fun <reified E : Enum<E>> randomEnum() = enumValues<E>().random()

  inline fun <reified P : PetaformNode> listOfSize(size: Int): List<P> =
      mutableListOf<P>().also {
        while (it.size < size) {
          it.add(random())
        }
      }

  inline fun <reified P : PetaformNode> setOfSize(size: Int): Set<P> =
      mutableSetOf<P>().also {
        while (it.size < size) {
          it.add(random())
        }
      }

  fun <T : Any?> choose(vararg choice: T): T = choice.random()

  inline fun <reified P : PetaformNode> register(noinline creator: () -> P) {
    register(P::class, creator)
  }

  fun <P : PetaformNode> register(type: KClass<P>, creator: () -> P) {
    registry[type] = creator
  }

  fun <T : Any?> choose(vararg weightToChoice: Pair<Int, T>): T {
    val sum =  weightToChoice.map { it.first }.sum()
    var skip = rng.nextInt(sum)
    for (wc in weightToChoice) {
      skip -= wc.first
      if (skip < 0) {
        return wc.second
      }
    }
    error("")
  }

  fun <T : Any?> chooseS(vararg weightToChoiceSupplier: Pair<Int, () -> T>): T {
    val sum =  weightToChoiceSupplier.map { it.first }.sum()
    var skip = rng.nextInt(sum)
    for (wc in weightToChoiceSupplier) {
      skip -= wc.first
      if (skip < 0) {
        return wc.second()
      }
    }
    error("")
  }

  private fun intensity() = choose(1 to randomEnum<Intensity>(), 3 to null)
}
