package com.mrll.javelin.api.smarttools.config;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.mrll.javelin.api.smarttools.exception.FatalSmartSortException;
import com.mrll.javelin.api.smarttools.model.delegates.MetadataUpdateEvent;
import com.mrll.javelin.api.smarttools.service.CategorizationService;
import com.mrll.javelin.api.smarttools.suscriber.CategorizationDeleteExhaustedHandler;
import com.mrll.javelin.api.smarttools.suscriber.CategorizationDeleteMessageFactory;
import com.mrll.javelin.api.smarttools.suscriber.CategorizationDeleteProcessor;
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
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CategorizationDeleteQueueConfig extends BaseMqConfig {

    @Value("${local-message.metadataEventsExchange}")
    private String metadataEventsExchange;
    @Value("${local-message.smartSortDeleteQueue}")
    private String smartSortDeleteQueue;
    @Value("${local-message.smartSortDeleteKey}")
    private String smartSortDeleteKey;

    @Value("${message.smartSortConsumers: 1}")
    private int concurrentConsumers;
    @Value("${message.smartSortMaxConsumers: 3}")
    private int maxConcurrentConsumers;

    @Value("${messaging-config.maxRetries: 5}")
    private int maxRetries;

    private AmqpTemplate amqpTemplate;
    private AmqpAdmin amqpAdmin;

    @Autowired
    private CategorizeMetadataValidator categorizeMetadataValidator;

    public CategorizationDeleteQueueConfig(@Value("${message.deadLetterExchange}") String deadLetterExchange,
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
    public RetryingJavelinMessageListener<MetadataUpdateEvent> deleteFilterListener(JwtConverter jwtConverter,
                                                                                        SecurityHelper securityHelper,
                                                                                        JavelinJwtService jwtService,
                                                                                        MessageProcessor deleteProcessor) {
        return new RetryingJavelinMessageListener<>(jwtConverter,
                securityHelper,
                jwtService,
                maxRetries,
                categorizationDeleteFactory(),
                deleteProcessor,
                categorizationDeleteExhaustedHandler(),
                categorizeMetadataValidator);
    }

    @Bean
    public MessageListenerContainer messageDeleteContainer(ConnectionFactory connectionFactory, MessageListener deleteFilterListener) {
        configureAndDeclareDirectExchangeQueue(amqpAdmin, metadataEventsExchange, smartSortDeleteQueue, smartSortDeleteKey);
        configureAndDeclareDeadLetterQueue(amqpAdmin, smartSortDeleteQueue);

        return new ContainerBuilder()
                .withConnectionFactory(connectionFactory)
                .withQueueName(smartSortDeleteQueue)
                .withMessageListener(deleteFilterListener)
                .withExceptionsToSwallow(FatalSmartSortException.class, JsonMappingException.class, RetriesExhaustedException.class)
                .withConsumerTagStrategy(appNameConsumerTagStrategy())
                .withConcurrentConsumers(concurrentConsumers)
                .withMaxConcurrentConsumers(maxConcurrentConsumers)
                .withAdviceChain(defaultAdviceChain(defaultMessageRecoverer(amqpTemplate,
                        metadataEventsExchange, smartSortDeleteKey, smartSortDeleteQueue)))
                .build();
    }

    @Bean
    public MessageFactory<MetadataUpdateEvent> categorizationDeleteFactory() {
        return new CategorizationDeleteMessageFactory();
    }

    @Bean
    public RetriesExhaustedHandler<MetadataUpdateEvent> categorizationDeleteExhaustedHandler() {
        return new CategorizationDeleteExhaustedHandler();
    }

    @Bean
    public MessageProcessor<MetadataUpdateEvent> deleteProcessor(CategorizationService categorizationService) {
        return new CategorizationDeleteProcessor(categorizationService);
    }
}
