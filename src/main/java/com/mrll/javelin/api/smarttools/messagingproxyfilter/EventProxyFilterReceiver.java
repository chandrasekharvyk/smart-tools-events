package com.mrll.javelin.api.smarttools.messagingproxyfilter;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mrll.javelin.api.smarttools.config.EventProxyFilterConfig;
import com.mrll.javelin.api.smarttools.exception.FatalSmartSortException;
import com.mrll.javelin.api.smarttools.model.delegates.MetadataUpdateEvent;
import com.mrll.javelin.api.smarttools.model.delegates.ProcessingAction;
import com.mrll.javelin.api.smarttools.model.delegates.UpdateType;
import com.mrll.javelin.api.smarttools.service.ProjectSettingsService;
import com.mrll.javelin.common.event.mq.JavelinMessageTemplate;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Component
public class EventProxyFilterReceiver {

    private ProjectSettingsService projectSettingsService;
    private JavelinMessageTemplate javelinMessageTemplate;
    private EventProxyFilterConfig eventProxyFilterConfig;

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Logger LOGGER = LoggerFactory.getLogger(EventProxyFilterReceiver.class);

    @Autowired
    public EventProxyFilterReceiver(ProjectSettingsService projectSettingsService,
                                    JavelinMessageTemplate javelinMessageTemplate,
                                    EventProxyFilterConfig eventProxyFilterConfig) {
        this.projectSettingsService = projectSettingsService;
        this.javelinMessageTemplate = javelinMessageTemplate;
        this.eventProxyFilterConfig = eventProxyFilterConfig;
    }

    public void handleValidMessage(MetadataUpdateEvent event) {
        LOGGER.info("method=handleValidMessage trace=smartToolsProcessing message=startMetadataUpdateEvent projectId={} metadataIds={} updateType={} processingType={}", event.getProjectId(),event.getMetadataIds(), event.getUpdateType(), event.getProcessingType());

        /** TODO with SmartSort project settings **/
        if (!isSmartToolsEnabled(event.getProjectId())) {
            LOGGER.info("method=handleValidMessage trace=smartToolsProcessing message=projectIsNotSmartEnabled details=processingEndsHere projectId={} metadataIds={} updateType={} processingType={}", event.getProjectId(),event.getMetadataIds(), event.getUpdateType(), event.getProcessingType());
            return; //Message will be consumed
        }

        if (event.getUpdateType().equals(UpdateType.DONE.toString())) {
            LOGGER.info("method=handleValidMessage trace=smartToolsProcessing message=processingDoneEvent projectId={} metadataIds={} updateType={} processingType={}", event.getProjectId(),event.getMetadataIds(), event.getUpdateType(), event.getProcessingType());
            processDoneEvent(event);
        } else if (event.getUpdateType().equals(UpdateType.REPLACE.toString())) {
            LOGGER.info("method=handleValidMessage trace=smartToolsProcessing message=processingReplaceEvent projectId={} metadataIds={} updateType={} processingType={}", event.getProjectId(),event.getMetadataIds(), event.getUpdateType(), event.getProcessingType());
            processReplaceEvent(event);
        } else {
            LOGGER.error("method=handleValidMessage trace=smartToolsProcessing message=invalidUpdateType details=processingEndsHere projectId={} metadataIds={} updateType={} processingType={}", event.getProjectId(),event.getMetadataIds(), event.getUpdateType(), event.getProcessingType());
            //throw new FatalSmartSortException("failed to process event - invalid update type");
            return;
        }
    }

