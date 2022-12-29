package dev.martianzoo.tfm.pets.ast

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.pets.PetsParser.parse
import dev.martianzoo.tfm.pets.actionToEffect
import dev.martianzoo.tfm.pets.actionsToEffects
import dev.martianzoo.tfm.pets.immediateToEffect
import org.junit.jupiter.api.Test

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

  fun checkActionToEffect(action: String, index: Int, effect: String) {
    assertThat(actionToEffect(parse(action), index))
        .isEqualTo(parse<Effect>(effect))
  }

  fun checkImmediateToEffect(immediate: String, effect: String) {
    assertThat(immediateToEffect(parse(immediate)))
        .isEqualTo(parse<Effect>(effect))
  }
}
