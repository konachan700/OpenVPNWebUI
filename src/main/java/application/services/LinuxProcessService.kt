package application.services

import org.springframework.stereotype.Service
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.stream.Collectors

@Service
open class LinuxProcessService {
    data class Proc(
            val thread : Thread,
            val name : String,
            var proc: Process? = null,
            val log : StringBuffer = StringBuffer(),
            val active : AtomicBoolean = AtomicBoolean(false)
    )
    private val processes = ConcurrentHashMap<String, Proc>()

    fun launchForResult(vararg cmd : String) : List<String> {
        val cmdList = cmd.toMutableList()
        val proc = ProcessBuilder().command(cmdList).start()
        proc.waitFor()
        proc.inputStream.use {
            val bufReader = BufferedReader(InputStreamReader(it))
            return bufReader.lines().collect(Collectors.toList())
        }
    }

    fun getLongTimeProcess(name : String) : Proc? {
        return processes[name]
    }

    /** WARNING!
     *  This code require this /etc/sudoers lines:
     *      user_name ALL=(ALL) NOPASSWD: /usr/sbin/openvpn
     *      user_name ALL=(ALL) NOPASSWD: /usr/bin/killall sudo
     *  Or you can run it as root.
     * */

    fun forgetLongTimeProcess(name : String) {
        //val log = launchForResult("sudo", "killall", "sudo").joinToString("\n")
        //println("LOG forgetLongTimeProcess: $log")
        processes[name]!!.proc!!.destroyForcibly().waitFor()
        processes.remove(name)
    }

    @Synchronized
    fun launchLongTimeProcess(name : String, vararg cmd : String) : Boolean {
        val lock = CountDownLatch(1)
        if (processes.containsKey(name)) {
            return false
        }
        val thread = Thread({
            val cmdList = cmd.toMutableList()
            val proc = ProcessBuilder().command(cmdList).start()
            processes[name]!!.active.set(true)
            processes[name]!!.proc = proc
            Thread.sleep(1500)
            if (proc.isAlive) {
                proc.waitFor()
                processes[name]!!.active.set(false)
            } else {

            }


//            proc.errorStream.bufferedReader().use { input ->
//                proc.inputStream.bufferedReader().use { error ->
//                    processes[name]!!.active.set(true)
//                    try {
//                        while(true) {
//                            val inputLine = input.readLine()
//                            if (inputLine != null) {
//                                processes[name]!!.log.append(inputLine)
//                                println("LOG_O: $inputLine")
//                            }
//                            Thread.yield()
//
//                            val errLine = error.readLine()
//                            if (errLine != null) {
//                                processes[name]!!.log.append(errLine)
//                                println("LOG_E: $errLine")
//                            }
//                            Thread.yield()
//
//                            if (!proc.isAlive) break
//                        }
//                    } catch (e : Exception) {
//                        processes[name]!!.active.set(false)
//                        println("LOG_ERROR: ${e.message}")
//                    }
//                }
//            }
        }, "ProcessThread-$name")

        val proc = Proc(thread = thread, name = name);
        processes[name] = proc
        processes[name]!!.thread.start()
        lock.await()

        return true
    }

}