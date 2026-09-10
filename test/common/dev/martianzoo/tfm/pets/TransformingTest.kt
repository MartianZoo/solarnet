package dev.martianzoo.tfm.pets

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.Transforming.replaceOwnerWith
import dev.martianzoo.pets.api.Exceptions.KindException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.NoOp
import dev.martianzoo.pets.ast.Instruction.Then
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.PetNode.Companion.replacer
import dev.martianzoo.pets.ast.PropertyValue
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.tfm.testlib.te
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/**
 * The [dev.martianzoo.pets.PetTransformer] contract. What the individual transformations *mean* is
 * `docs/pets-language-spec.md` (L9-4, L9-6, L12-2).
 */
internal class TransformingTest {

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
}
