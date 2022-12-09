package dev.martianzoo.tfm.petaform

import dev.martianzoo.tfm.petaform.Action.Cost
import dev.martianzoo.tfm.petaform.Effect.Trigger
import dev.martianzoo.tfm.petaform.Instruction.FromExpression
import dev.martianzoo.tfm.petaform.Instruction.FromIsBelow
import dev.martianzoo.tfm.petaform.Instruction.FromIsNowhere
import dev.martianzoo.tfm.petaform.Instruction.FromIsRightHere
import dev.martianzoo.tfm.petaform.Instruction.Intensity
import dev.martianzoo.util.multiset
import kotlin.math.pow
import kotlin.random.Random

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
      val specSizes = (multiset(32 to 0, 8 to 1, 2 to 2, 1 to 3)) // weight to value
      register {
        TypeExpression(
            randomName(),
            listOfSize(choose(specSizes)),
            refinement()
        )
      }
      register { QuantifiedExpression(choose(1 to null, 4 to recurse()), choose(null, null, 0, 1, 5, 42)) }

      val predicateTypes = (multiset(
          9 to Predicate.Min::class,
          4 to Predicate.Max::class,
          1 to Predicate.Exact::class,
          2 to Predicate.And::class,
          4 to Predicate.Or::class,
      ))
      register<Predicate> { recurse(choose(predicateTypes)) }
      register { Predicate.Min(qe = recurse()) }
      register { Predicate.Max(recurse()) }
      register { Predicate.Exact(recurse()) }
      register { Predicate.and(listOfSize(choose(2, 2, 2, 2, 3))) as Predicate.And }
      register { Predicate.or(listOfSize(choose(2, 2, 2, 2, 2, 3, 4))) as Predicate.Or }

      fun RandomGenerator<*>.intensity() = choose(3 to null, 1 to randomEnum<Intensity>())

      val instructionTypes = (multiset(
          19 to Instruction.Gain::class,
          10 to Instruction.Remove::class,
          2 to Instruction.Transmute::class,
          2 to Instruction.Gated::class,
          3 to Instruction.Per::class,
          1 to Instruction.Then::class,
          2 to Instruction.Or::class,
          3 to Instruction.Multi::class,
      ))
      register<Instruction> { recurse(choose(instructionTypes)) }
      register { Instruction.Gain(recurse(), intensity()) }
      register { Instruction.Remove(recurse(), intensity()) }
      register { Instruction.Transmute(recurse(), recurse<QuantifiedExpression>().scalar, intensity()) }
      register { Instruction.Gated(recurse(), recurse()) }
      register { Instruction.Per(recurse(), recurse()) }
      register { Instruction.then(listOfSize(choose(2, 2, 2, 3))) as Instruction.Then }
      register { Instruction.multi(listOfSize(choose(2, 2, 2, 2, 2, 3, 4))) as Instruction.Multi }
      register { Instruction.or(listOfSize(choose(2, 2, 2, 2, 3))) as Instruction.Or }

      register<FromExpression> {
        val one: TypeExpression = recurse()
        val two: TypeExpression = recurse()

        fun getTypes(type: TypeExpression): List<TypeExpression> =
            type.specializations.flatMap { getTypes(it) } + type

        val oneTypes = getTypes(one)
        val twoTypes = getTypes(two)

        var inject: TypeExpression
        var into: TypeExpression
        var target: TypeExpression

        if (oneTypes.size <= twoTypes.size) {
          inject = one
          into = two
          target = twoTypes.random()
        } else {
          inject = two
          into = one
          target = oneTypes.random()
        }

        val b = Random.Default.nextBoolean()

        fun convert(type: TypeExpression): FromExpression {
          if (type == target) {
            return FromIsRightHere(if (b) inject else target, if (b) target else inject)
          }
          val specs = type.specializations.map { convert(it) }
          return if (specs.all { it is FromIsNowhere }) {
            FromIsNowhere(type)
          } else {
            FromIsBelow(type.className, specs, type.predicate)
          }
        }
        convert(into)
      }

      val triggerTypes = (multiset(
          9 to Trigger.OnGain::class,
          4 to Trigger.OnRemove::class,
          3 to Trigger.Conditional::class,
          1 to Trigger.Now::class,
          1 to Trigger.Prod::class,
      ))
      register<Trigger> { recurse(choose(triggerTypes)) }
      register { Trigger.OnGain(recurse()) }
      register { Trigger.OnRemove(recurse()) }
      register { Trigger.Conditional(recurse(), recurse()) }
      register { Trigger.Now(recurse()) }
      register { Trigger.Prod(recurse()) }

      register { Effect(recurse(), recurse()) }

      val costTypes = (multiset(
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

    fun RandomGenerator<PetaformNode>.refinement() = chooseS(9 to { null }, 1 to { recurse<Predicate>() })
    fun RandomGenerator<PetaformNode>.randomName() = choose("Foo", "Bar", "Qux", "Abc", "Xyz", "Ooh", "Ahh", "Eep", "Wau")
  }
}
