package com.mrll.javelin.api.smarttools.config

import com.mrll.javelin.common.test.config.JwtEngineerotron
import com.mrll.javelin.common.test.config.RestClientConfigurer
import groovyx.net.http.HttpResponseDecorator
import groovyx.net.http.RESTClient
import org.apache.commons.lang3.RandomUtils
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import spock.util.concurrent.PollingConditions

import java.time.Instant

class TestProjectCreator {
    private static final Log LOG = LogFactory.getLog(getClass())
    PollingConditions pollingConditions

    AcceptanceSpecConfiguration configuration

    RESTClient docMetadataClient
    RESTClient projectCompositeRestClient

    private static final double pollingTimeoutInSeconds = 7d
    private static final double pollingInitialDelayInSeconds = 0.4d
    private static final double pollingDelayInSeconds = 4d

    TestProjectCreator(AcceptanceSpecConfiguration acceptanceSpecConfiguration = null) {
        if (acceptanceSpecConfiguration == null) {
            configuration = new AcceptanceSpecConfiguration()
        } else {
            configuration = acceptanceSpecConfiguration
        }

        docMetadataClient = new RESTClient(configuration.coreMetadataService)
        projectCompositeRestClient = new RESTClient(configuration.projectCompositeServiceUrl)

        this.pollingConditions = new PollingConditions(
                timeout: pollingTimeoutInSeconds,
                initialDelay: pollingInitialDelayInSeconds,
                delay: pollingDelayInSeconds
        )
    }

    String createProjectId() {

        String projectIdentifier = "acc-spec-smartsort-processing-" + RandomUtils.nextLong(2, 9999999999)

        def createRequestBodyMap = [
                "id"               : "Read only - should not deserialize but can serialize",
                "info"             : [
                        "name"                   : projectIdentifier,
                        "description"            : "testdescription",
                        "externalId"             : projectIdentifier,
                        "productCode"            : "MANDA",
                        "emailId"                : "smartsortadm@datasite.mailinator.com",
                        'scheduledActivationDate': Instant.now().toEpochMilli(),
                        "state"                  : "ACTIVE",
                        "dataCenter"             : configuration.dataCenter,
                        "demo"                   : true
                ],
                "deleted"          : false,
                "skipIndexCreation": false,
                "configurations"   : null,
                "entitlements"     : null
        ]

        projectCompositeRestClient =
                new RestClientConfigurer().configure(
                        configuration.projectCompositeServiceUrl,
                        null)

        def hitId = new RestClientConfigurer().generateHitId()
        def jwtEngineerotron = new JwtEngineerotron(configuration)

        String internalToken = jwtEngineerotron.generateInternalToken(
                configuration.internalTechUser.email,
                configuration.internalTechUser.password,
                'tech_service')


        def headers = getCustomHeader(internalToken, hitId)

        def response =
                projectCompositeRestClient.post(
                        path: "/api/v2/projects/demo",
                        contentType: MediaType.APPLICATION_JSON_UTF8_VALUE,
                        headers: headers,
                        body: createRequestBodyMap)

        String projectId = response?.data?.id

        assert projectId != null

        println "***** test project id created - projectId=${projectId}"

        pollingConditions.eventually {
            def getMetadataResponse =
                    projectCompositeRestClient.get(
                            path: "/api/projects/${projectId}",
                            headers: headers)
            println "***** polling for project creation - projectId=${projectId}"
            assert getMetadataResponse?.status == HttpStatus.OK.value()
        }

        println "***** test project successfully created - projectId=${projectId}"

        return projectId
    }

    def setupSandbox(final String token, final String thisProjectId) {
        HttpResponseDecorator updateProjectSettingResponse = updateProjectSetting(token, thisProjectId, 'sandbox', 'sandboxEnabled', 'true')
        assert updateProjectSettingResponse.status == 202
        HttpResponseDecorator getSandboxResponse

        pollingConditions.eventually {
            getSandboxResponse = docMetadataClient.get(
                    path: "/api/projects/${thisProjectId}/metadata/v2/sandbox",
                    headers: ['Authorization': "Bearer ${token}"]
            )
            assert getSandboxResponse.status == 200
            assert getSandboxResponse.data.type == 'SANDBOX'
            assert getSandboxResponse.data.name == 'Staging Folder'
        }

        return getSandboxResponse.data.id
    }

    HttpResponseDecorator updateProjectSetting(String token, String projectId, String configuration, String settingName, String value) {
        return projectCompositeRestClient.put(
                path: "/api/v2/projects/${projectId}/settings",
                contentType: MediaType.APPLICATION_JSON_UTF8_VALUE,
                headers: [
                        'Authorization'  : 'Bearer ' + token,
                        'X-CLIENT_HIT_ID': "acc-test-smartsort-project-${UUID.randomUUID()}"
                ],
                body: [
                        "configurations": [
                                [
                                        "configuration": configuration,
                                        "settingName"  : settingName,
                                        "value"        : value
                                ]
                        ]
                ])
    }

    Map getCustomHeader(String token, String hitId = null) {
        ['Authorization'  : "Bearer ${token}",
         'Content-Type'   : 'application/json',
         'X-CLIENT_HIT_ID': "acc-spec-${hitId == null ? UUID.randomUUID().toString() : hitId}"]
    }
}
