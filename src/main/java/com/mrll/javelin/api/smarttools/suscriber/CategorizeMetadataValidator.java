package com.mrll.javelin.api.smarttools.suscriber;

import com.mrll.javelin.api.smarttools.exception.FatalSmartSortException;
import com.mrll.javelin.api.smarttools.messagingproxyfilter.EventProxyFilterMessageValidator;
import com.mrll.javelin.api.smarttools.model.delegates.MetadataUpdateEvent;
import com.mrll.javelin.common.event.mq.exhaustion.MessageValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
public class CategorizeMetadataValidator implements MessageValidator<MetadataUpdateEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(EventProxyFilterMessageValidator.class);

    @Override
    public void validateMessage(MetadataUpdateEvent message) {
        try {
            Assert.notNull(message.getProjectId(), "Project ID must not be null");
            Assert.notNull(message.getMetadataIds(), "Metadata IDs must not be null");
            Assert.notEmpty(message.getMetadataIds(), "Metadata IDs must not be empty");
            Assert.isTrue(message.getProjectId().trim().length() > 0, "Project ID must not be empty");

        } catch (Exception ex) {
            String errorMessage = String.format("message=problemValidatingMessage messageObject=%s", message);
            LOG.debug(errorMessage, ex);
            throw new FatalSmartSortException(errorMessage, ex);
        }
    }
}
