
package com.bank.creditbatch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication()
public class CreditBatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(CreditBatchApplication.class, args);
    }
}
