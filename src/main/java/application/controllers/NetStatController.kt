package application.controllers

import application.services.LinuxProcessService
import application.services.NetstatService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(
        value = ["/netstat"],
        produces = ["application/json"]
)
open class NetStatController {
    @Autowired
    lateinit var netstatService: NetstatService

    @RequestMapping(
            value = ["/tupln"],
            method = [RequestMethod.GET])
    fun tupln(@RequestParam name : String) : Any = netstatService.tupln(name)

    @RequestMapping(
            value = ["/netdev"],
            method = [RequestMethod.GET])
    fun netdev() : Any = netstatService.shortNetInfo()

}