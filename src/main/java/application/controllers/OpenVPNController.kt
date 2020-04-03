package application.controllers

import application.services.FileService
import application.services.LinuxProcessService
import application.services.OpenVPNService
import application.utils.ZipUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.io.ByteArrayInputStream
import java.util.*

@RestController
@RequestMapping(
        value = ["/openvpn"],
        consumes = [ MediaType.APPLICATION_JSON_VALUE ],
        produces = [ MediaType.APPLICATION_JSON_VALUE ]
)
@ConfigurationProperties(prefix="settings")
open class OpenVPNController {
    @Autowired
    lateinit var openVPNService: OpenVPNService

    @Autowired
    lateinit var zipUtils: ZipUtils

    @Autowired
    lateinit var linuxProcessService: LinuxProcessService

    @Autowired
    lateinit var fileService: FileService

    lateinit var openVpnBinary : String

    @RequestMapping(
            value = ["/version"],
            method = [RequestMethod.GET]
    )
    fun ovpnversion() : Any = Collections.singletonMap("data",
            linuxProcessService
                    .launchForResult(openVpnBinary, "--version").joinToString("\r\n") { it.trim() })

    @RequestMapping(
            value = ["/log64k"],
            method = [RequestMethod.GET]
    )
    fun readLogs() : Any = Collections.singletonMap("data",
            fileService.readWith64kLimit(fileService.getServerLogFile()).joinToString("\r\n"))

    @RequestMapping(
            value = ["/config"],
            method = [RequestMethod.GET]
    )
    fun readConfigFile() : Any = openVPNService.readConfig()

    @RequestMapping(
            value = ["/config"],
            method = [RequestMethod.POST]
    )
    fun saveConfigFile(@RequestBody data : Map<String, String>) : Any =
            openVPNService.saveConfig(data["server"] ?: "", data["client"] ?: "")

    @RequestMapping(
            value = ["/settings"],
            method = [RequestMethod.GET]
    )
    fun certSettings() : Any = openVPNService.getCertSettings()

    @RequestMapping(
            value = ["/settings"],
            method = [RequestMethod.POST]
    )
    fun saveCertSettings(@RequestBody data : Map<String, String>) : Any = openVPNService.saveCertSettings(data)

    @RequestMapping(
            value = ["/createcert"],
            method = [RequestMethod.POST]
    )
    fun certSettings(@RequestBody data : Map<String, String>) : Any =
            openVPNService.createCertificate(data["name"] ?: "cert_${System.currentTimeMillis()}")

    @RequestMapping(
            value = ["/allcert"],
            method = [RequestMethod.GET]
    )
    fun certList() : Any = openVPNService.getUserCertsList()

    @RequestMapping(
            value = ["/ready"],
            method = [RequestMethod.GET]
    )
    fun isReady() : Any = openVPNService.isReady()

    @RequestMapping(
            value = ["/run"],
            method = [RequestMethod.POST]
    )
    fun runOpenVPN() : Any = openVPNService.runOpenVPN()

    @RequestMapping(
            value = ["/stop"],
            method = [RequestMethod.POST]
    )
    fun stopOpenVPN() : Any = openVPNService.stopOpenVPN()

    @RequestMapping(
            value = ["/certdl"],
            method = [RequestMethod.GET],
            consumes = [ MediaType.ALL_VALUE ]
    )
    fun downloadZip(@RequestParam type: String, @RequestParam file : String) : ResponseEntity<Resource> {
        var size = 0;
        val res = when(type) {
            "zip" -> {
                val list = openVPNService.getClientFiles(file)
                val zip = zipUtils.zip(list)
                size = zip.size
                InputStreamResource(ByteArrayInputStream(zip))
            }
            "ovpn" -> {
                val data = openVPNService.getOVPNFile(file)
                size = data.size
                InputStreamResource(ByteArrayInputStream(data))
            }
            else -> throw IllegalArgumentException("This type not exist")
        }
        val headers = HttpHeaders()
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=$file.$type");
        headers.add("Cache-Control", "no-cache, no-store, must-revalidate")
        headers.add("Pragma", "no-cache")
        headers.add("Expires", "0")
        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(size.toLong())
                .contentType(MediaType.parseMediaType("application/octet-stream"))
                .body(res);
    }
}