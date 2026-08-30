package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.canon.cardResourceType
import io.kotest.matchers.shouldBe
import kotlin.test.Test

private val decoyAnimalDeclarations =
    parseClasses(
        """
        CLASS DecoyAnimal : Cardbound
        CLASS DecoyBirds : ActionCard, ActiveCard<Class<ProjectCard>> {
          cost = 10
          requirement = HAS "13 OxygenStep"
          This:: AnimalTag<This>
          This: PROD[-2 Plant<Anyone>]
          End: VictoryPoint / DecoyAnimal<This>
          -> DecoyAnimal<This>
        }
        """
            .trimIndent()
    )

internal class DecoyBirdsTest :
    CardTest(additionalClassDeclarations = decoyAnimalDeclarations.toSet()) {
  @Test
  internal fun `cardbound cubes do not make a resource card`() {
    val game = newGame()
    val decoyBirdsClass = game.classTable.getClass(cn("DecoyBirds"))
    val resourceCardClass = game.classTable.getClass(cn("ResourceCard"))

    decoyBirdsClass.isSubtypeOf(resourceCardClass) shouldBe false
    cardResourceType(decoyBirdsClass) shouldBe null
  }
}
