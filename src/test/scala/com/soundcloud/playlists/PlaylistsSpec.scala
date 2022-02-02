package com.soundcloud.playlists

import cats.effect.{IO, Sync}
import com.soundcloud.playlists.Playlists.{
  ErrPlaylistIdInvalid,
  ErrPlaylistNotFound,
  ErrPlaylistTitleDuplicate,
  Playlist,
  PlaylistErrorMessage
}
import com.soundcloud.playlists.database.PlaylistRepository
import io.circe.syntax._
import munit.CatsEffectSuite
import org.flywaydb.core.api.FlywayException
import org.flywaydb.core.api.output.MigrateResult
import org.http4s
import org.http4s._
import org.http4s.circe._
import org.http4s.implicits._

import java.util.UUID

object TestMigrations {
  def migrate[F[_]: Sync](): fs2.Stream[F, MigrateResult] = {
    import org.flywaydb.core.Flyway
    val flyway: Flyway = Flyway.configure
      .dataSource(
        "jdbc:postgresql://localhost:5432/playlists",
        "postgres",
        "password"
      )
      .load

    fs2.Stream.eval {
      try {
        Sync[F].delay(flyway.migrate())
      } catch {
        case e: FlywayException => Sync[F].raiseError(e)
      }
    }
  }
}

class PlaylistsSpec extends CatsEffectSuite {

  test("Create Playlists") {
    val playlistTitle = s"Playlist ${UUID.randomUUID()}"
    val createdPlaylistResponse = createPlaylist(
      Playlist(title = playlistTitle, description = "my first playlist")
    )
    val createdPlaylist = createdPlaylistResponse.as[Playlist]
    assert(
      createdPlaylistResponse.status == Status.Created,
      s"Expected ${Status.Created} but received ${createdPlaylistResponse.status}"
    )
    assertIO(createdPlaylist.map(_.title), "new")
    assertIO(createdPlaylist.map(_.description), playlistTitle)
    assertIOBoolean(
      createdPlaylist.map(_.id.isDefined),
      "playlist id is not created yet."
    )
  }

  test("Create Playlists with invalid id") {
    val id = "invalid id"
    val getPlaylistResponse = getPlaylistById(id)
    val errorMessage = getPlaylistResponse.as[PlaylistErrorMessage]
    assert(
      getPlaylistResponse.status == Status.BadRequest,
      s"Expected ${Status.BadRequest} but received ${getPlaylistResponse.status}"
    )
    assertIO(
      errorMessage.map(_.message),
      s"error while retrieving playlist for $id"
    )
    assertIO(errorMessage.map(_.error), ErrPlaylistIdInvalid)
  }

  test("Get Playlists with non existing id") {
    val id = UUID.randomUUID().toString
    val getPlaylistResponse = getPlaylistById(id)
    val errorMessage = getPlaylistResponse.as[PlaylistErrorMessage]
    assert(
      getPlaylistResponse.status == Status.NotFound,
      s"Expected ${Status.NotFound} but received ${getPlaylistResponse.status}"
    )
    assertIO(
      errorMessage.map(_.message),
      s"error while retrieving playlist for $id"
    )
    assertIO(errorMessage.map(_.error), ErrPlaylistNotFound)
  }

  test("Get playlist with an existing and valid id") {
    val playlistTitle = s"Playlist ${UUID.randomUUID()}"

    val createdPlaylist = createPlaylist(
      Playlist(title = playlistTitle, description = "my first playlist")
    ).as[Playlist].unsafeRunSync()
    val playlistID = createdPlaylist.id.getOrElse(fail("id not created"))
    val getPlaylistResponse = getPlaylistById(playlistID.toString)
    val retrievedPlaylist = getPlaylistResponse.as[Playlist]
    assert(
      getPlaylistResponse.status == Status.Ok,
      s"Expected ${Status.Ok} but received ${getPlaylistResponse.status}"
    )
    assertIO(retrievedPlaylist.map(_.title), playlistTitle)
    assertIO(retrievedPlaylist.map(_.description), "my first playlist")
    assertIO(retrievedPlaylist.map(_.id.get), playlistID)
  }

