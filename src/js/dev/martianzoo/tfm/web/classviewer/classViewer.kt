@file:Suppress("UnsafeCastFromDynamic")

package dev.martianzoo.tfm.web.classviewer

import dev.martianzoo.engine.TypeDescription
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.Gated
import dev.martianzoo.pets.ast.Instruction.Remove
import dev.martianzoo.pets.ast.Instruction.Transmute
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.displayName
import dev.martianzoo.pets.types.Class
import dev.martianzoo.pets.types.ClassTable
import dev.martianzoo.pets.types.Type
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.text.EnglishCardTextRenderer
import kotlin.js.JSON
import kotlin.js.json
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLImageElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.KeyboardEvent

private const val MAX_SUGGESTIONS = 12
private const val MAX_EXAMPLES = 10
private const val CARD_DATABASE_INDEX =
    "https://raw.githubusercontent.com/hadronikle/" +
        "Complete-Terraforming-Mars-Card-Database/main/index.html"
private const val CARD_DATA_PREFIX = "const CARDS = "

private const val GAINS = "Gains"
private const val REMOVES = "Removes"
private const val GATES = "Gates"
private const val REQUIREMENTS = "Requirements"
private const val OTHER_METRICS = "Other Metrics"
private const val TRIGGERS = "Triggers"
private const val OTHER = "Other references"
private val usageCategories =
    listOf(GAINS, REMOVES, GATES, REQUIREMENTS, OTHER_METRICS, TRIGGERS, OTHER)
private val classWord = Regex("[A-Za-z][A-Za-z0-9_]*")

private data class CardImage(val thumbnail: String, val full: String)

private typealias UsageIndex = Map<ClassName, Map<String, Set<ClassName>>>

internal fun classViewerMain() {
  val fullClassTable = Canon.classTable
  val classes = fullClassTable.allClasses().sortedBy { it.className.toString().lowercase() }
  val classesByName = classes.associateBy { it.className.toString() }
  val classesByLowerName = classes.associateBy { it.className.toString().lowercase() }
  val usageIndex = buildUsageIndex(classes)
  val cardClassNames = Canon.cards.mapTo(hashSetOf(), Class::className)
  val cardTextRenderer = EnglishCardTextRenderer(fullClassTable)
  val input = element("class-search") as HTMLInputElement
  val suggestions = element("class-suggestions")
  val columns = element("class-columns") as HTMLElement
  var cardImages = emptyMap<String, CardImage>()
  var highlightedSuggestion = -1
  var visibleSuggestions = emptyList<Class>()
  val selectedClasses = mutableListOf(classesByName["TerraformingMars"] ?: classes.first())

  fun hideSuggestions() {
    suggestions.setAttribute("hidden", "")
    suggestions.innerHTML = ""
    highlightedSuggestion = -1
    visibleSuggestions = emptyList()
    input.setAttribute("aria-expanded", "false")
  }

  fun updateHash() {
    val value = selectedClasses.joinToString(",") { it.className.toString() }
    if (window.location.hash.removePrefix("#") != value) window.location.hash = value
  }

  fun renderColumns(advance: Boolean = false) {
    input.value = selectedClasses.lastOrNull()?.className?.toString().orEmpty()

    fun appendColumn(index: Int, klass: Class) {
      val column = document.createElement("article") as HTMLElement
      column.className = "class-column"
      renderColumn(
          column,
          klass,
          fullClassTable,
          classesByName,
          usageIndex,
          cardClassNames,
          cardImages,
          cardTextRenderer,
          openClass = { target ->
            val replacesLaterColumns = index < selectedClasses.lastIndex
            if (replacesLaterColumns) {
              selectedClasses.subList(index + 1, selectedClasses.size).clear()
            }
            selectedClasses.add(target)
            renderColumns(advance = !replacesLaterColumns)
            if (replacesLaterColumns) {
              window.requestAnimationFrame {
                columns
                    .asDynamic()
                    .scrollTo(json("left" to columns.scrollWidth, "behavior" to "smooth"))
              }
            }
            updateHash()
          },
          dismiss = {
            selectedClasses.removeAt(index)
            renderColumns()
            updateHash()
          },
      )
      columns.appendChild(column)
    }

    if (advance) {
      appendColumn(selectedClasses.lastIndex, selectedClasses.last())
      window.requestAnimationFrame {
        columns.asDynamic().scrollTo(json("left" to columns.scrollWidth, "behavior" to "smooth"))
      }
      return
    }

    val previousScrollLeft = columns.scrollLeft
    columns.innerHTML = ""
    selectedClasses.forEachIndexed(::appendColumn)
    columns.scrollLeft = previousScrollLeft
  }

  fun chooseFromSearch(klass: Class) {
    selectedClasses.clear()
    selectedClasses.add(klass)
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
    val nextClasses = names.mapNotNull(classesByName::get)
    if (nextClasses.isNotEmpty() && nextClasses != selectedClasses) {
      selectedClasses.clear()
      selectedClasses.addAll(nextClasses)
      renderColumns()
    }
  }
  window.addEventListener("hashchange", { selectFromHash() })

  element("class-count").textContent = "${classes.size} classes"
  selectFromHash()
  renderColumns()
  updateHash()
  loadCardImages { loaded ->
    cardImages = loaded
    renderColumns()
  }
}

