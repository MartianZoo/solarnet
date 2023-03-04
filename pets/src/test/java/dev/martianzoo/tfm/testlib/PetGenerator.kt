package dev.martianzoo.tfm.testlib

import com.google.common.truth.Truth.assertWithMessage
import dev.martianzoo.tfm.api.SpecialClassNames.MEGACREDIT
import dev.martianzoo.tfm.api.SpecialClassNames.OWNER
import dev.martianzoo.tfm.pets.Parsing.parseElement
import dev.martianzoo.tfm.pets.PetException
import dev.martianzoo.tfm.pets.ast.Action
import dev.martianzoo.tfm.pets.ast.Action.Cost
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Effect.Trigger
import dev.martianzoo.tfm.pets.ast.From
import dev.martianzoo.tfm.pets.ast.From.ComplexFrom
import dev.martianzoo.tfm.pets.ast.From.SimpleFrom
import dev.martianzoo.tfm.pets.ast.From.TypeAsFrom
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity
import dev.martianzoo.tfm.pets.ast.Metric
import dev.martianzoo.tfm.pets.ast.PetNode
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.ScaledTypeExpr
import dev.martianzoo.tfm.pets.ast.ScaledTypeExpr.Companion.scaledType
import dev.martianzoo.tfm.pets.ast.TypeExpr
import dev.martianzoo.tfm.testlib.PetToKotlin.p2k
import dev.martianzoo.util.HashMultiset
import dev.martianzoo.util.Multiset
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.reflect.KClass
import org.junit.jupiter.api.Assertions.fail

