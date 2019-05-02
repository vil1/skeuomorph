/*
 * Copyright 2018-2019 47 Degrees, LLC. <http://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package higherkindness.skeuomorph.openapi

import io.circe.Json
import qq.droste._
import cats.implicits._
import cats.Eq

import qq.droste.data.Fix
import qq.droste.macros.deriveTraverse

@deriveTraverse sealed trait JsonSchemaF[A]
object JsonSchemaF {
  @deriveTraverse final case class Property[A](name: String, tpe: A)

  final case class IntegerF[A]()  extends JsonSchemaF[A]
  final case class LongF[A]()     extends JsonSchemaF[A]
  final case class FloatF[A]()    extends JsonSchemaF[A]
  final case class DoubleF[A]()   extends JsonSchemaF[A]
  final case class StringF[A]()   extends JsonSchemaF[A]
  final case class ByteF[A]()     extends JsonSchemaF[A]
  final case class BinaryF[A]()   extends JsonSchemaF[A]
  final case class BooleanF[A]()  extends JsonSchemaF[A]
  final case class DateF[A]()     extends JsonSchemaF[A]
  final case class DateTimeF[A]() extends JsonSchemaF[A]
  final case class PasswordF[A]() extends JsonSchemaF[A]
  final case class ObjectF[A](name: String, properties: List[Property[A]], required: List[String])
      extends JsonSchemaF[A]
  final case class ArrayF[A](values: A)       extends JsonSchemaF[A]
  final case class EnumF[A](cases: List[A])   extends JsonSchemaF[A]
  final case class ReferenceF[A](ref: String) extends JsonSchemaF[A]

  def integer[T](): JsonSchemaF[T]  = IntegerF()
  def long[T](): JsonSchemaF[T]     = LongF()
  def float[T](): JsonSchemaF[T]    = FloatF()
  def double[T](): JsonSchemaF[T]   = DoubleF()
  def string[T](): JsonSchemaF[T]   = StringF()
  def byte[T](): JsonSchemaF[T]     = ByteF()
  def binary[T](): JsonSchemaF[T]   = BinaryF()
  def boolean[T](): JsonSchemaF[T]  = BooleanF()
  def date[T](): JsonSchemaF[T]     = DateF()
  def dateTime[T](): JsonSchemaF[T] = DateTimeF()
  def password[T](): JsonSchemaF[T] = PasswordF()
  def `object`[T](name: String, properties: List[Property[T]], required: List[String]): JsonSchemaF[T] =
    ObjectF(name, properties, required)
  def array[T](values: T): JsonSchemaF[T]       = ArrayF(values)
  def enum[T](cases: List[T]): JsonSchemaF[T]   = EnumF(cases)
  def reference[T](ref: String): JsonSchemaF[T] = ReferenceF[T](ref)

  def render: Algebra[JsonSchemaF, Json] = Algebra {
    case IntegerF()  => Json.fromString("integer")
    case LongF()     => Json.fromString("long")
    case FloatF()    => Json.fromString("float")
    case DoubleF()   => Json.fromString("double")
    case StringF()   => Json.fromString("string")
    case ByteF()     => Json.fromString("byte")
    case BinaryF()   => Json.fromString("binary")
    case BooleanF()  => Json.fromString("boolean")
    case DateF()     => Json.fromString("date")
    case DateTimeF() => Json.fromString("datetime")
    case PasswordF() => Json.fromString("password")
    case ObjectF(name, properties, required) =>
      Json.obj(
        name -> Json.obj(
          "type"       -> Json.fromString("object"),
          "properties" -> Json.obj(properties.map(prop => prop.name -> prop.tpe): _*),
          "required"   -> Json.fromValues(required.map(Json.fromString))
        )
      )
    case ArrayF(values) =>
      Json.obj(
        "type"  -> Json.fromString("array"),
        "items" -> Json.obj("type" -> values)
      )
    case EnumF(cases) =>
      Json.obj(
        "type" -> Json.fromString("string"),
        "enum" -> Json.fromValues(cases)
      )
    case ReferenceF(value) =>
      Json.obj(
        "$ref" -> Json.fromString(value)
      )

  }
  implicit def eqProperty[T: Eq]: Eq[Property[T]] = Eq.instance { (p1, p2) =>
    p1.name === p2.name && p1.tpe === p2.tpe
  }

  implicit def eqJsonSchemaF[T: Eq]: Eq[JsonSchemaF[T]] = Eq.instance {
    case (IntegerF(), IntegerF())                   => true
    case (LongF(), LongF())                         => true
    case (FloatF(), FloatF())                       => true
    case (DoubleF(), DoubleF())                     => true
    case (StringF(), StringF())                     => true
    case (ByteF(), ByteF())                         => true
    case (BinaryF(), BinaryF())                     => true
    case (BooleanF(), BooleanF())                   => true
    case (DateF(), DateF())                         => true
    case (DateTimeF(), DateTimeF())                 => true
    case (PasswordF(), PasswordF())                 => true
    case (ObjectF(n1, p1, r1), ObjectF(n2, p2, r2)) => n1 === n2 && p1 === p2 && r1 === r2
    case (ArrayF(v1), ArrayF(v2))                   => v1 === v2
    case (EnumF(c1), EnumF(c2))                     => c1 === c2
    case (ReferenceF(r1), ReferenceF(r2))           => r1 === r2
    case _                                          => false
  }

  def addressSchema: Fix[JsonSchemaF] =
    Fix(
      ObjectF(
        "address",
        List(
          Property("street_address", Fix(StringF())),
          Property("city", Fix(StringF())),
          Property("state", Fix(StringF()))
        ),
        List(
          "street_address",
          "city",
          "state"
        )
      ))
}
