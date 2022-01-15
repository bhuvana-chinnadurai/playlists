package com.soundcloud.playlists.database

import cats.effect.kernel.Sync
import com.soundcloud.playlists.Playlists
import com.soundcloud.playlists.Playlists.PlaylistError.{PlaylistDBServerError, PlaylistIdInvalid, PlaylistNotFound}
import com.soundcloud.playlists.Playlists.{Playlist, PlaylistErrorMessage}

import java.util.UUID

class PlaylistRepository[F[_] :Sync] extends Playlists[F] {

  import ctx._

  val playlists = quote {
    querySchema[Playlist]("playlists")
  }

  override def create(playlist: Playlist): F[Either[Playlists.PlaylistErrorMessage, Playlist]] = {

    Sync[F].delay {
      ctx.run(playlists.insert(_.title -> lift(playlist.title),_.description->lift(playlist.description)).returning(_.id)).map { uuid =>
        Right(playlist.copy(id = Some(uuid)))
      }.getOrElse(Left(PlaylistErrorMessage("error while creating playlists",PlaylistDBServerError)))
    }
  }
  def fromUUID(uuid: String): Option[UUID] = {
    try {
      Option(UUID.fromString(uuid))
    } catch {
      case _: IllegalArgumentException => None
    }
  }

  override def getById(id: String): F[Either[Playlists.PlaylistErrorMessage, Playlist]] = {
    fromUUID(id).map { id =>
      Sync[F].delay {
        ctx.run(playlists.filter(_.id.contains(lift(id)))) match {
          case Seq(playlist) => Right(playlist)
          case Seq() => Left(PlaylistErrorMessage(s"requested ${id} does not exist",PlaylistNotFound))
          case _ => Left(PlaylistErrorMessage(s"error while retriving playlist for ${id}",PlaylistDBServerError))
        }
      }
    }.getOrElse(Sync[F].pure(Left(PlaylistErrorMessage(s"requested ${id} is invalid",PlaylistIdInvalid))))
  }
}

