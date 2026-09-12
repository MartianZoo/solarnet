package dev.martianzoo.engine

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.agenttestsupport.testAgent
import dev.martianzoo.pets.data.Actor.Companion.ADMIN
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class TemporaryCleanupTest {
  @Test
  internal fun mandatoryCleanupBlocksIndirectAncestorsAndSurvivesRollback() {
    val game =
        Engine.newGame(
            testGamePremise(
                """
                CLASS RootScope : Scope, System { HAS MAX 1 This }
                CLASS PhaseScope : TemporaryScope<RootScope>, System
                CLASS Middle<PhaseScope> : System { HAS MAX 1 This }
                CLASS Chore<Middle> : Barrier, System
                CLASS Unrelated : Temporary, System { -This:: OtherDone }
                CLASS OtherDone : System
                """
            )
        )
    val admin = game.testAgent(ADMIN).also { it.autoExecPolicy = NONE }

    admin.beginOperation("RootScope, PhaseScope, Middle, Chore, Unrelated")

    admin.count("PhaseScope") shouldBe 1
    admin.count("Middle") shouldBe 1
    admin.count("Chore") shouldBe 1
    admin.count("Unrelated") shouldBe 0
    admin.count("OtherDone") shouldBe 1
    game.tasks.isEmpty() shouldBe true
    game.isIdle() shouldBe false
    val unfinished = game.timeline.checkpoint()

    admin.beginOperation("-Chore")

    admin.count("RootScope") shouldBe 1
    admin.count("PhaseScope") shouldBe 0
    admin.count("Middle") shouldBe 0
    admin.count("Chore") shouldBe 0
    game.isIdle() shouldBe true

    game.timeline.rollBack(unfinished)
    admin.beginOperation("Ok")

    admin.count("PhaseScope") shouldBe 1
    admin.count("Middle") shouldBe 1
    admin.count("Chore") shouldBe 1
    game.isIdle() shouldBe false

    admin.beginOperation("-Chore")

    admin.count("PhaseScope") shouldBe 0
    admin.count("Middle") shouldBe 0
    admin.count("Chore") shouldBe 0
    game.isIdle() shouldBe true
  }

  @Test
  internal fun removalRechecksMandatoryCleanupCreatedForAnotherScope() {
    val game =
        Engine.newGame(
            testGamePremise(
                """
                CLASS RootScope : Scope, System { HAS MAX 1 This }
                CLASS Left : TemporaryScope<RootScope>, System {
                  -This IF Right:: Guard<Right>
                }
                CLASS Right : TemporaryScope<RootScope>, System {
                  -This IF Left:: Guard<Left>
                }
                CLASS Guard<Scope> : Barrier, System
                """
            )
        )
    val admin = game.testAgent(ADMIN).also { it.autoExecPolicy = NONE }

    admin.beginOperation("RootScope, Left, Right")

    admin.count("TemporaryScope") shouldBe 1
    admin.count("Guard") shouldBe 1
    game.tasks.isEmpty() shouldBe true
    game.isIdle() shouldBe false

    admin.beginOperation("-Guard")

    admin.count("RootScope") shouldBe 1
    admin.count("TemporaryScope") shouldBe 0
    admin.count("Guard") shouldBe 0
    game.isIdle() shouldBe true
  }

  @Test
  internal fun dependentTemporaryFinishesBeforeItsParent() {
    val game =
        Engine.newGame(
            testGamePremise(
                """
                CLASS Outer : Temporary, System { HAS MAX 1 This }
                CLASS Inner<Outer> : Temporary, System { -This: Followup }
                CLASS Followup : System
                """
            )
        )
    val admin = game.testAgent(ADMIN).also { it.autoExecPolicy = NONE }

    admin.beginOperation("Outer, Inner")

    admin.count("Outer") shouldBe 1
    admin.count("Inner") shouldBe 0
    admin.count("Followup") shouldBe 0
    game.tasks.isEmpty() shouldBe false

    admin.doTask("Followup")

    admin.count("Outer") shouldBe 0
    admin.count("Followup") shouldBe 1
    game.isIdle() shouldBe true
  }

  @Test
  internal fun siblingScopesWaitForWorkQueuedByEachRemoval() {
    val game =
        Engine.newGame(
            testGamePremise(
                """
                CLASS RootScope : Scope, System { HAS MAX 1 This }
                CLASS Left : TemporaryScope<RootScope>, System { -This: Followup }
                CLASS Right : TemporaryScope<RootScope>, System { -This: Followup }
                CLASS Followup : System
                """
            )
        )
    val admin = game.testAgent(ADMIN).also { it.autoExecPolicy = NONE }

    admin.beginOperation("RootScope, Left, Right")

    admin.count("TemporaryScope") shouldBe 1
    admin.count("Followup") shouldBe 0
    game.tasks.isEmpty() shouldBe false

    admin.doTask("Followup")

    admin.count("TemporaryScope") shouldBe 0
    admin.count("Followup") shouldBe 1
    game.tasks.isEmpty() shouldBe false
    game.isIdle() shouldBe false

    admin.doTask("Followup")

    admin.count("RootScope") shouldBe 1
    admin.count("Followup") shouldBe 2
    game.isIdle() shouldBe true
  }
}
