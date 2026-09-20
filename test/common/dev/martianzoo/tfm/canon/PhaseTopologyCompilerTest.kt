package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.api.Exceptions.InvalidPetDefinitionException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.data.ClassDeclaration
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotContain
import kotlin.test.Test

internal class PhaseTopologyCompilerTest {
  @Test
  internal fun `multiple segments compile while an authored scope keeps its wake policy`() {
    val lowered =
        PhaseTopologyCompiler.lower(
            parseClasses(
                """
                ABSTRACT CLASS Module { phaseAfter = Requirement? }
                ABSTRACT CLASS Phase { phaseSegment = Requirement? }
                ABSTRACT CLASS PhaseScope<Phase>
                CLASS Temporary
                CLASS WorkflowStarted

                CLASS FirstStart : Phase { phaseSegment = HAS "FirstStart, FirstEnd" }
                CLASS FirstEnd : Phase

                CLASS SecondStart : Phase { phaseSegment = HAS "SecondStart, SecondEnd" }
                CLASS OptionalPhase : Phase
                CLASS SecondEnd : Phase
                CLASS OptionalModule : Module {
                  phaseAfter = HAS "OptionalPhase, SecondStart"
                }
                CLASS SecondStartScope : PhaseScope<SecondStart>
                """
                    .trimIndent()
            )
        )

    lowered.declaration("FirstStartScope").supertypes.map { it.className } shouldContain
        cn("Temporary")
    lowered.declaration("SecondStartScope").supertypes.map { it.className } shouldNotContain
        cn("Temporary")
    lowered
        .declaration("SecondStartScope")
        .authoredEffects
        .shouldContainExactly(
            parse<Effect>(
                "-This IF WorkflowStarted, OptionalModule:: OptionalPhase FROM SecondStart"
            ),
            parse<Effect>(
                "-This IF WorkflowStarted, MAX 0 OptionalModule:: SecondEnd FROM SecondStart"
            ),
        )
    lowered.declaration("SecondStart").authoredEffects shouldContain
        parse<Effect>("This IF WorkflowStarted:: SecondStartScope")
    lowered
        .declaration("OptionalPhaseScope")
        .authoredEffects
        .shouldContainExactly(
            parse<Effect>("-This IF WorkflowStarted:: SecondEnd FROM OptionalPhase")
        )
  }

  @Test
  internal fun `incomparable optional phases are rejected`() {
    val source =
        parseClasses(
            """
            ABSTRACT CLASS Module { phaseAfter = Requirement? }
            ABSTRACT CLASS Phase { phaseSegment = Requirement? }
            CLASS Start : Phase { phaseSegment = HAS "Start, End" }
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
