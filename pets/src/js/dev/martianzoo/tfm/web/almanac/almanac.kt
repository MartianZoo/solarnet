@file:Suppress("UnsafeCastFromDynamic")

package dev.martianzoo.tfm.web.almanac

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.types.Class
import dev.martianzoo.pets.types.ClassTable
import dev.martianzoo.search.CanonQuery
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.text.EnglishCardTextRenderer
import kotlin.js.json
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLImageElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLSelectElement
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.events.KeyboardEvent

private const val MAX_SUGGESTIONS = 12
private const val MAX_EXAMPLES = 10
internal const val IMAGE_ROOT = "https://www.martianzoo.dev/images"
private const val SUBCLASSES = "Subclasses"

private val classWord = Regex("[A-Za-z][A-Za-z0-9_]*")

private typealias UsageIndex = Map<ClassName, Map<String, Set<ClassName>>>

public fun main() {
  val fullClassTable = Canon.classTable
  val classes = fullClassTable.allClasses().sortedBy { it.className.toString().lowercase() }
  val classesByName = classes.associateBy { it.className.toString() }
  val classesByLowerName = classes.associateBy { it.className.toString().lowercase() }
  val usageIndex = buildUsageIndex(fullClassTable)
  val cardClassNames = Canon.cards.mapTo(hashSetOf(), Class::className)
  val cardTextRenderer = EnglishCardTextRenderer(fullClassTable)
  val input = element("class-search") as HTMLInputElement
  val queryInput = element("query-input") as HTMLTextAreaElement
  val queryForm = element("query-tree") as HTMLSelectElement
  val queries = mutableMapOf<CanonQuery.Form, CanonQuery>()
  val suggestions = element("class-suggestions")
  val columns = element("class-columns") as HTMLElement
  val darkTheme = element("dark-theme") as HTMLInputElement
  val root = document.documentElement!!
  darkTheme.checked = root.getAttribute("data-theme") == "dark"
  darkTheme.addEventListener(
      "change",
      {
        val theme = if (darkTheme.checked) "dark" else "light"
        root.setAttribute("data-theme", theme)
        runCatching { window.localStorage.setItem("almanac-theme", theme) }
      },
  )
  val useIcons = element("use-icons") as HTMLInputElement
  fun updateIcons() {
    columns.classList.toggle("use-icons", useIcons.checked)
  }
  useIcons.addEventListener("change", { updateIcons() })
  updateIcons()
  var highlightedSuggestion = -1
  var visibleSuggestions = emptyList<Class>()
  val selectedColumns =
      mutableListOf<AlmanacColumn>(
          AlmanacColumn.Details(
              classesByName.getValue(
                  listOf(
                          "AiCentral",
                          "ArcticAlgae",
                          "OptimalAerobraking",
                          "EnergyMarket",
                          "EarthOffice",
                          "RegolithEaters",
                          "ViralEnhancers",
                          "Predators",
                          "ProjectInspection",
                          "Insulation",
                          "CommunityServices",
                          "TharsisRepublic",
                          "Manutech",
                      )
                      .random()
              )
          )
      )
  val renderedColumns = mutableListOf<AlmanacColumn>()

  fun hideSuggestions() {
    suggestions.setAttribute("hidden", "")
    suggestions.innerHTML = ""
    highlightedSuggestion = -1
    visibleSuggestions = emptyList()
    input.setAttribute("aria-expanded", "false")
  }

  fun updateHash() {
    val value =
        selectedColumns.joinToString(",") {
          when (it) {
            is AlmanacColumn.Details -> it.klass.className.toString()
            is AlmanacColumn.Results -> "${it.klass.className}:${it.group.replace(' ', '_')}"
            AlmanacColumn.Examples -> "examples"
            is AlmanacColumn.Query -> "query:${it.form.name}:${encodeURIComponent(it.xpath)}"
          }
        }
    if (window.location.hash.removePrefix("#") != value) window.location.hash = value
  }

  fun renderColumns(scrollToLast: Boolean = false) {
    when (val last = selectedColumns.lastOrNull()) {
      is AlmanacColumn.Details -> input.value = last.klass.className.toString()
      is AlmanacColumn.Results -> input.value = last.klass.className.toString()
      else -> Unit
    }
    // Keep the shared prefix, including its scroll positions and sampled examples.
    val shared = renderedColumns.zip(selectedColumns).takeWhile { (a, b) -> a == b }.size
    while (renderedColumns.size > shared) {
      columns.removeChild(columns.lastChild!!)
      renderedColumns.removeAt(renderedColumns.lastIndex)
    }
    selectedColumns.drop(shared).forEachIndexed { offset, selection ->
      val index = shared + offset
      val column = document.createElement("article") as HTMLElement
      column.className = "class-column"
      fun open(target: AlmanacColumn) {
        selectedColumns.subList(index + 1, selectedColumns.size).clear()
        selectedColumns.add(target)
        renderColumns(scrollToLast = true)
        updateHash()
      }
      val openClass: (Class) -> Unit = { open(AlmanacColumn.Details(it)) }
      when (selection) {
        is AlmanacColumn.Details ->
            renderColumn(
                column,
                selection.klass,
                fullClassTable,
                classesByName,
                usageIndex,
                cardClassNames,
                cardTextRenderer,
                openClass,
                openResults = { group -> open(AlmanacColumn.Results(selection.klass, group)) },
            )
        AlmanacColumn.Examples ->
            renderQueryExamples(column) { example ->
              selectedColumns.subList(index + 1, selectedColumns.size).clear()
              renderColumns()
              updateHash()
              queryInput.value = example.xpath
              queryForm.value = example.form.name
              queryInput.focus()
            }
        is AlmanacColumn.Query -> {
          val query = queries.getOrPut(selection.form) { CanonQuery(selection.form) }
          renderQueryResults(column, selection, query) { target, text ->
            renderLinkedText(target, text, classesByName, openClass)
          }
        }
        is AlmanacColumn.Results -> {
          val names =
              usageGroups(selection.klass, fullClassTable, usageIndex).getValue(selection.group)
          column.innerHTML =
              """
              <header class="class-heading"><h2></h2></header>
              <section class="panel"><div class="class-list"></div></section>
              """
                  .trimIndent()
          renderLinkedText(
              column.querySelector("h2")!!,
              "${selection.klass.className} — ${selection.group} (${names.size})",
              classesByName,
              openClass,
              icons = false,
          )
          renderLinkedText(
              column.querySelector(".class-list")!!,
              names.sorted().joinToString("\n") {
                classLine(classesByName.getValue(it.toString()))
              },
              classesByName,
              openClass,
          )
        }
      }
      val dismiss = document.createElement("button")
      dismiss.className = "dismiss-column"
      dismiss.setAttribute("type", "button")
      dismiss.setAttribute(
          "aria-label",
          "Dismiss ${column.querySelector("h2")?.textContent.orEmpty()}",
      )
      dismiss.textContent = "×"
      dismiss.addEventListener(
          "click",
          {
            selectedColumns.removeAt(index)
            renderColumns()
            updateHash()
          },
      )
      column.appendChild(dismiss)
      columns.appendChild(column)
      renderedColumns.add(selection)
    }
    if (scrollToLast)
        window.requestAnimationFrame {
          val behavior =
              if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) "auto"
              else "smooth"
          columns.asDynamic().scrollTo(json("left" to columns.scrollWidth, "behavior" to behavior))
          window.asDynamic().scrollTo(json("top" to 0, "behavior" to behavior))
        }
  }

  fun appendColumn(selection: AlmanacColumn) {
    selectedColumns.add(selection)
    hideSuggestions()
    renderColumns(scrollToLast = true)
    updateHash()
  }

  element("query-examples")
      .addEventListener(
          "click",
          {
            appendColumn(AlmanacColumn.Examples)
          },
      )
  element("query-form")
      .addEventListener(
          "submit",
          { event ->
            event.preventDefault()
            val xpath = queryInput.value.trim()
            if (xpath.isNotEmpty()) {
              val selection = AlmanacColumn.Query(xpath, CanonQuery.Form.valueOf(queryForm.value))
              val status = element("query-status")
              status.textContent = "Running…"
              window.setTimeout(
                  {
                    appendColumn(selection)
                    status.textContent = ""
                  },
                  0,
              )
            } else {
              queryInput.focus()
            }
          },
      )

  fun chooseFromSearch(klass: Class) {
    selectedColumns.clear()
    selectedColumns.add(AlmanacColumn.Details(klass))
    hideSuggestions()
    renderColumns()
    updateHash()
  }

  fun updateHighlight(next: Int) {
    if (visibleSuggestions.isEmpty()) return
    highlightedSuggestion = next.coerceIn(visibleSuggestions.indices)
    val buttons = suggestions.querySelectorAll("button")
    for (index in 0 until buttons.length) {
      val button = buttons.item(index) as Element
      if (index == highlightedSuggestion) button.classList.add("highlighted")
      else button.classList.remove("highlighted")
    }
  }

  fun updateSuggestions() {
    val query = input.value.trim()
    if (query.isEmpty()) {
      hideSuggestions()
      return
    }
    val lowerQuery = query.lowercase()
    visibleSuggestions =
        classes
            .asSequence()
            .filter { it.className.toString().contains(lowerQuery, ignoreCase = true) }
            .sortedWith(
                compareBy<Class>(
                    { !it.className.toString().startsWith(lowerQuery, ignoreCase = true) },
                    { it.className.toString().length },
                    { it.className.toString() },
                )
            )
            .take(MAX_SUGGESTIONS)
            .toList()
    suggestions.innerHTML = ""
    visibleSuggestions.forEach { klass ->
      val button = document.createElement("button")
      button.setAttribute("type", "button")
      button.setAttribute("role", "option")
      button.textContent = klass.className.toString()
      klass.docstring?.takeIf(String::isNotBlank)?.let { docstring ->
        val detail = document.createElement("span")
        detail.textContent = docstring
        button.appendChild(detail)
      }
      button.addEventListener(
          "mousedown",
          { event ->
            event.preventDefault()
            chooseFromSearch(klass)
          },
      )
      suggestions.appendChild(button)
    }
    if (visibleSuggestions.isEmpty()) {
      hideSuggestions()
    } else {
      suggestions.removeAttribute("hidden")
      input.setAttribute("aria-expanded", "true")
      highlightedSuggestion = -1
    }
  }

  input.addEventListener("input", { updateSuggestions() })
  input.addEventListener("focus", { updateSuggestions() })
  input.addEventListener("blur", { hideSuggestions() })
  input.addEventListener(
      "keydown",
      keydown@{ rawEvent ->
        val event = rawEvent as KeyboardEvent
        when (event.key) {
          "ArrowDown" -> {
            event.preventDefault()
            updateHighlight(if (highlightedSuggestion < 0) 0 else highlightedSuggestion + 1)
          }
          "ArrowUp" -> {
            event.preventDefault()
            updateHighlight(
                if (highlightedSuggestion < 0) visibleSuggestions.lastIndex
                else highlightedSuggestion - 1
            )
          }
          "Enter" -> {
            val selected =
                visibleSuggestions.getOrNull(highlightedSuggestion)
                    ?: classesByLowerName[input.value.trim().lowercase()]
            if (selected != null) {
              event.preventDefault()
              chooseFromSearch(selected)
            }
          }
          "Escape" -> hideSuggestions()
        }
      },
  )

  fun selectFromHash() {
    val names = window.location.hash.removePrefix("#").split(',').filter(String::isNotEmpty)
    val nextColumns = names.mapNotNull { name ->
      if (name == "examples") return@mapNotNull AlmanacColumn.Examples
      if (name.startsWith("query:")) {
        val parts = name.split(':', limit = 3)
        return@mapNotNull try {
          AlmanacColumn.Query(decodeURIComponent(parts[2]), CanonQuery.Form.valueOf(parts[1]))
        } catch (_: Throwable) {
          null
        }
      }
      val parts = name.split(':', limit = 2)
      val klass = classesByName[parts.first()] ?: return@mapNotNull null
      val group = parts.getOrNull(1)?.replace('_', ' ')
      when {
        group == null -> AlmanacColumn.Details(klass)
        group == SUBCLASSES || group in usageCategories.map { "Usages: $it" } ->
            AlmanacColumn.Results(klass, group)
        else -> null
      }
    }
    if (nextColumns != selectedColumns) {
      selectedColumns.clear()
      selectedColumns.addAll(nextColumns)
      nextColumns.filterIsInstance<AlmanacColumn.Query>().lastOrNull()?.let {
        queryInput.value = it.xpath
        queryForm.value = it.form.name
      }
      renderColumns()
    }
  }
  window.addEventListener("hashchange", { selectFromHash() })

  element("class-count").textContent = "${classes.size} classes"
  if (window.location.hash.isNotEmpty()) selectFromHash()
  renderColumns()
  updateHash()
}

