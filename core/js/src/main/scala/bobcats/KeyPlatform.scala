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

import bobcats.AsymmetricKeyAlg.RSA
import cats.effect.kernel.Async
import org.scalajs.dom
import org.scalajs.dom.{EcKeyImportParams, HashAlgorithmIdentifier, RsaHashedImportParams}

import scala.scalajs.js

private[bobcats] trait KeyPlatform
private[bobcats] trait PublicKeyPlatform
private[bobcats] trait PrivateKeyPlatform
private[bobcats] trait SecretKeyPlatform

private[bobcats] trait SecretKeySpecPlatform[+A <: Algorithm]
private[bobcats] trait PrivateKeySpecPlatform[+A <: AsymmetricKeyAlg] { self: PrivateKeySpec[A] =>
	def toWebCryptoKey[F[_]](signature: AsymmetricKeyAlg.Signature)(
	  implicit F0: Async[F]
	): F[org.scalajs.dom.CryptoKey] = {
		val alg: org.scalajs.dom.KeyAlgorithm = algorithm  match {
			case rsaAlg: RSA => new RsaHashedImportParams {
					override val hash: HashAlgorithmIdentifier = signature.hash.toStringWebCrypto
					override val name: String = rsaAlg.toStringWebCrypto
			}
			case ecAlg @ bobcats.AsymmetricKeyAlg.ECKey(p) => new EcKeyImportParams {
				override val namedCurve: String = p.toString
				override val name: String = ecAlg.toStringWebCrypto
			}


		}
		F0.fromPromise(F0.delay {
			dom.crypto.subtle.importKey(
				dom.KeyFormat.pkcs8, key.toUint8Array,
				alg,
				true,
				js.Array(dom.KeyUsage.sign)
			)
		})
	}

}
private[bobcats] trait PublicKeySpecPlatform[+A <: AsymmetricKeyAlg] { self: PublicKeySpec[A] =>
	def toWebCryptoKey[F[_]](signature: AsymmetricKeyAlg.Signature)(
	  implicit F0: Async[F]
	): F[org.scalajs.dom.CryptoKey] = {
		val alg: org.scalajs.dom.KeyAlgorithm = algorithm  match {
			case rsaAlg: RSA => new RsaHashedImportParams {
					override val hash: HashAlgorithmIdentifier = signature.hash.toStringWebCrypto //toDomHash(signature.hash)
					override val name: String = rsaAlg.toStringWebCrypto
				}
			case ecAlg @ bobcats.AsymmetricKeyAlg.ECKey(p) => new EcKeyImportParams {
				override val namedCurve: String = p.toString
				override val name: String = ecAlg.toStringWebCrypto
			}
		}
		F0.fromPromise(F0.delay {
			dom.crypto.subtle.importKey(
				dom.KeyFormat.spki, key.toUint8Array, alg,
				true, //todo: do we always want extractable?
				js.Array(dom.KeyUsage.verify) //todo: we may want other key usages?
			)
		})
}
}
