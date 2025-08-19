package mau.exec

import mau.engine.ExternalRule

final case class WithUntrustedRules[A](value: A, untrustedRules: Set[ExternalRule]):
  def map[B](f: A => B): WithUntrustedRules[B] = copy(value = f(value))

  def flatMap[B](f: A => WithUntrustedRules[B]) = f(value).addUntrustedRules(untrustedRules)

  private def addUntrustedRules(others: Set[ExternalRule]): WithUntrustedRules[A] = copy(untrustedRules = untrustedRules ++ others)

object WithUntrustedRules:
  def apply[A](value: A): WithUntrustedRules[A] = WithUntrustedRules(value, Set.empty)
