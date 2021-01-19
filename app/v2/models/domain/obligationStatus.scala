/*
 * Copyright 2021 HM Revenue & Customs
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

package v2.models.domain

import play.api.libs.json._

sealed trait ObligationStatus

object ObligationStatus {
  def apply(status: String): ObligationStatus = status match {
    case "O" => OpenObligation
    case "F" => FulfilledObligation
    case _ => throw new IllegalArgumentException("Invalid status supplied for obligation status")
  }

  implicit val reads: Reads[ObligationStatus] = new Reads[ObligationStatus] {
    override def reads(json: JsValue): JsResult[ObligationStatus] = {
      json.validate[String] match {
        case JsSuccess(value, path) => JsSuccess(ObligationStatus(value), path)
        case failure@JsError(_) => failure
      }
    }
  }

  implicit val writes: Writes[ObligationStatus] = new Writes[ObligationStatus] {
    def writes(status: ObligationStatus): JsValue = status match {
      case FulfilledObligation => Json.toJson("Fulfilled")
      case OpenObligation => Json.toJson("Open")
    }
  }
}

case object OpenObligation extends ObligationStatus

case object FulfilledObligation extends ObligationStatus
