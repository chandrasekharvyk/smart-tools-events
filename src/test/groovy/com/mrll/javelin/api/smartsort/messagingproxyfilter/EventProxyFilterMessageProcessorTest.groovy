package com.mrll.javelin.api.smartsort.messagingproxyfilter

import com.mrll.javelin.api.smarttools.exception.FatalSmartSortException
import com.mrll.javelin.api.smarttools.model.delegates.MetadataUpdateEvent
import com.mrll.javelin.api.smarttools.model.delegates.ProcessingAction
import com.mrll.javelin.api.smarttools.model.delegates.UpdateType
import com.mrll.javelin.api.smarttools.messagingproxyfilter.EventProxyFilterMessageProcessor
import com.mrll.javelin.api.smarttools.messagingproxyfilter.EventProxyFilterReceiver
import spock.lang.Specification
import spock.lang.Subject

class EventProxyFilterMessageProcessorTest extends Specification {
    @Subject
    EventProxyFilterMessageProcessor eventProxyFilterMessageProcessor


    def 'Message successfully processed'() {

        given:
        def message = new MetadataUpdateEvent(projectId: 'project_id_1', updateType: UpdateType.DONE, processingType: ProcessingAction.DOC_REPLACE, metadataIds: ['m1'])
        EventProxyFilterReceiver eventProxyFilterReceiver = Mock()
        eventProxyFilterMessageProcessor = new EventProxyFilterMessageProcessor(eventProxyFilterReceiver)

        when:
        1 * eventProxyFilterReceiver.handleValidMessage(message)
        eventProxyFilterMessageProcessor.processMessage(message)

        then:
        notThrown FatalSmartSortException
    }

    def 'Message successfully processed for copy'() {

        given:
        def message = new MetadataUpdateEvent(projectId: 'project_id_1', updateType: UpdateType.DONE, processingType: ProcessingAction.DOC_COPY, metadataIds: ['m1'])
        EventProxyFilterReceiver eventProxyFilterReceiver = Mock()
        eventProxyFilterMessageProcessor = new EventProxyFilterMessageProcessor(eventProxyFilterReceiver)

        when:
        1 * eventProxyFilterReceiver.handleValidMessage(message)
        eventProxyFilterMessageProcessor.processMessage(message)

        then:
        notThrown FatalSmartSortException
    }

    def 'Downstream processing error throws an exception'() {

        given:
        EventProxyFilterReceiver eventProxyFilterReceiver = new EventProxyFilterReceiver(null, null, null)
        eventProxyFilterMessageProcessor = new EventProxyFilterMessageProcessor(eventProxyFilterReceiver)
        def message = new MetadataUpdateEvent(projectId: 'project_id_1', updateType: UpdateType.DONE, processingType: ProcessingAction.DOC_REPLACE, metadataIds: ['m1'])

        when:
        eventProxyFilterMessageProcessor.processMessage(message)

        then:
        thrown FatalSmartSortException
    }

}