private fun renderColumn(
    column: HTMLElement,
    klass: Class,
    classTable: ClassTable,
    classesByName: Map<String, Class>,
    usageIndex: UsageIndex,
    cardClassNames: Set<ClassName>,
    cardTextRenderer: EnglishCardTextRenderer,
    openClass: (Class) -> Unit,
    openResults: (String) -> Unit,
) {
  column.innerHTML =
      """
      <header class="class-heading">
        <h2 data-field="name"></h2>
      </header>
      <div class="image-box" data-field="image-box" hidden>
        <img data-field="image" hidden>
      </div>
      <section class="panel english-text-panel" data-field="top-text-panel" hidden>
        <h3>Top text</h3>
        <div data-field="top-text"></div>
      </section>
      <section class="panel english-text-panel" data-field="bottom-text-panel" hidden>
        <h3>Bottom text</h3>
        <div data-field="bottom-text"></div>
      </section>
      <section class="panel" data-field="docstring-panel" hidden>
        <h3>Docstring</h3>
        <div data-field="docstring"></div>
      </section>
      <section class="panel">
        <h3>Pets source <small>normalized</small></h3>
        <pre data-field="source"></pre>
      </section>
      <section class="panel" data-field="supertypes-panel">
        <h3 data-field="supertypes-heading">Supertypes</h3>
        <pre data-field="supertypes"></pre>
      </section>
      <section class="panel" data-field="dependencies-panel">
        <h3 data-field="dependencies-heading">Dependencies</h3>
        <pre data-field="dependencies"></pre>
      </section>
      <div class="usage-panels" data-field="usages"></div>
      <section class="panel" data-field="invariants-panel">
        <h3>Invariants</h3>
        <pre data-field="invariants"></pre>
      </section>
      """
          .trimIndent()

  fun linked(field: String, value: String) {
    renderLinkedText(
        column.field(field),
        value,
        classesByName,
        openClass,
        icons = field != "name" && field != "docstring",
    )
  }

  linked("name", klass.className.toString())
  if (klass.className in cardClassNames) {
    renderCardText(column, klass, cardTextRenderer)
  }
  klass.docstring?.takeIf(String::isNotBlank)?.let { docstring ->
    linked("docstring", docstring)
    column.field("docstring-panel").removeAttribute("hidden")
  }
  linked("source", klass.declaration.copy(docstring = null).toString())
  val supertypes =
      listOfNotNull(
          principalSuperclass(klass)
              ?.withAllDependencies(klass.defaultType.dependencies)
              ?.expressionFull
      )
  column.field("supertypes-heading").textContent = "Supertype"
  renderOptionalList(
      column,
      "supertypes",
      "supertypes-panel",
      supertypes,
      classesByName,
      openClass,
  )
  val dependencyLines =
      klass.dependencies.keys.map { key -> "$key: ${klass.dependencies.get(key).expressionFull}" }
  column.field("dependencies-heading").textContent = "Dependencies (${dependencyLines.size})"
  renderOptionalList(
      column,
      "dependencies",
      "dependencies-panel",
      dependencyLines,
      classesByName,
      openClass,
  )
  renderOptionalList(
      column,
      "invariants",
      "invariants-panel",
      klass.invariants,
      classesByName,
      openClass,
  )
  renderUsages(
      column.field("usages"),
      klass,
      classTable,
      usageIndex,
      classesByName,
      openClass,
      openResults,
  )
  renderImage(column, klass, cardClassNames)
}

