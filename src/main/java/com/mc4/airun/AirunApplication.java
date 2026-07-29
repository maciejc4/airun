package com.mc4.airun;

import com.mc4.airun.training.TrainingService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class AirunApplication {

	public static void main(String[] args) {
		SpringApplication.run(AirunApplication.class, args);
	}

	@Bean
	ApplicationRunner loadTrainingsAtStartup(TrainingService trainingService) {
		return arguments -> trainingService.loadTrainings();
	}

}
