package com.appboosty.premiumhelper.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object Zip {

    suspend fun zipFiles(toFile: String, files: List<String>) = withContext(Dispatchers.IO) {

        ZipOutputStream(FileOutputStream(toFile)).use { zos ->

            val buffer = ByteArray(8192)

            files.forEach { path ->

                FileInputStream(path).use { fis ->

                    val zipEntry = ZipEntry(path.substring(path.lastIndexOf("/") + 1))
                    zos.putNextEntry(zipEntry)

                    var read: Int
                    while (-1 != fis.read(buffer).also { read = it }) {
                        zos.write(buffer, 0, read)
                    }

                    zos.flush()
                    zos.closeEntry()

                }

            }

            zos.finish()
            zos.flush()
            zos.close()
        }
    }

}