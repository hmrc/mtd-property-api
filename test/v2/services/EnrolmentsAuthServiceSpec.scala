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

package v2.services

import org.scalamock.handlers.CallHandler
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisationException, InsufficientEnrolments, MissingBearerToken}
import uk.gov.hmrc.auth.core.authorise.{EmptyPredicate, Predicate}
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.http.HeaderCarrier
import v2.models.errors.AuthError

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class EnrolmentsAuthServiceSpec extends ServiceSpec {

  trait Test {
    val mockAuthConnector: AuthConnector = mock[AuthConnector]

    object MockedAuthConnector {
      def authorised[A](predicate: Predicate): CallHandler[Future[A]] = {
        (mockAuthConnector.authorise[A](_: Predicate, _: Retrieval[A])(_: HeaderCarrier, _: ExecutionContext))
          .expects(predicate, *, *, *)
      }
    }

    lazy val target = new EnrolmentsAuthService(mockAuthConnector)
  }

  "calling .authorised" when {

    "the user is authorised" should {
      "return a Right aligned Either" in new Test {

        val expected = Right(true)

        MockedAuthConnector.authorised(EmptyPredicate)
          .returns(Future.successful({}))

        private val result = await(target.authorised(EmptyPredicate))

        result shouldBe expected
      }
    }

    "the user is not logged in" should {
      "return an unauthenticated error" in new Test {

        val expected = Left(AuthError())

        MockedAuthConnector.authorised(EmptyPredicate)
          .returns(Future.failed(MissingBearerToken()))

        private val result = await(target.authorised(EmptyPredicate))

        result shouldBe expected
      }
    }

    "the user is not authorised" should {
      "return an unauthorised error" in new Test {

        val expected = Left(AuthError(true, false))

        MockedAuthConnector.authorised(EmptyPredicate)
          .returns(Future.failed(InsufficientEnrolments()))

        private val result = await(target.authorised(EmptyPredicate))

        result shouldBe expected
      }
    }

  }
}
