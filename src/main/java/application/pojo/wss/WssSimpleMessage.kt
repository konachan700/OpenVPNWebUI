package application.pojo.wss

data class WssSimpleMessage(
        val cmd: String,
        val data: Map<String, String>
)