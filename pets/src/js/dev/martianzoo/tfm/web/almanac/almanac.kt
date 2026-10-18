@file:Suppress("UnsafeCastFromDynamic")

package dev.martianzoo.tfm.web.almanac

import dev.martianzoo.pets.api.SystemClasses.THIS
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.types.Class
import dev.martianzoo.pets.types.ClassTable
import dev.martianzoo.search.CanonQuery
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.text.EnglishTextRenderer
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
internal const val IMAGE_ROOT = "https://www.martianzoo.dev/images"
private const val DIRECT_SUBCLASSES = "Direct subclasses"
private const val ALL_SUBCLASSES = "All subclasses"

private val classWord = Regex("[A-Za-z][A-Za-z0-9_]*")

private typealias UsageIndex = Map<ClassName, Map<String, Set<ClassName>>>

internal fun matchesClassQuery(name: String, query: String): Boolean {
  if (query.isEmpty()) return true
  if (query.drop(1).none(Char::isUpperCase)) return name.contains(query, ignoreCase = true)
  var start = name.indexOf(query.first(), ignoreCase = true)
  while (start >= 0) {
    var position = start
    var matched = true
    query.forEachIndexed { index, char ->
      if (!matched) return@forEachIndexed
      if (index > 0 && char.isUpperCase()) {
        position = name.indexOf(char, position)
      } else if (position >= name.length || !name[position].equals(char, ignoreCase = true)) {
        matched = false
      }
      if (position < 0) matched = false else position++
    }
    if (matched) return true
    start = name.indexOf(query.first(), start + 1, ignoreCase = true)
  }
  return false
}

