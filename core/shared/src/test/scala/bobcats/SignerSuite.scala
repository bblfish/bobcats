/*
 * Copyright 2021 Typelevel
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

package bobcats

import bobcats.util.PEMUtils
import cats.MonadError
import cats.syntax.all._
import munit.CatsEffectSuite
import scodec.bits.ByteVector

import scala.reflect.ClassTag
import scala.util.Try

/*
 * todo: does one need CatsEffectSuite?  (We don't use assertIO, ...)
 */
trait SignerSuite extends CatsEffectSuite {
  type MonadErr[T[_]] = MonadError[T, Throwable]

  val tests: Seq[SignatureExample] = SigningHttpMessages.signatureExamples

  def pemutils: PEMUtils

  def testSigner[F[_]: Signer: Verifier: MonadErr](
      sigTest: SignatureExample,
      pubKey: SPKIKeySpec[_],
      privKey: PKCS8KeySpec[_]
  )(implicit ct: ClassTag[F[_]]): Unit = {

    val signatureTxtF: F[ByteVector] =
      implicitly[MonadErr[F]].fromEither(ByteVector.encodeAscii(sigTest.sigtext))

    test(
      s"${sigTest.description} with ${ct.runtimeClass.getSimpleName()}: can verify generated signature") {
      for {
        sigTextBytes <- signatureTxtF
        sigFn <- Signer[F].sign(privKey, sigTest.signatureAlg)
        //todoL here it would be good to have a Seq of sigTest examples to test with the same sigFn
        signedTxt <- sigFn(sigTextBytes)
        verifyFn <- Verifier[F].verify(pubKey, sigTest.signatureAlg)
        b <- verifyFn(sigTextBytes, signedTxt)
      } yield {
        assertEquals(b, true, s"expected verify(>>${sigTest.sigtext}<<, >>$signedTxt<<)=true)")
      }
    }

    test(
      s"${sigTest.description} with ${ct.runtimeClass.getSimpleName()}: matches expected value") {
      for {
        sigTextBytes <- signatureTxtF
        expectedSig <- implicitly[MonadErr[F]].fromEither(
          ByteVector
            .fromBase64Descriptive(sigTest.signature, scodec.bits.Bases.Alphabets.Base64)
            .leftMap(new Exception(_))
        )
        verifyFn <- Verifier[F].verify(pubKey, sigTest.signatureAlg)
        b <- verifyFn(sigTextBytes, expectedSig)
      } yield {
        assertEquals(b, true, s"expected to verify >>${sigTest.sigtext}<<")
      }
    }
  }

  def extractKeys(
      ex: SignatureExample): (SPKIKeySpec[AsymmetricKeyAlg], PKCS8KeySpec[AsymmetricKeyAlg]) = {
    val res: Try[(SPKIKeySpec[AsymmetricKeyAlg], PKCS8KeySpec[AsymmetricKeyAlg])] = for {
      pub <- pemutils.getPublicKeyFromPEM(ex.keys.publicKeyNew, ex.keys.keyAlg)
      priv <- pemutils.getPrivateKeyFromPEM(ex.keys.privatePk8Key, ex.keys.keyAlg)
    } yield (pub, priv)

    test(s"${ex.description}: parsing public and private keys") {
      res.get
    }
    res.get
  }

  // subclasses should call run
  def run[F[_]: Signer: Verifier: MonadErr](
      tests: Seq[SignatureExample]
  )(implicit ct: ClassTag[F[_]]): Unit = {
    tests.foreach { sigTest =>
      // using flatmap here would not work as F is something like IO that would
      // delay the flapMap, meaning the tests would then not get registered.
      val keys = extractKeys(sigTest)
      testSigner[F](sigTest, keys._1, keys._2)
    }
  }
}
