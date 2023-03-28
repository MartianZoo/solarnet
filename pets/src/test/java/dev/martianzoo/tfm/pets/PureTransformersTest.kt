package dev.martianzoo.tfm.pets

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.api.SpecialClassNames.THIS
import dev.martianzoo.tfm.pets.Parsing.parseAsIs
import dev.martianzoo.tfm.pets.PureTransformers.actionListToEffects
import dev.martianzoo.tfm.pets.PureTransformers.actionToEffect
import dev.martianzoo.tfm.pets.PureTransformers.immediateToEffect
import dev.martianzoo.tfm.pets.PureTransformers.replaceAll
import dev.martianzoo.tfm.pets.ast.Action
import dev.martianzoo.tfm.pets.ast.Action.Companion.action
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Effect.Companion.effect
import dev.martianzoo.tfm.pets.ast.Effect.Trigger
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Companion.instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Gain
import dev.martianzoo.tfm.pets.ast.Metric.Count
import dev.martianzoo.tfm.pets.ast.Metric.Scaled
import dev.martianzoo.tfm.pets.ast.PetNode
import dev.martianzoo.tfm.pets.ast.ScaledExpression.Companion.scaledEx
import dev.martianzoo.tfm.testlib.te
import dev.martianzoo.util.toStrings
import kotlin.reflect.KClass
import org.junit.jupiter.api.Test

private class PureTransformersTest {
  @Test
  fun testActionToEffect() {
    fun checkActionToEffect(action: String, index: Int, effect: String) =
        assertThat(actionToEffect(action(action), index)).isEqualTo(effect(effect))

    checkActionToEffect("-> Ok", 5, "UseAction5<This>: Ok")
    checkActionToEffect("5 -> Ok", 1, "UseAction1<This>: -5! THEN Ok")
    checkActionToEffect("Foo -> Bar, Qux", 3, "UseAction3<This>: -Foo! THEN (Bar, Qux)")
    checkActionToEffect("Foo -> Bar THEN Qux", 42, "UseAction42<This>: -Foo! THEN Bar THEN Qux")
    checkActionToEffect(
        "Microbe<Anyone> -> Microbe<This>!",
        1,
        "UseAction1<This>: Microbe<This> FROM Microbe<Anyone>!", // TODO simplify
    )

    // t's not its job to recognize nonsense
    checkActionToEffect("Plant -> Plant", 2, "UseAction2<This>: Plant FROM Plant!")
  }

  @Test
  fun testActionsToEffects() {
    val actions: List<Action> = listOf("-> Foo", "Foo -> 5 Bar").map(::action)
    assertThat(actionListToEffects(actions))
        .containsExactly(
            effect("UseAction1<This>: Foo"),
            effect("UseAction2<This>: -Foo! THEN 5 Bar"),
        )
        .inOrder()
  }

  @Test
  fun testImmediateToEffect() {
    fun checkImmediateToEffect(immediate: String, effect: String) =
        assertThat(immediateToEffect(instruction(immediate))).isEqualTo(effect(effect))

    checkImmediateToEffect("Foo, Bar", "This: Foo, Bar")
    checkImmediateToEffect("Foo, Bar: Qux", "This: Foo, Bar: Qux")
    checkImmediateToEffect("Foo: Bar", "This: (Foo: Bar)")
  }

  @Test
  fun testFindAllClassNames() {
    val instr = instruction("@foo(Bar, Qux<Dog>)")
    assertThat(instr.descendantsOfType<ClassName>().toStrings())
        .containsExactly("Bar", "Qux", "Dog")
  }

  @Test
  fun testResolveSpecialThisType() {
    checkResolveThis<Instruction>("Foo<This>", cn("Bar").expr, "Foo<Bar>")
    checkResolveThis<Instruction>("Foo<This>", cn("Bar").expr, "Foo<Bar>")

    // looks like a plain textual replacement but we know what's really happening
    val petsIn =
        "-Ooh<Foo<Xyz, This, Qux>>: " +
            "5 Qux<Ooh, Xyz, Bar> OR 5 This?, =0 This: -Bar, 5: Foo<This>"
    val petsOut =
        "-Ooh<Foo<Xyz, It<Worked>, Qux>>: " +
            "5 Qux<Ooh, Xyz, Bar> OR 5 It<Worked>?, =0 It<Worked>: -Bar, 5: Foo<It<Worked>>"
    checkResolveThis<Effect>(petsIn, te("It<Worked>"), petsOut)

    // allows nonsense
    checkResolveThis<Instruction>("This<Foo>", cn("Bar").expr, "This<Foo>")
  }

  private inline fun <reified P : PetNode> checkResolveThis(
      original: String,
      thiss: Expression,
      expected: String,
  ) {
    checkResolveThis(P::class, original, thiss, expected)
  }

  private fun <P : PetNode> checkResolveThis(
      type: KClass<P>,
      original: String,
      thiss: Expression,
      expected: String,
  ) {
    val parsedOriginal = parseAsIs(type, original)
    val parsedExpected = parseAsIs(type, expected)
    val tx = parsedOriginal.replaceAll(THIS.expr, thiss)
    assertThat(tx).isEqualTo(parsedExpected)

    // more round-trip checking doesn't hurt
    assertThat(tx.toString()).isEqualTo(expected)
  }

  @Test
  fun testNonProd() {
    val x = effect("HAHA[Plant]: Heat, HAHA[Steel / 5 PowerTag]")
    assertThat(x)
        .isEqualTo(
            Effect(
                Trigger.Transform(Trigger.OnGainOf.create(cn("Plant").expr), "HAHA"),
                Instruction.Multi(listOf(
                    Gain(scaledEx(1, cn("Heat").expr)),
                    Instruction.Transform(
                        Instruction.Per(
                            Gain(scaledEx(1, cn("Steel").expr)),
                            Scaled(5, Count(cn("PowerTag").expr))),
                        "HAHA"))),
                false))
  }
}
