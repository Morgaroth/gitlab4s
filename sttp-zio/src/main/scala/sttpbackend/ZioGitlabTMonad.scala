package io.gitlab.mateuszjaje.gitlabclient
package sttpbackend

import apisv2.GitlabApiT

import zio.{UIO, ZIO}

object ZioGitlabTMonad {

  implicit val theMonad: GitlabApiT[UIO] = new GitlabApiT[UIO] {
    override def subFlatMap[A, B](fa: UIO[Either[GitlabError, A]])(f: A => Either[GitlabError, B]): UIO[Either[GitlabError, B]] =
      fa.flatMap(x => ZIO.succeed(x.flatMap(f)))

    override def pure[A](x: A): UIO[Either[GitlabError, A]] = ZIO.right(x)

    override def flatMap[A, B](fa: UIO[Either[GitlabError, A]])(f: A => UIO[Either[GitlabError, B]]): UIO[Either[GitlabError, B]] = {
      fa.flatMap { (data: Either[GitlabError, A]) =>
        data
          .map(f)
          .fold(
            err => ZIO.left(err),
            identity,
          )
      }
    }

    //    override def tailRecM[A, B](a: A)(f: A => UIO[Either[GitlabError, Either[A, B]]]): UIO[Either[GitlabError, B]] = {
    override def tailRecM[A, B](a: A)(f: A => UIO[Either[GitlabError, Either[A, B]]]): UIO[Either[GitlabError, B]] = {
      flatMap(f(a)) {
        case Left(a)  => tailRecM(a)(f)
        case Right(b) => pure(b)
      }
    }

    override def sequence[A](x: Vector[UIO[Either[GitlabError, A]]]): UIO[Either[GitlabError, Vector[A]]] = {
      ZIO.foreach(x)(identity).map {
        _.foldLeft[Either[GitlabError, Vector[A]]](Right(Vector.empty[A])) {
          case (e @ Left(_), _)       => e
          case (Right(acc), Right(e)) => Right(acc :+ e)
          case (_, Left(e))           => Left(e)
        }
      }
    }

  }

}
