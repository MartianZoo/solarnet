package dev.martianzoo.tfm.web.playground

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.catalog.GamePremise
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.types.ClassTable

/** The recording's live Mars tiles over its configured map areas. */
@Composable
internal fun MarsTab() {
  val areas = LocalSpecimen.current.map
  BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black)) {
    val side = if (maxWidth < maxHeight) maxWidth else maxHeight
    val left = (maxWidth - side) / 2
    val top = (maxHeight - side) / 2
    rememberBundledImage("files/map/mars.png")?.let { mars ->
      Image(
          mars,
          "Mars",
          Modifier.offset(left, top).size(side),
          contentScale = ContentScale.FillBounds,
      )
    }
    Canvas(Modifier.fillMaxSize()) {
      val unit = minOf(size.width, size.height) / 1000f
      val dx = (size.width - unit * 1000f) / 2f
      val dy = (size.height - unit * 1000f) / 2f
      for (area in areas) {
        val x = dx + (500f + 107f * (area.column - area.row / 2f - 2.5f)) * unit
        val y = dy + (125f + 93.5f * (area.row - 1)) * unit
        val hex =
            Path().apply {
              moveTo(x, y - 58f * unit)
              lineTo(x + 50.2f * unit, y - 29f * unit)
              lineTo(x + 50.2f * unit, y + 29f * unit)
              lineTo(x, y + 58f * unit)
              lineTo(x - 50.2f * unit, y + 29f * unit)
              lineTo(x - 50.2f * unit, y - 29f * unit)
              close()
            }
        val (ground, outline) =
            when (area.kind) {
              "WaterArea" -> Color(0x2149B9F0) to Color(0xFF5BC8F5)
              "NoctisArea" -> Color(0x21FFC93B) to Color(0xFFFFD557)
              else -> Color.White.copy(alpha = 0.025f) to Color(0xFFE1E7EF)
            }
        drawPath(hex, ground)
        drawPath(
            hex,
            outline.copy(
                alpha = if (area.kind == "WaterArea" || area.kind == "NoctisArea") 1f else 0.88f
            ),
            style =
                Stroke(
                    width =
                        if (area.kind == "WaterArea" || area.kind == "NoctisArea") 1.92f else 1.32f
                ),
        )
        if (area.tile == null && area.kind == "VolcanicArea") {
          val volcano =
              Path().apply {
                moveTo(x - 17.2f * unit, y + 29.6f * unit)
                lineTo(x - 6.8f * unit, y + 8.8f * unit)
                lineTo(x - 1.2f * unit, y + 16f * unit)
                lineTo(x + 6.8f * unit, y + 5.6f * unit)
                lineTo(x + 17.2f * unit, y + 29.6f * unit)
                close()
              }
          drawPath(volcano, Color(0xD2191311))
        }
      }
    }
    for (area in areas) {
      val centerX = 500f + 107f * (area.column - area.row / 2f - 2.5f)
      val centerY = 125f + 93.5f * (area.row - 1)
      val tile = area.tile
      if (tile == null) {
        if (area.kind == "NoctisArea") {
          for ((index, line) in listOf("Noctis", "City").withIndex()) {
            Text(
                line,
                Modifier.offset(
                        left + side * ((centerX - 35f) / 1000f),
                        top + side * ((centerY + 5f + index * 17f) / 1000f),
                    )
                    .width(side * 0.07f),
                color = Color(0xFF221D12),
                fontSize = (side.value * 0.016f).sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
          }
        }
        for ((index, bonus) in area.bonus.withIndex()) {
          val iconSize = minOf(side * 0.025f, playerCubeSize)
          val position =
              Modifier.offset(
                      left + side * ((centerX - 45.7f + index * 25f) / 1000f),
                      top + side * ((centerY - 23f) / 1000f),
                  )
                  .size(iconSize)
          val image = bonus.image?.let { rememberBundledImage("files/icons/$it.png") }
          if (image != null) {
            Image(image, bonus.image, position, contentScale = ContentScale.Fit)
          } else if (bonus.label != null) {
            Box(position, contentAlignment = Alignment.Center) {
              Text(bonus.label, color = Color.White, fontSize = (side.value * 0.014f).sp)
            }
          }
        }
        continue
      }
      val image =
          rememberBundledImage("files/cards/$tile.png")
              ?: if ("City" in tile) rememberBundledImage("files/cards/CityTile.png") else null
      val tileWidth = side * (if (tile == "GreeneryTile") 0.093f else 0.118f)
      val tileHeight = side * (if (tile == "GreeneryTile") 0.107f else 0.118f)
      if (image != null)
          Image(
              image,
              tile,
              Modifier.offset(
                      left + side * (centerX / 1000f) - tileWidth / 2,
                      top + side * (centerY / 1000f) - tileHeight / 2,
                  )
                  .size(tileWidth, tileHeight),
              contentScale = ContentScale.Fit,
          )
      if (area.owner != null) {
        ActorSquare(
            area.owner,
            playerCubeSize,
            Modifier.offset(
                x = left + side * (centerX / 1000f) - playerCubeSize / 2,
                y = top + side * (centerY / 1000f) - playerCubeSize / 2,
            ),
        )
      }
    }
  }
}

