package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.api.Exceptions.InvalidPetDefinitionException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.data.ClassDeclaration
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import kotlin.test.Test

internal class PhaseTopologyCompilerTest {
  @Test
  internal fun `multiple segments add transitions directly to their phases`() {
    val lowered =
        PhaseTopologyCompiler.lower(
            parseClasses(
                """
                ABSTRACT CLASS Module { phaseAfter = Requirement? }
                ABSTRACT CLASS Phase { phaseSegment = Requirement? }
                CLASS AdvancePhase<Phase>
                CLASS WorkflowStarted

                CLASS FirstStart : Phase { phaseSegment = HAS "FirstEnd" }
                CLASS FirstEnd : Phase

                CLASS SecondStart : Phase {
                  phaseSegment = HAS "SecondEnd"
                  This:: AdvancePhase<This>
                }
                CLASS OptionalPhase : Phase
                CLASS SecondEnd : Phase
                CLASS OptionalModule : Module {
                  phaseAfter = HAS "OptionalPhase, SecondStart"
                }
                """
                    .trimIndent()
            )
        )

    lowered
        .declaration("FirstStart")
        .authoredEffects
        .shouldContainExactly(
            parse<Effect>("-AdvancePhase<This> IF WorkflowStarted:: FirstEnd FROM This")
        )
    lowered
        .declaration("SecondStart")
        .authoredEffects
        .shouldContainExactly(
            parse<Effect>("This:: AdvancePhase<This>"),
            parse<Effect>(
                "-AdvancePhase<This> IF WorkflowStarted, OptionalModule:: OptionalPhase FROM This"
            ),
            parse<Effect>(
                "-AdvancePhase<This> IF WorkflowStarted, MAX 0 OptionalModule:: SecondEnd FROM This"
            ),
        )
    lowered
        .declaration("OptionalPhase")
        .authoredEffects
        .shouldContainExactly(
            parse<Effect>("-AdvancePhase<This> IF WorkflowStarted:: SecondEnd FROM This")
        )
  }

  @Test
  internal fun `transitive order preserves routes when an intermediate module is absent`() {
    val lowered =
        PhaseTopologyCompiler.lower(
            parseClasses(
                """
                ABSTRACT CLASS Module { phaseAfter = Requirement? }
                ABSTRACT CLASS Phase { phaseSegment = Requirement? }
                CLASS AdvancePhase<Phase>
                CLASS WorkflowStarted
                CLASS Start : Phase { phaseSegment = HAS "End" }
                CLASS Middle : Phase
                CLASS Last : Phase
                CLASS End : Phase
                CLASS MiddleModule : Module { phaseAfter = HAS "Middle, Start" }
                CLASS LastModule : Module { phaseAfter = HAS "Last, Middle" }
                """
            )
        )

    lowered
        .declaration("Start")
        .authoredEffects
        .shouldContainExactly(
            parse<Effect>(
                "-AdvancePhase<This> IF WorkflowStarted, MiddleModule:: Middle FROM This"
            ),
            parse<Effect>(
                "-AdvancePhase<This> IF WorkflowStarted, LastModule, MAX 0 MiddleModule:: Last FROM This"
            ),
            parse<Effect>(
                "-AdvancePhase<This> IF WorkflowStarted, MAX 0 MiddleModule, MAX 0 LastModule:: End FROM This"
            ),
        )
    lowered
        .declaration("Middle")
        .authoredEffects
        .shouldContainExactly(
            parse<Effect>("-AdvancePhase<This> IF WorkflowStarted, LastModule:: Last FROM This"),
            parse<Effect>(
                "-AdvancePhase<This> IF WorkflowStarted, MAX 0 LastModule:: End FROM This"
            ),
        )
  }

  @Test
  internal fun `incomparable optional phases are rejected`() {
    val source =
        parseClasses(
            """
            ABSTRACT CLASS Module { phaseAfter = Requirement? }
            ABSTRACT CLASS Phase { phaseSegment = Requirement? }
            CLASS Start : Phase { phaseSegment = HAS "End" }
            CLASS Left : Phase
            CLASS Right : Phase
            CLASS End : Phase
            CLASS LeftModule : Module { phaseAfter = HAS "Left, Start" }
            CLASS RightModule : Module { phaseAfter = HAS "Right, Start" }
            """
                .trimIndent()
        )

    shouldThrow<InvalidPetDefinitionException> { PhaseTopologyCompiler.lower(source) }
  }

  private fun List<ClassDeclaration>.declaration(name: String): ClassDeclaration = single {
    it.className == cn(name)
  }
}
