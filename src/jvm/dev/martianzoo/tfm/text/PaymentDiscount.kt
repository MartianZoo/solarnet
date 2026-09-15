package dev.martianzoo.tfm.text

import dev.martianzoo.pets.ast.ClassName

internal data class PaymentDiscount(
    val trigger: Clause,
    val reduction: ResourceAmount,
    val categoryReduction: Boolean = false,
) {
  data class Trigger(
      val clause: Clause.Simple,
      val categoryNoun: ComponentDescriber.Noun.Counted?,
      val billingResource: ClassName? = null,
  ) {
    fun accepts(reduction: ResourceAmount): Boolean =
        billingResource == null ||
            reduction.resource == null ||
            billingResource == reduction.resource
  }
}