private val RESOURCES = listOf("MC", "Steel", "Titanium", "Plant", "Energy", "Heat")

/** Every player side by side. */
@Composable
internal fun PlayersTab() {
  val specimen = LocalSpecimen.current
  val palette = LocalPalette.current
  TabColumn {
    ComparisonRow(header = true) {
      for (p in specimen.players) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { ActorSquare(p.name, 16.dp) }
      }
    }
    ComparisonRow("Terraform rating", "TerraformRating") {
      for (p in specimen.players) ComparisonValue(p.terraformRating)
    }
    ComparisonRow("Cards in hand", "ProjectCard") {
      for (p in specimen.players) ComparisonValue(p.handCards)
    }
    for (resource in RESOURCES) {
      ComparisonRow(resource, resource) {
        for (p in specimen.players) {
          val (stock, production) = p.resources.getValue(resource)
          ComparisonValue(stock, production)
        }
      }
    }
    Spacer(Modifier.height(palette.spacing))
    for (tag in specimen.players.first().tags.keys) {
      if (specimen.players.all { (it.tags[tag] ?: 0) == 0 }) continue
      ComparisonRow(tag.removeSuffix("Tag"), tag) {
        for (p in specimen.players) ComparisonValue(p.tags[tag] ?: 0)
      }
    }
    ComparisonRow("Cards played", null) {
      for (p in specimen.players) ComparisonValue(p.played.size)
    }
  }
}

@Composable
private fun ComparisonRow(
    label: String = "",
    icon: String? = null,
    header: Boolean = false,
    values: @Composable RowScope.() -> Unit,
) {
  val palette = LocalPalette.current
  Row(
      Modifier.fillMaxWidth().height(if (header) 30.dp else 26.dp),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    Row(Modifier.width(170.dp), verticalAlignment = Alignment.CenterVertically) {
      if (icon != null) GameIcon(icon, 18.dp) else Spacer(Modifier.width(18.dp))
      Spacer(Modifier.width(8.dp))
      Text(label, style = palette.body.copy(color = palette.dimText), maxLines = 1)
    }
    values()
  }
  if (!header)
      Box(Modifier.fillMaxWidth().height(0.5.dp).background(palette.line.copy(alpha = 0.4f)))
}

@Composable
private fun RowScope.ComparisonValue(value: Int, production: Int? = null) {
  val palette = LocalPalette.current
  Row(
      Modifier.weight(1f),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically,
  ) {
    Text("$value", style = palette.body)
    if (production != null) {
      Spacer(Modifier.width(6.dp))
      ProductionBadge(production)
    }
  }
}

