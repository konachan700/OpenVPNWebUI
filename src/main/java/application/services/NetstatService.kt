package application.services

import application.pojo.NetDevShortInfo
import application.pojo.NetstatShortInfo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File
import java.nio.file.Files
import java.util.*

@Service
open class NetstatService {

    @Autowired
    lateinit var linuxProcessService: LinuxProcessService

    fun shortNetInfo() : List<NetDevShortInfo> {
        val list = ArrayList<String>()
        list.addAll(Files.readAllLines(File("/proc/net/dev").toPath()))
        list.removeAt(0)
        list.removeAt(0)
        return list
                .map { it.trim().replace(':', ' ').replace("\\s+".toRegex(), " ") }
                .map { it.split(" ") }
                .map { NetDevShortInfo(it[0], it[1], it[2], it[3], it[9], it[10], it[11]) }
                .toList()
    }

    fun tupln(name : String) : Any {
        val listOut = linuxProcessService.launchForResult("ss", "-tuplnH")
        val list = listOut
                .asSequence()
                .map { it.replace("\\s+".toRegex(), " ") }
                .map { it.split("\\s".toRegex()) }
                .filter { it.size > 4 }
                .filter { it[0].trim().toLowerCase().contentEquals(name) }
                .filter { it[1].toUpperCase().contentEquals("LISTEN") ||
                        it[1].toUpperCase().contentEquals("UNCONN") }
                .map { parseLine(it) }
                .toList()
                .sortedBy { it.ip }
        return Collections.singletonMap("data", list)
    }

    private fun parseLine(line : List<String>) : NetstatShortInfo {
        val portData = line[4]
        val portPos = portData.lastIndexOf(':')
        val port = portData.substring(portPos + 1)
        val ip = portData.substring(0, portPos)
                .replace('[', ' ')
                .replace(']', ' ')
                .trim()
        return NetstatShortInfo(ip, port, line[0])
    }
}