package dev.martianzoo.tfm.data

import dev.martianzoo.data.Actor
import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.data.Owner
import dev.martianzoo.data.Player
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.data.Player.Companion.PLAYER2
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.testlib.assertFails
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class ActorTest {
  @Test
  fun actorOwnerAndPlayerRolesAreDistinct() {
    val player: Actor = PLAYER1
    Player.players(2).shouldContainExactly(PLAYER1, PLAYER2)
    (player is Owner) shouldBe true
    Owner.fromClassName(cn("Player1")) shouldBe PLAYER1
    (ENGINE is Player) shouldBe false
    (ENGINE is Owner) shouldBe false
    assertFails { Owner.fromClassName(cn("Engine")) }
    assertFails { Player(cn("Engine")) }
  }
}
