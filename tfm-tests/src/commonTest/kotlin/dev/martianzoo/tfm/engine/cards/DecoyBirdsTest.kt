package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.api.tfmAuthority
import dev.martianzoo.tfm.data.CardDefinition
import dev.martianzoo.tfm.data.CardDefinition.CardData
import io.kotest.matchers.shouldBe
import kotlin.test.Test

private val decoyAnimalDeclarations = parseClasses("CLASS DecoyAnimal : Cardbound")

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
    game.reader.tfmAuthority.cardResourceType(cn("DecoyBirds")) shouldBe null
    val decoyBirdsClass = game.classTable.getClass(cn("DecoyBirds"))
    val resourceCardClass = game.classTable.getClass(cn("ResourceCard"))

    decoyBirdsClass.isSubtypeOf(resourceCardClass) shouldBe false
  }
}
