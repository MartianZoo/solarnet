package dev.martianzoo.pets.data

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.Actor.Companion.ENGINE
import dev.martianzoo.pets.data.Player.Companion.PLAYER1
import dev.martianzoo.pets.data.Player.Companion.PLAYER2
import dev.martianzoo.pets.testlib.assertFails
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlin.test.assertSame

internal class ActorTest {
  @Test
  internal fun actorOwnerAndPlayerRolesAreDistinct() {
    val player: Actor = PLAYER1
    Player.players(2).shouldContainExactly(PLAYER1, PLAYER2)
    (player is Owner) shouldBe true
    assertSame(PLAYER1, Player(cn("Player1")))
    Player.isValid(cn("Player5")) shouldBe true
    Player.isValid("Player6") shouldBe false
    assertFails { Player(cn("Ellie")) }
    (ENGINE is Player) shouldBe false
    (ENGINE is Owner) shouldBe false
    assertFails { Player(cn("Engine")) }
  }
}
