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

package v2.models

import java.time.LocalDate

import org.scalatest.Assertion
import play.api.libs.json.{JsResultException, Json, JsonValidationError}
import support.UnitSpec
import v2.models.domain.{FulfilledObligation, Obligation, ObligationStatus, OpenObligation}

import scala.util.{Failure, Success, Try}

class ObligationSpec extends UnitSpec {

  val start: LocalDate = LocalDate.parse("2018-01-01")
  val end: LocalDate = LocalDate.parse("2018-01-01")
  val due: LocalDate = LocalDate.parse("2018-01-01")
  val statusFulfilled: FulfilledObligation.type = FulfilledObligation
  val statusOpen: ObligationStatus = OpenObligation
  val processed: Option[LocalDate] = Some(LocalDate.parse("2018-01-01"))
  val periodKey: String = ""

  val fulfilledObligationInputJson: String =
    """
      |{
      |  "status": "F",
      |  "inboundCorrespondenceFromDate": "2018-01-01",
      |  "inboundCorrespondenceToDate": "2018-01-01",
      |  "inboundCorrespondenceDateReceived": "2018-01-01",
      |  "inboundCorrespondenceDueDate": "2018-01-01",
      |  "periodKey": ""
      |}
    """.stripMargin

  val openObligationInputJson: String =
    """
      |{
      |  "status": "O",
      |  "inboundCorrespondenceFromDate": "2018-01-01",
      |  "inboundCorrespondenceToDate": "2018-01-01",
      |  "inboundCorrespondenceDueDate": "2018-01-01",
      |  "periodKey": ""
      |}
    """.stripMargin

  val fulfilledObligationOutputJson: String =
    """
      |{
      |  "status": "Fulfilled",
      |  "start": "2018-01-01",
      |  "end": "2018-01-01",
      |  "processed": "2018-01-01",
      |  "due": "2018-01-01"
      |}
    """.stripMargin

  val openObligationOutputJson: String =
    """
      |{
      |  "status": "Open",
      |  "start": "2018-01-01",
      |  "end": "2018-01-01",
      |  "due": "2018-01-01"
      |}
    """.stripMargin

  val fulfilledObligation: Obligation = Obligation(
    startDate = start,
    endDate = end,
    dueDate = due,
    status = statusFulfilled,
    processedDate = processed,
    periodKey = periodKey
  )

  val openObligation: Obligation = Obligation(
    startDate = start,
    endDate = end,
    dueDate = due,
    status = statusOpen,
    processedDate = None,
    periodKey = periodKey
  )

  "Creating a valid obligation" when {
    "the details represent a fulfilled obligation" should {

      val obligation = fulfilledObligation

      "result in an obligation with the correct start date" in {
        obligation.startDate shouldBe start
      }

      "result in an obligation with the correct end date" in {
        obligation.endDate shouldBe end
      }

      "result in an obligation with the correct due date" in {
        obligation.dueDate shouldBe due
      }

      "result in an obligation with the correct status" in {
        obligation.status shouldBe statusFulfilled
      }

      "result in an obligation with the correct processed date" in {
        obligation.processedDate shouldBe processed
      }

      "result in an obligation with the correct period key" in {
        obligation.periodKey shouldBe periodKey
      }
    }

    "the details represent an open obligation" should {

      val obligation = openObligation

      "result in an obligation with the correct start date" in {
        obligation.startDate shouldBe start
      }

      "result in an obligation with the correct end date" in {
        obligation.endDate shouldBe end
      }

      "result in an obligation with the correct due date" in {
        obligation.dueDate shouldBe due
      }

      "result in an obligation with the correct status" in {
        obligation.status shouldBe statusOpen
      }

      "result in an obligation with the correct processed date" in {
        obligation.processedDate shouldBe None
      }

      "result in an obligation with the correct period key" in {
        obligation.periodKey shouldBe periodKey
      }
    }

  }

  "Creating an invalid obligation" when {
    "a fulfilled obligation is missing the processed date" should {
      "throw an exception" in {

        Try(
          Obligation(
            startDate = start,
            endDate = end,
            dueDate = due,
            status = statusFulfilled,
            processedDate = None,
            periodKey = periodKey
          )
        ) match {
          case Success(_) => fail("Fulfilled obligation must have a processed date")
          case Failure(e) => e.getMessage shouldBe "Cannot create a fulfilled obligation without a processed date"
        }

      }
    }

    "an open obligation has a processed date" should {
      "throw an exception" in {

        Try(
          Obligation(
            startDate = start,
            endDate = end,
            dueDate = due,
            status = statusOpen,
            processedDate = processed,
            periodKey = periodKey
          )
        ) match {
          case Success(_) => fail("Open obligation must NOT have a processed date")
          case Failure(e) => e.getMessage shouldBe "Cannot create an open obligation with a processed date"
        }

      }
    }
  }

