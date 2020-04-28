package com.mrll.javelin.api.smarttools.messagingproxyfilter;


import com.mrll.javelin.api.smarttools.exception.FatalSmartSortException;
import com.mrll.javelin.api.smarttools.model.delegates.MetadataUpdateEvent;
import com.mrll.javelin.common.event.mq.exhaustion.MessageProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventProxyFilterMessageProcessor implements MessageProcessor<MetadataUpdateEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(EventProxyFilterMessageProcessor.class);

    private EventProxyFilterReceiver eventProxyFilterReceiver;

    public EventProxyFilterMessageProcessor(EventProxyFilterReceiver eventProxyFilterReceiver) {
        this.eventProxyFilterReceiver = eventProxyFilterReceiver;
    }

    @Override
    public void processMessage(MetadataUpdateEvent message) {
        try {
            LOG.info("method=processMessage trace=smartToolsProcessing message=receivedMessageFromQueue projectId={} metadataIds={} updateType={} processingType={}", message.getProjectId(),message.getMetadataIds(), message.getUpdateType(), message.getProcessingType());
            eventProxyFilterReceiver.handleValidMessage(message);
        } catch (FatalSmartSortException fre) {
            throw fre;
        } catch (Exception ex) {
            LOG.error("method=processMessage trace=smartToolsProcessing message=problemProcessingMessage projectId={} metadataIds={} updateType={} processingType={} errorMessage={}", message.getProjectId(),message.getMetadataIds(), message.getUpdateType(), message.getProcessingType(), ex.getMessage());
            String errorMessage = String.format("method=processMessage trace=smartTools message=problemProcessingMessage messageObject=%s", message);
            throw new FatalSmartSortException(errorMessage, ex);
        }
    }



}
