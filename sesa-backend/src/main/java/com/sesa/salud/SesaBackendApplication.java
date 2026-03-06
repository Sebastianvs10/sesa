/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@org.springframework.scheduling.annotation.EnableScheduling
public class SesaBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(SesaBackendApplication.class, args);
    }
}
