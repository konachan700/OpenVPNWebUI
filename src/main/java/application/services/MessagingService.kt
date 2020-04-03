package application.services

import application.pojo.WssCommands
import application.pojo.wss.WssSimpleRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service

@Service
open class MessagingService {
    @Autowired
    lateinit var msg : SimpMessagingTemplate

    fun sendToGeneralTopic(cmd: WssCommands, data: Any) {
        msg.convertAndSend("/topic/general", WssSimpleRequest(cmd.cmd, data))
    }
}