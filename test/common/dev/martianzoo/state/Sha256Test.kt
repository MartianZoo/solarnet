package dev.martianzoo.state

import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

internal class Sha256Test {
  @Test
  internal fun standardVectors() = runTest {
    sha256("") shouldBe "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
    sha256("abc") shouldBe "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
    sha256("é") shouldBe "4a99557e4033c3539de2eb65472017cad5f9557f7a0625a09f1c3f6e2ba69c4c"
    sha256("a".repeat(1000)) shouldBe
        "41edece42d63e8d9bf515a9ba6932e1c20cbc9f5a5d134645adb5db1b9737ea3"
  }
}
