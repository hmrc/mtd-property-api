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

package v2.fixtures

import play.api.libs.json.{JsValue, Json}

object DESErrorsFixture {

  private def desError(code:String, reason: String = "Do not care") = {
    Json.parse(
      s"""
         |{
         |  "code": "$code",
         |  "reason": "$reason"
         |}
         |""".stripMargin)
  }

  //Des Error Responses
  val serverErrorJson: JsValue = desError("SERVER_ERROR")
  val conflictErrorJson: JsValue = desError("CONFLICT")
  val notFoundErrorJson: JsValue = desError("NOT_FOUND")
  val earlySubmissionErrorJson: JsValue = desError("EARLY_SUBMISSION")

  val multipleErrorJson: JsValue = Json.parse(
    """
      |{
      |  "failures": [
      |    {
      |      "code": "INVALID_ACCOUNTINGPERIODSTARTDATE",
      |      "reason": "some reason"
      |    },
      |    {
      |      "code": "INVALID_ACCOUNTINGPERIODENDDATE",
      |      "reason": "some reason"
      |    }
      |  ]
      |}
    """.stripMargin)

  val bvrErrorJson: JsValue =
    Json.parse(
      """
        |{
        |  "bvrfailureResponseElement": {
        |    "validationRuleFailures": [
        |      {
        |        "id": "C55317",
        |        "type": "err",
        |        "text": "some text"
        |      },
        |      {
        |        "id": "C55318",
        |        "type": "err",
        |        "text": "some text"
        |      }
        |    ]
        |  }
        |}
      """.stripMargin)
}