/** Production as the board shows it: a small boxed, signed number. */
@Composable
internal fun ProductionBadge(production: Int) {
  val palette = LocalPalette.current
  Text(
      if (production < 0) "−${-production}" else "+$production",
      Modifier.border(
              palette.borderWidth,
              palette.mars.copy(alpha = 0.7f),
              RoundedCornerShape(3.dp),
          )
          .padding(horizontal = 4.dp),
      style = palette.body.copy(fontSize = palette.body.fontSize * 0.8f, color = palette.mars),
  )
}

/** Milestones and awards, as tiles on their tracks. */
@Composable
internal fun GoalsTab() {
  val specimen = LocalSpecimen.current
  val palette = LocalPalette.current
  TabColumn {
    SectionLabel("MILESTONES")
    Row(horizontalArrangement = Arrangement.spacedBy(palette.spacing)) {
      for (milestone in specimen.milestones) {
        GoalTile(milestone.name) {
          if (milestone.claimedBy != null) ActorSquare(milestone.claimedBy, 18.dp) else EmptySlot()
        }
      }
    }
    Spacer(Modifier.height(palette.spacing * 2))
    SectionLabel("AWARDS")
    Row(horizontalArrangement = Arrangement.spacedBy(palette.spacing)) {
      for (award in specimen.awards) {
        GoalTile(award.name) {
          if (award.funded) {
            Box(Modifier.size(18.dp).clip(CircleShape).background(palette.mars))
          } else {
            EmptySlot()
          }
        }
      }
    }
    Spacer(Modifier.height(palette.spacing))
    Text(
        "A cube marks a claimed milestone; a disc marks a funded award.",
        style = palette.body.copy(color = palette.dimText),
    )
  }
}

@Composable
private fun RowScope.GoalTile(name: String, marker: @Composable () -> Unit) {
  val palette = LocalPalette.current
  Column(
      Modifier.weight(1f)
          .height(96.dp)
          .clip(RoundedCornerShape(palette.cornerRadius))
          .background(palette.raised)
          .border(palette.borderWidth, palette.line, RoundedCornerShape(palette.cornerRadius))
          .padding(8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween,
  ) {
    ClassLinkedText(name, style = palette.body, maxLines = 1)
    marker()
  }
}

@Composable
private fun EmptySlot() {
  val palette = LocalPalette.current
  Box(Modifier.size(18.dp).border(1.dp, palette.line, RoundedCornerShape(3.dp)))
}

/** Live colony tiles and their production markers. */
@Composable
internal fun ColoniesTab() {
  val colonies = LocalSpecimen.current.colonies
  val palette = LocalPalette.current
  TabColumn {
    SectionLabel("COLONY TILES")
    if (colonies.isEmpty()) {
      Text("No colony tiles in play.", style = palette.body.copy(color = palette.dimText))
    }
    for (colony in colonies) {
      Row(
          Modifier.fillMaxWidth()
              .padding(vertical = palette.spacing / 2)
              .clip(RoundedCornerShape(palette.cornerRadius))
              .background(palette.raised)
              .padding(10.dp),
          verticalAlignment = Alignment.CenterVertically,
      ) {
        ClassLinkedText(colony.name, Modifier.width(110.dp), style = palette.body)
        for (step in 0..6) {
          Box(
              Modifier.padding(end = 8.dp)
                  .size(22.dp)
                  .clip(CircleShape)
                  .background(if (step == colony.production) palette.accent else palette.surface)
                  .border(1.dp, palette.line, CircleShape)
          )
        }
        Spacer(Modifier.weight(1f))
        colony.owners.forEach { ActorSquare(it, 14.dp, Modifier.padding(start = 5.dp)) }
      }
    }
  }
}

/** Parties, delegates, and the global event track. */
@Composable
internal fun TurmoilTab() {
  val specimen = LocalSpecimen.current
  val global = specimen.global
  val palette = LocalPalette.current
  TabColumn {
    Row(horizontalArrangement = Arrangement.spacedBy(palette.spacing * 3)) {
      KeyValue("CHAIRMAN") { ActorSquare(global.chairman, 14.dp) }
      KeyValue("RULING") { ClassLinkedText(global.ruling ?: "—", style = palette.body) }
      KeyValue("DOMINANT") { ClassLinkedText(global.dominant ?: "—", style = palette.body) }
    }
    Spacer(Modifier.height(palette.spacing))
    SectionLabel("PARTIES")
    for (party in specimen.turmoil.parties) {
      val ruling = party == global.ruling
      val dominant = party == global.dominant
      Row(
          Modifier.fillMaxWidth()
              .padding(vertical = 3.dp)
              .clip(RoundedCornerShape(palette.cornerRadius))
              .background(if (ruling) palette.accent.copy(alpha = 0.14f) else palette.raised)
              .border(
                  palette.borderWidth,
                  if (dominant) palette.mars else palette.line,
                  RoundedCornerShape(palette.cornerRadius),
              )
              .padding(horizontal = 10.dp, vertical = 7.dp),
          verticalAlignment = Alignment.CenterVertically,
      ) {
        ClassLinkedText(party, Modifier.width(130.dp), style = palette.body)
        val leader = specimen.turmoil.leaders[party]
        val seats =
            specimen.turmoil.delegates
                .filter { it.party == party }
                .flatMap { d -> List(d.count) { d.owner } }
                .sortedByDescending { it == leader }
        seats.forEachIndexed { i, owner ->
          val square = if (i == 0 && owner == leader) 16.dp else 12.dp
          ActorSquare(owner, square, Modifier.padding(end = 5.dp))
        }
        Spacer(Modifier.weight(1f))
        Text(
            listOfNotNull("ruling".takeIf { ruling }, "dominant".takeIf { dominant })
                .joinToString(", "),
            style = palette.label,
        )
      }
    }
    Spacer(Modifier.height(palette.spacing))
    SectionLabel("GLOBAL EVENTS")
    for ((position, event) in
        listOf(
            "CURRENT" to global.currentGlobalEvent,
            "COMING" to global.comingGlobalEvent,
            "DISTANT" to global.distantGlobalEvent,
        )) {
      Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(position, Modifier.width(90.dp), style = palette.label)
        PetsText(event ?: "—", color = palette.text)
      }
    }
  }
}

