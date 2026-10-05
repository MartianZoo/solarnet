package dev.martianzoo.engine

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.state.Catalog
import dev.martianzoo.state.ClassSelection
import dev.martianzoo.state.GameConfig
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class CatalogSetupTest {
  @Test
  internal fun genericCatalogInitializesRulesNamedPlayersAndCountedComponents() {
    val catalog =
        object : Catalog() {
          override val explicitClassDeclarations =
              parseClasses(
                      """
                      ABSTRACT CLASS Player : Owner, Actor { HAS =1 This }
                      CLASS Rules : System { HAS =1 This; This:: Marker }
                      CLASS Marker : System
                      CLASS Supply : System
                      CLASS Token<Player>
                      """
                          .trimIndent()
                  )
                  .toSet()
          override val modules = mapOf(cn("Rules") to emptySet<ClassSelection>())
        }
    val premise =
        catalog.gamePremise(
            GameConfig("Rules, 3 Supply", "Blue", "Red"),
            additionalInitialComponentTypes = setOf(parse("Token<Blue>")),
        )

    val game = Engine.newGame(premise)
    game.reader.count(parse<Metric>("Rules")) shouldBe 1
    game.reader.count(parse<Metric>("Marker")) shouldBe 1
    game.reader.count(parse<Metric>("Player")) shouldBe 2
    game.reader.count(parse<Metric>("Supply")) shouldBe 3
    game.reader.count(parse<Metric>("Token<Blue>")) shouldBe 1
    game.tasks.isEmpty() shouldBe true
  }
}
