package dev.martianzoo.tfm.pets

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.pets.PetsParser.parse
import dev.martianzoo.tfm.pets.ast.Action
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.PetsNode
import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression.Companion.te
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

class TransformsTest {
  @Test
  fun testActionToEffect() {
    checkActionToEffect("-> Ok", 5,
        "UseAction5<This>: Ok")
    checkActionToEffect("5 -> Ok", 1,
        "UseAction1<This>: -5! THEN Ok")
    checkActionToEffect("Foo -> Bar, Qux", 3,
        "UseAction3<This>: -Foo! THEN (Bar, Qux)")
    checkActionToEffect("Foo -> Bar THEN Qux", 42,
        "UseAction42<This>: -Foo! THEN Bar THEN Qux")
    checkActionToEffect("Microbe<Anyone> -> Microbe<This>", 1,
        "UseAction1<This>: Microbe<This> FROM Microbe<Anyone>")

    // t's not its job to recognize nonsense
    checkActionToEffect("Plant -> Plant", 2,
        "UseAction2<This>: Plant FROM Plant")
  }

  private fun checkActionToEffect(action: String, index: Int, effect: String) {
    assertThat(actionToEffect(parse(action), index))
        .isEqualTo(parse<Effect>(effect))
  }

  @Test fun testActionsToEffects() {
    val actions: List<Action> = listOf(
        "-> Foo",
        "Foo -> 5 Bar"
    ).map(::parse)
    assertThat(actionsToEffects(actions)).containsExactly(
        parse<Effect>("UseAction1<This>: Foo"),
        parse<Effect>("UseAction2<This>: -Foo! THEN 5 Bar"),
    ).inOrder()
  }

  @Test fun testImmediateToEffect() {
    checkImmediateToEffect("Foo, Bar", "This: Foo, Bar")
    checkImmediateToEffect("Foo, Bar: Qux", "This: Foo, Bar: Qux")
    checkImmediateToEffect("Foo: Bar", "This: (Foo: Bar)")
  }

  private fun checkImmediateToEffect(immediate: String, effect: String) {
    assertThat(immediateToEffect(parse(immediate)))
        .isEqualTo(parse<Effect>(effect))
  }

  @Test fun testResolveSpecialThisType() {
    checkResolveThis<Instruction>("Foo<This>", te("Bar"), "Foo<Bar>")
    checkResolveThis<Instruction>("Foo<This>", te("Bar"), "Foo<Bar>")

    // looks like a plain textual replacement but we know what's really happening
    val petsIn = "-Ooh<Foo<Xyz, This, Qux>>: " +
        "5 Qux<Ooh, Xyz, Bar> OR 5 This?, =0 This: -Bar, 5: Foo<This>"
    val petsOut = "-Ooh<Foo<Xyz, It<Worked>, Qux>>: " +
        "5 Qux<Ooh, Xyz, Bar> OR 5 It<Worked>?, =0 It<Worked>: -Bar, 5: Foo<It<Worked>>"
    checkResolveThis<Effect>(petsIn, te("It", te("Worked")), petsOut)

    // allows nonsense
    checkResolveThis<Instruction>("This<Foo>", te("Bar"), "This<Foo>")
  }

  private inline fun <reified P : PetsNode> checkResolveThis(
      original: String,
      thiss: TypeExpression,
      expected: String) {
    checkResolveThis(P::class, original, thiss, expected)
  }

  private fun <P : PetsNode> checkResolveThis(
      type: KClass<P>,
      original: String,
      thiss: TypeExpression,
      expected: String) {
    val parsedOriginal = parse(type, original)
    val parsedExpected = parse(type, expected)
    val tx = resolveSpecialThisType(parsedOriginal, thiss)
    assertThat(tx).isEqualTo(parsedExpected)

    // more round-trip checking doesn't hurt
    assertThat(tx.toString()).isEqualTo(expected)
  }
}
