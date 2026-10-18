package dev.martianzoo.tfm.web.almanac

import dev.martianzoo.search.CanonQuery
import kotlinx.browser.document
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLTextAreaElement

internal fun renderQueryExamples(column: HTMLElement, choose: (QueryExample) -> Unit) {
  column.innerHTML =
      """
      <header class="class-heading"><h2>Example queries</h2></header>
      <section class="panel query-panel">
        <p>Click a sentence to run it in this column, then edit and rerun it. Try changing a tag,
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
    queryFor: (CanonQuery.Form) -> CanonQuery,
    renderLinks: (Element, String) -> Unit,
    isIconic: (String) -> Boolean,
    onSubmit: (AlmanacColumn.Query) -> Unit,
) {
  column.innerHTML =
      """
      <header class="class-heading"><h2>Query results</h2></header>
      <form class="image-box query-editor-box query-source">
        <h3>XPath query</h3>
        <textarea class="query-input" aria-label="XPath query" spellcheck="false"></textarea>
        <div class="query-controls">
          <span>Searching <button class="query-form-toggle" type="button"></button> forms</span>
          <button type="submit">Run query</button>
        </div>
      </form>
      <section class="panel query-panel query-results" aria-live="polite"></section>
      """
          .trimIndent()
  val editor = column.querySelector("textarea") as HTMLTextAreaElement
  val toggle = column.querySelector(".query-form-toggle")!!
  val results = column.querySelector(".query-results")!!
  editor.value = selection.xpath
  var currentForm = selection.form
  fun describeForm() {
    val authored = currentForm == CanonQuery.Form.AUTHORED
    toggle.textContent = if (authored) "authored" else "lowered"
    toggle.setAttribute("title", if (authored) "switch to lowered" else "switch to authored")
    toggle.setAttribute("aria-label", toggle.getAttribute("title")!!)
  }
  fun showResults(xpath: String, form: CanonQuery.Form) {
    results.innerHTML = ""
    try {
      val matches = queryFor(form).evaluate(xpath)
      column.querySelector("h2")!!.textContent = "Query results (${matches.size})"
      if (matches.isEmpty()) results.textContent = "No matches."
      matches
          .sortedBy { !isIconic(it.name ?: it.owner.orEmpty()) }
          .forEach { match ->
            val row = document.createElement("div")
            row.className = "query-match"
            val owner = match.name ?: match.owner
            if (owner != null && !isIconic(owner)) row.classList.add("non-iconic-result")
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
  fun submit() {
    val xpath = editor.value.trim()
    if (xpath.isEmpty()) editor.focus()
    else {
      onSubmit(AlmanacColumn.Query(xpath, currentForm))
      showResults(xpath, currentForm)
    }
  }
  toggle.addEventListener(
      "click",
      {
        currentForm =
            if (currentForm == CanonQuery.Form.AUTHORED) CanonQuery.Form.ELABORATED
            else CanonQuery.Form.AUTHORED
        describeForm()
        submit()
      },
  )
  column
      .querySelector("form")!!
      .addEventListener(
          "submit",
          { event ->
            event.preventDefault()
            submit()
          },
      )
  describeForm()
  showResults(selection.xpath, selection.form)
}