internal fun renderCardText(
    column: Element,
    card: Class,
    renderer: EnglishCardTextRenderer,
) {
  val cardText = renderer.render(card)
  column.field("top-text").textContent = cardText.top
  column.field("bottom-text").textContent = cardText.bottom
  (column.field("top-text-panel") as HTMLElement).hidden = cardText.top.isBlank()
  (column.field("bottom-text-panel") as HTMLElement).hidden = cardText.bottom.isBlank()
}

private fun usageGroups(
    klass: Class,
    classTable: ClassTable,
    usageIndex: UsageIndex,
): Map<String, Set<ClassName>> {
  val subclasses =
      classTable
          .allSubclasses(klass)
          .sortedWith(compareBy({ -classTable.allSubclasses(it).size }, { it.className }))
          .mapTo(linkedSetOf(), Class::className) - klass.className
  return (listOf(SUBCLASSES to subclasses) +
          usageCategories.map { category ->
            "Usages: $category" to usageIndex[klass.className]?.get(category).orEmpty()
          })
      .toMap()
}

private fun renderUsages(
    container: Element,
    klass: Class,
    classTable: ClassTable,
    usageIndex: UsageIndex,
    classesByName: Map<String, Class>,
    openClass: (Class) -> Unit,
    openResults: (String) -> Unit,
) {
  container.innerHTML = ""
  usageGroups(klass, classTable, usageIndex).forEach { (label, names) ->
    if (names.isEmpty()) return@forEach
    val panel = document.createElement("section")
    panel.className = "panel usage-panel"
    val heading = document.createElement("h3")
    heading.textContent = "$label (${names.size})"
    val contents = document.createElement("div")
    contents.className = "class-list"
    val examples = randomExamples(names)
    renderLinkedText(
        contents,
        examples.joinToString("\n") { classLine(classesByName.getValue(it.toString())) },
        classesByName,
        openClass,
    )
    val remaining = names.size - examples.size
    if (remaining > 0) {
      val more = document.createElement("div")
      more.className = "more-count"
      more.appendChild(document.createTextNode("$remaining more "))
      val seeAll = document.createElement("button")
      seeAll.className = "class-link see-all"
      seeAll.setAttribute("type", "button")
      seeAll.textContent = "(see all)"
      seeAll.addEventListener("click", { openResults(label) })
      more.appendChild(seeAll)
      contents.appendChild(more)
    }
    panel.appendChild(heading)
    panel.appendChild(contents)
    container.appendChild(panel)
  }
}

