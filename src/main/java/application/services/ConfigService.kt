package application.services

import org.h2.mvstore.MVMap
import org.h2.mvstore.MVStore
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.PropertySource
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

@Service
@PropertySource("/config.properties")
open class ConfigService : CommandLineRunner {
    @Autowired
    lateinit var fs : FileService

    private lateinit var mvStore : MVStore
    private lateinit var settingsMap : MVMap<String, String>

    @PostConstruct
    fun init() {
        mvStore = MVStore.Builder()
                .fileName(fs.getKVStore().absolutePath)
                .encryptionKey("1234567890".toCharArray())
                .autoCommitDisabled()
                .cacheSize(64)
                .open()
        settingsMap = mvStore.openMap("cache-local")
    }

    fun getConfig(name: String, default: String) : String {
        return if (settingsMap.containsKey(name))
            if (settingsMap[name] == null) {
                default
            } else {
                val str = settingsMap[name]!!.toLowerCase()
                if (str.trim().isEmpty() ||
                        str.contains("undefined") ||
                        str.contains("nan") ||
                        str.contains("null")) {
                    default
                } else {
                    str
                }
            }
        else
            default
    }

    fun getAll() : Map<String, String> {
        val map = HashMap<String, String>()
        map.putAll(settingsMap);
        return map;
    }

    fun save(key: String, value: String) {
        settingsMap[key] = value
        mvStore.commit()
    }

    fun saveAll(data : Map<String, String>) {
        data.forEach { (k, v) ->
            settingsMap[k] = v
        }
        mvStore.commit()
        mvStore.compactRewriteFully()
    }

    override fun run(vararg args: String?) {}

    @PreDestroy
    fun onExit() {
        mvStore.commit()
        mvStore.compactRewriteFully()
        mvStore.close()
        println("!!! mvStore has been closed")
    }
}