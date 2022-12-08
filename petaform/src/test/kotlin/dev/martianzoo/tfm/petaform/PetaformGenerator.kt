package dev.martianzoo.tfm.petaform

import dev.martianzoo.tfm.petaform.Action.Cost
import dev.martianzoo.tfm.petaform.Effect.Trigger
import dev.martianzoo.tfm.petaform.Instruction.Intensity
import dev.martianzoo.util.multiset
import dev.martianzoo.util.toListWeirdly
import kotlin.math.pow

class PetaformGenerator(scaling: (Int) -> Double = { 1.5 / 1.2.pow(it) - 1.0 })
    : RandomGenerator<PetaformNode>(Registry, scaling) {

  inline fun <reified P : PetaformNode> testRandom(): Boolean {
    val node = makeRandomNode(P::class)
    val str = node.toString()
    val trip = PetaformParser.parse<P>(str)
    return trip == node && trip.toString() == str
  }

  private object Registry : RandomGenerator.Registry<PetaformNode>() {
    init {
      val specSizes = toListWeirdly(multiset(27 to 0, 9 to 1, 3 to 2, 1 to 3)) // count to value
      register {
        TypeExpression(
            choose("Foo", "Bar", "Qux", "Abc", "Xyz", "Ooh", "Ahh", "Eep", "Wau"),
            listOfSize(choose(specSizes)),
            chooseS(9 to { null }, 1 to { recurse() })
        )
      }
      register { QuantifiedExpression(recurse(), choose(1, 1, 3, 5, 11)) }

      val predicateTypes = toListWeirdly(multiset(
          9 to Predicate.Min::class,
          4 to Predicate.Max::class,
          1 to Predicate.Exact::class,
          2 to Predicate.And::class,
          4 to Predicate.Or::class,
      ))
      register<Predicate> { recurse(choose(predicateTypes)) }
      register { Predicate.Min(qe = recurse()) }
      register { Predicate.Max(qe = recurse()) }
      register { Predicate.Exact(recurse()) }
      register { Predicate.and(listOfSize(choose(2, 2, 2, 2, 3))) as Predicate.And }
      register { Predicate.or(listOfSize(choose(2, 2, 2, 2, 2, 3, 4))) as Predicate.Or }

      fun RandomGenerator<*>.intensity() = choose(3 to null, 1 to randomEnum<Intensity>())

      val instructionTypes = toListWeirdly(multiset(
          9 to Instruction.Gain::class,
          4 to Instruction.Remove::class,
          2 to Instruction.Transmute::class,
          2 to Instruction.Then::class,
          2 to Instruction.Gated::class,
          3 to Instruction.Per::class,
          3 to Instruction.Or::class,
          4 to Instruction.Multi::class,
      ))
      register<Instruction> { recurse(choose(instructionTypes)) }
      register { Instruction.Gain(recurse(), intensity()) }
      register { Instruction.Remove(recurse(), intensity()) }
      register { Instruction.Transmute(recurse(), recurse(), recurse<QuantifiedExpression>().scalar, intensity()) }
      register { Instruction.Gated(recurse(), recurse()) }
      register { Instruction.Per(recurse(), recurse()) }
      register { Instruction.then(listOfSize(choose(2, 2, 2, 3))) as Instruction.Then }
      register { Instruction.multi(listOfSize(choose(2, 2, 2, 2, 2, 3, 4))) as Instruction.Multi }
      register { Instruction.or(listOfSize(choose(2, 2, 2, 2, 3))) as Instruction.Or }

      val triggerTypes = toListWeirdly(multiset(
          9 to Trigger.OnGain::class,
          4 to Trigger.OnRemove::class,
          3 to Trigger.Conditional::class,
      ))
      register<Trigger> { recurse(choose(triggerTypes)) }
      register { Trigger.OnGain(recurse()) }
      register { Trigger.OnRemove(recurse()) }
      register { Trigger.Conditional(recurse(), recurse()) }

      register { Effect(recurse(), recurse()) }

      val costTypes = toListWeirdly(multiset(
          9 to Cost.Spend::class,
          3 to Cost.Per::class,
          3 to Cost.Or::class,
          2 to Cost.Multi::class,
      ))
      register<Cost> { recurse(choose(costTypes)) }
      register { Cost.Spend(qe = recurse()) }
      register { Cost.Per(recurse(), recurse()) }
      register { Cost.or(listOfSize(choose(2, 2, 2, 2, 3, 4))) as Cost.Or }
      register { Cost.and(listOfSize(choose(2, 2, 2, 3))) as Cost.Multi }

      register { Action(choose(1 to null, 3 to recurse<Cost>()), recurse<Instruction>()) }
    }
  }
}
