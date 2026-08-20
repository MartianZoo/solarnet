package dev.martianzoo.parity

import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.api.Exceptions.TaskException
import kotlin.js.JsNonModule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
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
                "firstPlayer": 1,
                "passedPlayers": [2],
                "players": [
                  {
                    "seat": 1,
                    "terraformRating": 21,
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
}

@JsModule("fs")
@JsNonModule
private external object NodeFileSystem {
  @Suppress("UnusedParameter") fun readFileSync(path: String, encoding: String): String
}

private object NodeFiles {
  fun readUtf8(path: String): String = NodeFileSystem.readFileSync("kotlin/$path", "utf8")
}
