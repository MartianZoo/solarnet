package dev.martianzoo.tfm.pets

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.pets.AstTransforms.actionToEffect
import dev.martianzoo.tfm.pets.AstTransforms.actionsToEffects
import dev.martianzoo.tfm.pets.AstTransforms.deprodify
import dev.martianzoo.tfm.pets.AstTransforms.immediateToEffect
import dev.martianzoo.tfm.pets.AstTransforms.replaceTypes
import dev.martianzoo.tfm.pets.Parsing.parseElement
import dev.martianzoo.tfm.pets.SpecialClassNames.THIS
import dev.martianzoo.tfm.pets.ast.Action
import dev.martianzoo.tfm.pets.ast.Action.Companion.action
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Effect.Companion.effect
import dev.martianzoo.tfm.pets.ast.Effect.Trigger
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Companion.instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Gain
import dev.martianzoo.tfm.pets.ast.PetNode
import dev.martianzoo.tfm.pets.ast.ScalarAndType.Companion.sat
import dev.martianzoo.tfm.pets.ast.TypeExpr
import dev.martianzoo.tfm.testlib.te
import dev.martianzoo.util.toStrings
import kotlin.reflect.KClass
import org.junit.jupiter.api.Test

private class AstTransformsTest {
  @Test
  fun testActionToEffect() {
    fun checkActionToEffect(action: String, index: Int, effect: String) =
        assertThat(actionToEffect(action(action), index)).isEqualTo(effect(effect))

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

  @Test
  fun testActionsToEffects() {
    val actions: List<Action> = listOf("-> Foo", "Foo -> 5 Bar").map(::action)
    assertThat(actionsToEffects(actions))
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
    assertThat(childNodesOfType<ClassName>(instr).toStrings()).containsExactly("Bar", "Qux", "Dog")
  }

  @Test
  fun testResolveSpecialThisType() {
    checkResolveThis<Instruction>("Foo<This>", cn("Bar").type, "Foo<Bar>")
    checkResolveThis<Instruction>("Foo<This>", cn("Bar").type, "Foo<Bar>")

    // looks like a plain textual replacement but we know what's really happening
    val petsIn =
        "-Ooh<Foo<Xyz, This, Qux>>: " +
            "5 Qux<Ooh, Xyz, Bar> OR 5 This?, =0 This: -Bar, 5: Foo<This>"
    val petsOut =
        "-Ooh<Foo<Xyz, It<Worked>, Qux>>: " +
            "5 Qux<Ooh, Xyz, Bar> OR 5 It<Worked>?, =0 It<Worked>: -Bar, 5: Foo<It<Worked>>"
    checkResolveThis<Effect>(petsIn, te("It<Worked>"), petsOut)

    // allows nonsense
    checkResolveThis<Instruction>("This<Foo>", cn("Bar").type, "This<Foo>")
  }

  private inline fun <reified P : PetNode> checkResolveThis(
      original: String,
      thiss: TypeExpr,
      expected: String,
  ) {
    checkResolveThis(P::class, original, thiss, expected)
  }

  private fun <P : PetNode> checkResolveThis(
      type: KClass<P>,
      original: String,
      thiss: TypeExpr,
      expected: String,
  ) {
    val parsedOriginal = parseElement(type, original)
    val parsedExpected = parseElement(type, expected)
    val tx = replaceTypes(parsedOriginal, THIS.type, thiss)
    assertThat(tx).isEqualTo(parsedExpected)

    // more round-trip checking doesn't hurt
    assertThat(tx.toString()).isEqualTo(expected)
  }

  val resources =
      listOf("StandardResource", "Megacredit", "Steel", "Titanium", "Plant", "Energy", "Heat")
          .map { cn(it) }
          .toSet()

  @Test
  fun testDeprodify_noProd() {
    val s = "Foo<Bar>: Bax OR Qux"
    val e: Effect = effect(s)
    val ep: Effect = deprodify(e, resources)
    assertThat(ep.toString()).isEqualTo(s)
  }

  @Test
  fun testDeprodify_simple() {
    val prodden: Effect = effect("This: PROD[Plant / PlantTag]")
    val deprodden: Effect = deprodify(prodden, resources)
    assertThat(deprodden.toString()).isEqualTo("This: Production<Class<Plant>> / PlantTag")
  }

  @Test
  fun testDeprodify_lessSimple() {
    // TODO adds unnecessary grouping, do we care?
    val prodden: Effect =
        effect(
            "PROD[Plant]: PROD[Ooh?, Steel. / Ahh, Foo<Xyz FROM " +
                "Heat>, -Qux!, 5 Ahh<Qux> FROM StandardResource], Heat")
    val expected: Effect =
        effect(
            "Production<Class<Plant>>: (Ooh?, Production<Class<Steel>>. / Ahh, Foo<Xyz FROM " +
                "Production<Class<Heat>>>, -Qux!, 5 Ahh<Qux> FROM Production<Class<StandardResource>>), Heat")
    val deprodden: Effect = deprodify(prodden, resources)
    assertThat(deprodden).isEqualTo(expected)
  }

  @Test
  fun testNonProd() {
    val x = effect("HAHA[Plant]: Heat, HAHA[Steel / 5 PowerTag]")
    assertThat(x)
        .isEqualTo(
            Effect(
                Trigger.Transform(Trigger.OnGainOf.create(cn("Plant").type), "HAHA"),
                Instruction.Multi(
                    Gain(sat(1, cn("Heat").type)),
                    Instruction.Transform(
                        Instruction.Per(
                            Gain(sat(1, cn("Steel").type)), sat(5, cn("PowerTag").type)),
                        "HAHA")),
                false))
  }
}
