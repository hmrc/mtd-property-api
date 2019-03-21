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

import play.api.libs.json.Json
import support.UnitSpec
import v2.models.domain.ObligationDetails

class ObligationDetailsSpec extends UnitSpec {

  val obligationDetailsWithSourceJson: String =
    """
      |{
      |  "obligations": [
      |    {
      |      "referenceType": "NINO",
      |      "referenceNumber": "AA123456A",
      |      "incomeSourceType": "ITSA",
      |      "obligationDetails": [
      |        {
      |          "status": "F",
      |          "inboundCorrespondenceFromDate": "2018-01-01",
      |          "inboundCorrespondenceToDate": "2018-01-01",
      |          "inboundCorrespondenceDateReceived": "2018-01-01",
      |          "inboundCorrespondenceDueDate": "2018-01-01",
      |          "periodKey": ""
      |        }
      |      ]
      |    }
      |  ]
      |}""".stripMargin

  val obligationDetailsWithoutSourceJson: String =
    """
      |{
      |  "obligations": [
      |    {
      |      "referenceType": "NINO",
      |      "referenceNumber": "AA123456A",
      |      "obligationDetails": [
      |        {
      |          "status": "F",
      |          "inboundCorrespondenceFromDate": "2018-01-01",
      |          "inboundCorrespondenceToDate": "2018-01-01",
      |          "inboundCorrespondenceDateReceived": "2018-01-01",
      |          "inboundCorrespondenceDueDate": "2018-01-01",
      |          "periodKey": ""
      |        }
      |      ]
      |    }
      |  ]
      |}""".stripMargin

  val obligationDetailsWithoutAnyObligationsJson: String =
    """
      |{
      |  "obligations": [
      |    {
      |      "referenceType": "NINO",
      |      "referenceNumber": "AA123456A",
      |      "obligationDetails": []
      |    }
      |  ]
      |}""".stripMargin

  val obligationDetailsWithMultipleObligationsJson: String =
    """
      |{
      |  "obligations": [
      |    {
      |      "referenceType": "NINO",
      |      "referenceNumber": "AA123456A",
      |      "obligationDetails": [
      |        {
      |          "status": "F",
      |          "inboundCorrespondenceFromDate": "2018-01-01",
      |          "inboundCorrespondenceToDate": "2018-01-01",
      |          "inboundCorrespondenceDateReceived": "2018-01-01",
      |          "inboundCorrespondenceDueDate": "2018-01-01",
      |          "periodKey": ""
      |        },
      |        {
      |          "status": "O",
      |          "inboundCorrespondenceFromDate": "2018-01-01",
      |          "inboundCorrespondenceToDate": "2018-01-01",
      |          "inboundCorrespondenceDueDate": "2018-01-01",
      |          "periodKey": ""
      |        }
      |      ]
      |    }
      |  ]
      |}""".stripMargin

  "Reading obligation details JSON" when {
    "an income source type is not defined" should {
      val json = Json.parse(obligationDetailsWithoutSourceJson)
      val details = (json \ "obligations").as[Seq[ObligationDetails]]

      "create a single obligation details" in {
        details.size shouldBe 1
      }

      "create obligation details with a source defined" in {
        details.head.incomeSourceType shouldBe None
      }
    }

    "an income source type is defined" should {
      val json = Json.parse(obligationDetailsWithSourceJson)
      val details = (json \ "obligations").as[Seq[ObligationDetails]]

      "create obligation details without a source defined" in {
        details.head.incomeSourceType shouldBe Some("ITSA")
      }
    }

    "no obligations are defined" should {
      val json = Json.parse(obligationDetailsWithoutAnyObligationsJson)
      val details = (json \ "obligations").as[Seq[ObligationDetails]]

      "create obligation details without any obligations" in {
        details.head.obligations.size shouldBe 0
      }
    }

    "one obligation is defined" should {
      val json = Json.parse(obligationDetailsWithSourceJson)
      val details = (json \ "obligations").as[Seq[ObligationDetails]]

      "create obligation details without any obligations" in {
        details.head.obligations.size shouldBe 1
      }
    }

    "two obligation are defined" should {
      val json = Json.parse(obligationDetailsWithMultipleObligationsJson)
      val details = (json \ "obligations").as[Seq[ObligationDetails]]

      "create obligation details with two obligations" in {
        details.head.obligations.size shouldBe 2
      }
    }

  }

}
