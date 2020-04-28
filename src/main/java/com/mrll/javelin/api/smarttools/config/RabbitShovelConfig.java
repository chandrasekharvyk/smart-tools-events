package com.mrll.javelin.api.smarttools.config;

import com.mrll.javelin.rabbit.shovel.config.ScheduledShovelAutomationConfig;
import com.mrll.javelin.rabbit.shovel.config.Shovels;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class RabbitShovelConfig implements ApplicationListener<ApplicationReadyEvent>, EnvironmentAware {
    private ConfigurableEnvironment environment;

    @Value("${vcap.application.name:#{null}}")
    private String cloudFoundryAppName;
    @Value("${spring.application.name:unknown}")
    private String springAppName;

    @Bean
    @ConfigurationProperties
    public Shovels shovels() {
        return new Shovels(cloudFoundryAppName, springAppName);
    }

    @Bean
    public TaskScheduler scheduler() {
        return new ThreadPoolTaskScheduler();
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = (ConfigurableEnvironment) environment;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        new ScheduledShovelAutomationConfig(environment, shovels()).startConfiguredSchedules();
    }
}
