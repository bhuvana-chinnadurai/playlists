package com.soundcloud.playlists.database

import cats.effect.kernel.Sync
import com.soundcloud.playlists.Playlists
import com.soundcloud.playlists.Playlists._

import java.util.UUID

class PlaylistRepository[F[_]: Sync] extends Playlists[F] {

  import ctx._

  val playlists = quote {
    querySchema[Playlist]("playlists")
  }

  override def create(
      playlist: Playlist
  ): F[Either[Playlists.PlaylistErrorMessage, Playlist]] = {

    Sync[F].delay {
      try {
        ctx
          .run(
            playlists
              .insert(
                _.title -> lift(playlist.title),
                _.description -> lift(playlist.description)
              )
              .returning(_.id)
          )
          .map { uuid =>
            Right(playlist.copy(id = Some(uuid)))
          }
          .getOrElse(
            Left(
              PlaylistErrorMessage(
                "error while creating playlist",
                ErrPlaylistDBServer
              )
            )
          )
      } catch {
        case e: Exception =>
          handleDBException(e, "error while creating playlists")
      }
    }
  }
  def fromUUID(uuid: String): Option[UUID] = {
    try {
      Option(UUID.fromString(uuid))
    } catch {
      case _: IllegalArgumentException => None
    }
  }

  override def getById(
      id: String
  ): F[Either[Playlists.PlaylistErrorMessage, Playlist]] = {
    fromUUID(id)
      .map { id =>
        Sync[F].delay {
          ctx.run(playlists.filter(_.id.contains(lift(id)))) match {
            case Seq(playlist) => Right(playlist)
            case Seq() =>
              Left(
                PlaylistErrorMessage(
                  s"error while retrieving playlist for $id",
                  ErrPlaylistNotFound
                )
              )
            case _ =>
              Left(
                PlaylistErrorMessage(
                  s"error while retrieving playlist for $id",
                  ErrPlaylistDBServer
                )
              )
          }
        }
      }
      .getOrElse(
        Sync[F].pure(
          Left(
            PlaylistErrorMessage(
              s"error while retrieving playlist for $id",
              ErrPlaylistIdInvalid
            )
          )
        )
      )
  }

  override def update(
      id: String,
      playlist: Playlist
  ): F[Either[PlaylistErrorMessage, Playlist]] = {
    fromUUID(id)
      .map { uuid =>
        Sync[F].delay {
          try {
            ctx.run(
              playlists
                .filter(_.id.contains(lift(uuid)))
                .update(
                  _.title -> lift(playlist.title),
                  _.description -> lift(playlist.description)
                )
            ) match {
              case x if x > 0 =>
                Right(
                  Playlist(
                    id = Some(uuid),
                    title = playlist.title,
                    description = playlist.description
                  )
                )
              case _ =>
                Left(
                  PlaylistErrorMessage(
                    s"error while updating playlist for $id",
                    ErrPlaylistNotFound
                  )
                )
            }
          } catch {
            case e: Exception =>
              handleDBException(e, s"error while updating playlists for $id")
          }
        }
      }
      .getOrElse(
        Sync[F].pure(
          Left(
            PlaylistErrorMessage(
              s"requested ${id} is invalid",
              ErrPlaylistIdInvalid
            )
          )
        )
      )
  }

  def handleDBException(exception: Exception, message: String) = {
    if (
      exception.getMessage.contains(
        "ERROR: duplicate key value violates unique constraint"
      )
    ) {
      Left(
        PlaylistErrorMessage(
          message,
          ErrPlaylistTitleDuplicate
        )
      )
    } else {
      Left(
        PlaylistErrorMessage(
          message,
          ErrPlaylistDBServer
        )
      )
    }
  }
  override def delete(id: String): F[Either[PlaylistErrorMessage, Unit]] = {
    fromUUID(id)
      .map { id =>
        Sync[F].delay {
          ctx.run(playlists.filter(_.id.contains(lift(id))).delete) match {
            case x if x > 0 => Right(())
            case _ =>
              Left(
                PlaylistErrorMessage(
                  s"error while deleting playlist for ${id}",
                  ErrPlaylistDBServer
                )
              )
          }
        }
      }
      .getOrElse(
        Sync[F].pure(
          Left(
            PlaylistErrorMessage(
              s"requested ${id} is invalid",
              ErrPlaylistIdInvalid
            )
          )
        )
      )
  }

}
