package io.legado.app.help.storage

import androidx.annotation.Keep
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonObject
import java.io.File
import java.util.zip.CRC32

/**
 * 备份包清单条目。
 *
 * [name] 为包内相对路径（文件或目录名）；[kind] 见 [BackupManifest] 中的常量。
 * 文件条目记录 size + crc32 用于完整性校验，目录条目只记录文件数与总大小。
 */
@Keep
data class BackupManifestEntry(
    val name: String = "",
    val kind: String = "",
    val size: Long = 0,
    val crc32: Long = 0,
    val files: Int = -1,
)

/**
 * 备份包清单（格式 v2）。
 *
 * 放在备份包根目录的 `manifest.json`，是恢复时的唯一事实来源：
 * - 包内声明了什么条目，恢复才允许改动什么条目；
 * - 条目带 size/crc32，恢复前可校验完整性；
 * - [volumes] 用于分卷包（云端增量），记录每个卷的校验信息。
 *
 * 旧包（无 manifest）按格式 v1 处理，行为退回"文件存在即可恢复"，
 * 但仍然遵守"不存在就不允许删除"的安全规则（见 [RestorePlan]）。
 */
@Keep
data class BackupManifest(
    val format: Int = FORMAT_V2,
    val createdAt: Long = 0,
    val appVersionCode: Int = 0,
    val dbVersion: Int = 0,
    val deviceName: String = "",
    val scope: String = "",
    val policyHint: String = POLICY_MERGE,
    val encrypted: Boolean = false,
    val entries: List<BackupManifestEntry> = emptyList(),
    val volumes: List<BackupManifestEntry> = emptyList(),
) {

    fun findEntry(name: String): BackupManifestEntry? = entries.firstOrNull { it.name == name }

    fun findVolume(name: String): BackupManifestEntry? = volumes.firstOrNull { it.name == name }

    companion object {

        const val FILE_NAME = "manifest.json"

        const val FORMAT_V1 = 1
        const val FORMAT_V2 = 2

        const val POLICY_MERGE = "merge"
        const val POLICY_REPLACE = "replace"

        const val KIND_DB = "db"
        const val KIND_PREFS = "prefs"
        const val KIND_CONFIG = "config"
        const val KIND_DIR = "dir"

        const val VOLUME_DB = "db.zip"
        const val VOLUME_CONFIG = "config.zip"
        const val VOLUME_CACHE = "cache.zip"
        const val VOLUME_ASSETS = "assets.zip"

        fun read(path: File): BackupManifest? {
            if (!path.exists() || path.length() <= 0) return null
            return runCatching {
                GSON.fromJsonObject<BackupManifest>(path.readText()).getOrNull()
            }.getOrNull()
        }

        fun write(path: File, manifest: BackupManifest) {
            path.writeText(GSON.toJson(manifest))
        }

        fun crc32(file: File): Long {
            val crc = CRC32()
            file.inputStream().buffered().use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    crc.update(buffer, 0, read)
                }
            }
            return crc.value
        }

        fun fileEntry(name: String, kind: String, file: File): BackupManifestEntry {
            return BackupManifestEntry(
                name = name,
                kind = kind,
                size = file.length(),
                crc32 = crc32(file),
            )
        }

        fun dirEntry(name: String, dir: File): BackupManifestEntry {
            var size = 0L
            var files = 0
            dir.walkTopDown().forEach { file ->
                if (file.isFile) {
                    size += file.length()
                    files++
                }
            }
            return BackupManifestEntry(name = name, kind = KIND_DIR, size = size, files = files)
        }

        /**
         * 校验包内容是否与 manifest 一致。
         *
         * 目录条目只校验存在性与文件数；文件条目校验大小与 CRC。
         * 返回空列表表示通过。
         */
        fun validate(path: File, manifest: BackupManifest): List<String> {
            val errors = mutableListOf<String>()
            if (manifest.format > FORMAT_V2) {
                errors.add("不支持的备份格式：${manifest.format}")
            }
            manifest.entries.forEach { entry ->
                val file = File(path, entry.name)
                if (!file.exists()) {
                    errors.add("缺少条目：${entry.name}")
                    return@forEach
                }
                if (entry.kind == KIND_DIR) {
                    if (!file.isDirectory) {
                        errors.add("条目类型不符：${entry.name}")
                    } else if (entry.files >= 0) {
                        val actual = file.walkTopDown().count { it.isFile }
                        if (actual != entry.files) {
                            errors.add("${entry.name} 文件数不一致")
                        }
                    }
                    return@forEach
                }
                if (entry.size > 0 && file.length() != entry.size) {
                    errors.add("${entry.name} 大小不一致")
                    return@forEach
                }
                if (entry.crc32 != 0L && crc32(file) != entry.crc32) {
                    errors.add("${entry.name} 校验失败")
                }
            }
            return errors
        }
    }
}
