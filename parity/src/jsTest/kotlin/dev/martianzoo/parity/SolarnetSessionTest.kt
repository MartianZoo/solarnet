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
      assertEquals("CorporationPhase", snapshot.getValue("phase").jsonPrimitive.content)
      assertEquals(
          listOf("Player1", "Player2"),
          snapshot.getValue("players").jsonArray.map { it.jsonPrimitive.content },
      )

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

      val afterBoth =
          Json.parseToJsonElement(
                  session.apply(
                      """{"operation":"selectCorporation","player":2,"corporation":"CrediCor","projectCards":0}"""
                  )
              )
              .jsonObject
      assertEquals("ActionPhase", afterBoth.getValue("phase").jsonPrimitive.content)

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
      assertEquals("ActionPhase", afterProject.getValue("phase").jsonPrimitive.content)

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
      session.apply(
          """{"operation":"standardProject","player":1,"project":"aquifer","target":{"spaceId":"04"}}"""
      )
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
      assertTrue(standardProjectLines.any { "+OceanTile<Tharsis_1_2" in it })
      assertTrue(standardProjectLines.any { "+TerraformRating<Player1>" in it })
      assertTrue(standardProjectLines.any { "+2 Steel<Player1>" in it })
      assertTrue(standardProjectLines.any { "NewTurn<Player2>" in it })

      assertFailsWith<TaskException> {
        session.apply("""{"operation":"endTurn","player":2}""")
      }
      session.apply("""{"operation":"pass","player":2}""")
      val passEvents =
          Json.parseToJsonElement(session.eventsSince(standardProjectCursor)).jsonObject
      val passLines = passEvents.getValue("lines").jsonArray.map { it.jsonPrimitive.content }
      assertTrue(passLines.any { "+Pass<Player2>" in it })
      assertTrue(passLines.any { "NewTurn<Player1>" in it })
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
      assertTrue(endTurnLines.any { "NewTurn<Player2>" in it })
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
