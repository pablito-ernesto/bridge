package ar.com.notarip.bridge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = { "ar.com.notarip.bridge" })
//@RabbitListener(queues = "bridge.payment")
public class Bridge {
	public static void main(String[] args) {
		SpringApplication.run(Bridge.class, args);
	}

//	@RabbitHandler
//	public void process(@Payload String foo) {
//		System.out.println(new Date() + ": " + foo);
//	}

}
