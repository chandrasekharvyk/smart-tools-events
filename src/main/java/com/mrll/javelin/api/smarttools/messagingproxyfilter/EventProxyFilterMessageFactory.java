package com.mrll.javelin.api.smarttools.messagingproxyfilter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mrll.javelin.api.smarttools.exception.FatalSmartSortException;
import com.mrll.javelin.api.smarttools.model.delegates.MetadataUpdateEvent;
import com.mrll.javelin.common.event.mq.exhaustion.MessageFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;

import java.io.IOException;

public class EventProxyFilterMessageFactory implements MessageFactory<MetadataUpdateEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(EventProxyFilterMessageFactory.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public MetadataUpdateEvent createMessage(Message message) {
        return createMessage(message.getBody());
    }

    @Override
    public MetadataUpdateEvent createMessage(byte[] input) {
        try {
            return MAPPER.readValue(input, MetadataUpdateEvent.class);
        } catch (JsonProcessingException ex) {
            String message = "message=jsonProcessingErrorHandlingMessage";
            LOG.error(message, ex);
            throw new FatalSmartSortException(message, ex);
        } catch (IOException ex) {
            String message = "message=ioErrorHandlingMessage";
            LOG.error(message, ex);
            throw new FatalSmartSortException(message, ex);
        }
    }
}
