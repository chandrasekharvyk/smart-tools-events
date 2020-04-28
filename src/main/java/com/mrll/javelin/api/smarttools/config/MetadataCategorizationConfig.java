package com.mrll.javelin.api.smarttools.config;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.mrll.javelin.api.smarttools.exception.FatalSmartSortException;
import com.mrll.javelin.api.smarttools.model.delegates.MetadataUpdateEvent;
import com.mrll.javelin.api.smarttools.service.ProjectSettingsService;
import com.mrll.javelin.api.smarttools.service.CategorizationService;
import com.mrll.javelin.api.smarttools.suscriber.CategorizeMetadataExhaustedHandler;
import com.mrll.javelin.api.smarttools.suscriber.CategorizeMetadataMessageFactory;
import com.mrll.javelin.api.smarttools.suscriber.CategorizeMetadataMessageProcessor;
import com.mrll.javelin.api.smarttools.suscriber.CategorizeMetadataValidator;
import com.mrll.javelin.common.event.config.BaseMqConfig;
import com.mrll.javelin.common.event.config.ContainerBuilder;
import com.mrll.javelin.common.event.mq.exhaustion.*;
import com.mrll.javelin.common.security.jwt.JwtConverter;
import com.mrll.javelin.common.security.service.JavelinJwtService;
import com.mrll.javelin.common.security.util.SecurityHelper;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetadataCategorizationConfig extends BaseMqConfig {

    @Value("${local-message.metadataCategorizationExchange}")
    private String metadataCategorizationExchange;
    @Value("${local-message.metadataCategorizationQueue}")
    private String metadataCategorizationQueue;
    @Value("${local-message.metadataCategorizationRoutingKey}")
    private String metadataCategorizationRoutingKey;
    @Value("${message.smartSortConsumers: 1}")
    private int concurrentConsumers;
    @Value("${message.smartSortMaxConsumers: 3}")
    private int maxConcurrentConsumers;

    @Value("${messaging-config.maxRetries: 5}")
    private int maxRetries;

    @Value("${web-event.smartCategorizationResultExchange}")
    private String smartCategorizationResultExchange;

    private AmqpTemplate amqpTemplate;
    private AmqpAdmin amqpAdmin;


    public MetadataCategorizationConfig(@Value("${message.deadLetterExchange}") String deadLetterExchange,
                                        @Value("${vcap.application.name:#{null}}") String cloudFoundryAppName,
                                        @Value("${spring.application.name:unknown}") String springAppName,
                                        @Value("${message.defaultInitiateInterval: 1000}") long defaultInitiateInterval,
                                        @Value("${message.defaultInitiateMaxAttempts: 3}") int defaultInitiateMaxAttempts,
                                        @Value("${message.defaultInitiateMultiplier: 2.0}") double defaultInitiateMultiplier,
                                        @Value("${message.defaultInitiateMaxInterval: 4000}") long defaultInitiateMaxInterval,
                                        AmqpTemplate amqpTemplate,
                                        AmqpAdmin amqpAdmin) {

        super(deadLetterExchange, cloudFoundryAppName, springAppName, defaultInitiateInterval, defaultInitiateMaxAttempts, defaultInitiateMultiplier, defaultInitiateMaxInterval);
        this.amqpTemplate = amqpTemplate;
        this.amqpAdmin = amqpAdmin;
    }

    @Bean
    public RetryingJavelinMessageListener<MetadataUpdateEvent> eventCategorizationListener(
            JwtConverter jwtConverter,
            SecurityHelper securityHelper,
            JavelinJwtService jwtService,
            MessageProcessor categorizationProcessor) {
        return new RetryingJavelinMessageListener<>(
                jwtConverter,
                securityHelper,
                jwtService,
                maxRetries,
                categorizationFactory(),
                categorizationProcessor,
                categorizationHandler(),
                categorizationValidator());
    }

    @Bean
    public MessageListenerContainer messageCategorizationContainer(ConnectionFactory connectionFactory, MessageListener eventCategorizationListener) {
        configureAndDeclareDirectExchangeQueue(amqpAdmin, metadataCategorizationExchange, metadataCategorizationQueue, metadataCategorizationRoutingKey);
        configureAndDeclareDeadLetterQueue(amqpAdmin, metadataCategorizationRoutingKey);

        return new ContainerBuilder()
                .withConnectionFactory(connectionFactory)
                .withQueueName(metadataCategorizationQueue)
                .withMessageListener(eventCategorizationListener)
                .withExceptionsToSwallow(FatalSmartSortException.class, JsonMappingException.class, RetriesExhaustedException.class)
                .withConsumerTagStrategy(appNameConsumerTagStrategy())
                .withConcurrentConsumers(concurrentConsumers)
                .withMaxConcurrentConsumers(maxConcurrentConsumers)
                .withAdviceChain(defaultAdviceChain(defaultMessageRecoverer(amqpTemplate,
                        metadataCategorizationExchange, metadataCategorizationRoutingKey, metadataCategorizationQueue)))
                .build();
    }

    @Bean
    public MessageValidator<MetadataUpdateEvent> categorizationValidator() {
        return new CategorizeMetadataValidator();
    }

    @Bean
    public MessageProcessor<MetadataUpdateEvent> categorizationProcessor(CategorizationService categorizationService) {
        return new CategorizeMetadataMessageProcessor(categorizationService);
    }

    @Bean
    public MessageFactory<MetadataUpdateEvent> categorizationFactory() {
        return new CategorizeMetadataMessageFactory();
    }

    @Bean
    public RetriesExhaustedHandler<MetadataUpdateEvent> categorizationHandler() {
        return new CategorizeMetadataExhaustedHandler();
    }

    // Ensure result exchange is declared
    @Bean
    public TopicExchange smartCategorizationResultEventExchange() {
        return new TopicExchange(smartCategorizationResultExchange);
    }

    public String getMetadataCategorizationExchange() {
        return metadataCategorizationExchange;
    }

    public String getMetadataCategorizationRoutingKey() {
        return metadataCategorizationRoutingKey;
    }

    /*@Bean
    public ProjectSettingsService getProjectSettingsService(){
        return new ProjectSettingsService();
    }*/
}
