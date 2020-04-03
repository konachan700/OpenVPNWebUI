package application.pojo

data class OpenVPNCertificate (
        val issuer: String,
        val subject: String,
        val validTo: String,
        val validFrom: String,
        val fileName: String
)