package io.legado.app.help.storage

import java.io.File

/**
 * 恢复策略。
 *
 * - [Merge]：只补不删（默认）。包内有的条目 upsert，本地有而包内没有的数据保留。
 * - [Replace]：包内声明且存在的条目整体覆盖（删除本地后写入），未声明的条目仍然不动。
 */
enum class RestorePolicy(val key: String) {
    Merge(BackupManifest.POLICY_MERGE),
    Replace(BackupManifest.POLICY_REPLACE);

    companion object {
        fun fromKey(key: String?): RestorePolicy =
            values().firstOrNull { it.key == key } ?: Merge
    }
}

/**
 * 恢复计划：决定"这次恢复可以动哪些条目、用哪种方式动"。
 *
 * 三条规则的落点：
 * 1. [has] 为 false 的条目，恢复流程不允许执行任何写/删动作；
 * 2. [shouldReplace] 只有显式覆盖策略 + 条目存在时才为 true；
 * 3. 旧包（无 manifest）按文件存在性推导，语义等同 v1。
 */
data class RestorePlan(
    val dir: File,
    val manifest: BackupManifest?,
    val selected: Set<String>?,
    val policy: RestorePolicy,
) {

    val isLegacy: Boolean get() = manifest == null

    /** 是否为分卷包（云端增量格式）。 */
    val isVolumeLayout: Boolean get() = !manifest?.volumes.isNullOrEmpty()

    /**
     * 判断条目是否参与本次恢复。
     *
     * 选择器恢复时 [selected] 为包内勾选的文件名集合，二者取交集。
     */
    fun has(name: String): Boolean {
        if (selected != null && name !in selected) return false
        if (manifest != null && manifest.findEntry(name) == null) return false
        return File(dir, name).exists()
    }

    fun hasAny(vararg names: String): Boolean = names.any { has(it) }

    /** 是否允许对该条目执行删除后整体覆盖。 */
    fun shouldReplace(name: String): Boolean = policy == RestorePolicy.Replace && has(name)

    fun describe(): String {
        val names = manifest?.entries?.map { it.name }?.take(12)?.joinToString(",")
        return "format=${manifest?.format ?: BackupManifest.FORMAT_V1} policy=${policy.key} " +
            "entries=${manifest?.entries?.size ?: -1} names=[${names.orEmpty()}]"
    }

    companion object {

        fun build(
            dir: File,
            selected: Set<String>? = null,
            policy: RestorePolicy = RestorePolicy.Merge,
        ): RestorePlan {
            val manifest = BackupManifest.read(File(dir, BackupManifest.FILE_NAME))
            return RestorePlan(
                dir = dir,
                manifest = manifest,
                selected = selected,
                policy = policy,
            )
        }
    }
}
