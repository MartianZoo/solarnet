package dev.martianzoo.tfm.pets

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.pets.PetParser.parsePets
import dev.martianzoo.tfm.pets.ast.Action
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Effect.Trigger
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Gain
import dev.martianzoo.tfm.pets.ast.Instruction.Per
import dev.martianzoo.tfm.pets.ast.PetNode
import dev.martianzoo.tfm.pets.ast.QuantifiedExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression.Companion.gte
import dev.martianzoo.tfm.pets.ast.TypeExpression.GenericTypeExpression
import kotlin.reflect.KClass
import org.junit.jupiter.api.Test

private class TransformsTest {
  @Test
  fun testActionToEffect() {
    checkActionToEffect("-> Ok", 5, "UseAction5<This>: Ok")
    checkActionToEffect("5 -> Ok", 1, "UseAction1<This>: -5! THEN Ok")
    checkActionToEffect("Foo -> Bar, Qux", 3, "UseAction3<This>: -Foo! THEN (Bar, Qux)")
    checkActionToEffect("Foo -> Bar THEN Qux", 42, "UseAction42<This>: -Foo! THEN Bar THEN Qux")
    checkActionToEffect(
        "Microbe<Anyone> -> Microbe<This>",
        1,
        "UseAction1<This>: Microbe<This> FROM Microbe<Anyone>",
    )

    // t's not its job to recognize nonsense
    checkActionToEffect("Plant -> Plant", 2, "UseAction2<This>: Plant FROM Plant")
  }

  private fun checkActionToEffect(action: String, index: Int, effect: String) {
    assertThat(actionToEffect(parsePets(action), index)).isEqualTo(parsePets<Effect>(effect))
  }

  @Test
  fun testActionsToEffects() {
    val actions: List<Action> = listOf("-> Foo", "Foo -> 5 Bar").map(::parsePets)
    assertThat(actionsToEffects(actions)).containsExactly(
        parsePets<Effect>("UseAction1<This>: Foo"),
        parsePets<Effect>("UseAction2<This>: -Foo! THEN 5 Bar"),
    ).inOrder()
  }

  @Test
  fun testImmediateToEffect() {
    checkImmediateToEffect("Foo, Bar", "This: Foo, Bar")
    checkImmediateToEffect("Foo, Bar: Qux", "This: Foo, Bar: Qux")
    checkImmediateToEffect("Foo: Bar", "This: (Foo: Bar)")
  }

  private fun checkImmediateToEffect(immediate: String, effect: String) {
    assertThat(immediateToEffect(parsePets(immediate))).isEqualTo(parsePets<Effect>(effect))
  }

  @Test
  fun testFindAllClassNames() {
    val instr = parsePets<Instruction>('$' + "foo(Bar, Qux<Dog>)")
    assertThat(instr.childNodesOfType<ClassName>().map { it.asString })
        .containsExactly("Bar", "Qux", "Dog")
  }

  @Test
  fun testResolveSpecialThisType() {
    checkResolveThis<Instruction>("Foo<This>", gte("Bar"), "Foo<Bar>")
    checkResolveThis<Instruction>("Foo<This>", gte("Bar"), "Foo<Bar>")

    // looks like a plain textual replacement but we know what's really happening
    val petsIn = "-Ooh<Foo<Xyz, This, Qux>>: " +
        "5 Qux<Ooh, Xyz, Bar> OR 5 This?, =0 This: -Bar, 5: Foo<This>"
    val petsOut = "-Ooh<Foo<Xyz, It<Worked>, Qux>>: " +
        "5 Qux<Ooh, Xyz, Bar> OR 5 It<Worked>?, =0 It<Worked>: -Bar, 5: Foo<It<Worked>>"
    checkResolveThis<Effect>(petsIn, gte("It", gte("Worked")), petsOut)

    // allows nonsense
    checkResolveThis<Instruction>("This<Foo>", gte("Bar"), "This<Foo>")
  }

  private inline fun <reified P : PetNode> checkResolveThis(
      original: String,
      thiss: GenericTypeExpression,
      expected: String,
  ) {
    checkResolveThis(P::class, original, thiss, expected)
  }

  private fun <P : PetNode> checkResolveThis(
      type: KClass<P>,
      original: String,
      thiss: GenericTypeExpression,
      expected: String,
  ) {
    val parsedOriginal = parsePets(type, original)
    val parsedExpected = parsePets(type, expected)
    val tx = replaceThis(parsedOriginal, thiss)
    assertThat(tx).isEqualTo(parsedExpected)

    // more round-trip checking doesn't hurt
    assertThat(tx.toString()).isEqualTo(expected)
  }

  val resources = listOf(
      "StandardResource",
      "Megacredit",
      "Steel",
      "Titanium",
      "Plant",
      "Energy",
      "Heat").map { ClassName(it) }.toSet()

  @Test
  fun testDeprodify_noProd() {
    val s = "Foo<Bar>: Bax OR Qux"
    val e: Effect = parsePets(s)
    val ep: Effect = deprodify(e, resources)
    assertThat(ep.toString()).isEqualTo(s)
  }

  @Test
  fun testDeprodify_simple() {
    val prodden: Effect = parsePets("This: PROD[Plant / PlantTag]")
    val deprodden: Effect = deprodify(prodden, resources)
    assertThat(deprodden.toString()).isEqualTo("This: Production<Plant.CLASS> / PlantTag")
  }

  @Test
  fun testDeprodify_lessSimple() {
    // TODO adds unnecessary grouping, do we care?
    val prodden: Effect = parsePets("PROD[Plant]: PROD[Ooh?, Steel. / Ahh, Foo<Xyz FROM " +
        "Heat>, -Qux!, 5 Ahh<Qux> FROM StandardResource], Heat")
    val expected: Effect = parsePets(
        "Production<Plant.CLASS>: (Ooh?, Production<Steel.CLASS>. / Ahh, Foo<Xyz FROM " +
        "Production<Heat.CLASS>>, -Qux!, 5 Ahh<Qux> FROM Production<StandardResource.CLASS>), Heat")
    val deprodden: Effect = deprodify(prodden, resources)
    assertThat(deprodden).isEqualTo(expected)
  }

  @Test
  fun testNonProd() {
    val x = parsePets<Effect>("HAHA[Plant]: Heat, HAHA[Steel / 5 PowerTag]")
    assertThat(x).isEqualTo(
        Effect(
            Trigger.Transform(
                Trigger.OnGain(gte("Plant")),
                "HAHA"
            ),
            Instruction.Multi(
                Gain(QuantifiedExpression(gte("Heat"), 1)),
                Instruction.Transform(
                    Per(
                        Gain(QuantifiedExpression(gte("Steel"), 1)),
                        QuantifiedExpression(gte("PowerTag"), 5)
                    ),
                    "HAHA"
                )
            ),
            false
        )
    )
  }
}
