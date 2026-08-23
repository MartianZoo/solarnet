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
                    action: "-> Plant",
                    effects: ["End: VictoryPoint"],
                  }],
                }
                """
            )
            .single()

    definition.asClassDeclaration.effects.shouldContainExactly(
        parse<Effect>("UseAction<This, First>: Plant"),
        parse<Effect>("End: VictoryPoint"),
    )
  }

  @Test
  fun standardResourceCostsBecomeInvoices() {
    val definition =
        JsonReader.readActions(
                """
                {
                  actions: [{ name: "ExampleSA", action: "4 Energy -> 2 Plant" }],
                }
                """
            )
            .single()

    definition.asClassDeclaration.effects.shouldContainExactly(
        parse<Effect>(
            "UseAction<This, First>: 4 Owed<Class<Energy>> THEN " +
                "Invoice<This, First, Class<Energy>>"
        ),
        parse<Effect>("-Invoice<This, First>: 2 Plant"),
    )
  }

  @Test
  fun standardResourceCostsMayBeReadFromAProperty() {
    val definition =
        JsonReader.readActions(
                """
                {
                  actions: [{ name: "ExampleSA", action: "1 / cost -> 2 Plant" }],
                }
                """
            )
            .single()

    definition.asClassDeclaration.effects.shouldContainExactly(
        parse<Effect>(
            "UseAction<This, First>: Owed<Class<Megacredit>> / This.cost THEN " +
                "Invoice<This, First>"
        ),
        parse<Effect>("-Invoice<This, First>: 2 Plant"),
    )
  }

  @Test
  fun variableStandardResourceCostsCarryTheChosenAmountAcrossPayment() {
    val definition =
        JsonReader.readActions(
                """
                {
                  actions: [{ name: "ExampleSA", action: "2X Energy -> 3X Plant, Steel" }],
                }
                """
            )
            .single()

    definition.asClassDeclaration.effects.shouldContainExactly(
        parse<Effect>(
            "UseAction<This, First>: 2X Owed<Class<Energy>> THEN " +
                "Invoice<This, First, Class<Energy>> THEN " +
                "MAX 0 Invoice: (3X Plant, Steel)"
        )
    )
  }
}
