/**
 * Ejecutor para envío de correos sin bloquear hilos HTTP ni transacciones.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class EmailAsyncConfig {

    public static final String EMAIL_TASK_EXECUTOR = "emailTaskExecutor";

    @Bean(name = EMAIL_TASK_EXECUTOR)
    public Executor emailTaskExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(2);
        ex.setMaxPoolSize(8);
        ex.setQueueCapacity(200);
        ex.setThreadNamePrefix("sesa-email-");
        ex.initialize();
        return ex;
    }
}
