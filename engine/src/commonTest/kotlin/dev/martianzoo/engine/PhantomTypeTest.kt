package dev.martianzoo.engine

import dev.martianzoo.api.Exceptions.DeadEndException
import dev.martianzoo.api.Exceptions.ExpressionException
import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.TfmAuthority
import dev.martianzoo.tfm.engine.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class PhantomTypeTest {
  private fun gameplay() = Engine.newGame(canonicalPremise()).gameplay(ENGINE).godMode()

  @Test
  internal fun `inactive types and their class literals count zero`() {
    val game = Engine.newGame(canonicalPremise())
    val gameplay = game.gameplay(ENGINE).godMode()
    val venusTag = gameplay.resolve("VenusTag")

    gameplay.count("VenusTag") shouldBe 0
    gameplay.count("Class<VenusTag>") shouldBe 0
    game.classTable.isActive(venusTag) shouldBe false
    game.classTable.isActive(gameplay.resolve("Class<VenusTag>")) shouldBe false
    game.reader.count(venusTag) shouldBe 0
    game.reader.containsAny(venusTag) shouldBe false
    game.reader.countComponent(venusTag) shouldBe 0
    game.reader.getComponents(venusTag).isEmpty() shouldBe true
  }

  @Test
  internal fun `unknown names remain errors`() {
    val gameplay = gameplay()

    shouldThrow<ExpressionException> { gameplay.count("Typo") }
    shouldThrow<ExpressionException> { gameplay.count("Class<Typo>") }
  }

  @Test
  internal fun `optional and amap phantom changes do nothing while mandatory changes die`() {
    val gameplay = gameplay()

    gameplay.manual("VenusTag?")
    gameplay.manual("VenusTag.")
    gameplay.manual("-VenusTag?")
    gameplay.manual("-VenusTag.")
    shouldThrow<DeadEndException> { gameplay.manual("VenusTag!") }
    shouldThrow<DeadEndException> { gameplay.manual("-VenusTag!") }
    gameplay.count("VenusTag") shouldBe 0
  }

  @Test
  internal fun `choices discard mandatory phantom branches`() {
    val gameplay = gameplay()

    gameplay.manual("VenusTag! OR Plant<Player1>!")

    gameplay.count("Plant<Player1>") shouldBe 1
  }

  @Test
  internal fun `component effects cannot quietly lose locked expansion classes`() {
    val probeAuthority =
        object : TfmAuthority() {
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
    val premise = canonicalPremise(authority = TfmAuthority.Composite(Canon, probeAuthority))

    shouldThrow<IllegalArgumentException> { Engine.newGame(premise) }
  }
}
