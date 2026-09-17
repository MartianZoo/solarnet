package dev.martianzoo.engine

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.agenttestsupport.testAgent
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.Actor.Companion.ADMIN
import dev.martianzoo.state.GameEvent.ChangeEvent
import dev.martianzoo.state.GameEvent.TaskAddedEvent
import dev.martianzoo.state.GameEvent.TaskEditedEvent
import dev.martianzoo.state.GameEvent.TaskRemovedEvent
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.TfmCatalog
import dev.martianzoo.tfm.engine.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class PhantomTypeTest {
  private fun agent() = Engine.newGame(canonicalPremise()).testAgent(ADMIN)

  @Test
  internal fun `uninhabited types and their class literals count zero`() {
    val game = Engine.newGame(canonicalPremise())
    val agent = game.testAgent(ADMIN)
    val venusTag = agent.resolve("VenusTag")

    agent.count("VenusTag") shouldBe 0
    agent.count("Class<VenusTag>") shouldBe 0
    game.classTable.isInhabited(venusTag) shouldBe false
    game.classTable.isInhabited(agent.resolve("Class<VenusTag>")) shouldBe false
    game.reader.count(venusTag) shouldBe 0
    game.reader.countComponent(venusTag) shouldBe 0
    game.reader.getComponents(venusTag).isEmpty() shouldBe true
  }

  @Test
  internal fun `unknown names remain errors`() {
    val agent = agent()

    shouldThrow<ExpressionException> { agent.count("Typo") }
    shouldThrow<ExpressionException> { agent.count("Class<Typo>") }
  }

  @Test
  internal fun `an included abstract Class without a concrete narrowing is uninhabited`() {
    val game =
        Engine.newGame(
            testGamePremise("ABSTRACT CLASS Empty\nCLASS Holder<Empty>\nCLASS Live", players = 1)
        )
    val agent = game.testAgent(PLAYER1)

    game.classTable.isInhabited(cn("Empty")) shouldBe false
    game.classTable.isInhabited(cn("Holder")) shouldBe false
    game.classTable.isInhabited(cn("Live")) shouldBe true
    agent.count("Empty") shouldBe 0
    agent.count("Class<Empty>") shouldBe 0
    agent.count("Class<Holder>") shouldBe 0
    agent.count("Class<Live>") shouldBe 1
    shouldThrow<DeadEndException> { agent.runOperation("Empty!") }
  }

  @Test
  internal fun `optional and amap phantom changes do nothing while mandatory changes die`() {
    val agent = agent()

    agent.runOperation("VenusTag?")
    agent.runOperation("VenusTag.")
    agent.runOperation("-VenusTag?")
    agent.runOperation("-VenusTag.")
    shouldThrow<DeadEndException> { agent.runOperation("VenusTag!") }
    shouldThrow<DeadEndException> { agent.runOperation("-VenusTag!") }
    agent.count("VenusTag") shouldBe 0
  }

  @Test
  internal fun `choices discard mandatory phantom branches`() {
    val agent = agent()

    agent.runOperation("VenusTag! OR Plant<Player1>!")
    agent.runOperation("Die! OR Plant<Player1>!")

    agent.count("Plant<Player1>") shouldBe 2
  }

  @Test
  internal fun `choices discard both structurally empty and zero-capacity branches`() {
    val game =
        Engine.newGame(
            testGamePremise(
                """
                ABSTRACT CLASS Empty
                CLASS ZeroCapacity { HAS MAX 0 This }
                CLASS Live
                """,
                players = 0,
            )
        )
    val agent = game.testAgent(ADMIN)

    agent.runOperation("Empty! OR Live!")
    agent.runOperation("ZeroCapacity! OR Live!")
    agent.runOperation("Empty?")
    agent.runOperation("ZeroCapacity?")

    agent.count("Live") shouldBe 2
  }

  @Test
  internal fun `an optional uninhabited effect adds no task lifecycle`() {
    val game =
        Engine.newGame(
            testGamePremise(
                """
                ABSTRACT CLASS Empty
                CLASS Source { HAS MAX 1 This; This: Empty? }
                """,
                players = 0,
            )
        )
    val agent = game.testAgent(ADMIN)
    val before = game.timeline.checkpoint()

    agent.runOperation("Source")

    game.tasks.isEmpty() shouldBe true
    game.events
        .entriesSince(before)
        .map { it::class }
        .shouldContainExactly(
            TaskAddedEvent::class,
            TaskEditedEvent::class,
            ChangeEvent::class,
            TaskRemovedEvent::class,
        )
  }

  @Test
  internal fun `an uninhabited effect choice is pruned before its task is added`() {
    val game =
        Engine.newGame(
            testGamePremise(
                """
                ABSTRACT CLASS Empty
                CLASS Live
                CLASS Source { HAS MAX 1 This; This: Empty! OR Live! }
                """,
                players = 0,
            )
        )
    val agent = game.testAgent(ADMIN).also { it.autoExecPolicy = NONE }

    agent.beginOperation("Source") {
      game.tasks.extract { it.instruction.toString() }.shouldContainExactly("Live!")
    }
  }

  @Test
  internal fun `component effects cannot quietly lose locked expansion classes`() {
    val probeCatalog =
        object : TfmCatalog() {
          override val explicitClassDeclarations =
              parseClasses(
                      """
                      CLASS PhantomEffectProbe {
                        HAS =1 This
                        This: VenusTag?
                        This: VenusTag.
                        VenusTag: Plant<Player1>!
                      }
                      """
                          .trimIndent()
                  )
                  .toSet()
        }
    // The probe names Player1, so the seats have to exist before its declaration is loaded.
    val premise =
        canonicalPremise(
            catalog = TfmCatalog.Composite(Canon.withPlayers(2), probeCatalog),
            initialComponentTypes = setOf(cn("PhantomEffectProbe").expression),
        )

    shouldThrow<IllegalArgumentException> { Engine.newGame(premise) }
  }
}
