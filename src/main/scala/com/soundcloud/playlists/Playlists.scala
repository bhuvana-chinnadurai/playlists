package com.soundcloud.playlists

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import org.http4s.{EntityDecoder, EntityEncoder}
import org.http4s.circe._
import cats.effect.Concurrent
import com.soundcloud.playlists.Playlists.{Playlist, PlaylistErrorMessage}

import java.util.UUID

trait Playlists[F[_]]{
  def create(playlist: Playlists.Playlist) :F[Either[PlaylistErrorMessage,Playlist]]
  def getById(playlistID :String) :F[Either[PlaylistErrorMessage,Playlist]]
}

object Playlists {

//  def impl[F[_] :Concurrent] :Playlists[F] = new Playlists[F] {
//
//    val playlists= new ListBuffer[Playlist]
//
//    def create(playlist: Playlist):F[Either[PlaylistErrorMessage,Playlist]] ={
//        val createdPlaylist=playlist.copy(id= Option(UUID.randomUUID()))
//        playlists+=createdPlaylist
//        Concurrent[F].pure(Right(createdPlaylist))
//    }
//
//    private def convertoUUID(id :String):Option[UUID]= {
//      try {
//        Option(UUID.fromString(id))
//      }catch {
//        case (_ :IllegalArgumentException)=>None
//      }
//    }
//
//    def getById(id: String):F[Either[PlaylistErrorMessage,Playlist]] ={
//      convertoUUID(id) match {
//        case Some(id)=>
//          Concurrent[F].pure(
//            playlists.find(_.id.contains(id)) match {
//              case Some(playlist) => Right(playlist)
//              case None => Left(PlaylistErrorMessage(s"requested ${id} does not exist",PlaylistNotFound))
//            }
//          )
//        case None=> Concurrent[F].pure(Left(PlaylistErrorMessage(s"requested ${id} is invalid",PlaylistIdInvalid)))
//      }
//    }
//
//  }

  sealed abstract class PlaylistError extends Exception with Product with Serializable

  object PlaylistError {
    final case object PlaylistNotFound extends PlaylistError
    final case object PlaylistIdInvalid extends PlaylistError
    final case object PlaylistDBServerError extends PlaylistError

    implicit val playlistErrorDecoder :Decoder[PlaylistError] = deriveDecoder[PlaylistError]
    implicit def playlistErrorEntityDecoder[F[_] :Concurrent]: EntityDecoder[F, PlaylistError] = jsonOf
    implicit val playlistErrorEncoder :Encoder[PlaylistError] = deriveEncoder[PlaylistError]
    implicit def playlistErrorEntityEncoder[F[_]]: EntityEncoder[F,PlaylistError] = jsonEncoderOf

  }

  case class Playlist(title :String,description :String,id :Option[UUID]=None)
    case class PlaylistErrorMessage (message :String,error :PlaylistError)

  object Playlist{
    implicit val playlistDecoder :Decoder[Playlist] = deriveDecoder[Playlist]
    implicit def playlistEntityDecoder[F[_] :Concurrent]: EntityDecoder[F, Playlist] = jsonOf
    implicit val playlistEncoder :Encoder[Playlist] = deriveEncoder[Playlist]
    implicit def playlistEntityEncoder[F[_]]: EntityEncoder[F,Playlist] = jsonEncoderOf
  }

  object PlaylistErrorMessage{
    implicit val playlistErrorMessageDecoder :Decoder[PlaylistErrorMessage] = deriveDecoder[PlaylistErrorMessage]
    implicit def playlistErrorMessageEntityDecoder[F[_] :Concurrent]: EntityDecoder[F, PlaylistErrorMessage] = jsonOf
    implicit val playlistErrorMessageEncoder :Encoder[PlaylistErrorMessage] = deriveEncoder[PlaylistErrorMessage]
    implicit def playlistErrorMessageEntityEncoder[F[_]]: EntityEncoder[F,PlaylistErrorMessage] = jsonEncoderOf
  }
}