private fun renderImage(
    column: Element,
    klass: Class,
    cardClassNames: Set<ClassName>,
) {
  val image = column.field("image") as HTMLImageElement
  val sources = buildList {
    if (klass.className in cardClassNames) add("$IMAGE_ROOT/classes-pubtext/${klass.className}.png")
    add("$IMAGE_ROOT/classes/${klass.className}.png")
    klass.className
        .toString()
        .takeIf { it.endsWith("Map") }
        ?.removeSuffix("Map")
        ?.let { add("$IMAGE_ROOT/classes/$it.png") }
  }
  var sourceIndex = 0

  image.alt = "${klass.className} reference image"
  image.addEventListener(
      "load",
      {
        image.removeAttribute("hidden")
        column.field("image-box").removeAttribute("hidden")
      },
  )
  image.addEventListener(
      "error",
      {
        sourceIndex++
        if (sourceIndex < sources.size) {
          image.src = sources[sourceIndex]
        } else {
          image.setAttribute("hidden", "")
        }
      },
  )
  image.src = sources.first()
}

private fun renderLinkedText(
    container: Element,
    value: String,
    classesByName: Map<String, Class>,
    openClass: (Class) -> Unit,
    icons: Boolean = true,
) {
  container.innerHTML = ""
  var offset = 0
  classWord.findAll(value).forEach { match ->
    if (match.range.first > offset) {
      container.appendChild(document.createTextNode(value.substring(offset, match.range.first)))
    }
    val klass = classesByName[match.value]
    if (klass == null) {
      container.appendChild(document.createTextNode(match.value))
    } else {
      val button = document.createElement("button")
      button.className = "class-link"
      button.setAttribute("type", "button")
      button.textContent = match.value
      if (icons) appendClassIcon(button, klass)
      button.addEventListener("click", { openClass(klass) })
      container.appendChild(button)
    }
    offset = match.range.last + 1
  }
  if (offset < value.length) container.appendChild(document.createTextNode(value.substring(offset)))
}

private fun randomExamples(names: Set<ClassName>): List<ClassName> =
    if (names.size <= MAX_EXAMPLES) names.sorted() else names.shuffled().take(MAX_EXAMPLES).sorted()

private fun classLine(klass: Class): String {
  val superclass = principalSuperclass(klass)
  return "${klass.className}" + if (superclass == null) "" else " (${superclass.className})"
}

private fun renderOptionalList(
    column: Element,
    field: String,
    block: String,
    values: Iterable<Any>,
    classesByName: Map<String, Class>,
    openClass: (Class) -> Unit,
) {
  val items = values.toList()
  if (items.isEmpty()) {
    column.field(block).setAttribute("hidden", "")
  } else {
    renderLinkedText(column.field(field), items.joinToString("\n"), classesByName, openClass)
  }
}

private fun Element.field(name: String): Element =
    checkNotNull(querySelector("[data-field='$name']"))

private fun element(id: String): Element = checkNotNull(document.getElementById(id))

private external fun encodeURIComponent(value: String): String

private external fun decodeURIComponent(value: String): String
