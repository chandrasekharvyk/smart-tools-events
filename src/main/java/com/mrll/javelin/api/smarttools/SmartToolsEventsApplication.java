package com.mrll.javelin.api.smarttools;

import com.mrll.javelin.common.audit.EnableJavelinCommonAudit;
import com.mrll.javelin.common.event.EnableJavelinCommonEvent;
import com.mrll.javelin.common.security.EnableJavelinCommonSecurity;
import com.mrll.javelin.common.security.service.TokenServiceDelegate;
import com.mrll.javelin.common.security.service.TokenServiceDelegateImpl;
import com.mrll.javelin.common.web.EnableJavelinCommonWeb;
import com.mrll.javelin.common.web.logging.LogHelper;
import com.mrll.javelin.common.web.util.MdcTaskDecorator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.AsyncConfigurerSupport;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutorService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

@SpringBootApplication
@EnableJavelinCommonWeb
@EnableJavelinCommonSecurity
@EnableJavelinCommonAudit
@EnableJavelinCommonEvent
@EnableAsync(proxyTargetClass = true)
public class SmartToolsEventsApplication extends AsyncConfigurerSupport {

    @Value("${executorsMaxPoolSize: 50}")
    private int executorsMaxPoolSize;

    @Value("${executorsCorePoolSize: 10}")
    private int executorsCorePoolSize;

    @Value("${executorsKeepAliveSeconds: 60}")
    private int executorsKeepAliveSeconds;


    @Autowired
    @Qualifier("javelinRestTemplate")
    private RestTemplate javelinRestTemplate;


    public static void main(String[] args) {
        LogHelper.setMdcForAppStartup();
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
        SpringApplication.run(SmartToolsEventsApplication.class, args);
    }

    @Bean
    public TokenServiceDelegate tokenServiceDelegate() {
        return new TokenServiceDelegateImpl(javelinRestTemplate, 0);
    }

    @Bean
    public ExecutorService getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(executorsCorePoolSize);
        executor.setMaxPoolSize(executorsMaxPoolSize);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setTaskDecorator(new MdcTaskDecorator());
        executor.setThreadNamePrefix("SSS-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setKeepAliveSeconds(executorsKeepAliveSeconds);

        ExecutorService executorService = Executors.newCachedThreadPool(executor);
        executor.initialize();

        return new DelegatingSecurityContextExecutorService(executorService);
    }
}
