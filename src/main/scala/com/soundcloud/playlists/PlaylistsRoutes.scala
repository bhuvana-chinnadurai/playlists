package com.soundcloud.playlists

import cats.effect.Concurrent
import cats.implicits._
import com.soundcloud.playlists.Playlists.{
  ErrPlaylistDBServer,
  ErrPlaylistIdInvalid,
  ErrPlaylistNotFound,
  ErrPlaylistTitleDuplicate,
  PlaylistErrorMessage,
  Playlist
}
import org.http4s.dsl.Http4sDsl
import org.http4s.{HttpRoutes, Response}

object PlaylistsRoutes {

  def playlistRoutes[F[_]: Concurrent](
      playlists: Playlists[F]
  ): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    def HandleError(errorResponse: PlaylistErrorMessage): F[Response[F]] = {
      errorResponse.error match {
        case ErrPlaylistNotFound       => NotFound(errorResponse)
        case ErrPlaylistIdInvalid      => BadRequest(errorResponse)
        case ErrPlaylistTitleDuplicate => BadRequest(errorResponse)
        case ErrPlaylistDBServer       => InternalServerError(errorResponse)
      }
    }
    HttpRoutes.of[F] {
      case request @ POST -> Root / "playlists" =>
        for {
          playlist <- request.as[Playlist]
          createdPlaylistE <- playlists.create(playlist)
          response <- createdPlaylistE match {
            case Left(message)   => HandleError(message)
            case Right(playlist) => Created(playlist)
          }
        } yield response

      case _ @GET -> Root / "playlists" / playlistId =>
        for {
          playlist <- playlists.getById(playlistId)
          response <- playlist match {
            case Right(playlist) => Ok(playlist)
            case Left(errorResponse) =>
              HandleError(errorResponse)
          }
        } yield response

      case request @ PUT -> Root / "playlists" / playlistId =>
        for {
          playlist <- request.as[Playlist]
          updatedPlaylistE <- playlists.update(playlistId, playlist)
          response <- updatedPlaylistE match {
            case Right(playlist) => Ok(playlist)
            case Left(errorResponse) =>
              HandleError(errorResponse)
          }
        } yield response

      case _ @DELETE -> Root / "playlists" / playlistId =>
        for {
          deletedPlaylistE <- playlists.delete(playlistId)
          response <- deletedPlaylistE match {
            case Right(()) => Ok()
            case Left(errorResponse) =>
              HandleError(errorResponse)
          }
        } yield response
    }
  }
}
