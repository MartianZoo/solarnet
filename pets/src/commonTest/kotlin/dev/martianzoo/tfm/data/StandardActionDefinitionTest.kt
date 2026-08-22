package dev.martianzoo.tfm.data

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class StandardActionDefinitionTest {
  @Test
  fun jsonNameIsTheCanonicalClassName() {
    val definition =
        JsonReader.readActions(
                """
                {
                  actions: [{ name: "ExampleSA", action: "-> Plant" }],
                }
                """
            )
            .single()

    definition.className shouldBe cn("ExampleSA")
    definition.asClassDeclaration.className shouldBe cn("ExampleSA")
  }

  @Test
  fun actionEffectsBelongToTheGeneratedClass() {
    val definition =
        JsonReader.readActions(
                """
                {
                  actions: [{
                    name: "ExampleSA",
                    action: "-> Payment<This, First>",
                    effects: ["CostPaid<This>: Plant"],
                  }],
                }
                """
            )
            .single()

    definition.asClassDeclaration.effects.shouldContainExactly(
        parse<Effect>("UseAction<This, First>: Payment<This, First>"),
        parse<Effect>("CostPaid<This>: Plant"),
    )
  }
}
