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

import cats.effect.kernel.Sync
import scodec.bits.ByteVector

import java.security
import java.security.spec.{MGF1ParameterSpec, PSSParameterSpec}

private[bobcats] trait SignerPlatform[F[_]]

private[bobcats] trait SignerCompanionPlatform {

  implicit def forSync[F[_]](implicit F: Sync[F]): Signer[F] =
    new UnsealedSigner[F] {
      // one would really want a type that pairs the PKA and Sig, so as not to leave impossible combinations open
      override def build( // it is not clear that adding [A <: PrivateKeyAlg, S <: PKA.Signature] helps
          spec: PrivateKey[_],
          sigType: AsymmetricKeyAlg.Signature): F[ByteVector => F[ByteVector]] =
        F.catchNonFatal {
          // I believe PrivateKeys are immutable so they can be re-used
          val priv: security.PrivateKey = spec.toJava

          (data: ByteVector) =>
            F.catchNonFatal {
              // signatures are not thread safe, so new ones must be created for each call
              // (or .toJava would need to be fiber aware)
              val sig: java.security.Signature = sigType.toJava
              sig.initSign(priv)
              sig.update(data.toByteBuffer)
              ByteVector.view(sig.sign())
            }
        }
    }
}

object SignerCompanionPlatform {
  // these are the values set in sun.security.util.SignatureUtil.PSSParamsHolder.PSS_512_SPEC
  def spec(salt: Int, sha: HashAlgorithm): PSSParameterSpec = new PSSParameterSpec(
    sha.toStringJava,
    "MGF1",
    new MGF1ParameterSpec(sha.toStringJava),
    salt,
    1)
}
