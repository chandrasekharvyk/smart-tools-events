package com.mrll.javelin.api.smarttools.config;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.mrll.javelin.api.smarttools.exception.FatalSmartSortException;
import com.mrll.javelin.api.smarttools.messagingproxyfilter.*;
import com.mrll.javelin.api.smarttools.model.delegates.MetadataUpdateEvent;
import com.mrll.javelin.api.smarttools.suscriber.CategorizeMetadataMessageProcessor;
import com.mrll.javelin.common.event.config.BaseMqConfig;
import com.mrll.javelin.common.event.config.ContainerBuilder;
import com.mrll.javelin.common.event.mq.exhaustion.*;
import com.mrll.javelin.common.security.jwt.JwtConverter;
import com.mrll.javelin.common.security.service.JavelinJwtService;
import com.mrll.javelin.common.security.util.SecurityHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EventProxyFilterConfig extends BaseMqConfig {

    @Value("${local-message.metadataEventsExchange}")
    private String metadataEventsExchange;
    @Value("${local-message.smartSortQueue}")
    private String smartSortQueue;
    @Value("${local-message.smartSortKey}")
    private String smartSortRoutingKey;

    @Value("${local-message.updateRoutingKey}")
    private String updateRoutingKey;

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

    private AmqpTemplate amqpTemplate;
    private AmqpAdmin amqpAdmin;
    private static final Logger LOG = LoggerFactory.getLogger(CategorizeMetadataMessageProcessor.class);

    public EventProxyFilterConfig(@Value("${message.deadLetterExchange}") String deadLetterExchange,
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
    public RetryingJavelinMessageListener<MetadataUpdateEvent> eventProxyFilterListener(JwtConverter jwtConverter,
                                                                                        SecurityHelper securityHelper,
                                                                                        JavelinJwtService jwtService,
                                                                                        MessageProcessor proxyProcessor) {
        return new RetryingJavelinMessageListener<>(
                jwtConverter,
                securityHelper,
                jwtService,
                maxRetries,
                proxyFactory(),
                proxyProcessor,
                proxyHandler(),
                proxyValidator());
    }

    @Bean
    public MessageListenerContainer messageProxyContainer(ConnectionFactory connectionFactory, MessageListener eventProxyFilterListener) {
        configureAndDeclareDirectExchangeQueue(amqpAdmin, metadataEventsExchange, smartSortQueue, smartSortRoutingKey);
        configureAndDeclareDeadLetterQueue(amqpAdmin, smartSortQueue);
        LOG.info("Listening on Exchange:{} queue: {} routingKey: {}", metadataEventsExchange, smartSortQueue, smartSortRoutingKey);
        return new ContainerBuilder()
                .withConnectionFactory(connectionFactory)
                .withQueueName(smartSortQueue)
                .withMessageListener(eventProxyFilterListener)
                .withExceptionsToSwallow(FatalSmartSortException.class, JsonMappingException.class, RetriesExhaustedException.class)
                .withConsumerTagStrategy(appNameConsumerTagStrategy())
                .withConcurrentConsumers(concurrentConsumers)
                .withMaxConcurrentConsumers(maxConcurrentConsumers)
                .withAdviceChain(defaultAdviceChain(defaultMessageRecoverer(amqpTemplate,
                        metadataEventsExchange, smartSortRoutingKey, smartSortQueue)))
                .build();
    }

    @Bean
    public MessageValidator<MetadataUpdateEvent> proxyValidator() {
        return new EventProxyFilterMessageValidator();
    }

    @Bean
    public MessageProcessor<MetadataUpdateEvent> proxyProcessor(EventProxyFilterReceiver eventProxyFilterReceiver) {
        return new EventProxyFilterMessageProcessor(eventProxyFilterReceiver);
    }

    @Bean
    public MessageFactory<MetadataUpdateEvent> proxyFactory() {
        return new EventProxyFilterMessageFactory();
    }

    @Bean
    public RetriesExhaustedHandler<MetadataUpdateEvent> proxyHandler() {
        return new EventProxyFilterExhaustedHandler();
    }

    public String getMetadataEventsExchange() {
        return metadataEventsExchange;
    }

    public String getSmartSortRoutingKey() {
        return smartSortRoutingKey;
    }

    public String getMetadataCategorizationExchange() {
        return metadataCategorizationExchange;
    }

    public String getMetadataCategorizationRoutingKey() {
        return metadataCategorizationRoutingKey;
    }
}

