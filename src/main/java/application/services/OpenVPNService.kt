package application.services

import application.pojo.OpenVPNCertificate
import application.pojo.WssCommands
import application.utils.X509Utils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.annotation.PostConstruct
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

@Service
@ConfigurationProperties(prefix="settings")
open class OpenVPNService {
    companion object {
        const val AUTOSTART_CONFIG = "bool_is_server_run_on_app_start"
    }

    @Autowired
    lateinit var msg : MessagingService

    lateinit var openVpnBinary : String

    open inner class OpenVPNRunnable : Runnable {
        private val active = AtomicBoolean(false)
        private val running = AtomicBoolean(true)
        private val process = AtomicReference<Process>()
        override fun run() {
            val cmd = listOf(openVpnBinary,
                    "--config", fs.getServerConfig().absolutePath,
                    "--management", "127.0.0.1", "37999"
            )
            while (running.get()) {
                try {
                    val proc = ProcessBuilder().command(cmd).start()
                    process.set(proc)
                    active.set(true)
                    msg.sendToGeneralTopic(WssCommands.IS_OVPN_RUN, true)
                    proc.waitFor()
                    msg.sendToGeneralTopic(WssCommands.IS_OVPN_RUN, false)
                    active.set(false)
                } catch (t : Throwable) {
                    msg.sendToGeneralTopic(WssCommands.IS_OVPN_RUN, false)
                    println("Error: ${t.message}")
                } finally {
                    Thread.sleep(5000)
                }
            }
        }
        fun stop() {
            try {
                running.set(false)
                process.get()?.destroyForcibly()?.waitFor()
                process.set(null)
            } catch (t : Throwable) {
                println("Error: ${t.message}")
            }
        }
        fun isRunning() : Boolean = active.get()
    }

    private var openVpnThread : Thread? = null
    private var openVpnRunnable : OpenVPNRunnable? = null

    private val formatter = SimpleDateFormat("dd-MM-yyyy HH:mm:ss")
    private val isDHPresent = AtomicBoolean(false)

    @Autowired
    lateinit var linuxProcessService: LinuxProcessService

    @Autowired
    lateinit var config : ConfigService

    @Autowired
    lateinit var x509Utils: X509Utils

    @Autowired
    lateinit var fs: FileService

    private fun serverStart() {
        if (openVpnRunnable != null) {
            openVpnRunnable!!.stop()
            openVpnRunnable = null
        }
        openVpnRunnable = OpenVPNRunnable()
        openVpnThread = Thread(openVpnRunnable, "OpenVPNThread_${System.currentTimeMillis()}")
        openVpnThread!!.start()
    }

    @PostConstruct
    fun init() {
        prepareServer()
        val isStartOnInit = config.getConfig(AUTOSTART_CONFIG, "false").contentEquals("true")
        if (isStartOnInit) {
            serverStart()
        }
    }

    fun getUserCertsList() : Any {
        val dir = fs.getRootDirectory()
        if (!dir.isDirectory) {
            return Collections.singletonMap("data", ArrayList<Any>())
        }
        if (!dir.exists()) {
            return Collections.singletonMap("data", ArrayList<Any>())
        }
        val list =  dir.listFiles()!!
                .filter { it.name.endsWith(".crt") }
                .filter { it.isFile && it.canRead() }
                .map {
                    val fact = CertificateFactory.getInstance("X.509")
                    val cert = FileInputStream(it).use { stream ->
                        fact.generateCertificate(stream) as X509Certificate
                    }
                    OpenVPNCertificate(
                            cert.issuerDN.name,
                            cert.subjectDN.name,
                            formatter.format(cert.notAfter),
                            formatter.format(cert.notBefore),
                            it.nameWithoutExtension)
                }
        return Collections.singletonMap("data", list)
    }

    fun getCertSettings() : Any {
        return Collections.singletonMap("data", config.getAll())
    }

    fun saveCertSettings(data : Map<String, String>) : Any {
        val dataOld = config.getAll()
        config.saveAll(data)
        if (data.keys
                        .filter { it.startsWith("s_issuer_") }
                        .filter { data[it]?.contentEquals(dataOld[it] ?: "undefined") == false }
                        .count() != 0) {
            x509Utils.deleteIfIssuerChanged()
        }
        return Collections.singletonMap("data", "OK")
    }

    fun createCertificate(certname: String) : Any {
        x509Utils.generateCert(certname)
        return Collections.singletonMap("data", "OK")
    }

    private fun prepareServer() : Any {
        x509Utils.generateCA()
        x509Utils.generateCrl()
        Thread({
            println("Start generating Diffie-Hellman (DH) parameters...")
            val timeStart = Date().time

            x509Utils.generateDH()

            val time = Date().time - timeStart
            println("Generating Diffie-Hellman (DH) parameters finished in ${time}ms")
            isDHPresent.set(true)
        }, "DH Generator").start()
        x509Utils.generateServerCert()
        return Collections.singletonMap("data", "OK")
    }

