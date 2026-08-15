package dev.martianzoo.tfm.data

import dev.martianzoo.data.Actor.Companion.ENGINE
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
    Player.players(2).shouldContainExactly(PLAYER1, PLAYER2)
    Player(cn("Ellie")).className shouldBe cn("Ellie")
    (ENGINE is Player) shouldBe false
    assertFails { Player(cn("Engine")) }
  }
}
