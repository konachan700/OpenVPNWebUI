package application.services

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileNotFoundException
import java.io.RandomAccessFile
import java.nio.file.Files

@Service
@ConfigurationProperties(prefix="settings")
open class FileService {
    companion object {
        private const val CONF_DIRECTORY_NAME = "conf"
        private const val SERVER_DIRECTORY_NAME = "server"
        private const val SERVER_DIRECTORY_LOG_NAME = "log"

        private const val SERVER_LOG_NAME = "openvpn.log"
        private const val SERVER_STAT_LOG_NAME = "openvpn-stat.log"

        private const val CA_KEY_FILE_NAME = "ca.key"
        private const val CA_CERT_FILE_NAME = "ca.crt"
        private const val CA_CRL_FILE_NAME = "ca.crl"

        private const val SERVER_KEY_FILE_NAME = "server.key"
        private const val SERVER_CERT_FILE_NAME = "server.crt"
        private const val SERVER_DH_PARAMS = "dh2048.pem"

        private const val SERVER_CONFIG = "server.conf"
        private const val CLIENT_CONFIG = "client.conf"

        private const val KV_STORE = "app.kv"
        private const val SQL_STORE = "app.h2sql"
    }

    lateinit var openvpnKeysPath : String

    private fun file(root: File, vararg path: String) : File {
        var file = root
        path.forEach {
            file = File(file, it)
        }
        return file
    }

    private fun dir(root: File, vararg path: String) : File {
        var file = root
        path.forEach {
            file = File(file, it)
        }
        if (!file.exists()) {
            if (!file.mkdirs()) {
                throw FileNotFoundException("Directory ${file.absolutePath} not exist and cannot be created")
            }
        }
        return file
    }

    fun getRootDirectory(): File = File(openvpnKeysPath).absoluteFile
    private fun getConfDirectory() = dir(getRootDirectory(), CONF_DIRECTORY_NAME)
    private fun getServerDirectory() = dir(getRootDirectory(), SERVER_DIRECTORY_NAME)
    private fun getServerLogDirectory() = dir(getRootDirectory(), SERVER_DIRECTORY_LOG_NAME)

    private fun getCAFile(name : String) = file(getServerDirectory(), name)
    private fun getConfFile(name : String) = file(getConfDirectory(), name)
    private fun getServerFile(name : String) = file(getServerDirectory(), name)
    private fun getServerLogFile(name : String) = file(getServerLogDirectory(), name)

    fun getClientKey(name : String) = file(getRootDirectory(), "$name.key")
    fun getClientCert(name : String) = file(getRootDirectory(), "$name.crt")

    fun getServerKey() = getServerFile(SERVER_KEY_FILE_NAME)
    fun getServerCert() = getServerFile(SERVER_CERT_FILE_NAME)
    fun getServerDH() = getServerFile(SERVER_DH_PARAMS)

    fun getCAKey() = getCAFile(CA_KEY_FILE_NAME)
    fun getCACert() = getCAFile(CA_CERT_FILE_NAME)
    fun getCACRL() = getCAFile(CA_CRL_FILE_NAME)

    fun getClientConfig() = getConfFile(CLIENT_CONFIG)
    fun getServerConfig() = getConfFile(SERVER_CONFIG)
    fun getKVStore() = getConfFile(KV_STORE)
    fun getSQLStore() = getConfFile(SQL_STORE)

    fun getServerLogFile() = getServerLogFile(SERVER_LOG_NAME)
    fun getServerStatLogFile() = getServerLogFile(SERVER_STAT_LOG_NAME)

    fun readWith64kLimit(file : File) : List<String> {
        return if (file.length() > (64 * 1024)) {
            readLastLines(file, 200);
        } else {
            Files.readAllLines(file.toPath())
        }
    }

    fun readLastLines(file : File, line : Int) : List<String> {
        val buf = StringBuilder()
        val lines = ArrayList<String>()
        var counter = 0

        RandomAccessFile(file, "r").use {
            val fSize = file.length() - 1
            for (seek in fSize downTo 0 step 1) {
                it.seek(seek)
                val c = it.read().toChar()
                if (c == '\n' || seek == 0L) {
                    buf.reverse()
                    lines.add(buf.toString())
                    buf.delete(0, buf.length)

                    counter++
                    if (counter == line) break
                } else {
                    buf.append(c)
                }
            }
        }
        lines.reverse()
        return lines
    }
}