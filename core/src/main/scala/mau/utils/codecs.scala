package mau.utils

import io.circe.*
import io.circe.generic.semiauto
import scala.collection.SeqMap

given [K: Encoder: Decoder, V: Encoder: Decoder]: Codec[Map[K, V]] =
  Codec.from[Seq[(K, V)]](Decoder.apply, Encoder.apply)
    .iemap(seq => Right(Map.from(seq)))(_.toSeq)

given [K: Encoder: Decoder, V: Encoder: Decoder]: Codec[SeqMap[K, V]] =
  Codec.from[Seq[(K, V)]](Decoder.apply, Encoder.apply)
    .iemap(seq => Right(SeqMap.from(seq)))(_.toSeq)

given [T: Codec]: Codec[Option[T]] = Codec.implied

given [T: Codec]: Codec[Seq[T]] = Codec.implied

given Codec[String] = Codec.implied
