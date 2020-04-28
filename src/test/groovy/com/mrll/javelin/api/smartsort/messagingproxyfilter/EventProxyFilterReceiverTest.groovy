package com.mrll.javelin.api.smartsort.messagingproxyfilter

import com.mrll.javelin.api.smarttools.config.EventProxyFilterConfig
import com.mrll.javelin.api.smarttools.exception.FatalSmartSortException
import com.mrll.javelin.api.smarttools.model.delegates.MetadataUpdateEvent
import com.mrll.javelin.api.smarttools.service.ProjectSettingsService
import com.mrll.javelin.api.smarttools.messagingproxyfilter.EventProxyFilterReceiver
import com.mrll.javelin.common.event.mq.JavelinMessageTemplate
import spock.lang.Specification
import spock.lang.Subject

class EventProxyFilterReceiverTest extends Specification {

    def mockProjectSettingsService = Mock(ProjectSettingsService)
    def mockJavelinMessageTemplate = Mock(JavelinMessageTemplate)
    def mockEventProxyFilterConfig = Mock(EventProxyFilterConfig)


    @Subject
    EventProxyFilterReceiver eventProxyFilterReceiver = new EventProxyFilterReceiver(mockProjectSettingsService, mockJavelinMessageTemplate, mockEventProxyFilterConfig)

    /** TODO : Restore when service uses SmartTools entitlement
     def 'Handle valid message with DONE type - SMART TOOLS CONFIG NOT ENABLED'() {given: 'valid messaging event'
     def metadataUpdateEvent =
     new MetadataUpdateEvent(
     userId: 'user_id',
     projectId: 'project_id',
     metadataIds: ['metadata_id'],
     updateType: 'DONE',
     processingType: 'DOC_UPLOAD')

     when:
     eventProxyFilterReceiver.handleValidMessage(metadataUpdateEvent)

     then:
     1 * mockProjectSettingsService.hasSmartToolsEntitlementEnabled(_ as String) >> false
     0 * mockCategorizationService.findDocCategory(metadataUpdateEvent)}**/


    def 'Handle valid message with DONE type'() {

        given: 'valid messaging event'
        def metadataUpdateEvent =
                new MetadataUpdateEvent(
                        userId: 'user_id',
                        projectId: 'project_id',
                        metadataIds: ['metadata_id'],
                        updateType: 'DONE',
                        processingType: 'DOC_UPLOAD')

        when:
        eventProxyFilterReceiver.handleValidMessage(metadataUpdateEvent)

        then:
        1 * mockProjectSettingsService.hasSmartToolsEntitlementEnabled(_ as String) >> true
        1 * mockJavelinMessageTemplate.send(_, _, _)
    }

    def 'Handle valid message with REPLACE type'() {

        given: 'valid messaging event'
        def metadataUpdateEvent =
                new MetadataUpdateEvent(
                        userId: 'user_id',
                        projectId: 'project_id',
                        metadataIds: ['metadata_id'],
                        updateType: 'DONE',
                        processingType: 'DOC_REPLACE')

        when:
        eventProxyFilterReceiver.handleValidMessage(metadataUpdateEvent)

        then:
        1 * mockProjectSettingsService.hasSmartToolsEntitlementEnabled(_ as String) >> true
        1 * mockJavelinMessageTemplate.send(_, _, _)
    }

    def 'Handle invalid message with unsupported type consumes Rabbit message'() {

        given: 'valid messaging event'
        def metadataUpdateEvent =
                new MetadataUpdateEvent(
                        projectId: 'project_id',
                        metadataIds: ['metadata_id'],
                        updateType: 'UNKNOWN',
                        processingType: 'DOC_UPLOAD')


        when:
        eventProxyFilterReceiver.handleValidMessage(metadataUpdateEvent)

        then:
        1 * mockProjectSettingsService.hasSmartToolsEntitlementEnabled(_ as String) >> true
        0 * mockJavelinMessageTemplate.send(_, _, _)
        //thrown FatalSmartSortException
    }

    def 'Handle smart enabled project with Unknown processingType'() {

        given: 'valid messaging event'
        def metadataUpdateEvent =
                new MetadataUpdateEvent(
                        projectId: 'project_id',
                        metadataIds: ['metadata_id'],
                        updateType: 'DONE',
                        processingType: 'UNKNOWN')


        when:
        eventProxyFilterReceiver.handleValidMessage(metadataUpdateEvent)

        then:
        1 * mockProjectSettingsService.hasSmartToolsEntitlementEnabled(_ as String) >> true
        0 * mockJavelinMessageTemplate.send(_, _, _)
        thrown FatalSmartSortException
    }

    def 'Handle Non smart enabled Project'() {

        given: 'valid messaging event'
        def metadataUpdateEvent =
                new MetadataUpdateEvent(
                        projectId: 'project_id',
                        metadataIds: ['metadata_id'],
                        updateType: 'UNKNOWN',
                        processingType: 'DOC_UPLOAD')


        when:
        eventProxyFilterReceiver.handleValidMessage(metadataUpdateEvent)

        then:
        1 * mockProjectSettingsService.hasSmartToolsEntitlementEnabled(_ as String) >> false
        0 * mockJavelinMessageTemplate.send(_, _, _)
        //thrown FatalSmartSortException
    }
}
