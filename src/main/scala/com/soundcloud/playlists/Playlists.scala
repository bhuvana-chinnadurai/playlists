package com.soundcloud.playlists

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import org.http4s.{EntityDecoder, EntityEncoder}
import org.http4s.circe._
import cats.effect.Concurrent
import com.soundcloud.playlists.Playlists.{Playlist, PlaylistErrorMessage}

import java.util.UUID

trait Playlists[F[_]] {
  def create(
      playlist: Playlists.Playlist
  ): F[Either[PlaylistErrorMessage, Playlist]]
  def getById(playlistID: String): F[Either[PlaylistErrorMessage, Playlist]]
  def update(
      playlistID: String,
      playlist: Playlists.Playlist
  ): F[Either[PlaylistErrorMessage, Playlist]]
  def delete(playlistID: String): F[Either[PlaylistErrorMessage, Unit]]
}

object Playlists {
  val ErrPlaylistTitleDuplicate = "playlist with title already exists"
  val ErrPlaylistIdInvalid = "playlist id is invalid"
  val ErrPlaylistNotFound = "playlist not found"
  val ErrPlaylistDBServer = "server error while interacting to playlists"

  case class Playlist(
      title: String,
      description: String,
      id: Option[UUID] = None
  )
  object Playlist {
    implicit val playlistDecoder: Decoder[Playlist] = deriveDecoder[Playlist]
    implicit def playlistEntityDecoder[F[_]: Concurrent]
        : EntityDecoder[F, Playlist] = jsonOf
    implicit val playlistEncoder: Encoder[Playlist] = deriveEncoder[Playlist]
    implicit def playlistEntityEncoder[F[_]]: EntityEncoder[F, Playlist] =
      jsonEncoderOf
  }

  case class PlaylistErrorMessage(message: String, error: String)

  object PlaylistErrorMessage {
    implicit val playlistErrorMessageDecoder: Decoder[PlaylistErrorMessage] =
      deriveDecoder[PlaylistErrorMessage]
    implicit def playlistErrorMessageEntityDecoder[F[_]: Concurrent]
        : EntityDecoder[F, PlaylistErrorMessage] = jsonOf
    implicit val playlistErrorMessageEncoder: Encoder[PlaylistErrorMessage] =
      deriveEncoder[PlaylistErrorMessage]
    implicit def playlistErrorMessageEntityEncoder[F[_]]
        : EntityEncoder[F, PlaylistErrorMessage] = jsonEncoderOf
  }
}
