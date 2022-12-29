package dev.martianzoo.tfm.pets.ast

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.pets.PetsParser.parse
import dev.martianzoo.tfm.pets.actionToEffect
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
  }

  fun checkActionToEffect(action: String, index: Int, effect: String) {
    assertThat(actionToEffect(parse(action), index))
        .isEqualTo(parse<Effect>(effect))
  }

  @Test fun testActionsToEffects() {
    val actions: List<Action> = listOf(
        "-> Ok",
        "Energy -> Heat",
        "-> -Energy THEN Heat"
    ).map(::parse)
    val effects: List<Effect> = listOf(
        "UseAction1<This>: Ok",
        "UseAction2<This>: -Energy THEN Heat",
        "UseAction3<This>: -Energy THEN Heat"
    ).map(::parse)
  }
}
