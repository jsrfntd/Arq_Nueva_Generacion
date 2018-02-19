package co.com.uniandes.quotationmicroservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class QuotationMicroserviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(QuotationMicroserviceApplication.class, args);
	}
}
