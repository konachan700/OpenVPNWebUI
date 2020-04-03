package application.utils

import application.services.ConfigService
import application.services.FileService
import org.bouncycastle.asn1.ASN1ObjectIdentifier
import org.bouncycastle.asn1.DERSequence
import org.bouncycastle.asn1.DERUTF8String
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.*
import org.bouncycastle.cert.X509v2CRLBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.util.io.pem.PemObject
import org.bouncycastle.util.io.pem.PemReader
import org.bouncycastle.util.io.pem.PemWriter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.io.*
import java.math.BigInteger
import java.nio.file.Files
import java.security.*
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*


@Component
open class X509Utils {
    @Autowired
    lateinit var config : ConfigService

    @Autowired
    lateinit var fs: FileService

    private fun genKeyPair() : KeyPair {
        val sr = SecureRandom()
        val keyGen = KeyPairGenerator.getInstance("RSA")
        keyGen.initialize(2048, sr);
        return keyGen.generateKeyPair()
    }

    private fun writePEMObject(obj : Any, file : File) {
        StringWriter().use { str ->
            JcaPEMWriter(str).use { pemWriter ->
                pemWriter.writeObject(obj)
                pemWriter.close()
            }
            Files.write(file.toPath(), str.buffer.substring(0).toByteArray())
        }
    }

    fun readPEMPrivateKey(file : File) : PrivateKey {
        val keyStr = String(Files.readAllBytes(file.toPath()))
        StringReader(keyStr).use {stringReader ->
            PemReader(stringReader).use {pemReader ->
                val obj = pemReader.readPemObject()
                val keySpec = PKCS8EncodedKeySpec(obj.content)
                val keyFactory = KeyFactory.getInstance("RSA")
                return keyFactory.generatePrivate(keySpec)
            }
        }
    }

    fun isCAExist() : Boolean {
        return (fs.getCACert().exists() && fs.getCAKey().exists() &&
                fs.getCACert().length() > 32 && fs.getCAKey().length() > 32)
    }

    private fun genX500NameForCA() : X500Name {
        return X500Name(
                "EMAILADDRESS=${config.getConfig("s_issuer_email", "medved@example.com")}, " +
                        "OID.2.5.4.41=${config.getConfig("s_issuer_cn", "Ivan Ivanov")}, " +
                        "CN=${config.getConfig("s_issuer_cn", "Ivan Ivanov")}, " +
                        "OU=${config.getConfig("s_issuer_ou", "Department of drinking vodka")}, " +
                        "O=${config.getConfig("s_issuer_o", "Balalayka LTD")}, " +
                        "L=${config.getConfig("s_issuer_l", "Moscow")}, " +
                        "ST=${config.getConfig("s_issuer_s", "MOS")}, " +
                        "C=${config.getConfig("s_issuer_c", "RU")}")
    }

    private fun saveCert(file : File, issuer: X500Name, owner: X500Name, ownPubKey: PublicKey, caKey: PrivateKey) {
        saveCert(file, issuer, owner, ownPubKey, caKey) {}
    }

