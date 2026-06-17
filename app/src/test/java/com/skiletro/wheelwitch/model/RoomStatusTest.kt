package com.skiletro.wheelwitch.model

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class RoomStatusTest {

    private val fullRoomJson = """
        {
          "rooms": [
            {
              "id": "abc123",
              "type": "mkw",
              "host": "Player1",
              "players": [
                {
                  "name": "Player1",
                  "friendCode": "1234-5678-9012",
                  "vr": 7200,
                  "br": 3500,
                  "isOpenHost": true,
                  "mii": { "data": "base64data", "name": "Player1Mii" }
                },
                {
                  "name": "Player2",
                  "friendCode": "9876-5432-1098",
                  "vr": 5400,
                  "br": 2800,
                  "isOpenHost": false,
                  "mii": null
                }
              ],
              "averageVR": 6300,
              "race": { "trackName": "Luigi Circuit" },
              "roomType": "Friend",
              "isPublic": false,
              "isJoinable": true,
              "isSuspended": false
            }
          ]
        }
    """.trimIndent()

    @Test
    fun `parse single full room`() {
        val rooms = parseRooms(fullRoomJson)
        assertThat(rooms).hasSize(1)

        val room = rooms[0]
        assertThat(room.id).isEqualTo("abc123")
        assertThat(room.type).isEqualTo("mkw")
        assertThat(room.host).isEqualTo("Player1")
        assertThat(room.averageVR).isEqualTo(6300)
        assertThat(room.trackName).isEqualTo("Luigi Circuit")
        assertThat(room.roomType).isEqualTo("Friend")
        assertThat(room.isPublic).isFalse()
        assertThat(room.isJoinable).isTrue()
        assertThat(room.isSuspended).isFalse()
    }

    @Test
    fun `parse players within a room`() {
        val rooms = parseRooms(fullRoomJson)
        val players = rooms[0].players

        assertThat(players).hasSize(2)

        assertThat(players[0].name).isEqualTo("Player1")
        assertThat(players[0].friendCode).isEqualTo("1234-5678-9012")
        assertThat(players[0].vr).isEqualTo(7200)
        assertThat(players[0].br).isEqualTo(3500)
        assertThat(players[0].isOpenHost).isTrue()
        assertThat(players[0].mii).isNotNull()
        assertThat(players[0].mii!!.data).isEqualTo("base64data")
        assertThat(players[0].mii!!.name).isEqualTo("Player1Mii")

        assertThat(players[1].name).isEqualTo("Player2")
        assertThat(players[1].friendCode).isEqualTo("9876-5432-1098")
        assertThat(players[1].vr).isEqualTo(5400)
        assertThat(players[1].br).isEqualTo(2800)
        assertThat(players[1].isOpenHost).isFalse()
        assertThat(players[1].mii).isNull()
    }

    @Test
    fun `parse room with null race`() {
        val json = """
            {
              "rooms": [
                {
                  "id": "r1",
                  "type": "mkw",
                  "host": "Host",
                  "players": [],
                  "race": null,
                  "roomType": "Public",
                  "isPublic": true,
                  "isJoinable": true,
                  "isSuspended": false
                }
              ]
            }
        """.trimIndent()
        val rooms = parseRooms(json)
        assertThat(rooms[0].trackName).isNull()
    }

    @Test
    fun `parse room with absent race field`() {
        val json = """
            {
              "rooms": [
                {
                  "id": "r1",
                  "type": "mkw",
                  "host": "Host",
                  "players": [],
                  "roomType": "Public",
                  "isPublic": true,
                  "isJoinable": true,
                  "isSuspended": false
                }
              ]
            }
        """.trimIndent()
        val rooms = parseRooms(json)
        assertThat(rooms[0].trackName).isNull()
    }

    @Test
    fun `parse empty rooms array`() {
        val json = """{"rooms": []}""".trimIndent()
        val rooms = parseRooms(json)
        assertThat(rooms).isEmpty()
    }

    @Test
    fun `parse room with default values for missing optional ints`() {
        val json = """
            {
              "rooms": [
                {
                  "id": "r1",
                  "type": "mkw",
                  "host": "Host",
                  "players": [
                    {
                      "name": "PlayerOne",
                      "friendCode": "1111-2222-3333"
                    }
                  ],
                  "roomType": "Unknown",
                  "isPublic": false,
                  "isJoinable": false,
                  "isSuspended": true
                }
              ]
            }
        """.trimIndent()
        val rooms = parseRooms(json)
        val player = rooms[0].players[0]
        assertThat(player.vr).isEqualTo(0)
        assertThat(player.br).isEqualTo(0)
        assertThat(player.isOpenHost).isFalse()
        assertThat(player.mii).isNull()
        assertThat(rooms[0].averageVR).isEqualTo(0)
    }

    @Test
    fun `parse room with empty track name`() {
        val json = """
            {
              "rooms": [
                {
                  "id": "r1",
                  "type": "mkw",
                  "host": "Host",
                  "players": [],
                  "race": { "trackName": "" },
                  "roomType": "Public",
                  "isPublic": true,
                  "isJoinable": true,
                  "isSuspended": false
                }
              ]
            }
        """.trimIndent()
        val rooms = parseRooms(json)
        assertThat(rooms[0].trackName).isNull()
    }
}
