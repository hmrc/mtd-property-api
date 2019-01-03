/*
 * Copyright 2019 HM Revenue & Customs
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

package v2.models

import play.api.libs.json.{JsString, Json}
import support.UnitSpec

import scala.util.{Failure, Success, Try}

class ObligationStatusSpec extends UnitSpec {

  "Reading an obligation status from JSON" when {
    "the JSON represents a valid fulfilled status" should {
      "create a fulfilled obligation status" in {
        val json = JsString("F")
        val status = json.as[ObligationStatus]
        status shouldBe FulfilledObligation
      }
    }

    "the JSON represents a valid open status" should {
      "create an open obligation status" in {
        val json = JsString("O")
        val status = json.as[ObligationStatus]
        status shouldBe OpenObligation
      }
    }

    "the JSON represents a invalid open status" should {
      "fail to parse" in {
        val json = JsString("X")
        Try(json.validate[ObligationStatus]) match {
          case Success(_) => fail("A JsError was expected")
          case Failure(e) => e.getMessage shouldBe "Invalid status supplied for obligation status"
        }
      }
    }

  }

  "Writing JSON to represent the obligation status" when {
    "the obligation is open" should {
      "generate the correct JSON" in {
        val json = Json.toJson(OpenObligation)
        json shouldBe JsString("Open")
      }
    }

    "the obligation is fulfilled" should {
      "generate the correct JSON" in {
        val json = Json.toJson(FulfilledObligation)
        json shouldBe JsString("Fulfilled")
      }
    }
  }
}
