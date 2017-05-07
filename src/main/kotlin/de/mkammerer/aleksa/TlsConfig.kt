package de.mkammerer.aleksa

/**
 * TLS config.
 *
 * The [keystore] points to the keystore file on disk. The [keystorePassword] specifies the password for the keystore,
 * [keyPassword] specifies the password for the key. [alias] sets the alias of the keypair which should be used. If null,
 * a matching keypair is chosen automatically.
 */
data class TlsConfig(
        val keystore: String,
        val keystorePassword: String,
        val keyPassword: String = keystorePassword,
        val alias: String? = null
)