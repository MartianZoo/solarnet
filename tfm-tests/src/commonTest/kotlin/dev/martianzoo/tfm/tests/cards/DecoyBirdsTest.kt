package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.canon.CardDefinition
import dev.martianzoo.tfm.canon.CardDefinition.CardData
import dev.martianzoo.tfm.canon.tfmCatalog
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

private val decoyBirds =
    CardDefinition(
        CardData(
            name = "DecoyBirds",
            deck = "PROJECT",
            tags = listOf("AnimalTag"),
            immediate = "PROD[-2 Plant<Anyone>]",
            actions = listOf("-> DecoyAnimal<This>"),
            effects = listOf("End: VictoryPoint / DecoyAnimal<This>"),
            requirement = "13 OxygenStep",
            cost = 10,
            projectKind = "ACTIVE",
        )
    )

internal class DecoyBirdsTest :
    CardTest(
        additionalClassDeclarations = decoyAnimalDeclarations.toSet(),
        additionalCardDefinitions = setOf(decoyBirds),
    ) {
  @Test
  internal fun `cardbound cubes do not make a resource card`() {
    val game = newGame()
    val decoyBirdsClass = game.classTable.getClass(cn("DecoyBirds"))
    val resourceCardClass = game.classTable.getClass(cn("ResourceCard"))

    decoyBirdsClass.isSubtypeOf(resourceCardClass) shouldBe false
    game.reader.tfmCatalog.cardResourceType(cn("DecoyBirds")) shouldBe null
  }
}
