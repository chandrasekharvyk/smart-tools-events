package com.mrll.javelin.api.smarttools.tests

import com.fasterxml.jackson.databind.ObjectMapper
import com.mrll.javelin.api.smarttools.config.AcceptanceSpecConfiguration
import com.mrll.javelin.api.smarttools.config.TestFixtureHelper
import com.mrll.javelin.api.smarttools.config.TokenCache
import com.mrll.javelin.api.smartsort.model.delegates.MoveToSuggestedFolders
import com.mrll.javelin.api.smartsort.publisher.model.CategorizationEvent
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.GetResponse
import groovy.json.JsonBuilder
import org.apache.http.entity.ContentType
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import spock.lang.IgnoreIf
import spock.lang.Shared
import spock.lang.Unroll
import spock.util.concurrent.PollingConditions

@IgnoreIf({['dev', 'stage', 'produs', 'prodeu'].contains(AcceptanceSpecConfiguration.targetEnv)})
class SmartSortProcessingAccSpec extends TestFixtureHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(SmartSortProcessingAccSpec.class)

    @Shared
    String creatorToken

    def setupSpec() {
        super.setuphelper()
        super.setVDR(false, false, true)

        adminUser = acceptanceSpecConfiguration.adminUser
        tokenCache = new TokenCache(acceptanceSpecConfiguration)

        creatorToken = tokenCache.getToken(adminUser, acceptanceSpecConfiguration.projectId)

        // Create + bind queue that only exists to verify message was published to exchange
        channel.queueDeclare(mqConfigs.smartCategorizationResultTestQueue, true, false, true, Map.of())
        channel.queueBind(mqConfigs.smartCategorizationResultTestQueue, mqConfigs.smartCategorizationResultExchange, '#')
    }

    def cleanupSpec() {
        super.cleanupHelper()
    }

    @IgnoreIf({['dev', 'stage', 'produs', 'prodeu'].contains(AcceptanceSpecConfiguration.targetEnv)})
    @Unroll
    def 'DMD post Message for #scenario'() {
        given:
        def categorizationPollingConditions = new PollingConditions(timeout: pollingTimeout, initialDelay: 2, delay: 2)
        categorizationPollingConditions.eventually {
            def getDownloadBlobResponse = azureblobdownloadClient.get(
                    path: "/javelin/api/blob/download/${testProjectId}/${pdfcBlobId}/exists",
                    headers: headers)
            assert getDownloadBlobResponse != null
            assert getDownloadBlobResponse.status == HttpStatus.OK.value()
            assert getDownloadBlobResponse?.responseData?.exists == true
        }
        when:
        def smartDocResponse
        def messageBody = [
                'projectId'     : testProjectId,
                'updateType'    : updateType,
                'eventDateTime' : new Date().time,
                'processingType': processingType,
                'metadataIds'   : [documentMetadataId]
        ]

        Map<String, Object> msgHeaders = new HashMap<String, Object>()
        msgHeaders.put("Authorization", jwtToken)

        channel.basicPublish(
                mqConfigs.metadataEventsExchange,
                mqConfigs.smartSortRoutingKey,
                new AMQP.BasicProperties.Builder()
                        .headers(msgHeaders)
                        .build(),
                new JsonBuilder(messageBody).toString().getBytes())

        LOGGER.info("***** published replace document message...on exchange ${messageBody} ${mqConfigs.metadataEventsExchange}")

        then:

        categorizationPollingConditions.eventually {
            smartDocResponse = smartSortServiceClient.get(
                    path: "/api/projects/${testProjectId}/categorization/${documentMetadataId}",
                    contentType: ContentType.APPLICATION_JSON,
                    headers: headers)
            assert smartDocResponse.status == HttpStatus.OK.value()
        }

        categorizationPollingConditions.eventually {
            GetResponse response = channel.basicGet(mqConfigs.smartCategorizationResultTestQueue, true)
            CategorizationEvent message = new ObjectMapper().readValue(response.body, CategorizationEvent.class)
            LOGGER.info("**** found websocket categorization event message body={}",message)
            assert message.getMetadataId() == documentMetadataId
            assert message.getProjectId() == testProjectId
            assert message.getFolderSuggestions().size() > 0
            assert message.getFolderSuggestions().get(0).path == "contracts"
            LOGGER.info("**** found websocket categorization event message")
        }

        smartDocResponse.responseData.categoryList.size > 0

        where:
        scenario                  | updateType | processingType
        'Categorize Done Doc'     | 'DONE'     | 'DOC_UPLOAD'
        'Categorize Replaced Doc' | 'DONE'     | 'DOC_REPLACE'
        //'Categorize Replaced Doc' | 'REPLACE'  | 'DOC_UPLOAD'
    }

    @IgnoreIf({['dev', 'stage', 'produs', 'prodeu'].contains(AcceptanceSpecConfiguration.targetEnv)})
    def 'Delete categorization record after deleting metadata in DMD'() {
        given:
        def metadataResponse = coreMetadataClient.get(
                path: "/api/projects/${testProjectId}/metadata/v2/${documentMetadataId}",
                contentType: ContentType.APPLICATION_JSON,
                headers: headers)
        assert metadataResponse.status == HttpStatus.OK.value()

        and: "Create SmartDoc that should be deleted"
        smartSortServiceClient.post(path: "/api/projects/${testProjectId}/categorization/categorize",
                contentType: ContentType.APPLICATION_JSON,
                headers: ['Authorization': "Bearer ${jwtToken}"],
                body: [projectId: testProjectId, metadataId: documentMetadataId, blobId: pdfcOcrBlobId, categoryList: [['label': "material contracts and agreements", "probability": 0.9715654850006104],
                                                                                                                       ["label": "vendor contracts", "probability": 0.9508551955223083]]])
        when:
        def smartDocResponse
        def messageBody = [
                'projectId'    : testProjectId,
                'updateType'   : 'DELETE',
                'eventDateTime': new Date().time,
                'metadataIds'  : [documentMetadataId]
        ]

        Map<String, Object> msgHeaders = new HashMap<String, Object>()
        msgHeaders.put("Authorization", jwtToken)

        channel.basicPublish(
                mqConfigs.metadataEventsExchange,
                mqConfigs.smartSortDeleteKey,
                new AMQP.BasicProperties.Builder()
                        .headers(msgHeaders)
                        .build(),
                new JsonBuilder(messageBody).toString().getBytes())
        def smartDocPollingConditions = new PollingConditions(timeout: pollingTimeout, initialDelay: 2, delay: 2)
        smartDocPollingConditions.eventually {
            try {
                smartDocResponse = smartSortServiceClient.get(
                        path: "/api/projects/${testProjectId}/categorization/categorize",
                        contentType: ContentType.APPLICATION_JSON,
                        headers: headers)

            } catch (Exception ex) {
                smartDocResponse = true
            }
        }

        then: "Should throw Exception and smartDocResponse should be true"
        smartDocResponse
    }

    @IgnoreIf({['dev', 'stage', 'produs', 'prodeu'].contains(AcceptanceSpecConfiguration.targetEnv)})
    @Unroll
    def 'Move to suggested folderSuggestion'() {
        given:
        def suggestedDestination
        def suggestedMoveResponse
        def categorizationPollingConditions = new PollingConditions(timeout: pollingTimeout, initialDelay: 2, delay: 2)
        categorizationPollingConditions.eventually {
            def getDownloadBlobResponse = azureblobdownloadClient.get(
                    path: "/javelin/api/blob/download/${testProjectId}/${pdfcBlobId}/exists",
                    headers: headers)
            assert getDownloadBlobResponse != null
            assert getDownloadBlobResponse.status == HttpStatus.OK.value()
            assert getDownloadBlobResponse?.responseData?.exists == true
        }
        when:
        def messageBody = [
                'projectId'     : testProjectId,
                'updateType'    : updateType,
                'eventDateTime' : new Date().time,
                'processingType': processingType,
                'metadataIds'   : [documentMetadataId]
        ]

        Map<String, Object> msgHeaders = new HashMap<String, Object>()
        msgHeaders.put("Authorization", jwtToken)

        channel.basicPublish(
                mqConfigs.metadataEventsExchange,
                mqConfigs.smartSortRoutingKey,
                new AMQP.BasicProperties.Builder()
                        .headers(msgHeaders)
                        .build(),
                new JsonBuilder(messageBody).toString().getBytes())

        LOGGER.info("***** published replace document message... ${messageBody}")

        then:

        categorizationPollingConditions.eventually {
            GetResponse response = channel.basicGet(mqConfigs.smartCategorizationResultTestQueue, true)
            CategorizationEvent message = new ObjectMapper().readValue(response.body, CategorizationEvent.class)
            LOGGER.info("**** found websocket categorization event message body={}",message)
            assert message.getMetadataId() == documentMetadataId
            assert message.getProjectId() == testProjectId
            assert message.getFolderSuggestions().get(0).getFolderId() != null
            suggestedDestination = message.getFolderSuggestions().get(0).getFolderId()
        }

        categorizationPollingConditions.eventually {
            suggestedMoveResponse = smartSortServiceClient.post(path: "/api/projects/${testProjectId}/categorization/moveDocument/${suggestedDestination}",
                    contentType: ContentType.APPLICATION_JSON,
                    headers: ['Authorization': "Bearer ${jwtToken}"],
                    body: new MoveToSuggestedFolders().setMetadataIds([documentMetadataId]))
            assert suggestedMoveResponse.responseData.value == 1
        }

        where:
        scenario                        | updateType | processingType
        'Categorize Done Doc with Move' | 'DONE'     | 'DOC_UPLOAD'
    }
}
