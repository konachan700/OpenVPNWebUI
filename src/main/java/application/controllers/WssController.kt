package application.controllers

import application.pojo.WssCommands
import application.pojo.wss.WssSimpleMessage
import application.pojo.wss.WssSimpleRequest
import application.services.FileService
import application.services.OpenVPNService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.stereotype.Controller
import java.util.*

@Controller
open class WssController {
    @Autowired
    lateinit var fs: FileService

    @Autowired
    lateinit var openVPNService: OpenVPNService

    @MessageMapping("/general")
    @SendTo("/topic/general")
    open fun greeting(message: WssSimpleMessage): WssSimpleRequest {
        return when (message.cmd) {
            WssCommands.LOGS.cmd              -> WssSimpleRequest(message.cmd,
                    fs.readLastLines(fs.getServerLogFile(), 200).joinToString("\r\n"))
            WssCommands.IS_OVPN_RUN.cmd       -> WssSimpleRequest(message.cmd, openVPNService.checkOpenVPNState())
            else                              -> WssSimpleRequest(message.cmd, Collections.singletonList("ERROR"))

        }
    }
}