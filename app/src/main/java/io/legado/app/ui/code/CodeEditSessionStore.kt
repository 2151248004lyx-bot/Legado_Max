package io.legado.app.ui.code

import splitties.init.appCtx
import java.io.File
import java.io.IOException
import java.util.UUID

/**
 * Stores editable text outside Activity Intents and saved-state Bundles.
 *
 * The session id is the only value crossing Binder. Files stay in the app-private cache directory
 * and are removed by the caller when the edit flow finishes.
 */
internal class CodeEditSessionStore(
    private val directory: File,
) {

    companion object {
        val app: CodeEditSessionStore by lazy {
            CodeEditSessionStore(File(appCtx.cacheDir, "code_edit_sessions"))
        }

        private val sessionIdPattern = Regex(
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
        )
    }

    @Synchronized
    fun create(text: String): String {
        val sessionId = UUID.randomUUID().toString()
        write(sessionId, text)
        return sessionId
    }

    @Synchronized
    fun read(sessionId: String): String? {
        val file = sessionFile(sessionId)
        val backup = backupFile(sessionId)
        if (!file.exists() && backup.exists()) {
            backup.renameTo(file)
        }
        return file.takeIf(File::isFile)?.readText(Charsets.UTF_8)
    }

    @Synchronized
    fun write(sessionId: String, text: String) {
        ensureDirectory()
        val file = sessionFile(sessionId)
        val pending = pendingFile(sessionId)
        val backup = backupFile(sessionId)
        pending.delete()
        pending.writeText(text, Charsets.UTF_8)

        if (file.exists()) {
            backup.delete()
            if (!file.renameTo(backup)) {
                pending.delete()
                throw IOException("无法备份代码编辑会话")
            }
        }

        if (!pending.renameTo(file)) {
            backup.renameTo(file)
            pending.delete()
            throw IOException("无法更新代码编辑会话")
        }
        backup.delete()
    }

    @Synchronized
    fun delete(sessionId: String) {
        sessionFile(sessionId).delete()
        pendingFile(sessionId).delete()
        backupFile(sessionId).delete()
        directory.delete()
    }

    private fun ensureDirectory() {
        if (!directory.exists() && !directory.mkdirs()) {
            throw IOException("无法创建代码编辑会话目录")
        }
        if (!directory.isDirectory) {
            throw IOException("代码编辑会话目录不可用")
        }
    }

    private fun sessionFile(sessionId: String): File {
        require(sessionIdPattern.matches(sessionId)) { "无效的代码编辑会话" }
        return File(directory, "$sessionId.txt")
    }

    private fun pendingFile(sessionId: String) = File(directory, "$sessionId.new")

    private fun backupFile(sessionId: String) = File(directory, "$sessionId.bak")
}
