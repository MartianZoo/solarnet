package dev.martianzoo.engine

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.agenttestsupport.testAgent
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.NotFullySpecifiedException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.testsupport.PLAYER1
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test

internal class TaskIntersectionTest {
  private val game =
      Engine.newGame(
          testGamePremise(
              """
      ABSTRACT CLASS Color {
        HAS MAX 1 This
        CLASS Red
        CLASS Blue
      }
      ABSTRACT CLASS Shape {
        HAS MAX 1 This
        CLASS Circle
        CLASS Square
      }
      CLASS Piece<Color, Shape>
      CLASS Notice<Color>
      CLASS Marker
      CLASS RedOffer { This: Piece<Red, Shape> }
      CLASS BlueOffer { This: Piece<Blue, Shape> }
      ABSTRACT CLASS LeftChoice
      ABSTRACT CLASS RightChoice
      CLASS FirstChoice : LeftChoice, RightChoice
      CLASS SecondChoice : LeftChoice, RightChoice
      """
          )
      )
  private val agent = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }

  init {
    agent.runOperation("Red, Blue, Circle, Square")
  }

  @Test
  fun `input and task can constrain different dependencies`() {
    agent.addTasks("Piece<Red, Shape>")

    agent.doTask("Piece<Color, Circle>")

    agent.count("Piece<Red, Circle>") shouldBe 1
    agent.count("Piece<Blue, Circle>") shouldBe 0
    game.tasks.isEmpty() shouldBe true
  }

  @Test
  fun `selecting a compatible instruction also commits its narrowing`() {
    agent.addTasks("Piece<Red, Shape>")

    agent.selectTask("Piece<Color, Circle>")

    agent.count("Piece<Red, Circle>") shouldBe 1
    game.tasks.isEmpty() shouldBe true
  }

  @Test
  fun `selection cannot discard a choice when resolving a gate splits the task`() {
    val task = agent.addTasks("MAX 0 Marker: (Piece<Red, Shape>, Marker)").single()
    val before = game.timeline.checkpoint()
    val original = game.tasks.getTaskData(task)

    for (narrowing in
        listOf("Piece<Red, Circle>, Marker", "MAX 0 Marker: (Piece<Red, Circle>, Marker)")) {
      shouldThrow<TaskException> { agent.selectTask(narrowing) }
          .message
          .shouldContain("splits during selection")

      game.timeline.checkpoint() shouldBe before
      game.tasks.getTaskData(task) shouldBe original
      agent.count("Piece") shouldBe 0
      agent.count("Marker") shouldBe 0
    }

    agent.selectTask(task)
    agent.selectTask("Piece<Red, Circle>")
    agent.doTask("Marker")

    agent.count("Piece<Red, Circle>") shouldBe 1
    agent.count("Piece<Red, Square>") shouldBe 0
    agent.count("Marker") shouldBe 1
    game.tasks.isEmpty() shouldBe true
  }

  @Test
  fun `a task copy binds linked X without a form`() {
    agent.addTasks("X Marker THEN X Notice<Red>")
    val pending = agent.tasks.extract { it }.single()

    agent.selectTask(pending.bindXTo(3))
    agent.doTask("3 Notice<Red>")

    agent.count("Marker") shouldBe 3
    agent.count("Notice<Red>") shouldBe 3
  }

  @Test
  fun `a broader input inherits the task constraint`() {
    agent.addTasks("Piece<Red, Circle>")

    agent.doTask("Piece<Color, Shape>")

    agent.count("Piece<Red, Circle>") shouldBe 1
    game.tasks.isEmpty() shouldBe true
  }

  @Test
  fun `a strict match does not hide another intersecting task`() {
    agent.addTasks("Piece<Color, Circle>, Piece<Red, Shape>")
    val before = game.timeline.checkpoint()

    shouldThrow<TaskException> { agent.doTask("Piece<Color, Circle>") }
        .message
        .shouldContain("ambiguous")

    game.timeline.checkpoint() shouldBe before
    agent.count("Piece") shouldBe 0
  }

  @Test
  fun `a task id disambiguates intersections`() {
    agent.addTasks("Piece<Blue, Shape>")
    val red = agent.addTasks("Piece<Red, Shape>").single()

    agent.doTask("Piece<Color, Circle>", red)

    agent.count("Piece<Red, Circle>") shouldBe 1
    agent.count("Piece<Blue>") shouldBe 0
    game.tasks.ids().size shouldBe 1
  }

  @Test
  fun `selected tasks also accept intersections`() {
    val red = agent.addTasks("Piece<Red, Shape>").single()
    agent.selectTask(red)

    agent.doTask("Piece<Color, Circle>")

    agent.count("Piece<Red, Circle>") shouldBe 1
    game.tasks.isEmpty() shouldBe true
  }

  @Test
  fun `intersection narrows both sides of a transmutation`() {
    agent.runOperation("Piece<Blue, Circle>")
    agent.addTasks("Piece<Red, Shape> FROM Piece<Blue, Shape>")

    agent.doTask("Piece<Color, Circle> FROM Piece<Color, Circle>")

    agent.count("Piece<Blue, Circle>") shouldBe 0
    agent.count("Piece<Red, Circle>") shouldBe 1
  }

  @Test
  fun `removal retains constraints from both inputs`() {
    agent.runOperation("Piece<Red, Circle>, Piece<Red, Square>, Piece<Blue, Circle>")
    agent.addTasks("-Piece<Red, Shape>")

    agent.doTask("-Piece<Color, Circle>")

    agent.count("Piece<Red, Circle>") shouldBe 0
    agent.count("Piece<Red, Square>") shouldBe 1
    agent.count("Piece<Blue, Circle>") shouldBe 1
  }

  @Test
  fun `intersection selects an OR arm`() {
    agent.addTasks("Piece<Red, Shape> OR Marker")

    agent.doTask("Piece<Color, Circle>")

    agent.count("Piece<Red, Circle>") shouldBe 1
    agent.count("Marker") shouldBe 0
  }

  @Test
  fun `intersection does not choose among remaining alternatives`() {
    agent.addTasks("Piece<Red, Circle> OR Piece<Red, Square>")
    val before = game.timeline.checkpoint()

    shouldThrow<NotFullySpecifiedException> { agent.doTask("Piece<Color, Shape>") }

    game.timeline.checkpoint() shouldBe before
    agent.count("Piece") shouldBe 0
  }

  @Test
  fun `disjoint choices and incompatible counts leave tasks unchanged`() {
    agent.addTasks("2 Piece<Red, Shape>")
    val before = game.timeline.checkpoint()

    shouldThrow<TaskException> { agent.doTask("2 Piece<Blue, Circle>") }
    shouldThrow<TaskException> { agent.doTask("3 Piece<Color, Circle>") }

    game.timeline.checkpoint() shouldBe before
    agent.count("Piece") shouldBe 0
  }

  @Test
  fun `ordinary narrowTask still requires a narrowing`() {
    agent.selectTask(agent.addTasks("Piece<Red, Shape>").single())
    val before = game.timeline.checkpoint()

    shouldThrow<NarrowingException> { agent.narrowTask("Piece<Color, Circle>") }

    game.timeline.checkpoint() shouldBe before
    agent.count("Piece") shouldBe 0
  }

  @Test
  fun `a THEN head intersection retains the shared choice in its continuation`() {
    agent.addTasks("Piece<Chosen@Color, Circle> THEN Notice<Chosen@Color>")

    agent.doTask("Piece<Red, Shape>")
    agent.doTask("Notice<Color>")

    agent.count("Piece<Red, Circle>") shouldBe 1
    agent.count("Notice<Red>") shouldBe 1
    agent.count("Notice<Blue>") shouldBe 0
  }

  @Test
  fun `context class disambiguates intersecting tasks`() {
    agent.addTasks("RedOffer, BlueOffer")
    agent.doTask("RedOffer")
    agent.doTask("BlueOffer")

    agent.doTask("Piece<Color, Circle>", cn("RedOffer"))

    agent.count("Piece<Red, Circle>") shouldBe 1
    agent.count("Piece<Blue, Circle>") shouldBe 0
    game.tasks.ids().size shouldBe 1
  }

  @Test
  fun `omitted quantifiers still inherit while explicit incompatible quantifiers fail`() {
    agent.addTasks("Piece<Red, Shape>.")
    val before = game.timeline.checkpoint()

    shouldThrow<TaskException> { agent.doTask("Piece<Color, Circle>!") }
    game.timeline.checkpoint() shouldBe before
    agent.doTask("Piece<Color, Circle>")

    agent.count("Piece<Red, Circle>") shouldBe 1
  }

  @Test
  fun `intersection works after resolving a gate`() {
    agent.runOperation("Marker")
    agent.addTasks("Marker: Piece<Red, Shape>")

    agent.doTask("Piece<Color, Circle>")

    agent.count("Piece<Red, Circle>") shouldBe 1
    game.tasks.isEmpty() shouldBe true
  }

  @Test
  fun `submitted groups execute the intersection of every member`() {
    agent.addTasks("(Piece<Red, Shape>, Notice<Red>) OR Marker")

    agent.doTask("Piece<Color, Circle>, Notice<Color>")

    agent.count("Piece<Red, Circle>") shouldBe 1
    agent.count("Notice<Red>") shouldBe 1
    agent.count("Marker") shouldBe 0
    game.tasks.isEmpty() shouldBe true
  }

  @Test
  fun `tryTask keeps an incomplete intersection pending and executes a complete one`() {
    val task = agent.addTasks("Piece<Red, Shape>").single()

    agent.tryTask("Piece<Color, Shape>")
    game.tasks.ids() shouldBe listOf(task)
    agent.count("Piece") shouldBe 0
    agent.tryTask("Piece<Color, Circle>")

    agent.count("Piece<Red, Circle>") shouldBe 1
    game.tasks.isEmpty() shouldBe true
  }

  @Test
  fun `an omitted quantifier cannot weaken an incompatible count into an optional ceiling`() {
    agent.addTasks("2 Piece<Red, Shape>?")
    val before = game.timeline.checkpoint()

    shouldThrow<TaskException> { agent.doTask("3 Piece<Color, Circle>") }

    game.timeline.checkpoint() shouldBe before
    agent.count("Piece") shouldBe 0
  }

  @Test
  fun `an unnamed type overlap cannot be ignored when matching tasks`() {
    agent.addTasks("LeftChoice?, RightChoice?")
    val before = game.timeline.checkpoint()

    shouldThrow<TaskException> { agent.doTask("LeftChoice!") }
        .message
        .shouldContain("more specific choice")

    game.timeline.checkpoint() shouldBe before
    agent.count("LeftChoice") shouldBe 0
  }

  @Test
  fun `incompatible counts disambiguate otherwise overlapping type domains`() {
    agent.addTasks("FirstChoice!, 2 RightChoice!")

    agent.doTask("LeftChoice!")

    agent.count("FirstChoice") shouldBe 1
    game.tasks.ids().size shouldBe 1
  }
}
