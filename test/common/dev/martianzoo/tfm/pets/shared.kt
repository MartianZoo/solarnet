package dev.martianzoo.tfm.pets

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.PetNode
import io.kotest.matchers.shouldBe
import kotlin.reflect.KClass

private inline fun <reified T : PetNode> checkBothWays(asText: String, node: T) =
    checkBothWays(T::class, asText, node)

private fun <T : PetNode> checkBothWays(type: KClass<T>, asText: String, node: T) {
  node.toString() shouldBe asText
  parse(type, asText) shouldBe node
}

internal inline fun <reified T : PetNode> testRoundTrip(start: String, end: String = start) =
    testRoundTrip(T::class, start, end)

internal fun <T : PetNode> testRoundTrip(type: KClass<T>, start: String, end: String = start) =
    parse(type, start).toString() shouldBe end

internal inline fun <reified T : PetNode> testRoundTrip(start: T, end: T = start) =
    testRoundTrip(T::class, start, end)

internal fun <T : PetNode> testRoundTrip(type: KClass<T>, start: T, end: T = start) =
    parse(type, start.toString()) shouldBe end