/** The game file's resolved GameConfig and GamePremise selections. */
@Composable
internal fun ConfigTab(config: GameConfig?, premise: GamePremise?) {
  val palette = LocalPalette.current
  TabColumn {
    SectionLabel("FILE CONFIG · RESOLVED")
    if (premise == null) return@TabColumn
    if (config == null) {
      Text("Game-file config is absent from this recording.", style = palette.body)
    } else {
      Text(
          listOf(config.playerNames.joinToString(), config.toString())
              .filter(String::isNotBlank)
              .joinToString(),
          style = palette.mono,
      )
    }
    Spacer(Modifier.height(palette.spacing))
    SectionLabel("GAME PREMISE")
    val table = premise.classTable
    val milestone = table.getClass(cn("Milestone"))
    val award = table.getClass(cn("Award"))
    val card = table.getClass(cn("CardFront"))
    val selections =
        premise.classSelections
            .filter { it.included }
            .groupBy { selection ->
              val name = selection.className
              val klass = table.getClass(name)
              when {
                name in premise.catalog.modules -> "Modules"
                klass.isSubtypeOf(milestone) -> "Milestone"
                klass.isSubtypeOf(award) -> "Awards"
                klass.isSubtypeOf(card) -> "Cards"
                else -> "Other"
              }
            }
            .mapValues { (_, choices) ->
              choices.map { "${it.className}" }
            }
    val sections =
        listOf(
            "Players" to premise.playerNames.map { it.toString() },
            "Modules" to selections["Modules"].orEmpty(),
            "Milestone" to selections["Milestone"].orEmpty(),
            "Awards" to selections["Awards"].orEmpty(),
            "Cards" to selections["Cards"].orEmpty(),
            "Other" to
                selections["Other"].orEmpty() +
                    premise.componentAdjustments.map { (name, count) -> "$count $name" },
        )
    for ((title, entries) in sections) {
      SectionLabel(title.uppercase())
      for (entry in entries) {
        Row(Modifier.padding(vertical = 2.dp)) {
          Text("• ", style = palette.mono)
          ClassLinkedText(entry)
        }
      }
      Spacer(Modifier.height(palette.spacing))
    }
  }
}

