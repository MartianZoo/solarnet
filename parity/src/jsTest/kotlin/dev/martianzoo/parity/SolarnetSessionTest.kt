package dev.martianzoo.parity

import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.api.Exceptions.TaskException
import kotlin.js.JsNonModule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal class SolarnetSessionTest {
  @Test
  fun createsCanonicalGameInNode() {
    val session = SolarnetSession("CorporateEraExpansion", 2, NodeFiles::readUtf8)
    try {
      val snapshot = Json.parseToJsonElement(session.snapshot()).jsonObject
      assertEquals(1, snapshot.getValue("generation").jsonPrimitive.content.toInt())
      assertEquals("corporation", snapshot.getValue("phase").jsonPrimitive.content)
      assertEquals(1, snapshot.getValue("firstPlayer").jsonPrimitive.content.toInt())

      val initialEvents = Json.parseToJsonElement(session.eventsSince(0)).jsonObject
      val initialCursor = initialEvents.getValue("nextCursor").jsonPrimitive.content.toInt()
      assertTrue(initialEvents.getValue("lines").jsonArray.isNotEmpty())
      assertTrue(
          Json.parseToJsonElement(session.eventsSince(initialCursor))
              .jsonObject
              .getValue("lines")
              .jsonArray
              .isEmpty()
      )

      session.apply(
          """{"operation":"selectCorporation","player":1,"corporation":"InterplanetaryCinematics","projectCards":1}"""
      )
      val firstMoveEvents = Json.parseToJsonElement(session.eventsSince(initialCursor)).jsonObject
      val firstMoveCursor = firstMoveEvents.getValue("nextCursor").jsonPrimitive.content.toInt()
      assertTrue(firstMoveCursor > initialCursor)
      assertTrue(firstMoveEvents.getValue("lines").jsonArray.isNotEmpty())
      assertTrue(
          firstMoveEvents
              .getValue("lines")
              .jsonArray
              .map { it.jsonPrimitive.content }
              .none { "Task" in it }
      )

      val afterBoth =
          Json.parseToJsonElement(
                  session.apply(
                      """{"operation":"selectCorporation","player":2,"corporation":"CrediCor","projectCards":0}"""
                  )
              )
              .jsonObject
      assertEquals("action", afterBoth.getValue("phase").jsonPrimitive.content)

      val secondMoveEvents =
          Json.parseToJsonElement(session.eventsSince(firstMoveCursor)).jsonObject
      val secondMoveCursor = secondMoveEvents.getValue("nextCursor").jsonPrimitive.content.toInt()
      assertTrue(secondMoveCursor > firstMoveCursor)
      assertTrue(secondMoveEvents.getValue("lines").jsonArray.isNotEmpty())

      val afterProject =
          Json.parseToJsonElement(
                  session.apply(
                      """{"operation":"playProject","player":1,"cardId":"105","payment":{"megacredits":1,"steel":0,"titanium":0}}"""
                  )
              )
              .jsonObject
      assertEquals("action", afterProject.getValue("phase").jsonPrimitive.content)

      val projectEvents = Json.parseToJsonElement(session.eventsSince(secondMoveCursor)).jsonObject
      assertTrue(
          projectEvents.getValue("nextCursor").jsonPrimitive.content.toInt() > secondMoveCursor
      )
      val projectLines = projectEvents.getValue("lines").jsonArray.map { it.jsonPrimitive.content }
      assertTrue(projectLines.any { "EarthOffice" in it })
      assertTrue(
          projectLines.any {
            "Pay<Player1, Class<Megacredit>> FROM Megacredit<Player1>" in it
          }
      )

      assertFailsWith<NarrowingException> {
        session.apply("""{"operation":"pass","player":1}""")
      }
      session.apply("""{"operation":"standardProject","player":1,"project":"aquifer"}""")
      val standardProjectEvents =
          Json.parseToJsonElement(
                  session.eventsSince(
                      projectEvents.getValue("nextCursor").jsonPrimitive.content.toInt()
                  )
              )
              .jsonObject
      val standardProjectCursor =
          standardProjectEvents.getValue("nextCursor").jsonPrimitive.content.toInt()
      val standardProjectLines =
          standardProjectEvents.getValue("lines").jsonArray.map { it.jsonPrimitive.content }
      assertTrue(standardProjectLines.any { "-18 Megacredit<Player1>" in it })
      assertTrue(standardProjectLines.none { ": +OceanTile<" in it })
      assertTrue(standardProjectLines.none { "NewTurn<Player2>" in it })

      session.apply("""{"operation":"placeTile","player":1,"tile":"ocean","spaceId":"04"}""")
      val tileEvents =
          Json.parseToJsonElement(session.eventsSince(standardProjectCursor)).jsonObject
      val tileCursor = tileEvents.getValue("nextCursor").jsonPrimitive.content.toInt()
      val tileLines = tileEvents.getValue("lines").jsonArray.map { it.jsonPrimitive.content }
      assertTrue(tileLines.any { "+OceanTile<Tharsis_1_2" in it })
      assertTrue(tileLines.any { "+TerraformRating<Player1>" in it })
      assertTrue(tileLines.any { "+2 Steel<Player1>" in it })
      assertTrue(tileLines.none { "NewTurn<Player2>" in it })

      assertFailsWith<TaskException> {
        session.apply("""{"operation":"endTurn","player":2}""")
      }
      val afterPass =
          Json.parseToJsonElement(session.apply("""{"operation":"pass","player":2}""")).jsonObject
      assertEquals(
          Json.parseToJsonElement(
              """
              {
                "generation": 1,
                "phase": "action",
                "gameEnd": false,
                "firstPlayer": 1,
                "passedPlayers": [2],
                "waitingPlayers": [1],
                "secondActionPlayers": [],
                "players": [
                  {
                    "seat": 1,
                    "terraformRating": 21,
                    "victoryPoints": 0,
                    "resources": {
                      "megacredits": 8,
                      "steel": 22,
                      "titanium": 0,
                      "plants": 0,
                      "energy": 0,
                      "heat": 0
                    },
                    "production": {
                      "megacredits": 0,
                      "steel": 0,
                      "titanium": 0,
                      "plants": 0,
                      "energy": 0,
                      "heat": 0
                    },
                    "handCount": 0,
                    "playedCardIds": ["105", "B04"]
                  },
                  {
                    "seat": 2,
                    "terraformRating": 20,
                    "victoryPoints": 0,
                    "resources": {
                      "megacredits": 57,
                      "steel": 0,
                      "titanium": 0,
                      "plants": 0,
                      "energy": 0,
                      "heat": 0
                    },
                    "production": {
                      "megacredits": 0,
                      "steel": 0,
                      "titanium": 0,
                      "plants": 0,
                      "energy": 0,
                      "heat": 0
                    },
                    "handCount": 0,
                    "playedCardIds": ["B01"]
                  }
                ],
                "globalParameters": {"temperature": -30, "oxygen": 0, "oceans": 1},
                "tiles": [{"row": 1, "column": 2, "kind": "ocean", "owner": null}]
              }
              """
          ),
          afterPass,
      )
      val passEvents = Json.parseToJsonElement(session.eventsSince(tileCursor)).jsonObject
      val passLines = passEvents.getValue("lines").jsonArray.map { it.jsonPrimitive.content }
      assertTrue(passLines.any { "+Pass<Player2>" in it })
      assertTrue(passLines.none { "NewTurn<Player1>" in it })
    } finally {
      session.close()
    }
  }

  @Test
  fun executesVerifiedCardActionAndFiltersItsLog() {
    val session = SolarnetSession("CorporateEraExpansion", 2, NodeFiles::readUtf8)
    try {
      session.apply(
          """{"operation":"selectCorporation","player":1,"corporation":"InterplanetaryCinematics","projectCards":1}"""
      )
      session.apply(
          """{"operation":"selectCorporation","player":2,"corporation":"CrediCor","projectCards":0}"""
      )
      session.apply(
          """{"operation":"playProject","player":1,"cardId":"013","payment":{"megacredits":1,"steel":13,"titanium":0}}"""
      )
      val cursor =
          Json.parseToJsonElement(session.eventsSince(0))
              .jsonObject
              .getValue("nextCursor")
              .jsonPrimitive
              .content
              .toInt()

      val snapshot =
          Json.parseToJsonElement(
                  session.apply("""{"operation":"cardAction","player":1,"cardId":"013"}""")
              )
              .jsonObject
      val player1 = snapshot.getValue("players").jsonArray[0].jsonObject
      val resources = player1.getValue("resources").jsonObject
      assertEquals(31, resources.getValue("megacredits").jsonPrimitive.content.toInt())
      assertEquals(6, resources.getValue("steel").jsonPrimitive.content.toInt())

      val lines =
          Json.parseToJsonElement(session.eventsSince(cursor))
              .jsonObject
              .getValue("lines")
              .jsonArray
              .map { it.jsonPrimitive.content }
      assertTrue(lines.any { "-Steel<Player1>" in it })
      assertTrue(lines.any { "+5 Megacredit<Player1>" in it })
      assertTrue(lines.none { "Task" in it })
    } finally {
      session.close()
    }
  }

  @Test
  fun projectsPlayedEventIdFromItsTypedCardDependency() {
    val session = SolarnetSession("CorporateEraExpansion", 2, NodeFiles::readUtf8)
    try {
      session.apply(
          """{"operation":"selectCorporation","player":1,"corporation":"CrediCor","projectCards":1}"""
      )
      session.apply(
          """{"operation":"selectCorporation","player":2,"corporation":"InterplanetaryCinematics","projectCards":0}"""
      )
      val snapshot =
          Json.parseToJsonElement(
                  session.apply(
                      """{"operation":"playProject","player":1,"cardId":"112","payment":{"megacredits":7,"steel":0,"titanium":0}}"""
                  )
              )
              .jsonObject
      val player1 =
          snapshot
              .getValue("players")
              .jsonArray
              .map { it.jsonObject }
              .single { it.getValue("seat").jsonPrimitive.content.toInt() == 1 }
      assertEquals(
          listOf("112", "B01"),
          player1.getValue("playedCardIds").jsonArray.map { it.jsonPrimitive.content },
      )
    } finally {
      session.close()
    }
  }

  @Test
  fun resolvesSimplifiedCanonCardFromPrintedId() {
    val session = SolarnetSession("CorporateEraExpansion", 2, NodeFiles::readUtf8)
    try {
      session.apply(
          """{"operation":"selectCorporation","player":1,"corporation":"InterplanetaryCinematics","projectCards":1}"""
      )
      session.apply(
          """{"operation":"selectCorporation","player":2,"corporation":"CrediCor","projectCards":0}"""
      )
      val snapshot =
          Json.parseToJsonElement(
                  session.apply(
                      """{"operation":"playProject","player":1,"cardId":"110","payment":{"megacredits":4,"steel":0,"titanium":0}}"""
                  )
              )
              .jsonObject
      val playedCardIds =
          snapshot
              .getValue("players")
              .jsonArray[0]
              .jsonObject
              .getValue("playedCardIds")
              .jsonArray
              .map { it.jsonPrimitive.content }
      assertEquals(listOf("110", "B04"), playedCardIds)
    } finally {
      session.close()
    }
  }

  @Test
  fun rejectsProjectPaymentWithoutChangingStateOrHistory() {
    val session = SolarnetSession("CorporateEraExpansion", 2, NodeFiles::readUtf8)
    try {
      session.apply(
          """{"operation":"selectCorporation","player":1,"corporation":"InterplanetaryCinematics","projectCards":1}"""
      )
      session.apply(
          """{"operation":"selectCorporation","player":2,"corporation":"CrediCor","projectCards":0}"""
      )
      val beforeSnapshot = session.snapshot()
      val beforeEvents = Json.parseToJsonElement(session.eventsSince(0)).jsonObject
      val cursor = beforeEvents.getValue("nextCursor").jsonPrimitive.content.toInt()

      val invalidPayments =
          listOf(
              """{"megacredits":0,"steel":0,"titanium":0}""",
              """{"megacredits":2,"steel":0,"titanium":0}""",
              """{"megacredits":0,"steel":1,"titanium":0}""",
              """{"megacredits":0,"steel":0,"titanium":1}""",
          )
      invalidPayments.forEach { payment ->
        assertFails {
          session.apply(
              """{"operation":"playProject","player":1,"cardId":"105","payment":$payment}"""
          )
        }
        assertEquals(beforeSnapshot, session.snapshot())
        val afterEvents = Json.parseToJsonElement(session.eventsSince(cursor)).jsonObject
        assertEquals(cursor, afterEvents.getValue("nextCursor").jsonPrimitive.content.toInt())
        assertTrue(afterEvents.getValue("lines").jsonArray.isEmpty())
      }

      session.apply(
          """{"operation":"playProject","player":1,"cardId":"105","payment":{"megacredits":1,"steel":0,"titanium":0}}"""
      )
    } finally {
      session.close()
    }
  }

  @Test
  fun endsOnlyAnOfferedSecondAction() {
    val session = SolarnetSession("CorporateEraExpansion", 2, NodeFiles::readUtf8)
    try {
      session.apply(
          """{"operation":"selectCorporation","player":1,"corporation":"InterplanetaryCinematics","projectCards":1}"""
      )
      session.apply(
          """{"operation":"selectCorporation","player":2,"corporation":"CrediCor","projectCards":0}"""
      )
      session.apply(
          """{"operation":"playProject","player":1,"cardId":"105","payment":{"megacredits":1,"steel":0,"titanium":0}}"""
      )
      val beforeEndTurn =
          Json.parseToJsonElement(session.eventsSince(0))
              .jsonObject
              .getValue("nextCursor")
              .jsonPrimitive
              .content
              .toInt()

      session.apply("""{"operation":"endTurn","player":1}""")
      val endTurnLines =
          Json.parseToJsonElement(session.eventsSince(beforeEndTurn))
              .jsonObject
              .getValue("lines")
              .jsonArray
              .map { it.jsonPrimitive.content }
      assertTrue(endTurnLines.none { "NewTurn<Player2>" in it })
      assertFailsWith<TaskException> {
        session.apply("""{"operation":"endTurn","player":2}""")
      }
    } finally {
      session.close()
    }
  }

  @Test
  fun completesMinimalTwoPlayerGameThroughExportedMoves() {
    val session = SolarnetSession("CorporateEraExpansion", 2, NodeFiles::readUtf8)
    try {
      fun apply(move: String): JsonObject = Json.parseToJsonElement(session.apply(move)).jsonObject

      fun int(snapshot: JsonObject, key: String): Int =
          snapshot.getValue(key).jsonPrimitive.content.toInt()

      fun global(snapshot: JsonObject, key: String): Int =
          int(snapshot.getValue("globalParameters").jsonObject, key)

      fun waitingPlayer(snapshot: JsonObject): Int =
          snapshot.getValue("waitingPlayers").jsonArray.first().jsonPrimitive.content.toInt()

      fun player(snapshot: JsonObject, seat: Int): JsonObject =
          snapshot
              .getValue("players")
              .jsonArray
              .map { it.jsonObject }
              .single { int(it, "seat") == seat }

      fun resource(snapshot: JsonObject, seat: Int, kind: String): Int =
          int(player(snapshot, seat).getValue("resources").jsonObject, kind)

      val oceanSpaces = ArrayDeque(listOf("04", "06", "07", "13", "28", "32", "33", "34", "43"))
      val greenerySpaces =
          mapOf(
              1 to
                  ArrayDeque(
                      listOf(
                          "35",
                          "27",
                          "26",
                          "25",
                          "24",
                          "23",
                          "16",
                          "15",
                          "09",
                          "08",
                          "14",
                          "21",
                          "29",
                          "30",
                          "31",
                      )
                  ),
              2 to
                  ArrayDeque(
                      listOf(
                          "55",
                          "54",
                          "53",
                          "59",
                          "60",
                          "61",
                          "56",
                          "57",
                          "50",
                          "49",
                          "48",
                          "47",
                          "46",
                          "39",
                          "40",
                      )
                  ),
          )

      fun place(seat: Int, tile: String): JsonObject {
        val space =
            if (tile == "ocean") oceanSpaces.removeFirst()
            else greenerySpaces.getValue(seat).removeFirst()
        return apply(
            """{"operation":"placeTile","player":$seat,"tile":"$tile","spaceId":"$space"}"""
        )
      }

      apply(
          """{"operation":"selectCorporation","player":1,"corporation":"CrediCor","projectCards":1}"""
      )
      var snapshot =
          apply(
              """{"operation":"selectCorporation","player":2,"corporation":"Teractor","projectCards":0}"""
          )
      assertEquals("action", snapshot.getValue("phase").jsonPrimitive.content)

      var builtCity = false
      var powerPlantsBuilt = 0
      var soldPatent = false
      var convertedHeat = false
      var convertedPlants = false
      var acceptedMoves = 2

      while (!snapshot.getValue("gameEnd").jsonPrimitive.content.toBoolean()) {
        check(acceptedMoves < 500) { "minimal full-game driver did not converge: $snapshot" }
        when (snapshot.getValue("phase").jsonPrimitive.content) {
          "research" -> {
            val seat = waitingPlayer(snapshot)
            snapshot = apply("""{"operation":"buyCards","player":$seat,"count":0}""")
          }
          "action" -> {
            val seat = waitingPlayer(snapshot)
            val megacredits = resource(snapshot, seat, "megacredits")
            val temperature = global(snapshot, "temperature")
            val oceans = global(snapshot, "oceans")
            val oxygen = global(snapshot, "oxygen")
            when {
              seat == 1 && !builtCity && megacredits >= 25 -> {
                apply("""{"operation":"standardProject","player":1,"project":"city"}""")
                snapshot =
                    apply("""{"operation":"placeTile","player":1,"tile":"city","spaceId":"36"}""")
                builtCity = true
              }
              seat == 1 && powerPlantsBuilt < 3 && megacredits >= 11 -> {
                snapshot =
                    apply("""{"operation":"standardProject","player":1,"project":"powerPlant"}""")
                powerPlantsBuilt++
              }
              seat == 1 && !soldPatent -> {
                snapshot = apply("""{"operation":"sellPatents","player":1,"count":1}""")
                soldPatent = true
              }
              oxygen < 14 && resource(snapshot, seat, "plants") >= 8 -> {
                apply("""{"operation":"convertPlants","player":$seat}""")
                snapshot = place(seat, "greenery")
                convertedPlants = true
              }
              temperature < 8 && resource(snapshot, seat, "heat") >= 8 -> {
                val oldTemperature = temperature
                val actionSnapshot = apply("""{"operation":"convertHeat","player":$seat}""")
                snapshot =
                    if (oldTemperature == -2 && oceans < 9) place(seat, "ocean") else actionSnapshot
                convertedHeat = true
              }
              temperature < 8 && megacredits >= 14 -> {
                val oldTemperature = temperature
                val actionSnapshot =
                    apply("""{"operation":"standardProject","player":$seat,"project":"asteroid"}""")
                snapshot =
                    if (oldTemperature == -2 && oceans < 9) place(seat, "ocean") else actionSnapshot
              }
              oceans < 9 && megacredits >= 18 -> {
                apply("""{"operation":"standardProject","player":$seat,"project":"aquifer"}""")
                snapshot = place(seat, "ocean")
              }
              oxygen < 14 && megacredits >= 23 -> {
                apply("""{"operation":"standardProject","player":$seat,"project":"greenery"}""")
                snapshot = place(seat, "greenery")
              }
              else -> {
                val secondActionSeats =
                    snapshot.getValue("secondActionPlayers").jsonArray.map {
                      it.jsonPrimitive.content.toInt()
                    }
                snapshot =
                    if (seat in secondActionSeats) {
                      apply("""{"operation":"endTurn","player":$seat}""")
                    } else {
                      apply("""{"operation":"pass","player":$seat}""")
                    }
              }
            }
          }
          "finalGreenery" -> {
            val seat = waitingPlayer(snapshot)
            snapshot =
                if (resource(snapshot, seat, "plants") >= 8) {
                  apply("""{"operation":"convertPlants","player":$seat}""")
                  place(seat, "greenery")
                } else {
                  apply("""{"operation":"declineFinalGreenery","player":$seat}""")
                }
          }
          else -> error("Unexpected full-game phase: $snapshot")
        }
        acceptedMoves++
      }

      assertTrue(int(snapshot, "generation") > 1)
      assertEquals("end", snapshot.getValue("phase").jsonPrimitive.content)
      assertEquals(8, global(snapshot, "temperature"))
      assertEquals(14, global(snapshot, "oxygen"))
      assertEquals(9, global(snapshot, "oceans"))
      assertTrue(builtCity)
      assertEquals(3, powerPlantsBuilt)
      assertTrue(soldPatent)
      assertTrue(convertedHeat)
      assertTrue(convertedPlants)
      assertTrue(
          snapshot
              .getValue("players")
              .jsonArray
              .map { int(it.jsonObject, "victoryPoints") }
              .all { it > 0 }
      )
      val tiles = snapshot.getValue("tiles").jsonArray.map { it.jsonObject }
      assertEquals(9, tiles.count { it.getValue("kind").jsonPrimitive.content == "ocean" })
      assertTrue(
          tiles.any {
            it.getValue("kind").jsonPrimitive.content == "city" &&
                it.getValue("owner").jsonPrimitive.content.toInt() == 1
          }
      )
      assertTrue(
          tiles.any {
            it.getValue("kind").jsonPrimitive.content == "greenery" &&
                it.getValue("owner").jsonPrimitive.content.toInt() in 1..2
          }
      )
    } finally {
      session.close()
    }
  }
}

@JsModule("fs")
@JsNonModule
private external object NodeFileSystem {
  @Suppress("UnusedParameter") fun readFileSync(path: String, encoding: String): String
}

private object NodeFiles {
  fun readUtf8(path: String): String = NodeFileSystem.readFileSync("kotlin/$path", "utf8")
}
