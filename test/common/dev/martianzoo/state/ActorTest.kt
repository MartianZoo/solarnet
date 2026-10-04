package dev.martianzoo.state

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
  internal fun actorAnyoneAndPlayerRolesAreDistinct() {
    val player: Actor = PLAYER1
    Player.players(2).shouldContainExactly(PLAYER1, PLAYER2)
    Player.players(6).last() shouldBe Player(cn("Player6"))
    (player is Anyone) shouldBe true
    Player(cn("Player1")) shouldBe PLAYER1
    Player(cn("Yellow")).className shouldBe cn("Yellow")
    (ADMIN is Player) shouldBe false
    (ADMIN is Anyone) shouldBe false
    shouldThrow<RuntimeException> { Player(cn("Admin")) }
    shouldThrow<IllegalArgumentException> { Player.players(-1) }
  }
}
