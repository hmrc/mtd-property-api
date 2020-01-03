/*
 * Copyright 2020 HM Revenue & Customs
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

package v2.models.errors

import play.api.libs.json.Json
import support.UnitSpec

class ErrorWrapperSpec extends UnitSpec {

  val correlationId = "X-123"

  "Rendering a error response with one error" should {
    val error = ErrorWrapper(Some(correlationId), Error("CODE", "Message"), None)

    val json = Json.parse(
      """
        |{
        |   "code": "CODE",
        |   "message": "Message"
        |}
      """.stripMargin
    )

    "generate the correct JSON" in {
      Json.toJson(error) shouldBe json
    }
  }

  "Rendering a error response with one error and an empty sequence of errors" should {
    val error = ErrorWrapper(Some(correlationId), Error("CODE", "Message"), Some(Seq.empty))

    val json = Json.parse(
      """
        |{
        |   "code": "CODE",
        |   "message": "Message"
        |}
      """.stripMargin
    )

    "generate the correct JSON" in {
      Json.toJson(error) shouldBe json
    }
  }

  "Rendering a error response with two errors" should {
    val error = ErrorWrapper(Some(correlationId),
      Error("CODE", "Message"),
      Some(
        Seq(
          Error("CODE2", "Message 2"),
          Error("CODE3", "Message 3")
        )
      )
    )

    val json = Json.parse(
      """
        |{
        |   "code": "CODE",
        |   "message": "Message",
        |   "errors": [
        |       {
        |         "code": "CODE2",
        |         "message": "Message 2"
        |       },
        |       {
        |         "code": "CODE3",
        |         "message": "Message 3"
        |       }
        |   ]
        |}
      """.stripMargin
    )

    "generate the correct JSON" in {
      Json.toJson(error) shouldBe json
    }
  }

}