    private fun saveCert(file : File, issuer: X500Name, owner: X500Name, ownPubKey: PublicKey, caKey: PrivateKey, ext : (JcaX509v3CertificateBuilder) -> Unit) {
        val certNewBuilder = JcaX509v3CertificateBuilder(issuer, BigInteger(64, SecureRandom()),
                Date(), Date(System.currentTimeMillis() + (5L * 365L * 24L * 60L * 60L * 1000L)), owner, ownPubKey)
        val signer = JcaContentSignerBuilder("SHA256withRSA").build(caKey);

        ext.invoke(certNewBuilder)

        val certHolder = certNewBuilder.build(signer)
        val cert = JcaX509CertificateConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME).getCertificate(certHolder);
        writePEMObject(cert, file)
    }

    fun generateServerCert() {
        generateCertInt(fs.getServerKey(), fs.getServerCert())
    }

    fun generateCert(name : String) {
        generateCertInt(fs.getClientKey(name), fs.getClientCert(name))
    }

    private fun generateCertInt(key: File, cert: File) {
        if (key.exists() && cert.exists()) {
            println("generateCert: certificate ${cert.name} already exist")
            return
        }

        val caKey = readPEMPrivateKey(fs.getCAKey())

        val kp = genKeyPair()
        writePEMObject(kp.private, key)

        val certFactory = CertificateFactory.getInstance("X.509")
        val certx = certFactory.generateCertificate(ByteArrayInputStream(Files.readAllBytes(fs.getCACert().toPath()))) as X509Certificate
        val issuer = JcaX509CertificateHolder(certx).subject
        val owner = X500Name(
                "EMAILADDRESS=${config.getConfig("s_subject_email", "medved@example.com")}, " +
                        "OID.2.5.4.41=${config.getConfig("s_issuer_cn", "Ivan Ivanov")}, " +
                        "CN=${config.getConfig("s_subject_cn", "Ivan Ivanov")}, " +
                        "OU=${config.getConfig("s_subject_ou", "Department of drinking vodka")}, " +
                        "O=${config.getConfig("s_subject_o", "Balalayka LTD")}, " +
                        "L=${config.getConfig("s_subject_l", "Moscow")}, " +
                        "ST=${config.getConfig("s_subject_s", "MOS")}, " +
                        "C=${config.getConfig("s_subject_c", "RU")}")
        saveCert(cert, issuer, owner, kp.public, caKey) { certX ->
            certX.addExtension(
                    Extension.subjectKeyIdentifier,
                    false,
                    JcaX509ExtensionUtils().createSubjectKeyIdentifier(kp.public));

            certX.addExtension(
                    Extension.authorityKeyIdentifier,
                    false,
                    JcaX509ExtensionUtils().createAuthorityKeyIdentifier(certx.publicKey));

            certX.addExtension(
                    Extension.extendedKeyUsage,
                    false,
                    ExtendedKeyUsage(arrayOf(KeyPurposeId.id_kp_clientAuth, KeyPurposeId.id_kp_serverAuth)));

            certX.addExtension(Extension.basicConstraints, false, BasicConstraints(false))
            certX.addExtension(Extension.keyUsage,
                    true,
                    KeyUsage(KeyUsage.digitalSignature))

            certX.addExtension(Extension.subjectAlternativeName, false, genDNSASN(config.getConfig("s_subject_cn", "Ivan Ivanov")))
        }
    }

    fun deleteIfIssuerChanged() {
        fs.getCAKey().delete()
        fs.getCACert().delete()
        fs.getServerKey().delete()
        fs.getServerCert().delete()

        generateCA()
        generateServerCert()

        fs.getRootDirectory().listFiles()!!
                .filter { it.isFile }
                .filter { it.name.toLowerCase().endsWith(".key") || it.name.toLowerCase().endsWith(".crt") }
                .forEach { it.delete() }
    }

    fun generateCA() {
        if (isCAExist()) {
            return
        }

        val kp = genKeyPair()
        writePEMObject(kp.private, fs.getCAKey())

        val ownName = genX500NameForCA()
        saveCert(fs.getCACert(), ownName, ownName, kp.public, kp.private) { cert ->
            cert.addExtension(
                    Extension.subjectKeyIdentifier,
                    false,
                    JcaX509ExtensionUtils().createSubjectKeyIdentifier(kp.public));

            cert.addExtension(
                    Extension.authorityKeyIdentifier,
                    false,
                    JcaX509ExtensionUtils().createAuthorityKeyIdentifier(kp.public));

            cert.addExtension(
                    Extension.extendedKeyUsage,
                    false,
                    ExtendedKeyUsage(arrayOf(KeyPurposeId.id_kp_clientAuth, KeyPurposeId.id_kp_serverAuth)));

            cert.addExtension(Extension.basicConstraints, false, BasicConstraints(true))
            cert.addExtension(Extension.keyUsage,
                    true,
                    KeyUsage(KeyUsage.digitalSignature or KeyUsage.keyEncipherment or KeyUsage.dataEncipherment
                        or KeyUsage.keyAgreement or KeyUsage.nonRepudiation or KeyUsage.cRLSign or KeyUsage.keyCertSign))

            cert.addExtension(Extension.subjectAlternativeName, false, genDNSASN(config.getConfig("s_issuer_cn", "Ivan Ivanov")))
        }
    }

    private fun genDNSASN(name : String) : GeneralNames {
        val DNS_SRV_OID = ASN1ObjectIdentifier( "1.3.6.1.5.5.7.8.7" )
        val otherName = DERSequence(arrayOf(DNS_SRV_OID, DERUTF8String(name)))
        return GeneralNames(GeneralName(GeneralName.dNSName, otherName))
    }

    fun generateDH() {
        if (fs.getServerDH().exists()) {
            println("generateDH: dhPEMFile already exist")
            return
        }

        val apg = AlgorithmParameterGenerator.getInstance("DH")
        apg.init(2048, SecureRandom())
        val params = apg.generateParameters().encoded
        FileWriter(fs.getServerDH()).use { fileWriter ->
            PemWriter(fileWriter).use { pemWriter ->
                pemWriter.writeObject(PemObject("DH PARAMETERS", params))
            }
        }
    }

    fun generateCrl() {
        if (fs.getCACRL().exists()) {
            return
        }

        val certFactory = CertificateFactory.getInstance("X.509")
        val cert = certFactory.generateCertificate(ByteArrayInputStream(Files.readAllBytes(fs.getCACert().toPath()))) as X509Certificate
        val x500name = JcaX509CertificateHolder(cert).subject

        val crl = X509v2CRLBuilder(x500name, cert.notBefore)
        crl.setNextUpdate(cert.notAfter)
        crl.addCRLEntry(BigInteger.ONE, Date(), CRLReason.privilegeWithdrawn)

        val pk = readPEMPrivateKey(fs.getCAKey())

        val sigGen = JcaContentSignerBuilder("SHA1withRSA").setProvider("BC").build(pk);
        val crldata = crl.build(sigGen);

        FileWriter(fs.getCACRL()).use { fileWriter ->
            PemWriter(fileWriter).use { pemWriter ->
                pemWriter.writeObject(PemObject("X509 CRL", crldata.encoded))
            }
        }
    }
}