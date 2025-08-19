package mau.exec

import io.circe.Codec
import io.circe.generic.semiauto

import scala.util.control.NonFatal

enum ExecResult[+A]:
  case Success(value: A) extends ExecResult[A]
  case Error(error: String) extends ExecResult[Nothing]
  case Fatal(error: String) extends ExecResult[Nothing]

  def map[B](f: A => B): ExecResult[B] = this match
    case Success(value) => Success(f(value))
    case failed: (Error | Fatal) => failed

  def flatMap[B](f: A => ExecResult[B]): ExecResult[B] = this match
    case Success(value) => f(value)
    case failed: (Error | Fatal) => failed

  def onFailure(f: () => Unit): ExecResult[A] =
    this match
      case failed: (Error | Fatal) => f()
      case _ => ()
    this

object ExecResult:
  def tryOrError[A](f: => A): ExecResult[A] =
    try ExecResult.Success(f)
    catch
      case NonFatal(cause) => ExecResult.Error(cause.toString)
      case cause: Throwable => ExecResult.Fatal(cause.toString)
    
  def tryOrFatal[A](f: => A): ExecResult[A] =
    try ExecResult.Success(f)
    catch case cause: Throwable => ExecResult.Fatal(cause.toString)

  given [T: Codec]: Codec[ExecResult[T]] = semiauto.deriveCodec[ExecResult[T]]
