package dev.martianzoo.tfm.text

import dev.martianzoo.pets.ast.ClassName

internal data class PaymentDiscount(
    val trigger: Clause,
    val reduction: ResourceAmount,
    val categoryReduction: Boolean = false,
    val objectPronoun: Boolean,
) {
  data class Trigger(
      val clause: Clause.Simple,
      val categoryNoun: ComponentDescriber.Noun.Counted?,
      val billingResource: ClassName? = null,
      val objectPronoun: Boolean,
  ) {
    fun accepts(reduction: ResourceAmount): Boolean =
        billingResource == null ||
            reduction.resource == null ||
            billingResource == reduction.resource
  }
}