private fun renderColumn(
    column: HTMLElement,
    klass: Class,
    classTable: ClassTable,
    classesByName: Map<String, Class>,
    usageIndex: UsageIndex,
    cardClassNames: Set<ClassName>,
    cardImages: Map<String, CardImage>,
    cardTextRenderer: EnglishCardTextRenderer,
    openClass: (Class) -> Unit,
    dismiss: () -> Unit,
) {
  column.innerHTML =
      """
      <button class="dismiss-column" type="button" aria-label="Dismiss ${klass.className}">×</button>
      <div class="image-box" data-field="image-box">
        <img data-field="image" hidden>
      </div>
      <header class="class-heading">
        <h2 data-field="name"></h2>
      </header>
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

  column.querySelector(".dismiss-column")?.addEventListener("click", { dismiss() })
  val description = TypeDescription(classTable, klass.defaultType)
  fun linked(field: String, value: String) {
    renderLinkedText(column.field(field), value, classesByName, openClass)
  }

  linked("name", klass.className.toString())
  if (klass.className in cardClassNames) {
    renderCardText(column, klass, cardTextRenderer)
  }
  description.docstring?.takeIf(String::isNotBlank)?.let { docstring ->
    linked("docstring", docstring)
    column.field("docstring-panel").removeAttribute("hidden")
  }
  linked("source", klass.declaration.copy(docstring = null).toString())
  val supertypes = description.supertypes.map(Type::expressionFull)
  column.field("supertypes-heading").textContent = "Supertypes (${supertypes.size})"
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
      description.classInvariants,
      classesByName,
      openClass,
  )
  renderUsages(
      column.field("usages"),
      klass,
      description,
      usageIndex,
      classesByName,
      openClass,
  )
  renderImage(column, klass, cardClassNames, cardImages)
}

internal fun renderCardText(
    column: Element,
    card: Class,
    renderer: EnglishCardTextRenderer,
) {
  val cardText = renderer.render(card)
  column.field("top-text").textContent = cardText.top
  column.field("bottom-text").textContent = cardText.bottom
  column.field("top-text-panel").removeAttribute("hidden")
  column.field("bottom-text-panel").removeAttribute("hidden")
}

private fun renderUsages(
    container: Element,
    klass: Class,
    description: TypeDescription,
    usageIndex: UsageIndex,
    classesByName: Map<String, Class>,
    openClass: (Class) -> Unit,
) {
  container.innerHTML = ""
  val groups =
      listOf("Subclasses" to (description.subclassNames - klass.className)) +
          usageCategories.map { category ->
            "Usages: $category" to usageIndex[klass.className]?.get(category).orEmpty()
          }
  groups.forEach { (label, names) ->
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
      more.textContent = "($remaining more)"
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
    cardImages: Map<String, CardImage>,
) {
  val image = column.field("image") as HTMLImageElement
  val cardImage =
      if (klass.className in cardClassNames) {
        cardImages[normalizeName(displayName(Canon, klass.className))]
      } else {
        null
      }
  val sources = buildList {
    cardImage?.let {
      add(it.full)
      add(it.thumbnail)
    }
    add("/gameviewer/images/${klass.className}.png")
    klass.className
        .toString()
        .takeIf { it.endsWith("Map") }
        ?.removeSuffix("Map")
        ?.let { add("/gameviewer/images/$it.png") }
  }
  var sourceIndex = 0

  image.alt = "${klass.className} reference image"
  image.addEventListener(
      "load",
      {
        image.removeAttribute("hidden")
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
      button.addEventListener("click", { openClass(klass) })
      container.appendChild(button)
    }
    offset = match.range.last + 1
  }
  if (offset < value.length) container.appendChild(document.createTextNode(value.substring(offset)))
}

private fun buildUsageIndex(classes: List<Class>): UsageIndex {
  val index = mutableMapOf<ClassName, MutableMap<String, MutableSet<ClassName>>>()
  classes.forEach { user ->
    val declaration = user.declaration
    val roots = declaration.allNodes - declaration.className - declaration.supertypes
    roots.forEach { root ->
      collectUsages(root, emptySet()) { target, category ->
        index.getOrPut(target, ::linkedMapOf).getOrPut(category, ::linkedSetOf).add(user.className)
      }
    }
  }
  return index.mapValues { (_, categories) ->
    categories.mapValues { (_, users) -> users.toSet() }
  }
}

private fun collectUsages(
    node: PetNode,
    inheritedCategories: Set<String>,
    record: (ClassName, String) -> Unit,
) {
  when (node) {
    is Gated -> {
      collectUsages(node.gate, inheritedCategories + GATES, record)
      collectUsages(node.inner, inheritedCategories, record)
      return
    }
    is Transmute -> {
      collectUsages(node.gaining, inheritedCategories + GAINS, record)
      collectUsages(node.removing, inheritedCategories + REMOVES, record)
      return
    }
    is Requirement -> {
      node.immediateChildren().forEach { child ->
        collectUsages(child, inheritedCategories + REQUIREMENTS, record)
      }
      return
    }
    else -> Unit
  }
  val category =
      when (node) {
        is Gain -> GAINS
        is Remove -> REMOVES
        is Metric -> if (REQUIREMENTS in inheritedCategories) null else OTHER_METRICS
        is Effect.Trigger -> TRIGGERS
        else -> null
      }
  val categories = if (category == null) inheritedCategories else inheritedCategories + category
  if (node is ClassName) {
    (categories.ifEmpty { setOf(OTHER) }).forEach { record(node, it) }
  }
  node.immediateChildren().forEach { child -> collectUsages(child, categories, record) }
}

private fun loadCardImages(onLoaded: (Map<String, CardImage>) -> Unit) {
  window
      .fetch(CARD_DATABASE_INDEX)
      .then { response ->
        if (!response.ok) error("HTTP ${response.status}")
        response.text()
      }
      .then { source ->
        val json = source.substringAfter(CARD_DATA_PREFIX).substringBefore("];", "") + "]"
        check(json.length > 2) { "card database did not contain its card catalog" }
        val records = JSON.parse<Array<dynamic>>(json)
        onLoaded(
            buildMap {
              records.forEach { record ->
                val name = normalizeName(record.name as String)
                if (name !in this) {
                  put(name, CardImage(record.thumb as String, record.img as String))
                }
              }
            }
        )
      }
      .catch { onLoaded(emptyMap()) }
}

private fun normalizeName(value: String): String = value.lowercase().filter(Char::isLetterOrDigit)

private fun randomExamples(names: Set<ClassName>): List<ClassName> =
    if (names.size <= MAX_EXAMPLES) names.sorted() else names.shuffled().take(MAX_EXAMPLES).sorted()

private fun classLine(klass: Class): String {
  val superclasses = klass.declaration.supertypes.map { it.className }.sorted()
  return buildString {
    append(klass.className)
    if (superclasses.isNotEmpty()) superclasses.joinTo(this, ", ", " (", ")")
  }
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
