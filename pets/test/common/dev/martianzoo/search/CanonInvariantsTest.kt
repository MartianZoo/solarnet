package dev.martianzoo.search

import dev.martianzoo.search.CanonQuery.Form
import kotlin.test.Test
import kotlin.test.assertTrue

/** The XPath queries are the Canon-wide checks; CanonQueryTest exercises each with invalid data. */
internal class CanonInvariantsTest {
  @Test fun authoredCanonInvariants() = check(Form.AUTHORED)

  @Test fun elaboratedCanonInvariants() = check(Form.ELABORATED)

  private fun check(form: Form) {
    val prefix = "canon-invariants/${form.name.lowercase()}/"
    val filenames = GeneratedQueryResources.filenames.filter { it.startsWith(prefix) }.sorted()
    assertTrue(filenames.isNotEmpty(), "No $form Canon invariant queries")
    val query = CanonQuery(form)
    filenames.forEach { filename ->
      val offenders = query.evaluate(GeneratedQueryResources.read(filename))
      assertTrue(
          offenders.isEmpty(),
          "$filename:\n${offenders.joinToString("\n") { it.describe() }}",
      )
    }
  }
}
