package dev.martianzoo.engine

import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.data.Actor.Companion.ENGINE
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.TfmCatalog
import dev.martianzoo.tfm.engine.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class PhantomTypeTest {
  private fun agent() = Engine.newGame(canonicalPremise()).agent(ENGINE)

  @Test
  internal fun `inactive types and their class literals count zero`() {
    val game = Engine.newGame(canonicalPremise())
    val agent = game.agent(ENGINE)
    val venusTag = agent.resolve("VenusTag")

    agent.count("VenusTag") shouldBe 0
    agent.count("Class<VenusTag>") shouldBe 0
    game.classTable.isActive(venusTag) shouldBe false
    game.classTable.isActive(agent.resolve("Class<VenusTag>")) shouldBe false
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
  internal fun `optional and amap phantom changes do nothing while mandatory changes die`() {
    val agent = agent()

    agent.manual("VenusTag?")
    agent.manual("VenusTag.")
    agent.manual("-VenusTag?")
    agent.manual("-VenusTag.")
    shouldThrow<DeadEndException> { agent.manual("VenusTag!") }
    shouldThrow<DeadEndException> { agent.manual("-VenusTag!") }
    agent.count("VenusTag") shouldBe 0
  }

  @Test
  internal fun `choices discard mandatory phantom branches`() {
    val agent = agent()

    agent.manual("VenusTag! OR Plant<Player1>!")

    agent.count("Plant<Player1>") shouldBe 1
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
                        VenusTag<TagHolder>: Plant<Player1>!
                      }
                      """
                          .trimIndent()
                  )
                  .toSet()
        }
    val premise = canonicalPremise(catalog = TfmCatalog.Composite(Canon, probeCatalog))

    shouldThrow<IllegalArgumentException> { Engine.newGame(premise) }
  }
}