    private void processDoneEvent(final MetadataUpdateEvent metadataUpdateEvent) {
        LOGGER.info("method=processDoneEvent trace=smartToolsProcessing message=start projectId={} metadataIds={} updateType={} processingType={}", metadataUpdateEvent.getProjectId(),metadataUpdateEvent.getMetadataIds(), metadataUpdateEvent.getUpdateType(), metadataUpdateEvent.getProcessingType());
        if (StringUtils.isEmpty(metadataUpdateEvent.getProcessingType())) {
            LOGGER.error("method=processDoneEvent trace=smartToolsProcessing message=processTypeIsEmpty details=processingEndsHere projectId={} metadataIds={} updateType={} processingType={}", metadataUpdateEvent.getProjectId(),metadataUpdateEvent.getMetadataIds(), metadataUpdateEvent.getUpdateType(), metadataUpdateEvent.getProcessingType());
            throw new FatalSmartSortException("failed to process event - process type does not exist");
        } else if (metadataUpdateEvent.getProcessingType().equals(ProcessingAction.DOC_UPLOAD.toString())) {
            LOGGER.info("method=processDoneEvent trace=smartToolsProcessing message=processingAnUploadEvent projectId={} metadataIds={} updateType={} processingType={}", metadataUpdateEvent.getProjectId(),metadataUpdateEvent.getMetadataIds(), metadataUpdateEvent.getUpdateType(), metadataUpdateEvent.getProcessingType());
            publishCategorisable(metadataUpdateEvent);
        } else if (metadataUpdateEvent.getProcessingType().equals(ProcessingAction.DOC_REPLACE.toString())) {
            LOGGER.info("method=processDoneEvent trace=smartToolsProcessing message=processingAReplaceEvent projectId={} metadataIds={} updateType={} processingType={}", metadataUpdateEvent.getProjectId(),metadataUpdateEvent.getMetadataIds(), metadataUpdateEvent.getUpdateType(), metadataUpdateEvent.getProcessingType());
            publishCategorisable(metadataUpdateEvent);
        } else if (metadataUpdateEvent.getProcessingType().equals(ProcessingAction.DOC_COPY.toString())) {
            LOGGER.info("method=processDoneEvent trace=smartToolsProcessing message=processingACopyEvent projectId={} metadataIds={} updateType={} processingType={}", metadataUpdateEvent.getProjectId(),metadataUpdateEvent.getMetadataIds(), metadataUpdateEvent.getUpdateType(), metadataUpdateEvent.getProcessingType());
            //TODO: ST-186 handle copies so old categories are copied over directly, not thru re-prediction
            publishCategorisable(metadataUpdateEvent);
        } else {
            LOGGER.error("method=processDoneEvent trace=smartToolsProcessing message=unknownProcessingType details=processingEndsHere projectId={} metadataIds={} updateType={} processingType={}", metadataUpdateEvent.getProjectId(),metadataUpdateEvent.getMetadataIds(), metadataUpdateEvent.getUpdateType(), metadataUpdateEvent.getProcessingType());
            throw new FatalSmartSortException("failed to process event - unknown processing type");
        }
    }

    private void processReplaceEvent(final MetadataUpdateEvent metadataUpdateEvent) {
        LOGGER.info("method=processReplaceEvent trace=smartToolsProcessing message=start projectId={} metadataIds={} updateType={} processingType={}", metadataUpdateEvent.getProjectId(),metadataUpdateEvent.getMetadataIds(), metadataUpdateEvent.getUpdateType(), metadataUpdateEvent.getProcessingType());
        publishCategorisable(metadataUpdateEvent);
    }

    private void publishCategorisable(MetadataUpdateEvent event) {
        LOGGER.info("method=publishCategorisable trace=smartToolsProcessing message=start projectId={} metadataIds={} updateType={} processingType={}", event.getProjectId(),event.getMetadataIds(), event.getUpdateType(), event.getProcessingType());
        List<String> metadataIds = event.getMetadataIds();
        metadataIds.forEach(metadataId -> {
            event.setMetadataIds(Arrays.asList(metadataId));
            try {
                Message message = MessageBuilder
                        .withBody(MAPPER.writeValueAsBytes(event))
                        .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                        .setHeader("prId", event.getProjectId())
                        .setHeader("type", event.getUpdateType())
                        .build();

                javelinMessageTemplate.send(eventProxyFilterConfig.getMetadataCategorizationExchange(), eventProxyFilterConfig.getMetadataCategorizationRoutingKey(), message);
                LOGGER.info("method=publishCategorisable trace=smartToolsProcessing message=messageSentToExchange projectId={} metadataIds={} exchange={} routingKey={} messageAddedTime={} messageHeaders={}",
                event.getProjectId(),event.getMetadataIds(), eventProxyFilterConfig.getMetadataCategorizationExchange(), eventProxyFilterConfig.getMetadataCategorizationRoutingKey(),
                        Instant.now(), message.getMessageProperties());
            } catch (JsonProcessingException e) {
                LOGGER.error("method=publishCategorisable trace=smartToolsProcessing message=failedParsing details={} projectId={} metadataIds={} updateType={} processingType={}", e.getMessage(), event.getProjectId(),event.getMetadataIds(), event.getUpdateType(), event.getProcessingType());
                throw new FatalSmartSortException("Failed parsing event", e);
            } catch (RuntimeException e) {
                LOGGER.error("method=publishCategorisable trace=smartToolsProcessing message=failedPostingToExchange details={} projectId={} metadataIds={} updateType={} processingType={}", e.getMessage(), event.getProjectId(),event.getMetadataIds(), event.getUpdateType(), event.getProcessingType());
                throw new FatalSmartSortException("Failed posting smartEnabled Project", e);
            }
        });
    }

    private Boolean isSmartToolsEnabled(final String projectId) {
        LOGGER.info("method=isSmartToolsEnabled trace=smartToolsProcessing message=start projectId={} ", projectId);
        return projectSettingsService.hasSmartToolsEntitlementEnabled(projectId);
    }

}
