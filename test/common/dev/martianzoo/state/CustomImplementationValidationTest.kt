package dev.martianzoo.state

import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.catalogtestsupport.testCatalog
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.Exceptions.InvalidPetDefinitionException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Metric
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class CustomImplementationValidationTest {
  @Test
  internal fun `T2-9 a class cannot have both custom kinds`() {
    shouldThrow<InvalidPetDefinitionException> {
      testCatalog("CLASS Neighbor : CustomMetric, CustomInstruction").classTable
    }
  }

  @Test
  internal fun catalogAndPassiveWorldDoNotRequireCustomImplementations() {
    val source =
        "CLASS Garden : CustomMetric\n" +
            "CLASS Landscape<Component> : CustomMetric\n" +
            "CLASS Replant : CustomInstruction\n" +
            "CLASS Token"
    val catalog = testCatalog(source)

    catalog.classTable.getClass(cn("Garden")).declaration.customMetric shouldBe true
    catalog.classTable.getClass(cn("Replant")).declaration.customInstruction shouldBe true
    catalog.customClassDeclarations.map { it.className } shouldBe
        listOf(cn("Garden"), cn("Landscape"), cn("Replant"))
    val premise = catalog.gamePremise(GameConfig("Garden, Landscape, Replant, Token"))
    val world = GameWorld(premise)

    shouldThrow<ExpressionException> { world.reader.count(parse<Metric>("Garden")) }.detail shouldBe
        "custom metric `Garden` has no implementation"
    shouldThrow<ExpressionException> {
          world.reader.count(parse<Metric>("Landscape<Component>"))
        }
        .detail shouldBe "custom metric `Landscape` has no implementation"
  }
}
