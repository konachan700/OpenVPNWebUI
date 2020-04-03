package application.pojo

data class NetDevShortInfo(
        val iface: String,
        val inBytes: String,
        val inPackets: String,
        val inErrors: String,
        val outBytes: String,
        val outPackets: String,
        val outErrors: String
)