/** The selected Pets class and the selected task, using the loaded recording's class table. */
@Composable
internal fun PetsInfoTab(selectedTask: Int?, selectedClass: String?, classTable: ClassTable?) {
  val specimen = LocalSpecimen.current
  val palette = LocalPalette.current
  TabColumn {
    SectionLabel("SELECTED CLASS")
    val klass = selectedClass?.let { classTable?.findClass(cn(it)) }
    if (klass == null) {
      Text(
          "Select a class name anywhere in the workspace.",
          style = palette.body.copy(color = palette.dimText),
      )
    } else {
      val tree = remember(specimen) { TypeTree(specimen) }
      val present =
          specimen.components.filter { klass.className.toString() in tree.ancestors(it.className) }
      RawField("name", klass.className.toString())
      klass.docstring?.takeIf(String::isNotBlank)?.let { RawField("docstring", it) }
      SectionLabel("PETS DECLARATION")
      ClassLinkedText(klass.declaration.copy(docstring = null).toString(), Modifier.fillMaxWidth())
      Spacer(Modifier.height(palette.spacing))
      RawField("baseType", klass.baseType.toString())
      RawField("supertypes", klass.allSuperclasses().joinToString(", ") { it.className.toString() })
      RawField(
          "dependencies",
          klass.dependencies.keys.joinToString(", ") { key ->
            "$key: ${klass.dependencies.get(key).expressionFull}"
          },
      )
      if (klass.invariants.isNotEmpty()) {
        SectionLabel("INVARIANTS")
        klass.invariants.forEach { ClassLinkedText(it.toString(), Modifier.fillMaxWidth()) }
      }
      classTable?.allSubclasses(klass)?.let { subclasses ->
        val names =
            subclasses.map { it.className.toString() }.filter { it != selectedClass }.sorted()
        if (names.isNotEmpty()) {
          SectionLabel("SUBCLASSES (${names.size})")
          ClassLinkedText(names.take(40).joinToString(", "), Modifier.fillMaxWidth())
        }
      }
      RawField("presentTypes", "${present.size}")
      RawField("presentComponents", "${present.sumOf { it.count }}")
    }
    Spacer(Modifier.height(palette.spacing * 2))
    val task = specimen.tasks.find { it.id == selectedTask }
    SectionLabel("SELECTED TASK")
    if (task == null) {
      Text("Select a task in the task queue.", style = palette.body.copy(color = palette.dimText))
    } else {
      RawField("id", "${task.id}")
      RawField("instruction", task.instruction)
      RawField("then", task.then ?: "")
      RawField("assignee", task.assignee)
      RawField("selected", "${task.selected}")
      RawField("cause", "VIA ${task.givenBy} BECAUSE ${task.because}")
    }
  }
}

@Composable
private fun RawField(name: String, value: String) {
  val palette = LocalPalette.current
  Row(Modifier.padding(vertical = 2.dp)) {
    Text(name, Modifier.width(174.dp), style = palette.mono.copy(color = palette.dimText))
    ClassLinkedText(value, Modifier.weight(1f))
  }
}

@Composable
private fun KeyValue(key: String, value: @Composable () -> Unit) {
  val palette = LocalPalette.current
  Row(verticalAlignment = Alignment.CenterVertically) {
    Text(key, style = palette.label)
    Spacer(Modifier.width(8.dp))
    value()
  }
}

@Composable
internal fun SectionLabel(text: String) {
  val palette = LocalPalette.current
  Text(text, Modifier.padding(bottom = 6.dp), style = palette.label)
}

@Composable
private fun TabColumn(content: @Composable ColumnScope.() -> Unit) {
  val palette = LocalPalette.current
  Column(
      Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(palette.spacing * 1.6f),
      content = content,
  )
}
