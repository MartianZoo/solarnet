package dev.martianzoo.tfm.web.almanac

import dev.martianzoo.search.CanonQuery
import kotlinx.browser.document
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement

internal fun renderQueryExamples(column: HTMLElement, choose: (QueryExample) -> Unit) {
  column.innerHTML =
      """
      <header class="class-heading"><h2>Example queries</h2></header>
      <section class="panel query-panel">
        <p>Click a sentence to fill the editor, then edit it or run it. Try changing a tag,
        resource, number, or class name. Class names are case-sensitive.</p>
        <p>Authored keeps declared actions and PROD. Elaborated includes inherited effects,
        fills defaults, and lowers actions and production.</p>
        <p>These XPath 1.0 queries search the Pets tree. They find instructions inside choices
        and conditions; they do not prove those paths are playable, add separate gains together,
        or follow triggered consequences.</p>
      </section>
      """
          .trimIndent()
  queryExamples
      .groupBy { it.section }
      .forEach { (section, examples) ->
        val panel = document.createElement("section")
        panel.className = "panel query-panel"
        val heading = document.createElement("h3")
        heading.textContent = section
        panel.appendChild(heading)
        examples.forEach { example ->
          val button = document.createElement("button")
          button.className = "query-example"
          button.setAttribute("type", "button")
          button.textContent = example.description
          button.addEventListener("click", { choose(example) })
          panel.appendChild(button)
        }
        column.appendChild(panel)
      }
}

internal fun renderQueryResults(
    column: HTMLElement,
    selection: AlmanacColumn.Query,
    query: CanonQuery,
    renderLinks: (Element, String) -> Unit,
) {
  column.innerHTML =
      """
      <header class="class-heading"><h2>Query results</h2></header>
      <section class="panel query-panel query-source"><h3></h3><pre></pre></section>
      <section class="panel query-panel query-results" aria-live="polite"></section>
      """
          .trimIndent()
  column.querySelector("h3")!!.textContent =
      if (selection.form == CanonQuery.Form.AUTHORED) "Authored tree" else "Elaborated tree"
  column.querySelector("pre")!!.textContent = selection.xpath
  val results = column.querySelector(".query-results")!!
  try {
    val matches = query.evaluate(selection.xpath)
    column.querySelector("h2")!!.textContent = "Query results (${matches.size})"
    if (matches.isEmpty()) results.textContent = "No matches."
    matches.forEach { match ->
      val row = document.createElement("div")
      row.className = "query-match"
      val owner = match.name ?: match.owner
      if (owner != null) renderLinks(row, owner)
      if (match.name == null) {
        val value = document.createElement("pre")
        value.textContent = match.pets ?: match.value.ifEmpty { "(empty string)" }
        row.appendChild(value)
      }
      results.appendChild(row)
    }
  } catch (error: Throwable) {
    column.querySelector("h2")!!.textContent = "Query error"
    results.textContent = "Could not run this XPath 1.0 query: ${error.message}"
  }
}
