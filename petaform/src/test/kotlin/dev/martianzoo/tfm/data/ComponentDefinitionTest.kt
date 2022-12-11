package dev.martianzoo.tfm.data

import com.google.common.collect.MultimapBuilder
import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.petaform.Action
import dev.martianzoo.tfm.petaform.Action.Cost
import dev.martianzoo.tfm.petaform.Component
import dev.martianzoo.tfm.petaform.Effect
import dev.martianzoo.tfm.petaform.Effect.Trigger
import dev.martianzoo.tfm.petaform.Instruction
import dev.martianzoo.tfm.petaform.Instruction.FromExpression
import dev.martianzoo.tfm.petaform.PetaformNode
import dev.martianzoo.tfm.petaform.PetaformParser.parse
import dev.martianzoo.tfm.petaform.Predicate
import dev.martianzoo.tfm.petaform.QuantifiedExpression
import dev.martianzoo.tfm.petaform.TypeExpression
import dev.martianzoo.tfm.types.ComponentClassLoader
import org.junit.jupiter.api.Test

// Not testing much, just a bit of the canon data
class ComponentDefinitionTest {

  inline fun <reified P : PetaformNode> testRoundTrip(node: P) {
    assertThat(parse<P>(node.toString())).isEqualTo(node)
  }

  inline fun <reified P : PetaformNode> testRoundTrip(str: String) {
    assertThat(parse<P>(str).toString()).isEqualTo(str)
  }

  @Test
  fun foo() {
    val data = Canon.componentDefinitions
    val tr = data["TerraformRating"]!!
    assertThat(tr.name).isEqualTo("TerraformRating")
    assertThat(tr.abstract).isFalse()
    assertThat(tr.supertypesText).containsExactly("Owned<Player>")
    assertThat(tr.dependenciesText).isEmpty()
    assertThat(tr.effectsText).containsExactly("ProductionPhase: 1", "End: VictoryPoint")
  }

  @Test
  fun slurp() {
    val defns = Canon.allDefinitions
    assertThat(defns.size).isGreaterThan(550)

    val loader = ComponentClassLoader()
    loader.loadAll(defns.values)
    val table = loader.snapshot()

    table.all().forEach { clazz ->
      val def = defns[clazz.name]!!
      if (def.supertypesText.isNotEmpty()) {
        // checkRoundTrip(cc.supertypesText, rc.superclasses)
      }
      checkRoundTrip(listOfNotNull(def.immediateText), listOfNotNull(clazz.immediate))
      checkRoundTrip(def.actionsText, clazz.actions)
      checkRoundTrip(def.effectsText, clazz.effects)
      // deps??
    }
  }

  @Test fun nuts() {
    val defns = Canon.allDefinitions
    val loader = ComponentClassLoader()
    loader.loadAll(defns.values)
    val table = loader.snapshot()

    val mmap = MultimapBuilder.treeKeys().hashSetValues().build<String, PetaformNode>()
    table.all().forEach { clazz ->
      (clazz.effects + clazz.actions + listOfNotNull(clazz.immediate))
          .map { san(it) }
          .flatMap { listOf(it) + it.descendants() }
          .forEach { mmap.put(it::class.qualifiedName, it) }
    }

    mmap.keySet().forEach {
      println(it)
      println()
      mmap.get(it).map { it.toString() }.sorted().forEach(::println)
      println()
      println()
    }
  }

  fun san(i: Int?): Int? {
    return when (i) {
      null -> null
      0 -> 0
      else -> 5
    }
  }

  fun <P : PetaformNode> san(coll: Iterable<P>) = coll.map { san(it) }.sortedBy { it.toString().length }

  fun <P : PetaformNode?> san(n: P): P {
    if (n == null) return null as P
    return n.apply {
      when (this) {
        is TypeExpression -> TypeExpression("Foo", san(specializations), san(predicate))
        is QuantifiedExpression -> QuantifiedExpression(san(typeExpression), san(scalar))

        is Predicate.Or -> Predicate.Or(san(predicates))
        is Predicate.And -> Predicate.And(san(predicates))
        is Predicate.Min -> Predicate.Min(san(qe))
        is Predicate.Max -> Predicate.Max(san(qe))
        is Predicate.Exact -> Predicate.Exact(san(qe))
        is Predicate.Prod -> Predicate.Prod(san(predicate))

        is Instruction.Gain -> copy(san(qe))
        is Instruction.Remove -> copy(san(qe))
        is Instruction.Gated -> Instruction.Gated(san(predicate), san(instruction))
        is Instruction.Then -> Instruction.Then(san(instructions))
        is Instruction.Or -> Instruction.Or(san(instructions))
        is Instruction.Multi -> Instruction.Multi(san(instructions))
        is Instruction.Transmute -> copy(san(trans), san(scalar))
        is Instruction.FromIsBelow -> Instruction.FromIsBelow("Foo", san(specializations), san(predicate))
        is Instruction.FromIsRightHere -> Instruction.FromIsRightHere(san(to), san(from))
        is Instruction.FromIsNowhere -> Instruction.FromIsNowhere(san(type))
        is Instruction.Per -> Instruction.Per(san(instruction), san(qe))
        is Instruction.Prod -> Instruction.Prod(san(instruction))
        is Instruction.Custom -> Instruction.Custom("foo", san(arguments))

        is Trigger.OnGain -> copy(san(expression))
        is Trigger.OnRemove -> copy(san(expression))
        is Trigger.Conditional -> copy(san(trigger), san(predicate))
        is Trigger.Now -> Trigger.Now(san(predicate))
        is Trigger.Prod -> copy(san(trigger))
        is Effect -> copy(san(trigger), san(instruction))

        is Cost.Spend -> copy(san(qe))
        is Cost.Per -> copy(san(cost), san(qe))
        is Cost.Or -> copy(san(costs))
        is Cost.Multi -> copy(san(costs))
        is Cost.Prod -> copy(san(cost))
        is Action -> copy(san(cost), san(instruction))

        is Predicate -> TODO()
        is Instruction -> TODO()
        is Cost -> TODO()
        is FromExpression -> TODO()
        is Trigger -> TODO()
        is Component -> TODO()

        else -> { error("this really oughtta be impossible") }
      } as P
    }
  }

  fun checkRoundTrip(source: Collection<String>, cooked: Collection<PetaformNode>) {
    assertThat(source.size).isEqualTo(cooked.size)
    source.zip(cooked).forEach {
      assertThat("${it.second}").isEqualTo(it.first)
    }
  }
}