  test("Updating playlist with the new title and description") {
    val playlistTitle = s"playlist-title-${UUID.randomUUID()}"

    val createdPlaylist = createPlaylist(
      Playlist(title = playlistTitle, description = "my first playlist")
    ).as[Playlist].unsafeRunSync()

    val playlistID = createdPlaylist.id.getOrElse(fail("id not created"))

    val tobeUpdated = Playlist(
      id = Some(playlistID),
      title = s"$playlistID-updated title",
      description = "updated description"
    )
    val updatedPlaylistResponse =
      updatePlaylist(playlistID.toString, tobeUpdated)
    val updatedPlaylist = updatedPlaylistResponse.as[Playlist]

    assert(
      updatedPlaylistResponse.status == Status.Ok,
      s"Expected ${Status.Ok} but received ${updatedPlaylistResponse.status}"
    )
    assertIO(updatedPlaylist.map(_.title), "updated title")
    assertIO(updatedPlaylist.map(_.description), "updated description")
    assertIO(updatedPlaylist.map(_.id.get), playlistID)
  }

  test("Updating playlist with the already existing title") {
    val playlistTitle = s"playlist-title-${UUID.randomUUID()}"

    //Create the first playlist;
    createPlaylist(
      Playlist(title = playlistTitle, description = "my first playlist")
    ).as[Playlist].unsafeRunSync()

    //Create the second playlist;
    val createdPlaylist = createPlaylist(
      Playlist(
        title = s"playlist-title-${UUID.randomUUID()}",
        description = "my second playlist"
      )
    )

    val playlist2 = createdPlaylist.as[Playlist].unsafeRunSync()

    val playlist2Id = playlist2.id.getOrElse(fail("id not created"))

    //Updating the second playlist to an existing title.
    val tobeUpdated = Playlist(
      id = Some(playlist2Id),
      title = playlistTitle,
      description = "updated description"
    )
    val updatedPlaylistResponse =
      updatePlaylist(playlist2Id.toString, tobeUpdated)

    val updatedPlaylistErrorMessage =
      updatedPlaylistResponse.as[PlaylistErrorMessage]

    assert(
      updatedPlaylistResponse.status == Status.BadRequest,
      s"Expected ${Status.BadRequest} but received ${updatedPlaylistResponse.status}"
    )
    assertIO(
      updatedPlaylistErrorMessage.map(_.error),
      ErrPlaylistTitleDuplicate
    )
    assertIO(
      updatedPlaylistErrorMessage.map(_.message),
      s"error while updating playlists for $playlist2Id"
    )
  }

  test("Deleting playlist with the playlist id") {
    val playlistTitle = s"playlist-title-${UUID.randomUUID()}"

    //Create a playlist1;
    val playlist = createPlaylist(
      Playlist(title = playlistTitle, description = "my first playlist")
    ).as[Playlist].unsafeRunSync()

    val playlist1Id = playlist.id.getOrElse(fail("id not created"))

    val deletedPlaylistResponse = deletePlaylist(playlist1Id.toString)
    assert(
      deletedPlaylistResponse.status == Status.Ok,
      s"Expected ${Status.InternalServerError} but received ${deletedPlaylistResponse.status}"
    )
  }

  var server: http4s.HttpApp[IO] = {
    TestMigrations.migrate[IO]().compile.drain.unsafeRunSync()
    PlaylistsRoutes.playlistRoutes[IO](new PlaylistRepository[IO]).orNotFound
  }

  private[this] def getPlaylistById(id: String): Response[IO] = {
    val getPlaylist = Request[IO](Method.GET, uri"/playlists" / id)
    this.server.run(getPlaylist).unsafeRunSync()
  }

  private[this] def createPlaylist(playlist: Playlist): Response[IO] = {
    val postPlaylist =
      Request[IO](Method.POST, uri"/playlists").withEntity(playlist.asJson)
    this.server.run(postPlaylist).unsafeRunSync()
  }

  private[this] def updatePlaylist(
      id: String,
      playlist: Playlist
  ): Response[IO] = {
    val putPlaylist =
      Request[IO](Method.PUT, uri"/playlists" / id).withEntity(playlist.asJson)
    this.server.run(putPlaylist).unsafeRunSync()
  }
  private[this] def deletePlaylist(id: String): Response[IO] = {
    val deletePlaylist = Request[IO](Method.DELETE, uri"/playlists" / id)
    this.server.run(deletePlaylist).unsafeRunSync()
  }
}