  "Reading an obligation from JSON" when {
    "the JSON represents a valid fulfilled obligation" should {

      val json = Json.parse(fulfilledObligationInputJson)

      val expectedObligation = fulfilledObligation

      "create an obligation with the correct details" in {
        val obligation = json.as[Obligation]

        obligation shouldBe expectedObligation
      }
    }

    "the JSON represents a valid open obligation" should {

      val json = Json.parse(openObligationInputJson)

      val expectedObligation = openObligation

      "create an obligation with the correct details" in {
        val obligation = json.as[Obligation]

        obligation shouldBe expectedObligation
      }
    }

    "the JSON represents a fulfilled obligation with a missing processed data" should {

      val json = Json.parse(
        """
          |{
          |  "status": "F",
          |  "inboundCorrespondenceFromDate": "2018-01-01",
          |  "inboundCorrespondenceToDate": "2018-01-01",
          |  "inboundCorrespondenceDueDate": "2018-01-01",
          |  "periodKey": ""
          |}
        """.stripMargin
      )

      "throw an error" in {
        Try(json.as[Obligation]) match {
          case Success(_) => fail("An error was expected")
          case Failure(e) => e.getMessage shouldBe "Cannot create a fulfilled obligation without a processed date"
        }
      }
    }

    "the JSON represents an open obligation with a processed data" should {

      val json = Json.parse(
        """
          |{
          |  "status": "O",
          |  "inboundCorrespondenceFromDate": "2018-01-01",
          |  "inboundCorrespondenceToDate": "2018-01-01",
          |  "inboundCorrespondenceDueDate": "2018-01-01",
          |  "inboundCorrespondenceDateReceived": "2018-01-01",
          |  "periodKey": ""
          |}
        """.stripMargin
      )

      "throw an error" in {
        Try(json.as[Obligation]) match {
          case Success(_) => fail("An error was expected")
          case Failure(e) => e.getMessage shouldBe "Cannot create an open obligation with a processed date"
        }
      }
    }

    val jsonWithAllFields = fulfilledObligationInputJson

    testMandatoryProperty("status")
    testMandatoryProperty("inboundCorrespondenceFromDate")
    testMandatoryProperty("inboundCorrespondenceToDate")
    testMandatoryProperty("inboundCorrespondenceDueDate")
    testMandatoryProperty("periodKey")

    testPropertyType("status")
    testPropertyType("inboundCorrespondenceFromDate")
    testPropertyType("inboundCorrespondenceToDate")
    testPropertyType("inboundCorrespondenceDueDate")
    testPropertyType("periodKey")

    def testMandatoryProperty(property: String): Unit = {
      s"the JSON is missing the required property $property" should {

        val json = Json.parse(
          jsonWithAllFields.replace(property, s"_$property")
        )

        val result = Try(json.as[Obligation])

        "only throw one error" in {
          result match {
            case Failure(e: JsResultException) => e.errors.size shouldBe 1
            case _ => fail("A JSON error was expected")
          }
        }

        "throw the error against the correct property" in {
          result match {
            case Failure(e: JsResultException) =>
              val propertyName = getOnlyJsonErrorPath(e)
              if (propertyName.isRight) {
                propertyName.right.get shouldBe s".$property"
              }
            case _ => fail("A JSON error was expected")
          }
        }

        "throw a missing path error" in {
          result match {
            case Failure(e: JsResultException) =>
              val message = getOnlyJsonErrorMessage(e)
              if (message.isRight) {
                message.right.get shouldBe "error.path.missing"
              }
            case _ => fail("A JSON error was expected")
          }
        }
      }
    }

    def testPropertyType(property: String): Unit = {

      val invalidTypedJson: String = jsonWithAllFields.split('\n').map { line =>
        if (line.trim.startsWith(s""""$property""")) {
          s""""$property":[],"""
        } else {
          line
        }
      }.mkString(" ").replace("], }", "] }")

      s"the JSON has the wrong data type for property $property" should {

        val json = Json.parse(invalidTypedJson)

        val result = Try(json.as[Obligation])

        "only throw one error" in {
          result match {
            case Failure(e: JsResultException) => e.errors.size shouldBe 1
            case _ => fail("A JSON error was expected")
          }
        }

        "throw the error against the correct property" in {
          result match {
            case Failure(e: JsResultException) =>
              val propertyName = getOnlyJsonErrorPath(e)
              if (propertyName.isRight) {
                propertyName.right.get shouldBe s".$property"
              }
            case _ => fail("A JSON error was expected")
          }
        }

        "throw an invalid type error" in {
          result match {
            case Failure(e: JsResultException) =>
              val message = getOnlyJsonErrorMessage(e)
              if (message.isRight) {
                message.right.get should startWith("error.expected.")
              }
            case _ => fail("A JSON error was expected")
          }
        }
      }
    }

  }

  "Writing an obligation to JSON" when {

    "The obligation is open" should {
      "generate the correct JSON" in {
        val json = Json.toJson(openObligation)
        val expectedJson = Json.parse(openObligationOutputJson)
        json shouldBe expectedJson
      }
    }

    "The obligation is fulfilled" should {
      "generate the correct JSON" in {
        val json = Json.toJson(fulfilledObligation)
        val expectedJson = Json.parse(fulfilledObligationOutputJson)
        json shouldBe expectedJson
      }
    }
  }

  def getOnlyJsonErrorPath(ex: JsResultException): Either[Assertion, String] = {
    ex.errors match {
      case (jsonPath, _) :: Nil => Right(jsonPath.path.last.toJsonString)
      case _ :: _ => Left(cancel("Too many JSON errors only expected one."))
    }
  }

  def getOnlyJsonErrorMessage(ex: JsResultException): Either[Assertion, String] = {
    ex.errors match {
      case (_, JsonValidationError(onlyError :: Nil) :: Nil) :: Nil => Right(onlyError)
      case (_, JsonValidationError(_ :: _) :: Nil) :: Nil => Left(cancel("Too many error messages for property"))
      case _ :: _ => Left(cancel("Too many JSON errors only expected one."))
    }
  }
}
