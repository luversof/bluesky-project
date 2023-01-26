package net.luversof;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TestApplication {
	public static void main(String[] args) {
//		Security.setProperty("jdk.tls.disabledAlgorithms", "SSLv3, RC4, DES, MD5withRSA, DH keySize < 1024, EC keySize < 224, 3DES_EDE_CBC, anon, NULL");
//    	Security.setProperty("jdk.certpath.disabledAlgorithms", "jdk.certpath.disabledAlgorithms=MD2, MD5, SHA1 jdkCA & usage TLSServer, RSA keySize < 1024, DSA keySize < 1024, EC keySize < 224");
		SpringApplication.run(TestApplication.class, args);
	}
}
