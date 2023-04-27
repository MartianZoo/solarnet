package dev.martianzoo.tfm.pets

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.api.SpecialClassNames.THIS
import dev.martianzoo.tfm.pets.Parsing.parseAsIs
import dev.martianzoo.tfm.pets.Transforming.actionListToEffects
import dev.martianzoo.tfm.pets.Transforming.actionToEffect
import dev.martianzoo.tfm.pets.Transforming.immediateToEffect
import dev.martianzoo.tfm.pets.ast.Action
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.PetNode
import dev.martianzoo.tfm.pets.ast.PetNode.Companion.replacer
import dev.martianzoo.tfm.testlib.te
import dev.martianzoo.util.toStrings
import kotlin.reflect.KClass
import org.junit.jupiter.api.Test

private class TransformingTest {
  @Test
  fun testActionToEffect() {
    fun checkActionToEffect(action: String, index: Int, effect: String) {
      val parsedA: Action = parseAsIs(action)
      val parsedE: Effect = parseAsIs(effect)
      assertThat(actionToEffect(parsedA, index)).isEqualTo(parsedE)
    }

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
    val actions: List<Action> = listOf("-> Foo", "Foo -> 5 Bar").map(::parseAsIs)
    assertThat(actionListToEffects(actions))
        .containsExactly(
            parseAsIs<Effect>("UseAction1<This>: Foo"),
            parseAsIs<Effect>("UseAction2<This>: -Foo! THEN 5 Bar"),
        )
        .inOrder()
  }

  @Test
  fun testImmediateToEffect() {
    fun checkImmediateToEffect(immediate: String, effect: String) {
      val immed: Instruction = parseAsIs(immediate)
      val fx: Effect = parseAsIs(effect)
      assertThat(immediateToEffect(immed)).isEqualTo(fx)
    }

    checkImmediateToEffect("Foo, Bar", "This: Foo, Bar")
    checkImmediateToEffect("Foo, Bar: Qux", "This: Foo, Bar: Qux")
    checkImmediateToEffect("Foo: Bar", "This: (Foo: Bar)")
  }

  @Test
  fun testFindAllClassNames() {
    val instr: Instruction = parseAsIs("@foo(Bar, Qux<Dog>)")
    assertThat(instr.descendantsOfType<ClassName>().toStrings())
        .containsExactly("Bar", "Qux", "Dog")
  }

  @Test
  fun testResolveSpecialThisType() {
    checkResolveThis<Instruction>("Foo<This>", cn("Bar").expression, "Foo<Bar>")
    checkResolveThis<Instruction>("Foo<This>", cn("Bar").expression, "Foo<Bar>")

    // looks like a plain textual replacement but we know what's really happening
    val petsIn =
        "-Ooh<Foo<Xyz, This, Qux>>: " +
            "5 Qux<Ooh, Xyz, Bar> OR 5 This?, =0 This: -Bar, 5: Foo<This>"
    val petsOut =
        "-Ooh<Foo<Xyz, It<Worked>, Qux>>: " +
            "5 Qux<Ooh, Xyz, Bar> OR 5 It<Worked>?, =0 It<Worked>: -Bar, 5: Foo<It<Worked>>"
    checkResolveThis<Effect>(petsIn, te("It<Worked>"), petsOut)

    // allows nonsense
    checkResolveThis<Instruction>("This<Foo>", cn("Bar").expression, "This<Foo>")
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
    val tx = replacer(THIS.expression, thiss).transform(parsedOriginal)
    assertThat(tx).isEqualTo(parsedExpected)

    // more round-trip checking doesn't hurt
    assertThat(tx.toString()).isEqualTo(expected)
  }
}
