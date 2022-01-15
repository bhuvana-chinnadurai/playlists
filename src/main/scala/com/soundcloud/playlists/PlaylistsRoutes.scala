package com.soundcloud.playlists

import cats.effect.Concurrent
import cats.implicits._
import com.soundcloud.playlists.Playlists.PlaylistError.{PlaylistDBServerError, PlaylistIdInvalid, PlaylistNotFound}
import com.soundcloud.playlists.Playlists.{Playlist, PlaylistErrorMessage}
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

object PlaylistsRoutes {

  def playlistRoutes[F[_]: Concurrent](playlists: Playlists[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._
    HttpRoutes.of[F] {
      case request @POST -> Root / "playlists" =>
        for {
          playlist <- request.as[Playlist]
          createdPlaylistE <- playlists.create(playlist)
          response<-createdPlaylistE match {
          case Left(message) => BadRequest(message)
          case Right(playlist) => Created(playlist)
        }
        } yield response

      case _ @GET->Root / "playlists" /playlistId =>
        for{
          playlist<-playlists.getById(playlistId)
          response<- playlist match {
          case Right(playlist) => Ok(playlist)
          case Left(errorResponse @PlaylistErrorMessage(_,_))  =>
            errorResponse.error match {
            case PlaylistNotFound => NotFound(errorResponse)
            case PlaylistIdInvalid=> BadRequest(errorResponse)
            case PlaylistDBServerError =>InternalServerError(errorResponse)
          }
          }
        } yield response
    }
  }
}