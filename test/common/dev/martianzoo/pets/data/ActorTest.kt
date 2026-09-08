package dev.martianzoo.pets.data

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.Actor.Companion.ADMIN
import dev.martianzoo.pets.data.Player.Companion.PLAYER1
import dev.martianzoo.pets.data.Player.Companion.PLAYER2
import dev.martianzoo.tfm.testlib.assertFails
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class ActorTest {
  @Test
  internal fun actorOwnerAndPlayerRolesAreDistinct() {
    val player: Actor = PLAYER1
    Player.players(2).shouldContainExactly(PLAYER1, PLAYER2)
    (player is Owner) shouldBe true
    Player(cn("Player1")) shouldBe PLAYER1
    Player(cn("Yellow")).className shouldBe cn("Yellow")
    (ADMIN is Player) shouldBe false
    (ADMIN is Owner) shouldBe false
    assertFails { Player(cn("Admin")) }
  }
}
