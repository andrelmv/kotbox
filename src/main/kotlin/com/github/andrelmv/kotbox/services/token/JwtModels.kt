package com.github.andrelmv.kotbox.services.token

enum class AlgorithmKind(val label: String) {
    HMAC("Secret key:"),
    RSA("Private key:"),
    ECDSA("Private key:"),
    RSA_PSS("Private key:"),
    EDDSA("Private key:"),
}

enum class SignatureAlgorithm(
    val displayName: String,
    val jwtHeaderValue: String,
    val kind: AlgorithmKind,
    val jdkAlgorithm: String,
    val minKeyBytes: Int,
    val ecComponentSize: Int = 0,
) {
    HMAC256("HMAC256 (HS256)", "HS256", AlgorithmKind.HMAC, "HmacSHA256", 32),
    HMAC384("HMAC384 (HS384)", "HS384", AlgorithmKind.HMAC, "HmacSHA384", 48),
    HMAC512("HMAC512 (HS512)", "HS512", AlgorithmKind.HMAC, "HmacSHA512", 64),
    RSA256("RSA256 (RS256)", "RS256", AlgorithmKind.RSA, "SHA256withRSA", 0),
    RSA384("RSA384 (RS384)", "RS384", AlgorithmKind.RSA, "SHA384withRSA", 0),
    RSA512("RSA512 (RS512)", "RS512", AlgorithmKind.RSA, "SHA512withRSA", 0),
    ECDSA256("ECDSA256 (ES256)", "ES256", AlgorithmKind.ECDSA, "SHA256withECDSA", 0, 32),
    ECDSA384("ECDSA384 (ES384)", "ES384", AlgorithmKind.ECDSA, "SHA384withECDSA", 0, 48),
    ECDSA512("ECDSA512 (ES512)", "ES512", AlgorithmKind.ECDSA, "SHA512withECDSA", 0, 66),
    PS256("PS256", "PS256", AlgorithmKind.RSA_PSS, "RSASSA-PSS", 0),
    PS384("PS384", "PS384", AlgorithmKind.RSA_PSS, "RSASSA-PSS", 0),
    PS512("PS512", "PS512", AlgorithmKind.RSA_PSS, "RSASSA-PSS", 0),
    EDDSA("EdDSA", "EdDSA", AlgorithmKind.EDDSA, "Ed25519", 0),
    ;

    override fun toString() = displayName
}

data class DecodeResult(
    val header: String,
    val payload: String,
    val encodedSignature: String,
    val isSignatureValid: Boolean = false,
)

data class EncodeResult(
    val encodedToken: String,
    val encodedHeader: String,
    val encodedPayload: String,
    val encodedSignature: String,
)

enum class SecretKeyEncoding(val displayName: String) {
    RAW("Raw"),
    BASE32("Base32 Encoded"),
    BASE64("Base64 Encoded"),
}

data class SigningConfig(
    val algorithm: SignatureAlgorithm,
    val secretKey: String,
    val privateKeyPem: String,
    val strictValidation: Boolean,
    val secretKeyEncoding: SecretKeyEncoding = SecretKeyEncoding.RAW,
)
