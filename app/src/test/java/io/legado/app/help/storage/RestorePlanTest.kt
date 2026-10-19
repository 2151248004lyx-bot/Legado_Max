package io.legado.app.help.storage

import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * 恢复计划的安全语义：缺条目不删、清单优先、策略只作用于存在的条目。
 */
class RestorePlanTest {

    private val dirs = mutableListOf<File>()

    private fun tempDir(): File {
        return Files.createTempDirectory("restore-plan").toFile().also { dirs.add(it) }
    }

    private fun writeManifest(dir: File, vararg names: String) {
        val entries = names.map { name ->
            val file = File(dir, name)
            if (file.isDirectory) {
                BackupManifest.dirEntry(name, file)
            } else {
                BackupManifest.fileEntry(name, BackupManifest.KIND_DB, file)
            }
        }
        BackupManifest.write(
            File(dir, BackupManifest.FILE_NAME),
            BackupManifest(entries = entries),
        )
    }

    @After
    fun tearDown() {
        dirs.forEach { it.deleteRecursively() }
    }

    @Test
    fun `legacy package only restores existing files`() {
        val dir = tempDir()
        File(dir, "bookSource.json").writeText("[]")

        val plan = RestorePlan.build(dir, null, RestorePolicy.Merge)

        assertTrue(plan.isLegacy)
        assertTrue(plan.has("bookSource.json"))
        assertFalse(plan.has("bookshelf.json"))
        assertFalse(plan.shouldReplace("bookshelf.json"))
    }

    @Test
    fun `merge never deletes existing entries`() {
        val dir = tempDir()
        File(dir, "bookshelf.json").writeText("[]")

        val plan = RestorePlan.build(dir, null, RestorePolicy.Merge)

        assertTrue(plan.has("bookshelf.json"))
        assertFalse(plan.shouldReplace("bookshelf.json"))
    }

    @Test
    fun `replace only applies to existing entries`() {
        val dir = tempDir()
        File(dir, "bookshelf.json").writeText("[]")

        val plan = RestorePlan.build(dir, null, RestorePolicy.Replace)

        assertTrue(plan.shouldReplace("bookshelf.json"))
        assertFalse(plan.shouldReplace("bookmark.json"))
    }

    @Test
    fun `manifest declared but missing file is not restorable`() {
        val dir = tempDir()
        val entry = BackupManifestEntry(
            name = "bookshelf.json",
            kind = BackupManifest.KIND_DB,
            size = 2,
            crc32 = 1,
        )
        BackupManifest.write(
            File(dir, BackupManifest.FILE_NAME),
            BackupManifest(entries = listOf(entry)),
        )

        val plan = RestorePlan.build(dir, null, RestorePolicy.Replace)

        assertFalse(plan.has("bookshelf.json"))
        assertFalse(plan.shouldReplace("bookshelf.json"))
    }

    @Test
    fun `file not declared in manifest is ignored`() {
        val dir = tempDir()
        File(dir, "bookshelf.json").writeText("[]")
        File(dir, "bookSource.json").writeText("[]")
        writeManifest(dir, "bookshelf.json")

        val plan = RestorePlan.build(dir, null, RestorePolicy.Replace)

        assertTrue(plan.has("bookshelf.json"))
        assertFalse(plan.has("bookSource.json"))
        assertFalse(plan.shouldReplace("bookSource.json"))
    }

    @Test
    fun `selection intersects package content`() {
        val dir = tempDir()
        File(dir, "bookshelf.json").writeText("[]")
        File(dir, "bookSource.json").writeText("[]")

        val plan = RestorePlan.build(
            dir,
            selected = setOf("bookSource.json", "notExists.json"),
            policy = RestorePolicy.Merge,
        )

        assertFalse(plan.has("bookshelf.json"))
        assertTrue(plan.has("bookSource.json"))
        assertFalse(plan.has("notExists.json"))
    }

    @Test
    fun `validate detects corrupted file`() {
        val dir = tempDir()
        val file = File(dir, "bookshelf.json")
        file.writeText("[]")
        writeManifest(dir, "bookshelf.json")
        val manifest = BackupManifest.read(File(dir, BackupManifest.FILE_NAME))!!

        assertTrue(BackupManifest.validate(dir, manifest).isEmpty())

        file.writeText("[1,2,3]")

        assertFalse(BackupManifest.validate(dir, manifest).isEmpty())
    }

    @Test
    fun `content signature changes when entry changes`() {
        val dir = tempDir()
        val file = File(dir, "bookshelf.json")
        file.writeText("[]")
        writeManifest(dir, "bookshelf.json")
        val first = BackupManifest.read(File(dir, BackupManifest.FILE_NAME))!!.contentSignature()

        file.writeText("[1,2,3]")
        writeManifest(dir, "bookshelf.json")
        val second = BackupManifest.read(File(dir, BackupManifest.FILE_NAME))!!.contentSignature()

        assertFalse(first == second)
    }
}
