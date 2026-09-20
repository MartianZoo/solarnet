package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.ast.Metric
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class ProdTest {
  @Test
  internal fun resourceDifferenceBecomesRepresentedClassDifference() {
    val source = parse<Metric>("StandardResource(NOT MC)")

    Prod.handler(Canon.classTable).transform(source) shouldBe
        parse<Metric>("Production<Class<StandardResource>(NOT Class<MC>)>")
  }

  @Test
  internal fun resourceDifferenceRetainsSharedDependencies() {
    val source = parse<Metric>("StandardResource<Owner>(NOT MC<Owner>)")

    Prod.handler(Canon.classTable).transform(source) shouldBe
        parse<Metric>("Production<Owner, Class<StandardResource>(NOT Class<MC>)>")
  }

  @Test
  internal fun resourceDifferenceCannotChangeDependencies() {
    val source = parse<Metric>("StandardResource<Owner>(NOT MC<Player2>)")

    shouldThrow<ExpressionException> { Prod.handler(Canon.classTable).transform(source) }
  }
}