    fun isCAExist() : Any {
        return Collections.singletonMap("data",
                if (x509Utils.isCAExist()) "YES" else "NO")
    }

    fun isDHExist() : Any {
        return Collections.singletonMap("data",
                if (isDHPresent.get()) "YES" else "NO")
    }

    fun isReady() : Any {
        return Collections.singletonMap("data",
                if (isDHPresent.get() && x509Utils.isCAExist()) "YES" else "NO")
    }

    fun getClientFiles(name : String) : List<File> {
        val list = ArrayList<File>()
        list.add(fs.getCACert())
        list.add(fs.getClientConfig())
        list.add(fs.getClientCert(name))
        list.add(fs.getClientKey(name))
        return list
    }

    fun readConfig() : Any {
        val map = HashMap<String, String>()
        map["server"] = if (fs.getServerConfig().exists()) {
            Base64.getEncoder().encodeToString(Files.readAllBytes(fs.getServerConfig().toPath()))
        } else {
            val cls = ClassPathResource("example/server.conf")
            Base64.getEncoder().encodeToString(cls.inputStream.readBytes())
        }
        map["client"] = if (fs.getClientConfig().exists()) {
            Base64.getEncoder().encodeToString(Files.readAllBytes(fs.getClientConfig().toPath()))
        } else {
            val cls = ClassPathResource("example/client.conf")
            Base64.getEncoder().encodeToString(cls.inputStream.readBytes())
        }
        return Collections.singletonMap("data", map)
    }

    fun saveConfig(server: String, client: String) : Any {
        val serverConf = String(Base64.getDecoder().decode(server))
                .split("\n")
                .asSequence()
                .filter { it.isNotBlank() }
                .map { it.replace("\\s+".toRegex(), " ") }
                .map { line ->
                    val arr = line.split("\\s+".toRegex(), 2)
                    if (arr.size == 2) {
                        when (arr[0]) {
                            "ca" -> "ca " + fs.getCACert().absolutePath
                            "cert" -> "cert " + fs.getServerCert().absolutePath
                            "key" -> "key " + fs.getServerKey().absolutePath
                            "dh" -> "dh " + fs.getServerDH().absolutePath
                            "crl-verify" -> "crl-verify " + fs.getCACRL().absolutePath
                            "log" -> "log " + fs.getServerLogFile().absolutePath
                            "log-append" -> "log-append " + fs.getServerLogFile().absolutePath
                            "status" -> "status " + fs.getServerStatLogFile().absolutePath
                            else -> arr[0] + " " + arr[1]
                        }
                    } else  {
                        arr[0]
                    }
                }
                .toList()
                .joinToString("\n")
                .toByteArray()
        Files.write(fs.getClientConfig().toPath(), Base64.getDecoder().decode(client), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)
        Files.write(fs.getServerConfig().toPath(), serverConf, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)
        return Collections.singletonMap("data", "OK")
    }

    private fun notIn(data: String, vararg s : String) : Boolean {
        return s.none { data.startsWith(it) }
    }

    fun getOVPNFile(name: String) : ByteArray {
        val ca = String(Files.readAllBytes(fs.getCACert().toPath()))
        val clientCert = String(Files.readAllBytes(fs.getClientCert(name).toPath()))
        val clientKey = String(Files.readAllBytes(fs.getClientKey(name).toPath()))
        val clientConfig = String(Files.readAllBytes(fs.getClientConfig().toPath()))

        val clientConfData = clientConfig.replace("\r", "").split("\n")
                .filter { notIn(it, "ca", "cert", "key") }
                .joinToString("\n")

        val sb = StringBuilder()
        sb.append(clientConfData)
                .append("\n<ca>\n")
                .append(ca)
                .append("</ca>\n<cert>\n")
                .append(clientCert)
                .append("</cert>\n<key>\n")
                .append(clientKey)
                .append("</key>\n")
        return sb.toString().toByteArray()
    }

    fun runOpenVPN() : Any {
        return if (openVpnRunnable?.isRunning() == true) {
            Collections.singletonMap("data", "AR")
        } else {
            serverStart()
            config.save(AUTOSTART_CONFIG, "true")
            Collections.singletonMap("data", "OK")
        }
    }

    fun stopOpenVPN() : Any {
        return if (openVpnRunnable?.isRunning() == true) {
            openVpnRunnable!!.stop()
            openVpnRunnable = null
            config.save(AUTOSTART_CONFIG, "false")
            Collections.singletonMap("data", "OK")
        } else {
            Collections.singletonMap("data", "ERR")
        }
    }

    fun checkOpenVPNState() : Boolean {
        return openVpnRunnable?.isRunning() ?: false
    }
}
