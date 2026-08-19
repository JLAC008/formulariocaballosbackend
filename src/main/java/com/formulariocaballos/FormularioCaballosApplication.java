package com.formulariocaballos;

import com.formulariocaballos.config.EnvFileLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FormularioCaballosApplication {

    public static void main(String[] args) {
        EnvFileLoader.load();
        SpringApplication.run(FormularioCaballosApplication.class, args);
    }
}
