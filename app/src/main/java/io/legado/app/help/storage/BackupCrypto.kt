package io.legado.app.help.storage

import io.legado.app.exception.NoStackTraceException
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * 整包加密工具（可选，默认关闭）。
 *
 * 文件格式：魔数 "LGBK1" + 12 字节 IV + AES/GCM 密文（含 16 字节认证 tag）。
 * 密钥由用户设置的备份密码经 SHA-256 派生；未设密码时拒绝加密，
 * 避免继续用 `MD5("")` 这种可预测密钥假装加密。
 */
object BackupCrypto {

    private const val MAGIC = "LGBK1"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_LENGTH = 12
    private const val TAG_BITS = 128
    private const val BUFFER_SIZE = 64 * 1024

    fun isEncrypted(file: File): Boolean {
        if (!file.exists() || file.length() < MAGIC.length + IV_LENGTH) return false
        return runCatching {
            file.inputStream().use { input ->
                val bytes = ByteArray(MAGIC.length)
                readFully(input, bytes)
                bytes.toString(Charsets.US_ASCII) == MAGIC
            }
        }.getOrDefault(false)
    }

    fun encrypt(src: File, dst: File, password: String) {
        if (password.isBlank()) throw NoStackTraceException("未设置备份密码，无法加密")
        val iv = ByteArray(IV_LENGTH).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, key(password), GCMParameterSpec(TAG_BITS, iv))
        }
        dst.outputStream().buffered().use { output ->
            output.write(MAGIC.toByteArray(Charsets.US_ASCII))
            output.write(iv)
            CipherOutputStream(output, cipher).use { cipherOutput ->
                src.inputStream().buffered().use { it.copyTo(cipherOutput, BUFFER_SIZE) }
            }
        }
    }

    fun decrypt(src: File, dst: File, password: String) {
        if (password.isBlank()) throw NoStackTraceException("未设置备份密码，无法解密")
        src.inputStream().buffered().use { input ->
            val magicBytes = ByteArray(MAGIC.length)
            readFully(input, magicBytes)
            val magic = magicBytes.toString(Charsets.US_ASCII)
            if (magic != MAGIC) throw NoStackTraceException("不是加密的备份文件")
            val iv = ByteArray(IV_LENGTH)
            readFully(input, iv)
            val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.DECRYPT_MODE, key(password), GCMParameterSpec(TAG_BITS, iv))
            }
            CipherInputStream(input, cipher).use { cipherInput ->
                dst.outputStream().buffered().use { output ->
                    cipherInput.copyTo(output, BUFFER_SIZE)
                }
            }
        }
    }

    private fun readFully(input: InputStream, bytes: ByteArray) {
        var offset = 0
        while (offset < bytes.size) {
            val read = input.read(bytes, offset, bytes.size - offset)
            if (read <= 0) throw NoStackTraceException("加密备份文件损坏")
            offset += read
        }
    }

    private fun key(password: String): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(password.toByteArray(Charsets.UTF_8))
        return SecretKeySpec(digest, "AES")
    }
}
