package mau.edition

import io.circe.Codec

final case class Completion(
  label: String,
  detail: String,
  tpe: String,
  order: Option[Int],
  insert: Insert,
  additionalInserts: Seq[Insert],
  symbol: Option[String]
) derives Codec

case class Insert(text: String, startLine: Int, startChar: Int, endLine: Int, endChar: Int) derives Codec
