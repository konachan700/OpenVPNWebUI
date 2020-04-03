package application.utils

import org.springframework.stereotype.Component
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


@Component
open class ZipUtils {
    fun zip(srcFiles: List<File>) : ByteArray {
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zios ->
            srcFiles
                    .filter { it.exists() && it.isFile && it.canRead() }
                    .forEach {
                        val zipEntry = ZipEntry(it.name)
                        zios.putNextEntry(zipEntry)
                        val bytesOfFile = Files.readAllBytes(it.toPath())
                        zios.write(bytesOfFile)
                    }

        }
        return bytes.toByteArray()
    }
}