internal class PetGenerator(scaling: (Int) -> Double) :
    RandomGenerator<PetNode>(Registry, scaling) {

  constructor(greed: Double = 0.8, backoff: Double = 0.15) : this(scaling(greed, backoff))

  private object Registry : RandomGenerator.Registry<PetNode>() {
    init {
      val specSizes = multiset(8 to 0, 4 to 1, 2 to 2, 1 to 3) // weight to value
      register { cn(randomName()) }
      register(TypeExpr::class) {
        TypeExpr(
            recurse(),
            listOfSize(choose(specSizes)),
            refinement(),
            null) // choose(10 to null, 2 to 1, 1 to 2))
      }
      register {
        scaledType(choose(0, 1, 1, 1, 5, 11), choose(1 to MEGACREDIT.type, 3 to recurse()))
      }

      val metricTypes =
          multiset(
              4 to Metric.Count::class,
              1 to Metric.Max::class,
          )
      register(Metric::class) { recurse(choose(metricTypes)) }
      register { Metric.Count(scaledType = recurse()) }
      register { Metric.Max(metric = recurse(), maximum = choose(5, 11)) }

      val requirementTypes =
          multiset(
              9 to Requirement.Min::class,
              4 to Requirement.Max::class,
              2 to Requirement.Exact::class,
              5 to Requirement.Or::class,
              3 to Requirement.And::class,
              1 to Requirement.Transform::class,
          )
      register(Requirement::class) { recurse(choose(requirementTypes)) }
      register { Requirement.Min(scaledType = recurse()) }
      register { Requirement.Max(scaledType = recurse()) }
      register { Requirement.Exact(scaledType = recurse()) }
      register { Requirement.Or(setOfSize(choose(2, 2, 2, 2, 2, 3, 4))) }
      register { Requirement.And(listOfSize(choose(2, 2, 2, 2, 3))) }
      register { Requirement.Transform(recurse(), "PROD") }

      fun RandomGenerator<*>.intensity() = choose(3 to null, 1 to randomEnum<Intensity>())

      val instructionTypes =
          multiset(
              9 to Instruction.Gain::class,
              4 to Instruction.Remove::class,
              3 to Instruction.Per::class,
              2 to Instruction.Gated::class,
              2 to Instruction.Transmute::class,
              1 to Instruction.Custom::class,
              1 to Instruction.Then::class,
              3 to Instruction.Or::class,
              5 to Instruction.Multi::class,
              1 to Instruction.Transform::class,
          )
      register(Instruction::class) { recurse(choose(instructionTypes)) }
      register { Instruction.Gain(recurse(), intensity()) }
      register { Instruction.Remove(recurse(), intensity()) }
      register { Instruction.Per(recurse(), recurse()) }
      register { Instruction.Gated(recurse(), recurse()) }
      register { Instruction.Transmute(recurse(), recurse<ScaledTypeExpr>().scalar, intensity()) }
      register { Instruction.Custom("name", listOfSize(choose(1, 1, 1, 2))) }
      register { Instruction.Then(listOfSize(choose(2, 2, 2, 3))) }
      register { Instruction.Or(setOfSize(choose(2, 2, 2, 2, 3))) }
      register { Instruction.Multi(listOfSize(choose(2, 2, 2, 2, 2, 3, 4))) }
      register { Instruction.Transform(recurse(), "PROD") }

      register(From::class) {
        val one: TypeExpr = recurse()
        val two: TypeExpr = recurse()

        fun getTypes(typeExpr: TypeExpr): List<TypeExpr> =
            typeExpr.arguments.flatMap(::getTypes) + typeExpr

        val oneTypes = getTypes(one)
        val twoTypes = getTypes(two)

        val inject: TypeExpr
        val into: TypeExpr
        val target: TypeExpr

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

        fun convert(typeExpr: TypeExpr): From {
          if (typeExpr == target) {
            return SimpleFrom(if (b) inject else target, if (b) target else inject)
          }
          val args = typeExpr.arguments.map(::convert)
          return if (args.all { it is TypeAsFrom }) {
            TypeAsFrom(typeExpr)
          } else {
            ComplexFrom(typeExpr.className, args, typeExpr.refinement)
          }
        }
        convert(into)
      }

      val triggerTypes =
          multiset(
              9 to Trigger.WhenGain::class,
              2 to Trigger.WhenRemove::class,
              9 to Trigger.OnGainOf::class,
              5 to Trigger.OnRemoveOf::class,
              5 to Trigger.ByTrigger::class,
              1 to Trigger.Transform::class,
          )
      register(Trigger::class) { recurse(choose(triggerTypes)) }
      register { Trigger.WhenGain }
      register { Trigger.WhenRemove }
      register { Trigger.OnGainOf.create(recurse()) as Trigger.OnGainOf }
      register { Trigger.OnRemoveOf.create(recurse()) as Trigger.OnRemoveOf }
      register { Trigger.ByTrigger(recurse(), choose(1 to OWNER, 1 to cn("Player2"))) }
      register { Trigger.Transform(recurse(), "PROD") }

      register { Effect(recurse(), recurse(), choose(true, false)) }

      val costTypes =
          multiset(
              9 to Cost.Spend::class,
              3 to Cost.Per::class,
              3 to Cost.Or::class,
              2 to Cost.Multi::class,
              2 to Cost.Transform::class,
          )
      register(Cost::class) { recurse(choose(costTypes)) }
      register { Cost.Spend(scaledType = recurse()) }
      register { Cost.Per(recurse(), recurse()) }
      register { Cost.Or(setOfSize(choose(2, 2, 2, 2, 3, 4))) }
      register { Cost.Multi(listOfSize(choose(2, 2, 2, 3))) }
      register { Cost.Transform(recurse(), "PROD") }

      register { Action(choose(1 to null, 3 to recurse()), recurse()) }
    }

    override fun <T : PetNode> invoke(type: KClass<T>, gen: RandomGenerator<PetNode>): T? {
      return try {
        super.invoke(type, gen)
      } catch (e: PetException) {
        null // TODO this better
      }
    }

    fun RandomGenerator<PetNode>.refinement() =
        chooseS(9 to { null }, 1 to { recurse<Requirement>() })

    fun RandomGenerator<PetNode>.randomName() =
        choose("Foo", "Bar", "Qux", "Abc", "Xyz", "Ooh", "Ahh", "Eep", "Wau")
  }

  inline fun <reified T : PetNode> goNuts(count: Int = 10_000) {
    return goNuts(T::class, count)
  }

  fun <T : PetNode> goNuts(type: KClass<T>, count: Int = 10_000) {
    for (i in 1..count) {
      val randomNode = makeRandomNode(type)

      val originalStringOut = randomNode.toString()

      val reparsedNode =
          try {
            parseElement(type, originalStringOut)
          } catch (e: Exception) {
            fail("node was ${p2k(randomNode)}", e)
          }

      assertWithMessage("intermediate string form was $originalStringOut")
          .that(reparsedNode)
          .isEqualTo(randomNode)

      val regurgitated = reparsedNode.toString()
      assertWithMessage("intermediate parsed form was:\n${p2k(reparsedNode)}")
          .that(regurgitated)
          .isEqualTo(originalStringOut)
    }
  }

  inline fun <reified T : PetNode> findAverageTextLength(): Int {
    val samples = 1000
    val sum = (1..samples).sumOf { makeRandomNode<T>().toString().length }
    return (sum.toDouble() / samples).roundToInt()
  }

  inline fun <reified T : PetNode> printTestStrings(count: Int) {
    for (i in 1..count) {
      println(makeRandomNode<T>())
    }
  }

  inline fun <reified T : PetNode> printTestStringOfEachLength(maxLength: Int) {
    getTestStringOfEachLength<T>(maxLength).forEach(::println)
  }

  inline fun <reified T : PetNode> getTestStringOfEachLength(maxLength: Int): List<String> {
    require(maxLength >= 20) // just cause

    val set = sortedSetOf<String>(Comparator.comparing { it.length })

    // Don't track the short strings because some lengths might be impossible
    // If we didn't get to length 9 yet we probably weren't going to.
    var need = maxLength - 10
    var tried = 0
    while (need > 0) {
      if (tried++ == 100_000) error("whoops")
      val s = makeRandomNode<T>().toString()
      if (s.length <= maxLength) {
        if (set.add(s) && s.length > 10) need--
      }
    }
    return set.toList()
  }

  inline fun <reified T : PetNode> generateTestApiConstructions(count: Int = 10) {
    for (i in 1..count) {
      val node = makeRandomNode<T>()
      println("checkBothWays(\"$node\", ${p2k(node)})")
    }
  }

  inline fun <reified T : PetNode> uniqueNodes(
      count: Int = 100,
      depthLimit: Int = 10,
      stopAtDrySpell: Int = 200,
  ): Set<T> {
    val set = mutableSetOf<T>()
    var drySpell = 0
    while (set.size < count && drySpell < stopAtDrySpell) {
      val node = makeRandomNode<T>()
      if (node.descendantCount() <= depthLimit && set.add(node)) {
        drySpell = 0
      } else {
        drySpell++
      }
    }
    return set
  }
}

private fun <T : Any> multiset(vararg pairs: Pair<Int, T>): Multiset<T> {
  val result = HashMultiset<T>()
  pairs.forEach { (count, element) -> result.add(element, count) }
  return result
}

fun scaling(greed: Double, backoff: Double): (Int) -> Double {
  require(backoff >= 0)
  require(greed > -1.0)
  require(greed < 1.0)
  // must be in range -1..1
  return { (greed + 1) / (backoff + 1).pow(it) - 1 }
}
