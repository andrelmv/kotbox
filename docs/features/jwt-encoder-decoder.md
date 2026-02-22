# JWT Encoder/Decoder

The **JWT Encoder/Decoder** is a complete tool for working with JSON Web Tokens directly inside IntelliJ IDEA.

---

## What is JWT?

JSON Web Token (JWT) is an open standard ([RFC 7519](https://tools.ietf.org/html/rfc7519)) that defines a compact and self-contained way to securely transmit information between parties as a JSON object.

JWTs are widely used for:

- **Authentication** - User login in web applications
- **Authorization** - Access control to resources
- **Information Exchange** - Secure data transfer between services

---

## How to Access

To open the JWT Encoder/Decoder:

1. From the menu, go to **View** → **Tool Windows** → **Kotlin Toolbox**
2. In the **Developer Toolbox** window, select the **JWT Encoder/Decoder** tab

<div style="text-align: center;" markdown="1">

![JWT Tool Window](../assets/images/jwt/jwt-demo.gif){ width="50%" .skip-lightbox }

</div>

---

## Encoding a JWT

Follow the steps below to create a new JWT token:

### 1. Select the Algorithm

At the top of the tool, choose the signing algorithm:

- **HS256/HS384/HS512** - HMAC with SHA-256/384/512
- **RS256/RS384/RS512** - RSA with SHA-256/384/512
- **ES256/ES384/ES512** - ECDSA with P-256/384/521
- **PS256/PS384/PS512** - RSA-PSS with SHA-256/384/512
- **EdDSA** - EdDSA using Ed25519

### 2. Configure the Payload

Enter the JSON payload in the editor. Example:

```json
{
  "sub": "1234567890",
  "name": "John Doe",
  "iat": 1516239022,
  "exp": 1735689600
}
```

!!! info "Common Fields"
    - `sub` (subject) - User identifier
    - `iat` (issued at) - Issuance date (Unix timestamp)
    - `exp` (expiration) - Expiration date (Unix timestamp)
    - `iss` (issuer) - Token issuer
    - `aud` (audience) - Token recipient

### 3. Add the Key / Secret

Depending on the chosen algorithm:

=== "HMAC (HS256/384/512)"

    Enter a symmetric secret. Supports:

    - **RAW** - Plain text
    - **Base32** - Base32 encoding
    - **Base64** - Base64 encoding

    Example:
    ```
    my-secret-key-min-256-bits-long
    ```

=== "RSA (RS256/384/512, PS256/384/512)"

    Paste your RSA private key in PEM format:

    ```
    -----BEGIN PRIVATE KEY-----
    MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQC7...
    -----END PRIVATE KEY-----
    ```

=== "ECDSA (ES256/384/512)"

    Paste your EC private key in PEM format:

    ```
    -----BEGIN EC PRIVATE KEY-----
    MHcCAQEEIIGlRNt...
    -----END EC PRIVATE KEY-----
    ```

=== "EdDSA"

    Paste your Ed25519 private key:

    ```
    -----BEGIN PRIVATE KEY-----
    MC4CAQAwBQYDK2VwBCIEII...
    -----END PRIVATE KEY-----
    ```

### 4. Generate the Token

Click the **Encode** button to generate the JWT. The token will appear in the output section.

!!! success "Token Generated!"
    The token can be copied and used in your applications.

---

## Decoding a JWT

To decode and verify an existing JWT token:

### 1. Paste the Token

In the input field, paste the full JWT token:

```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c
```

### 2. View Header and Payload

The tool automatically decodes and shows:

- **Header** - Algorithm and token type
- **Payload** - Data contained in the token

```json title="Header"
{
  "alg": "HS256",
  "typ": "JWT"
}
```

```json title="Payload"
{
  "sub": "1234567890",
  "name": "John Doe",
  "iat": 1516239022
}
```

### 3. Verify the Signature

To verify whether the token is valid:

1. Select the correct algorithm (indicated in the header)
2. Provide the public key (for RSA/ECDSA) or secret (for HMAC)
3. Click **Verify**

!!! success "Valid Signature ✓"
    If the signature is valid, you will see a success message

!!! danger "Invalid Signature ✗"
    If the signature is invalid, the token may have been tampered with

---

## Supported Algorithms

### HMAC (Symmetric)

| Algorithm | Description        | Key Size                    |
|-----------|--------------------|-----------------------------|
| **HS256** | HMAC using SHA-256 | Minimum 256 bits (32 bytes) |
| **HS384** | HMAC using SHA-384 | Minimum 384 bits (48 bytes) |
| **HS512** | HMAC using SHA-512 | Minimum 512 bits (64 bytes) |

**When to use:** Environments where client and server share the same secret.

### RSA (Asymmetric)

| Algorithm | Description       | Key Size                      |
|-----------|-------------------|-------------------------------|
| **RS256** | RSA using SHA-256 | Minimum 2048 bits recommended |
| **RS384** | RSA using SHA-384 | Minimum 2048 bits recommended |
| **RS512** | RSA using SHA-512 | Minimum 2048 bits recommended |

**When to use:** Server signs (private key) and clients verify (public key).

### RSA-PSS (Asymmetric)

| Algorithm | Description           | Key Size                      |
|-----------|-----------------------|-------------------------------|
| **PS256** | RSA-PSS using SHA-256 | Minimum 2048 bits recommended |
| **PS384** | RSA-PSS using SHA-384 | Minimum 2048 bits recommended |
| **PS512** | RSA-PSS using SHA-512 | Minimum 2048 bits recommended |

**When to use:** More secure alternative to traditional RSA.

### ECDSA (Asymmetric)

| Algorithm | Description                   | Curve             |
|-----------|-------------------------------|-------------------|
| **ES256** | ECDSA using P-256 and SHA-256 | P-256 (secp256r1) |
| **ES384** | ECDSA using P-384 and SHA-384 | P-384 (secp384r1) |
| **ES512** | ECDSA using P-521 and SHA-512 | P-521 (secp521r1) |

**When to use:** Better performance than RSA at the same security level.

### EdDSA (Asymmetric)

| Algorithm | Description         | Curve   |
|-----------|---------------------|---------|
| **EdDSA** | EdDSA using Ed25519 | Ed25519 |

**When to use:** Best performance and security for digital signatures.

---

## Key Formats

The tool supports multiple encoding formats for symmetric keys (HMAC):

### RAW (Plain Text)
```
my-super-secret-key-that-is-at-least-256-bits
```

### Base32
```
JBSWY3DPEBLW64TMMQQQ====
```

### Base64
```
bXktc3VwZXItc2VjcmV0LWtleQ==
```

!!! tip "Tip"
    Use Base64 when you need to work with arbitrary bytes or share keys securely.

---

## Best Practices

!!! success "Recommendations"
    - Use asymmetric algorithms (RSA, ECDSA, EdDSA) in production
    - Always set an `exp` (expiration) for your tokens
    - Keep tokens short — do not include sensitive data
    - Use HTTPS when transmitting JWTs
    - Store private keys securely
    - Rotate keys periodically

!!! warning "Avoid"
    - Storing passwords or sensitive data in the payload
    - Using HMAC with weak keys (< 256 bits)
    - Trusting tokens without verifying the signature
    - Tokens without expiration
    - Sharing private keys

---

## Troubleshooting

### Invalid token when decoding

- Check that the token is complete (3 parts separated by `.`)
- Make sure there are no extra spaces
- Confirm the format is valid (Base64URL encoding)

### Signature verification error

- Confirm you are using the correct algorithm
- Check that the key/secret is correct
- For asymmetric algorithms, make sure you are using the right key (private for signing, public for verifying)

### Error generating token

- Check that the payload is valid JSON
- Confirm the key meets the minimum required length
- For PEM keys, make sure to include the BEGIN/END lines

---

<div style="text-align: center;" markdown="1">

**Want to explore other features?**

[Kotlin Inlay Hints](kotlin-inlay-hints.md){ .md-button }

</div>
