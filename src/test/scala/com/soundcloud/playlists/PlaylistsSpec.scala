package com.soundcloud.playlists

import cats.effect.IO
import com.soundcloud.playlists.Playlists.{Playlist, PlaylistErrorMessage}
import com.soundcloud.playlists.database.{Migrations, PlaylistRepository}
import org.http4s._
import org.http4s.implicits._
import munit.CatsEffectSuite
import org.http4s
import io.circe.syntax._
import org.http4s.circe._

import java.util.UUID

class PlaylistsSpec extends CatsEffectSuite {

  test("When request to create playlists") {
    val playlistTitle=s"Playlist ${UUID.randomUUID()}"
    val createdPlaylistResponse=createPlaylists(Playlist(title=playlistTitle,description="my first playlist"))
    val createdPlaylist= createdPlaylistResponse.as[Playlist]
    assert(createdPlaylistResponse.status == Status.Created,s"Expected ${Status.Created} but received ${createdPlaylistResponse.status}")
    assertIO(createdPlaylist.map(_.title), "new")
    assertIO(createdPlaylist.map(_.description), playlistTitle)
    assertIOBoolean(createdPlaylist.map(_.id.isDefined),"playlist id is not created yet.")
  }


  test("When request for playlist by invalid id") {
    val id= "invalid id"
    val getPlaylistResponse = getPlaylistById(id)
    val errorMessage= getPlaylistResponse.as[PlaylistErrorMessage]
    assert(getPlaylistResponse.status == Status.BadRequest,s"Expected ${Status.BadRequest} but received ${getPlaylistResponse.status}")
    assertIO(errorMessage.map(_.message),s"requested ${id} is invalid")

  }

  test("When request for playlist by non existing id") {
    val id=UUID.randomUUID().toString
    val getPlaylistResponse = getPlaylistById(id)
    val errorMessage= getPlaylistResponse.as[PlaylistErrorMessage]
    assert(getPlaylistResponse.status == Status.NotFound,s"Expected ${Status.NotFound} but received ${getPlaylistResponse.status}")
    assertIO(errorMessage.map(_.message),s"requested ${id} does not exist")
  }

  test("When requests for playlist by existing and valid id") {
    val playlistTitle=s"Playlist ${UUID.randomUUID()}"

    val createdPlaylist= createPlaylists(Playlist(title=playlistTitle,description="my first playlist")).as[Playlist].unsafeRunSync()
    val playlistID = createdPlaylist.id.getOrElse(fail("id not created"))
    val getPlaylistResponse = getPlaylistById(playlistID.toString)
    val retrievedPlaylist= getPlaylistResponse.as[Playlist]
    assert(getPlaylistResponse.status == Status.Ok,s"Expected ${Status.Ok} but received ${getPlaylistResponse.status}")
    assertIO(retrievedPlaylist.map(_.title), playlistTitle)
    assertIO(retrievedPlaylist.map(_.description), "my first playlist")
    assertIO(retrievedPlaylist.map(_.id.get),playlistID)
  }

  var server : http4s.HttpApp[IO]={
    Migrations.migrate[IO]().compile.drain.unsafeRunSync()
    PlaylistsRoutes.playlistRoutes[IO](new PlaylistRepository[IO]).orNotFound
  }

  private[this] def getPlaylistById(id :String): Response[IO] = {
    val getPlaylist = Request[IO](Method.GET, uri"/playlists"/id)
    this.server.run(getPlaylist).unsafeRunSync()
  }

  private[this] def createPlaylists(playlist: Playlist): Response[IO] = {
    val postPlaylist = Request[IO](Method.POST, uri"/playlists").withEntity(playlist.asJson)
    this.server.run(postPlaylist).unsafeRunSync()
  }
}