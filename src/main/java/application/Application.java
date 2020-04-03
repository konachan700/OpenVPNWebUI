package application;

import org.apache.commons.io.IOUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.Security;
import java.util.Arrays;

@SpringBootApplication
public class Application extends SpringBootServletInitializer {
    public static void main(String[] args) {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

        final File tsPath = new File("./truststore.jks").getAbsoluteFile();
        if (!tsPath.exists()) {
            try (InputStream storeStream = Application.class.getResourceAsStream("/keystore/keystore.jks");
                 FileOutputStream fos = new FileOutputStream(tsPath)) {
                IOUtils.copy(storeStream, fos);
                System.out.println("Keystore created: " + tsPath.getAbsolutePath());
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        } else {
            System.out.println("Keystore already existed: " + tsPath.getAbsolutePath());
        }

        //System.setProperty("javax.net.debug", "all");
        System.setProperty("javax.net.ssl.trustStoreType", "JKS");
        System.setProperty("javax.net.ssl.trustStore", tsPath.getAbsolutePath());
        System.setProperty("javax.net.ssl.trustStorePassword", "1234567890");

        SpringApplication.run(Application.class, Arrays.copyOf(args, args.length));
    }

    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        System.setProperty("javax.xml.bind.JAXBContextFactory", "com.sun.xml.bind.v2.ContextFactory");
        return application.sources(Application.class);
    }
}
