package application.services

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
open class LogService {
    @Autowired
    lateinit var msg : MessagingService

    val thread = Thread({
        while (true) {
            Thread.sleep(1)

        }
    },"LogService")
}