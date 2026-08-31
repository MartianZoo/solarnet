package dev.martianzoo.tfm.pets

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.Transforming.actionListToEffects
import dev.martianzoo.pets.Transforming.actionToEffect
import dev.martianzoo.pets.Transforming.immediateToEffect
import dev.martianzoo.pets.Transforming.replaceOwnerWith
import dev.martianzoo.pets.Transforming.replaceThisExpressionsWith
import dev.martianzoo.pets.api.Exceptions.KindException
import dev.martianzoo.pets.ast.Action
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.NoOp
import dev.martianzoo.pets.ast.Instruction.Then
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.pets.ast.PetNode.Companion.replacer
import dev.martianzoo.pets.ast.PropertyValue
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.tfm.testlib.te
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.reflect.KClass
import kotlin.test.Test

internal class TransformingTest {
  @Test
  internal fun testActionToEffect() {
    fun checkActionToEffect(action: String, index: Int, effect: String) {
      val parsedA: Action = parse(action)
      val parsedE: Effect = parse(effect)
      actionToEffect(parsedA, index) shouldBe parsedE
    }

    checkActionToEffect("5 MC -> Ok", 1, "UseAction<This, First>: -5 MC! THEN Ok")
    checkActionToEffect("Foo -> Bar, Qux", 3, "UseAction<This, Third>: -Foo! THEN (Bar, Qux)")
    checkActionToEffect(
        "Microbe<Anyone> -> Microbe<This>!",
        1,
        "UseAction<This, First>: -Microbe<Anyone>! THEN Microbe<This>!",
    )

    checkActionToEffect("Plant -> Plant", 2, "UseAction<This, Second>: -Plant! THEN Plant")

    shouldThrow<IllegalArgumentException> { actionToEffect(parse("-> Ok"), 4) }
  }

  @Test
  internal fun testActionsToEffects() {
    val actions: List<Action> = listOf("-> Foo", "Foo -> 5 Bar").map(::parse)
    actionListToEffects(actions)
        .shouldContainExactly(
            parse<Effect>("UseAction<This, First>: Foo"),
            parse<Effect>("UseAction<This, Second>: -Foo! THEN 5 Bar"),
        )
  }

  @Test
  internal fun testImmediateToEffect() {
    fun checkImmediateToEffect(immediate: String, effect: String) {
      val immed: InstructionTree = parse(immediate)
      val fx: Effect = parse(effect)
      immediateToEffect(immed) shouldBe fx
    }

    checkImmediateToEffect("Foo, Bar", "This: Foo, Bar")
    checkImmediateToEffect("Foo, Bar: Qux", "This: Foo, Bar: Qux")
    checkImmediateToEffect("Foo: Bar", "This: (Foo: Bar)")
  }

  @Test
  internal fun instructionTransformPreservesTheKindNotTheConcreteType() {
    val original = parse<Instruction>("Plant") as Gain

    replacer(original, NoOp).transformInstruction(original) shouldBe NoOp
  }

  @Test
  internal fun instructionTransformDeduplicatesCollapsedOrArms() {
    val transformed =
        replaceOwnerWith(cn("Player1").expression)
            .transformInstructionTree(parse("Foo<Owner> OR Foo<Player1>"))

    transformed shouldBe parse<Instruction>("Foo<Player1>")
  }

  @Test
  internal fun expressionReplacementTraversesDeepCompositeTreesAndPropertyValues() {
    val foo = te("Foo")
    val player2 = te("Player2")
    val original =
        parse<InstructionTree>(
            "Foo BY Foo, EVAL Foo.requirement: Bar<Foo>, " +
                "Qux<Foo> / EVAL Foo.score THEN (EVAL Foo.requirement: Abc<Foo>, Xyz<Foo>)"
        )
    val transformer = replacer(foo, player2)

    val transformed = transformer.transformInstructionTree(original)

    transformed shouldBe
        parse<InstructionTree>(
            "Player2 BY Player2, EVAL Player2.requirement: Bar<Player2>, " +
                "Qux<Player2> / EVAL Player2.score THEN " +
                "(EVAL Player2.requirement: Abc<Player2>, Xyz<Player2>)"
        )
    original.descendantsOfType<Expression>().count { it == foo } shouldBe 9
    transformed.descendantsOfType<Expression>().none { it == foo } shouldBe true
    (transformed as InstructionGroup).instructions.size shouldBe 3
    val then = transformed.instructions.single { it is Then } as Then
    then.instructions.size shouldBe 2

    transformer.transformPropertyValue(
        PropertyValue.MetricValue(parse<Metric>("EVAL Foo.score"))
    ) shouldBe PropertyValue.MetricValue(parse<Metric>("EVAL Player2.score"))
    transformer.transformPropertyValue(
        PropertyValue.RequirementValue(parse<Requirement>("EVAL Foo.requirement"))
    ) shouldBe PropertyValue.RequirementValue(parse<Requirement>("EVAL Player2.requirement"))
  }

  @Test
  internal fun cardinalityExpansionRequiresTheInstructionTreeEntryPoint() {
    val original = parse<Instruction>("Foo")
    val expanded = parse<InstructionTree>("Bar, Qux")
    val transformer = replacer(original, expanded)

    transformer.transformInstructionTree(original) shouldBe expanded
    shouldThrow<KindException> { transformer.transformInstruction(original) }
  }

  @Test
  internal fun testResolveSpecialThisType() {
    checkResolveThis<Instruction>("Foo<This>", cn("Bar").expression, "Foo<Bar>")
    checkResolveThis<Instruction>("Foo<This>", cn("Bar").expression, "Foo<Bar>")

    // looks like a plain textual replacement but we know what's really happening
    val petsIn =
        "-Ooh<Foo<Xyz, This, Qux>>: " +
            "5 Qux<Ooh, Xyz, Bar> OR 5 This?, =0 This: -Bar, 5 MC: Foo<This>"
    val petsOut =
        "-Ooh<Foo<Xyz, It<Worked>, Qux>>: " +
            "5 Qux<Ooh, Xyz, Bar> OR 5 It<Worked>?, =0 It<Worked>: -Bar, 5 MC: Foo<It<Worked>>"
    checkResolveThis<Effect>(petsIn, te("It<Worked>"), petsOut)

    checkResolveThis<Instruction>("This<Foo>", cn("Bar").expression, "Bar<Foo>")
    checkResolveThis<Instruction>("This<Foo>", te("Bar<Qux>"), "Bar<Foo>")
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
    val parsedOriginal = parse(type, original)
    val parsedExpected = parse(type, expected)
    val transformer = replaceThisExpressionsWith(thiss)
    val tx =
        when (parsedOriginal) {
          is Effect -> transformer.transformEffect(parsedOriginal)
          is Instruction -> transformer.transformInstruction(parsedOriginal)
          else -> error("Test does not handle the ${parsedOriginal.kind} kind")
        }
    tx shouldBe parsedExpected

    // more round-trip checking doesn't hurt
    tx.toString() shouldBe expected
  }
}
