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

sealed trait Algorithm {
  private[bobcats] def toStringJava: String
  private[bobcats] def toStringNodeJS: String
  private[bobcats] def toStringWebCrypto: String
}

sealed trait HashAlgorithm extends Algorithm
object HashAlgorithm {

  case object MD5 extends HashAlgorithm {
    private[bobcats] override def toStringJava: String = "MD5"
    private[bobcats] override def toStringNodeJS: String = "md5"
    private[bobcats] override def toStringWebCrypto: String =
      throw new UnsupportedOperationException
  }

  case object SHA1 extends HashAlgorithm {
    private[bobcats] override def toStringJava: String = "SHA-1"
    private[bobcats] override def toStringNodeJS: String = "sha1"
    private[bobcats] override def toStringWebCrypto: String = "SHA-1"
  }

  case object SHA256 extends HashAlgorithm {
    private[bobcats] override def toStringJava: String = "SHA-256"
    private[bobcats] override def toStringNodeJS: String = "sha256"
    private[bobcats] override def toStringWebCrypto: String = "SHA-256"
  }

  case object SHA512 extends HashAlgorithm {
    private[bobcats] override def toStringJava: String = "SHA-512"
    private[bobcats] override def toStringNodeJS: String = "sha512"
    private[bobcats] override def toStringWebCrypto: String = "SHA-512"
  }
}

sealed trait HmacAlgorithm extends Algorithm {
  private[bobcats] def minimumKeyLength: Int
}
object HmacAlgorithm {

  private[bobcats] def fromStringJava(algorithm: String): Option[HmacAlgorithm] =
    algorithm match {
      case "HmacSHA1" => Some(SHA1)
      case "HmacSHA256" => Some(SHA256)
      case "HmacSHA512" => Some(SHA512)
      case _ => None
    }

  case object SHA1 extends HmacAlgorithm {
    private[bobcats] override def toStringJava: String = "HmacSHA1"
    private[bobcats] override def toStringNodeJS: String = "sha1"
    private[bobcats] override def toStringWebCrypto: String = "SHA-1"
    private[bobcats] override def minimumKeyLength: Int = 20
  }

  case object SHA256 extends HmacAlgorithm {
    private[bobcats] override def toStringJava: String = "HmacSHA256"
    private[bobcats] override def toStringNodeJS: String = "sha256"
    private[bobcats] override def toStringWebCrypto: String = "SHA-256"
    private[bobcats] override def minimumKeyLength: Int = 32
  }

  case object SHA512 extends HmacAlgorithm {
    private[bobcats] override def toStringJava: String = "HmacSHA512"
    private[bobcats] override def toStringNodeJS: String = "sha512"
    private[bobcats] override def toStringWebCrypto: String = "SHA-512"
    private[bobcats] override def minimumKeyLength: Int = 64
  }
}

// Public Key Algorithm
sealed trait PKA extends Algorithm {
  type Private <: PrivateKeyAlg //matching private key type
  val Private: Private
}

object PKA {
  sealed trait Signature extends Algorithm
  trait RSA
  //not sure what a good name for this is, or if that is the right object
  case object RSA extends PKA with RSA {
    override type Private = PrivateKeyAlg.RSA.type
    override val Private = PrivateKeyAlg.RSA

    override private[bobcats] def toStringJava = "RSA"
    override private[bobcats] def toStringNodeJS = ???
    override private[bobcats] def toStringWebCrypto = ???
  }

  case object SHA512 extends Signature with RSA {
    override private[bobcats] def toStringJava = "SHA512withRSA"
    override private[bobcats] def toStringNodeJS = ???
    override private[bobcats] def toStringWebCrypto = ???
  }
}

sealed trait PrivateKeyAlg extends Algorithm

object PrivateKeyAlg {

  //not sure what a good name for this is, or if that is the right object, should it perhaps just be RSA?
  //like the matching private key?
  //it may be good for Public/Private keys to have them defined clearly as pairs, so as to
  //help developers find the matching piece via the type system.
  case object RSA extends PrivateKeyAlg {
    override private[bobcats] def toStringJava = "RSASSA-PSS"
    override private[bobcats] def toStringNodeJS = ???
    override private[bobcats] def toStringWebCrypto = ???
  }
}

