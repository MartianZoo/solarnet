package dev.martianzoo.tfm.web.playground

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** The slim vertical summary of global state. */
@Composable
internal fun GlobalDashboard() {
  val global = LocalSpecimen.current.global
  val palette = LocalPalette.current
  val openClass = LocalOpenClass.current
  Surface {
    Column(
        Modifier.fillMaxSize().padding(vertical = palette.spacing),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
      Stat("GEN") { BigValue("${global.generation}") }
      Stat("PHASE") {
        val phase = global.phase
        val name = phase?.let { "${it}Phase" }
        val image = name?.let { rememberClassIcon(it) }
        if (image == null) Value(phase ?: "—")
        else {
          Tooltip("$phase phase") {
            Image(
                image,
                name,
                Modifier.height(20.dp)
                    .width(20.dp * image.intrinsicSize.width / image.intrinsicSize.height)
                    .clickable {
                      openClass(checkNotNull(name))
                    },
            )
          }
        }
      }
      Stat("TURN") {
        if (global.playerOnTurn == null) Value("—") else ActorSquare(global.playerOnTurn, 14.dp)
      }
      Stat("FIRST") { ActorSquare(global.startingPlayer, 14.dp) }
      Stat("TEMP", "TemperatureStep") { Value("${-30 + 2 * global.temperatureSteps} °C") }
      Stat("OXYGEN", "OxygenStep") { Value("${global.oxygenSteps} %") }
      Stat("OCEANS", "OceanTile") { Value("${global.oceans}") }
      Stat("VENUS", "VenusStep") { Value("${2 * global.venusSteps} %") }
      Stat("RULING") { Value(global.ruling ?: "—") }
      Stat("CHAIR") { ActorSquare(global.chairman, 14.dp) }
    }
  }
}

@Composable
private fun Stat(label: String, choice: String? = null, value: @Composable () -> Unit) {
  val palette = LocalPalette.current
  Column(
      choice?.let { choiceModifier(listOf(it, "Class<$it>")) } ?: Modifier,
      horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(label, style = palette.label)
    Spacer(Modifier.height(3.dp))
    value()
  }
}

@Composable
private fun Value(text: String) {
  val palette = LocalPalette.current
  ClassLinkedText(text, style = palette.body, maxLines = 1)
}

@Composable
private fun BigValue(text: String) {
  val palette = LocalPalette.current
  Text(
      text,
      style =
          palette.body.copy(
              fontSize = palette.body.fontSize * 1.9f,
              fontWeight = FontWeight.SemiBold,
          ),
  )
}

/** Which player the player area shows, and which way the last switch swiped. */
@Stable
internal class ViewedPlayer(private val count: Int) {
  var index by mutableIntStateOf(0)
    private set

  var direction by mutableIntStateOf(1)
    private set

  fun step(by: Int) {
    direction = by
    index = (index + by + count) % count
  }
}

/** Shows [content] for the viewed player, swiping horizontally when the player changes. */
@Composable
internal fun PlayerSwipe(
    viewed: ViewedPlayer,
    content: @Composable (Specimen.PlayerState) -> Unit,
) {
  val players = LocalSpecimen.current.players
  AnimatedContent(
      viewed.index,
      Modifier.fillMaxSize(),
      transitionSpec = {
        val d = viewed.direction
        slideInHorizontally(tween(240)) { d * it } togetherWith
            slideOutHorizontally(tween(240)) { -d * it }
      },
  ) { index ->
    content(players[index])
  }
}

/** One of the tall, narrow rectangles flanking the player area. */
@Composable
internal fun PlayerArrow(symbol: String, onClick: () -> Unit) {
  val palette = LocalPalette.current
  Surface(Modifier.clickable(onClick = onClick), color = palette.raised) {
    Text(
        symbol,
        Modifier.align(Alignment.TopCenter).padding(top = 24.dp),
        style = palette.body.copy(fontSize = palette.body.fontSize * 1.6f, color = palette.dimText),
    )
  }
}

private val RESOURCES = listOf("MC", "Steel", "Titanium", "Plant", "Energy", "Heat")

/** Stocks and production, terraform rating, hand size, and tags. */
@Composable
internal fun PlayerDashboard(player: Specimen.PlayerState) {
  val palette = LocalPalette.current
  val openClass = LocalOpenClass.current
  Surface {
    Row(
        Modifier.fillMaxSize().padding(horizontal = palette.spacing * 1.4f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      ActorSquare(player.name, 30.dp)
      Rule()
      Counter("TerraformRating", player.terraformRating, "TR")
      Spacer(Modifier.width(palette.spacing * 1.5f))
      Counter("ProjectCard", player.handCards, "CARDS")
      Spacer(Modifier.width(palette.spacing * 1.5f))
      Counter("EventTag", player.eventCards, "EVENTS")
      Rule()
      for (resource in RESOURCES) {
        val (stock, production) = player.resources.getValue(resource)
        Column(
            Modifier.padding(horizontal = palette.spacing * 0.8f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Row(
              choiceModifier(listOf(resource, "Class<$resource>", "$resource<${player.name}>")) {
                openClass(resource)
              },
              verticalAlignment = Alignment.CenterVertically,
          ) {
            GameIcon(resource, 26.dp, interactive = false)
            Spacer(Modifier.width(6.dp))
            Text("$stock", style = palette.body.copy(fontSize = palette.body.fontSize * 1.35f))
          }
          Spacer(Modifier.height(4.dp))
          ProductionBadge(production, resource, player.name)
        }
      }
      Rule()
      Row(
          Modifier.weight(1f),
          horizontalArrangement = Arrangement.spacedBy(palette.spacing * 0.8f),
          verticalAlignment = Alignment.CenterVertically,
      ) {
        for ((tag, count) in player.tags) {
          if (count == 0) continue
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            GameIcon(tag, 22.dp)
            Text("$count", style = palette.body)
          }
        }
      }
    }
  }
}

@Composable
private fun Counter(icon: String, value: Int, label: String) {
  val palette = LocalPalette.current
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      GameIcon(icon, 24.dp)
      Spacer(Modifier.width(6.dp))
      Text("$value", style = palette.body.copy(fontSize = palette.body.fontSize * 1.35f))
    }
    Text(label, style = palette.label)
  }
}
