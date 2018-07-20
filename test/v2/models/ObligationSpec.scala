/*
 * Copyright 2018 HM Revenue & Customs
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
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsResultException, Json}
import support.UnitSpec

import scala.util.{Failure, Success, Try}

class ObligationSpec extends UnitSpec {

  val start: LocalDate = LocalDate.parse("2018-01-01")
  val end: LocalDate = LocalDate.parse("2018-01-01")
  val due: LocalDate = LocalDate.parse("2018-01-01")
  val statusFulfilled: FulfilledObligation.type = FulfilledObligation
  val statusOpen: ObligationStatus = OpenObligation
  val processed = Some(LocalDate.parse("2018-01-01"))
  val periodKey: String = ""

  "Creating an obligation with valid details" should {

    val obligation = Obligation(
      startDate = start,
      endDate = end,
      dueDate = due,
      status = statusFulfilled,
      processedDate = processed,
      periodKey = periodKey
    )

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

  "Creating an fulfilled obligation without a processed date" should {
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

  "Creating an open obligation with a processed date" should {
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

  "Reading an obligation from JSON" when {
    "the JSON represents a valid fulfilled obligation" should {

      val json = Json.parse(
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
      )

      val expectedObligation = Obligation(
        startDate = start,
        endDate = end,
        dueDate = due,
        status = statusFulfilled,
        processedDate = processed,
        periodKey = periodKey
      )

      "create an obligation with the correct details" in {
        val obligation = json.as[Obligation]

        obligation shouldBe expectedObligation
      }
    }

    "the JSON represents a valid open obligation" should {

      val json = Json.parse(
        """
          |{
          |  "status": "O",
          |  "inboundCorrespondenceFromDate": "2018-01-01",
          |  "inboundCorrespondenceToDate": "2018-01-01",
          |  "inboundCorrespondenceDueDate": "2018-01-01",
          |  "periodKey": ""
          |}
        """.stripMargin
      )

      val expectedObligation = Obligation(
        startDate = start,
        endDate = end,
        dueDate = due,
        status = statusOpen,
        processedDate = None,
        periodKey = periodKey
      )

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

    val jsonWithAllFields =
      """
        |{
        |  "status": "F",
        |  "inboundCorrespondenceFromDate": "2018-01-01",
        |  "inboundCorrespondenceToDate": "2018-01-01",
        |  "inboundCorrespondenceDueDate": "2018-01-01",
        |  "inboundCorrespondenceDateReceived": "2018-01-01",
        |  "periodKey": ""
        |}
      """.stripMargin

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

  def getOnlyJsonErrorPath(ex: JsResultException): Either[Assertion, String] = {
    ex.errors match {
      case (jsonPath, _) :: Nil => Right(jsonPath.path.last.toJsonString)
      case _ :: _ => Left(cancel("Too many JSON errors only expected one."))
    }
  }

  def getOnlyJsonErrorMessage(ex: JsResultException): Either[Assertion, String] = {
    ex.errors match {
      case (_, ValidationError(onlyError :: Nil) :: Nil) :: Nil => Right(onlyError)
      case (_, ValidationError(_ :: _) :: Nil) :: Nil => Left(cancel("Too many error messages for property"))
      case _ :: _ => Left(cancel("Too many JSON errors only expected one."))
    }
  }
}
