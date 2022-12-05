package dev.martianzoo.tfm.petaform.parser

import dev.martianzoo.tfm.petaform.api.Effect
import dev.martianzoo.tfm.petaform.parser.PetaformParser.parse
import org.junit.jupiter.api.Test

class EffectTest {
  @Test
  fun condit() {
    parse<Effect>("This IF =5 This: That")
  }
}