public fun main() {
  val fullClassTable = Canon.classTable
  val classes = fullClassTable.allClasses().sortedBy { it.className.toString().lowercase() }
  val classesByName = classes.associateBy { it.className.toString() }
  val classesByLowerName = classes.associateBy { it.className.toString().lowercase() }
  val queries = mutableMapOf<CanonQuery.Form, CanonQuery>()
  val usageIndex =
      buildUsageIndex(
          fullClassTable,
          queries.getOrPut(CanonQuery.Form.ELABORATED) { CanonQuery(CanonQuery.Form.ELABORATED) },
      )
  val cardClassNames = Canon.cards.mapTo(hashSetOf(), Class::className)
  val iconic = fullClassTable.getClass(cn("Iconic"))
  val isIconic: (String) -> Boolean = { name ->
    classesByName[name]?.let { it !== iconic && it.isSubtypeOf(iconic) } == true
  }
  val englishTextRenderer = EnglishTextRenderer(fullClassTable)
  val goalKinds = listOf("Milestone", "Award").map { fullClassTable.getClass(cn(it)) }
  val input = element("class-search") as HTMLInputElement
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
  var highlightedSuggestion = -1
  var visibleSuggestions = emptyList<Class>()
  val selectedPanels =
      mutableListOf(
          AlmanacPanel(
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
      )
  val panelElements = mutableMapOf<AlmanacPanel, HTMLElement>()

  fun hideSuggestions() {
    suggestions.setAttribute("hidden", "")
    suggestions.innerHTML = ""
    highlightedSuggestion = -1
    visibleSuggestions = emptyList()
    input.setAttribute("aria-expanded", "false")
  }

  fun updateHash() {
    val value =
        selectedPanels.joinToString(",") { panel ->
          val token =
              when (val selection = panel.selection) {
                is AlmanacColumn.Details -> selection.klass.className.toString()
                is AlmanacColumn.Results ->
                    "${selection.klass.className}:${selection.group.replace(' ', '_')}"
                AlmanacColumn.Examples -> "examples"
                is AlmanacColumn.Query ->
                    "query:${selection.form.name}:${encodeURIComponent(selection.xpath)}"
              }
          if (panel.pinned) "!$token" else token
        }
    if (window.location.hash.removePrefix("#") != value) window.location.hash = value
  }

  fun keepPinnedRightOf(panel: AlmanacPanel) {
    val index = selectedPanels.indexOf(panel)
    if (index < 0) return
    val pinned = selectedPanels.drop(index + 1).filter { it.pinned }
    selectedPanels.subList(index + 1, selectedPanels.size).clear()
    selectedPanels.addAll(pinned)
  }

  fun renderColumns(scrollToLast: Boolean = false) {
    panelElements.keys
        .filter { it !in selectedPanels }
        .forEach { removed ->
          columns.removeChild(panelElements.remove(removed)!!)
        }
    selectedPanels.forEachIndexed { index, panel ->
      if (panel in panelElements) {
        val existing = panelElements.getValue(panel)
        if (columns.children.item(index) !== existing)
            columns.insertBefore(existing, columns.children.item(index))
        return@forEachIndexed
      }
      val selection = panel.selection
      val column = document.createElement("article") as HTMLElement
      column.className = "class-column"
      fun open(target: AlmanacColumn) {
        keepPinnedRightOf(panel)
        selectedPanels.add(AlmanacPanel(target))
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
                isIconic,
                englishTextRenderer,
                goalKinds,
                openClass,
                openResults = { group -> open(AlmanacColumn.Results(selection.klass, group)) },
            )
        AlmanacColumn.Examples ->
            renderQueryExamples(column) { example ->
              keepPinnedRightOf(panel)
              panel.selection = AlmanacColumn.Query(example.xpath, example.form)
              columns.removeChild(panelElements.remove(panel)!!)
              renderColumns()
              updateHash()
            }
        is AlmanacColumn.Query -> {
          renderQueryResults(
              column,
              selection,
              queryFor = { form -> queries.getOrPut(form) { CanonQuery(form) } },
              renderLinks = { target, text ->
                renderLinkedText(target, text, classesByName, openClass)
              },
              isIconic = isIconic,
              onSubmit = { updated ->
                keepPinnedRightOf(panel)
                panel.selection = updated
                renderColumns()
                updateHash()
              },
          )
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
              currentClass = selection.klass.className,
          )
          renderClassList(
              column.querySelector(".class-list")!!,
              names.sortedWith(usageOrder(isIconic)),
              classesByName,
              openClass,
              selection.klass.className,
              isIconic,
          )
        }
      }
      val title = column.querySelector("h2")?.textContent.orEmpty()
      val pin = document.createElement("button")
      pin.className = "pin-column"
      pin.setAttribute("type", "button")
      pin.innerHTML =
          """<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M8 3h8v6l2 3v2H6v-2l2-3V3Z"/><path d="M12 14v8"/></svg>"""
      fun updatePin() {
        pin.setAttribute("aria-pressed", panel.pinned.toString())
        pin.setAttribute("aria-label", "${if (panel.pinned) "Unpin" else "Pin"} $title")
        pin.setAttribute("title", if (panel.pinned) "Unpin panel" else "Pin panel")
        column.classList.toggle("is-pinned", panel.pinned)
      }
      updatePin()
      pin.addEventListener(
          "click",
          {
            panel.pinned = !panel.pinned
            updatePin()
            updateHash()
          },
      )
      column.appendChild(pin)
      val dismiss = document.createElement("button")
      dismiss.className = "dismiss-column"
      dismiss.setAttribute("type", "button")
      dismiss.setAttribute(
          "aria-label",
          "Dismiss $title",
      )
      dismiss.textContent = "×"
      dismiss.addEventListener(
          "click",
          {
            selectedPanels.remove(panel)
            renderColumns()
            updateHash()
          },
      )
      column.appendChild(dismiss)
      panelElements[panel] = column
      columns.insertBefore(column, columns.children.item(index))
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
    selectedPanels.add(AlmanacPanel(selection))
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

  fun chooseFromSearch(klass: Class) {
    selectedPanels.removeAll { !it.pinned }
    selectedPanels.add(AlmanacPanel(AlmanacColumn.Details(klass)))
    input.value = ""
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
            .filter { matchesClassQuery(it.className.toString(), query) }
            .sortedWith(
                compareBy<Class>(
                    { !isIconic(it.className.toString()) },
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
      if (!isIconic(klass.className.toString())) button.classList.add("non-iconic-result")
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
    val nextPanels = names.mapNotNull { token ->
      val pinned = token.startsWith("!")
      val name = token.removePrefix("!")
      if (name == "examples") return@mapNotNull AlmanacPanel(AlmanacColumn.Examples, pinned)
      if (name.startsWith("query:")) {
        val parts = name.split(':', limit = 3)
        return@mapNotNull try {
          AlmanacPanel(
              AlmanacColumn.Query(decodeURIComponent(parts[2]), CanonQuery.Form.valueOf(parts[1])),
              pinned,
          )
        } catch (_: Throwable) {
          null
        }
      }
      val parts = name.split(':', limit = 2)
      val klass = classesByName[parts.first()] ?: return@mapNotNull null
      val group = parts.getOrNull(1)?.replace('_', ' ')
      val selection =
          when {
            group == null -> AlmanacColumn.Details(klass)
            group == DIRECT_SUBCLASSES ||
                group == ALL_SUBCLASSES ||
                group in usageCategories.map { "Usages: $it" } ->
                AlmanacColumn.Results(klass, group)
            else -> null
          }
      selection?.let { AlmanacPanel(it, pinned) }
    }
    if (
        nextPanels.map { it.selection to it.pinned } !=
            selectedPanels.map { it.selection to it.pinned }
    ) {
      val previous = selectedPanels.toList()
      selectedPanels.clear()
      nextPanels.forEachIndexed { index, next ->
        val existing = previous.getOrNull(index)
        selectedPanels.add(
            if (existing?.selection == next.selection && existing.pinned == next.pinned) existing
            else next
        )
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
    isIconic: (String) -> Boolean,
    englishTextRenderer: EnglishTextRenderer,
    goalKinds: List<Class>,
    openClass: (Class) -> Unit,
    openResults: (String) -> Unit,
) {
  column.innerHTML =
      """
      <header class="class-heading">
        <h2 data-field="name"></h2>
      </header>
      <div class="image-box" data-field="image-box">
        <img data-field="image" hidden>
        <span class="image-placeholder" data-field="image-placeholder" hidden>No image available</span>
      </div>
      <section class="panel english-text-panel" data-field="top-text-panel" hidden>
        <h3>Top text (generated)</h3>
        <div data-field="top-text"></div>
      </section>
      <section class="panel english-text-panel" data-field="bottom-text-panel" hidden>
        <h3>Bottom text (generated)</h3>
        <div data-field="bottom-text"></div>
      </section>
      <section class="panel english-text-panel" data-field="text-panel" hidden>
        <h3>Text (generated)</h3>
        <div data-field="text"></div>
      </section>
      <section class="panel" data-field="docstring-panel" hidden>
        <h3>Docstring</h3>
        <div data-field="docstring"></div>
      </section>
      <section class="panel" data-field="supertypes-panel">
        <h3>Direct supertypes</h3>
        <pre data-field="supertypes"></pre>
      </section>
      <section class="panel" data-field="dependencies-panel">
        <h3>Direct dependencies</h3>
        <pre data-field="dependencies"></pre>
      </section>
      <section class="panel" data-field="invariants-panel">
        <h3>Invariants</h3>
        <pre data-field="invariants"></pre>
      </section>
      <section class="panel" data-field="properties-panel">
        <h3>Properties</h3>
        <pre data-field="properties"></pre>
      </section>
      <section class="panel" data-field="actions-panel">
        <h3>Actions</h3>
        <pre data-field="actions"></pre>
      </section>
      <section class="panel" data-field="effects-panel">
        <h3>Effects</h3>
        <pre data-field="effects"></pre>
      </section>
      <div class="usage-panels" data-field="usages"></div>
      <section class="panel">
        <h3>Pets source</h3>
        <pre data-field="source"></pre>
      </section>
      """
          .trimIndent()

  fun linked(field: String, value: String) {
    renderLinkedText(
        column.field(field),
        value,
        classesByName,
        openClass,
        currentClass = klass.className,
    )
  }

  linked("name", klass.className.toString())
  if (klass.className in cardClassNames) {
    renderCardText(column, klass, englishTextRenderer)
  } else if (!klass.abstract && goalKinds.any(klass::isSubtypeOf)) {
    renderGoalText(column, klass, englishTextRenderer)
  }
  klass.docstring?.takeIf(String::isNotBlank)?.let { docstring ->
    linked("docstring", docstring)
    column.field("docstring-panel").removeAttribute("hidden")
  }
  val directSupertypes =
      klass.declaration.supertypes.ifEmpty {
        klass.directSuperclasses.map { it.className.expression }
      }
  renderOptionalList(
      column,
      "supertypes",
      "supertypes-panel",
      directSupertypes,
      classesByName,
      openClass,
      klass.className,
  )
  renderOptionalList(
      column,
      "dependencies",
      "dependencies-panel",
      klass.declaration.dependencies,
      classesByName,
      openClass,
      klass.className,
  )

  val hierarchy = linkedSetOf<Class>()
  fun include(source: Class) {
    if (!hierarchy.add(source)) return
    source.directSuperclasses.forEach(::include)
  }
  include(klass)
  fun inheritedLines(values: (Class) -> List<String>): List<String> = hierarchy.flatMap { source ->
    values(source).map { value ->
      if (source === klass) value else "$value (from ${source.className})"
    }
  }
  renderOptionalList(
      column,
      "invariants",
      "invariants-panel",
      inheritedLines { source ->
        Requirement.split(source.declaration.invariants)
            .filter { invariant ->
              source === klass ||
                  invariant.descendantsOfType<Expression>().any { it.className == THIS }
            }
            .sortedBy { it.toString() }
            .map { "HAS $it" }
      },
      classesByName,
      openClass,
      klass.className,
  )
  renderOptionalList(
      column,
      "properties",
      "properties-panel",
      inheritedLines { source ->
        source.declaration.properties.map { (name, value) -> "$name = $value" }
      },
      classesByName,
      openClass,
      klass.className,
  )
  renderOptionalList(
      column,
      "actions",
      "actions-panel",
      inheritedLines { source -> source.declaration.authoredActions.map { it.toString() } },
      classesByName,
      openClass,
      klass.className,
  )
  renderOptionalList(
      column,
      "effects",
      "effects-panel",
      inheritedLines { source -> source.declaration.authoredEffects.map { it.toString() } },
      classesByName,
      openClass,
      klass.className,
  )
  renderUsages(
      column.field("usages"),
      klass,
      classTable,
      usageIndex,
      classesByName,
      isIconic,
      openClass,
      openResults,
  )
  linked("source", klass.declaration.toString())
  renderImage(column, klass, cardClassNames)
}

internal fun renderCardText(
    column: Element,
    card: Class,
    renderer: EnglishTextRenderer,
) {
  val cardText = renderer.renderCard(card)
  column.field("top-text").textContent = cardText.top
  column.field("bottom-text").textContent = cardText.bottom
  (column.field("top-text-panel") as HTMLElement).hidden = cardText.top.isBlank()
  (column.field("bottom-text-panel") as HTMLElement).hidden = cardText.bottom.isBlank()
}

internal fun renderGoalText(column: Element, goal: Class, renderer: EnglishTextRenderer) {
  val text = renderer.renderGoal(goal)
  column.field("text").textContent = text
  (column.field("text-panel") as HTMLElement).hidden = text.isBlank()
}

private fun usageGroups(
    klass: Class,
    classTable: ClassTable,
    usageIndex: UsageIndex,
): Map<String, Set<ClassName>> {
  val directSubclasses =
      classTable.directSubclasses(klass).mapTo(linkedSetOf(), Class::className) - klass.className
  val allSubclasses =
      classTable.allSubclasses(klass).mapTo(linkedSetOf(), Class::className) - klass.className
  return (listOf(DIRECT_SUBCLASSES to directSubclasses, ALL_SUBCLASSES to allSubclasses) +
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
    isIconic: (String) -> Boolean,
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
    val examples = randomExamples(names, isIconic)
    renderClassList(
        contents,
        examples,
        classesByName,
        openClass,
        klass.className,
        isIconic,
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
        if (image.naturalHeight > 0) {
          val scale = minOf(2.0, 250.0 / image.naturalHeight)
          image.style.width = "${image.naturalWidth * scale}px"
        }
        image.removeAttribute("hidden")
        column.field("image-placeholder").setAttribute("hidden", "")
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
          column.field("image-placeholder").removeAttribute("hidden")
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
    currentClass: ClassName? = null,
) {
  container.innerHTML = ""
  var offset = 0
  classWord.findAll(value).forEach { match ->
    if (match.range.first > offset) {
      container.appendChild(document.createTextNode(value.substring(offset, match.range.first)))
    }
    val klass = classesByName[if (match.value == "PROD") "Production" else match.value]
    if (klass == null || klass.className == currentClass) {
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

private fun usageOrder(isIconic: (String) -> Boolean): Comparator<ClassName> =
    compareBy({ !isIconic(it.toString()) }, { it.toString() })

private fun randomExamples(names: Set<ClassName>, isIconic: (String) -> Boolean): List<ClassName> =
    names
        .shuffled()
        .sortedBy { !isIconic(it.toString()) }
        .take(MAX_EXAMPLES)
        .sortedWith(usageOrder(isIconic))

private fun renderClassList(
    container: Element,
    names: List<ClassName>,
    classesByName: Map<String, Class>,
    openClass: (Class) -> Unit,
    currentClass: ClassName,
    isIconic: (String) -> Boolean,
) {
  container.innerHTML = ""
  names.forEach { name ->
    val row = document.createElement("div")
    row.className = "class-result"
    if (!isIconic(name.toString())) row.classList.add("non-iconic-result")
    renderLinkedText(
        row,
        classLine(classesByName.getValue(name.toString())),
        classesByName,
        openClass,
        currentClass = currentClass,
    )
    container.appendChild(row)
  }
}

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
    currentClass: ClassName,
) {
  val items = values.toList()
  if (items.isEmpty()) {
    column.field(block).setAttribute("hidden", "")
  } else {
    renderLinkedText(
        column.field(field),
        items.joinToString("\n"),
        classesByName,
        openClass,
        currentClass = currentClass,
    )
  }
}

private fun Element.field(name: String): Element =
    checkNotNull(querySelector("[data-field='$name']"))

private fun element(id: String): Element = checkNotNull(document.getElementById(id))

private external fun encodeURIComponent(value: String): String

private external fun decodeURIComponent(value: String): String
