package dev.martianzoo.state

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.testsupport.PLAYER2
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class ActorTest {
  @Test
  internal fun playersHaveDistinctSeatsAndExcludeAdmin() {
    Player.players(2).shouldContainExactly(PLAYER1, PLAYER2)
    Player.players(6).last() shouldBe Player(cn("Player6"))
    Player(cn("Player1")) shouldBe PLAYER1
    Player(cn("Yellow")).className shouldBe cn("Yellow")
    (ADMIN is Player) shouldBe false
    shouldThrow<RuntimeException> { Player(cn("Admin")) }
    shouldThrow<IllegalArgumentException> { Player.players(-1) }
  }

  @Test
  internal fun componentOwnershipExcludesActorOnlyIdentities() {
    val table =
        testCatalog(
                """
                ABSTRACT CLASS Player : Owner, Actor { CLASS Blue }
                CLASS Passive : Owner
                CLASS Plant : Owned
                """
                    .trimIndent()
            )
            .classTable

    fun component(expression: String) = Component(table.resolve(parse(expression)))

    component("Admin").owner shouldBe null
    component("Blue").owningPlayer shouldBe Player(cn("Blue"))
    component("Plant<Blue>").owner shouldBe table.resolve(parse("Blue"))
    component("Plant<Blue>").owningPlayer shouldBe Player(cn("Blue"))
    component("Passive").owner shouldBe table.resolve(parse("Passive"))
    component("Plant<Passive>").owner shouldBe table.resolve(parse("Passive"))
    component("Plant<Passive>").owningPlayer shouldBe null
  }
}
