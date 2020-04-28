package com.mrll.javelin.api.smarttools.config

import com.mrll.javelin.common.test.client.RabbitTestClient
import com.mrll.javelin.common.test.config.AbstractAcceptanceSpecConfigurationDefault
import com.mrll.javelin.common.test.config.JwtEngineerotron
import com.mrll.javelin.common.test.config.RestClientConfigurer
import com.mrll.javelin.common.test.config.TokenServiceCredentials
import com.mrll.javelin.common.test.spec.JavelinRestSpecification
import com.mrll.javelin.ufo.testkit.AcceptanceTestConfig
import com.mrll.javelin.ufo.testkit.http.FileStoreClient
import com.mrll.javelin.ufo.testkit.http.ImportManifestClient
import com.rabbitmq.client.Channel
import groovyx.net.http.HttpResponseDecorator
import groovyx.net.http.RESTClient
import org.apache.http.entity.ContentType
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.client.RestTemplate
import spock.lang.Shared
import spock.util.concurrent.PollingConditions

class TestFixtureHelper extends JavelinRestSpecification {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestFixtureHelper.class)

    @Shared
    AcceptanceSpecConfiguration acceptanceSpecConfiguration

    @Shared
    TokenCache tokenCache

    @Shared
    TestProjectCreator testProjectCreator

    @Shared
    RESTClient coreMetadataClient, azureblobdownloadClient, redactionServiceClient, projectCompositeRestClient, redactedTermsSearchServiceClient, smartSortServiceClient
    @Shared
    Channel channel
    @Shared
    FileStoreClient fileStoreClient
    @Shared
    ImportManifestClient importManifestClient
    @Shared
    RabbitTestClient rabbitTestClient
    @Shared
    JwtEngineerotron jwtEngineerotron

    @Shared
    Map headers

    @Shared
    String fileroomId, documentMetadataId, excelDocumentMetadataId, copiedDocumentMetadataId

    @Shared
    TokenServiceCredentials adminUser, reviewerUser
    @Shared
    MQConfigs mqConfigs
    @Shared
    String jwtToken
    @Shared
    String sandBoxID
    @Shared
    String pdfcOcrBlobId, pdfcOriginalBlobId, pdfcBlobId, pdcfXodBlobId

    @Shared
    String excelOcrBlobId, excelOriginalBlobId, excelBlobId, excelXodBlobId

    @Shared
    int sizeOfFile, pageCount

    @Shared
    int pollingTimeout = 300

    @Shared
    PollingConditions pollingConditions

    @Shared
    def testProjectId

    RestTemplate restTemplate
    private static final double pollingTimeoutInSeconds = 7d
    private static final double pollingInitialDelayInSeconds = 0.2d
    private static final double pollingDelayInSeconds = 1d

    static private final String smartEnabledConfig = "smartTools"
    static private final String smartEnabledSetting = "smartToolsEnabled"

    def setuphelper() {

        acceptanceSpecConfiguration = new AcceptanceSpecConfiguration()
        tokenCache = new TokenCache(acceptanceSpecConfiguration)
        testProjectCreator = new TestProjectCreator(acceptanceSpecConfiguration)

        this.pollingConditions = new PollingConditions(
                timeout: pollingTimeoutInSeconds,
                initialDelay: pollingInitialDelayInSeconds,
                delay: pollingDelayInSeconds
        )

        smartSortServiceClient = new RESTClient(acceptanceSpecConfiguration.targetHost)
        adminUser = acceptanceSpecConfiguration.adminUser
        reviewerUser = acceptanceSpecConfiguration.reviewerUser
        mqConfigs = acceptanceSpecConfiguration.mqConfigs


        jwtEngineerotron = new JwtEngineerotron(acceptanceSpecConfiguration)

        restTemplate = new RestTemplate()

        rabbitTestClient = new RabbitTestClient(acceptanceSpecConfiguration.rabbitUri)
        channel = rabbitTestClient.getNewChannel()
        channel.exchangeDeclare(mqConfigs.metadataEventsExchange, 'direct', true)

        coreMetadataClient = new RESTClient(acceptanceSpecConfiguration.coreMetadataService)
        coreMetadataClient.handler.failure = { resp, data ->
            resp.setData(data)
            String headers = ""
            resp.headers.each { h ->
                headers = headers + "${h.name} : ${h.value}\n"
            }
            return resp
        }

        redactionServiceClient = new RESTClient(acceptanceSpecConfiguration.redactionServiceUrl)
        redactionServiceClient.handler.failure = { resp, data ->
            resp.setData(data)
            String headers = ""
            resp.headers.each { h ->
                headers = headers + "${h.name} : ${h.value}\n"
            }
            return resp
        }

        azureblobdownloadClient = new RESTClient(acceptanceSpecConfiguration.azureblobdownloadServiceUrl)
        azureblobdownloadClient.handler.failure = { resp, data ->
            resp.setData(data)
            String headers = ""
            resp.headers.each { h ->
                headers = headers + "${h.name} : ${h.value}\n"
            }
            return resp
        }

        redactedTermsSearchServiceClient = new RESTClient(acceptanceSpecConfiguration.redactedTermsSearchServiceUrl)
        redactedTermsSearchServiceClient.handler.failure = { resp, data ->
            resp.setData(data)
            String headers = ""
            resp.headers.each { h ->
                headers = headers + "${h.name} : ${h.value}\n"
            }
            return resp
        }

        projectCompositeRestClient =
                new RestClientConfigurer().configure(
                        acceptanceSpecConfiguration.projectCompositeServiceUrl,
                        null)

        fileStoreClient = FileStoreClient.fromConfig(new AcceptanceTestConfig())

        importManifestClient = ImportManifestClient.fromConfig(new AcceptanceTestConfig())

        testProjectId = testProjectCreator.createProjectId()

        // this is temporary until the adminUser / reviewerUser are refactored to remove their associated  project id
        // and the testProjectId is used for all projectId references...
        adminUser.projectId = testProjectId
        reviewerUser.projectId = testProjectId

        // default to null so we know if we never created id
        excelBlobId = null
        LOGGER.info("adminUser.email={} adminUser.password={} testProjectId={}", adminUser.email, adminUser.password, testProjectId)

        pollingConditions.within(5.0) {
            assert jwtEngineerotron.generateToken(adminUser.email, adminUser.password, testProjectId)
        }
        jwtToken = jwtEngineerotron.generateToken(adminUser.email, adminUser.password, testProjectId)
        println("***** created jwt - jwtToken=${jwtToken}")

        headers = getCustomHeader(jwtToken)

        println("***** created headers - headers=${headers}")

        sandBoxID = testProjectCreator.setupSandbox(jwtToken, testProjectId)
        createFolder(jwtToken, testProjectId, sandBoxID)
        testProjectCreator.updateProjectSetting(jwtToken, testProjectId, smartEnabledConfig, smartEnabledSetting, 'true')
    }

    def cleanupHelper() {

        if (fileroomId) {
            coreMetadataClient.post(
                    path: "/api/projects/${testProjectId}/metadata/v2/delete",
                    contentType: ContentType.APPLICATION_JSON,
                    body: [metadataIds: [fileroomId], permanentDelete: true],
                    headers: headers)
        }

        rabbitTestClient?.close()

        // clean up any keys that did not get deleted but should have
        //deleteAzureBlobs(jwtToken, adminUser.projectId)

        // delete the blobs we created
        deleteBlob(jwtToken, adminUser.projectId, pdfcOcrBlobId)
        deleteBlob(jwtToken, adminUser.projectId, pdfcOriginalBlobId)
        deleteBlob(jwtToken, adminUser.projectId, pdfcBlobId)
        deleteBlob(jwtToken, adminUser.projectId, pdcfXodBlobId)

        if (excelBlobId != null) {
            deleteBlob(jwtToken, adminUser.projectId, excelBlobId)
            deleteBlob(jwtToken, adminUser.projectId, excelOcrBlobId)
            deleteBlob(jwtToken, adminUser.projectId, excelOriginalBlobId)
            deleteBlob(jwtToken, adminUser.projectId, excelXodBlobId)
        }
        def smartDocProjectRes = smartSortServiceClient.get(
                path: "/api/projects/${testProjectId}/categorization",
                contentType: ContentType.APPLICATION_JSON,
                headers: headers)
        if (smartDocProjectRes.status == HttpStatus.OK.value()) {
            def deletedProject = smartSortServiceClient.delete(
                    path: "/api/projects/${testProjectId}/categorization",
                    contentType: ContentType.APPLICATION_JSON,
                    headers: headers)
            deletedProject.responseData == 1
        }
        deleteProject()
    }

    private deleteBlob(String authToken, String projectId, String blobIndexId) {

        // first check if exists, otherwise get a 400 error when you try to delete

        def getDownloadBlobResponse = azureblobdownloadClient.get(
                path: "/javelin/api/blob/download/${projectId}/${blobIndexId}/exists",
                headers: headers)

        if ((getDownloadBlobResponse != null) && (getDownloadBlobResponse.status == HttpStatus.OK.value()) && (getDownloadBlobResponse?.responseData?.exists == true)) {
            String URIString = acceptanceSpecConfiguration.azureblobdeleteServiceUrl + '/javelin/api/blob/delete'
            HttpHeaders httpHeaders = new HttpHeaders()
            httpHeaders.setContentType(MediaType.APPLICATION_JSON)
            httpHeaders.add('Authorization', 'Bearer ' + authToken)
            Map body = [
                    containerName: testProjectId,
                    files        : [blobIndexId]
            ]

            restTemplate.exchange(
                    new RequestEntity(body, httpHeaders, HttpMethod.DELETE, new URI(URIString)),
                    String)
        }
    }

    private deleteProject() {

        if (!acceptanceSpecConfiguration.protectedProjects.contains(testProjectId)) {

            println("***** deleting test project - testProjectId=${testProjectId}")

            def hitId = restClientConfig.generateHitId()

            def destroyToken =
                    jwtEngineerotron.generateInternalToken(
                            acceptanceSpecConfiguration.internalTechUser.email,
                            acceptanceSpecConfiguration.internalTechUser.password,
                            'tech_service',
                            hitId)

            def headers = getCustomHeader(destroyToken, hitId)
            headers['FORCE_PROJECT_DELETE'] = "true"

            def deleteResponse =
                    projectCompositeRestClient.delete(
                            path: "/api/v2/projects/${testProjectId}/deleteTestProject",
                            contentType: MediaType.APPLICATION_JSON_UTF8_VALUE,
                            query: [deleteAudits: true],
                            headers: headers)

            assert deleteResponse.status == HttpStatus.OK.value()

            println("***** successfully deleted test project - testProjectId=${testProjectId}")
        }
    }

    def setVDR(boolean createExcelFile, boolean createCopyFile, boolean smartEnabled = false) {

        Map fileRoomMetadataBody = [
                content: [[
                                  "correlationId"      : "string",
                                  "name"               : 'Acceptance Test SmartSort' + new Date().time,
                                  "specialFileHandling": "NONE",
                                  "type"               : "FILEROOM"
                          ]],
        ]

        // 'create fileroom'
        def metadataResponse = coreMetadataClient.post(
                path: "/api/projects/${testProjectId}/metadata/v2",
                contentType: ContentType.APPLICATION_JSON,
                headers: headers,
                body: fileRoomMetadataBody)


        assert metadataResponse.status == HttpStatus.CREATED.value()
        assert metadataResponse.data.content.size() == 1
        fileroomId = metadataResponse.data.content.find({ it.type == 'FILEROOM' }).id
        assert fileroomId != null

        String filepath = '/testfile.pdf'
        String fileExt = 'pdf'
        String fileName = 'testfile'
        sizeOfFile = 541000
        int pageCount = 3
        String ocrFilepath = '/testfile.txt'
        String ocrFileExt = 'txt'
        pdfcBlobId = UUID.randomUUID().toString() + 'PDFC'
        pdfcOcrBlobId = UUID.randomUUID().toString() + '_OCR'
        pdfcOriginalBlobId = UUID.randomUUID().toString()
        pdcfXodBlobId = UUID.randomUUID().toString() + '_XOD'

        File resourcesDirectory = new File("src/acceptance/resources")
        String tempName = resourcesDirectory.getAbsolutePath() + filepath
        String pathWithNoPercent20s = tempName.replaceAll('%20', ' ')

        String ocrTempName = resourcesDirectory.getAbsolutePath() + ocrFilepath

        String ocrPathWithNoPercent20s = ocrTempName.replaceAll('%20', ' ')

        // first, upload the blob files
        uploadBlobFile(adminUser.projectId, pdfcOriginalBlobId, fileName, fileExt, pathWithNoPercent20s)
        uploadBlobFile(adminUser.projectId, pdfcBlobId, fileName, fileExt, pathWithNoPercent20s)
        uploadBlobFile(adminUser.projectId, pdfcOcrBlobId, fileName, ocrFileExt, ocrPathWithNoPercent20s)
        uploadBlobFile(adminUser.projectId, pdcfXodBlobId, fileName, fileExt, pathWithNoPercent20s)

        // next, manually create the doucment in metadata
        documentMetadataId = createDocument(adminUser.projectId, fileroomId, jwtToken, pdfcBlobId, pdfcOcrBlobId, pdfcOriginalBlobId, pdcfXodBlobId, 'pdf', 'txt')

        if (createExcelFile) {
            String excelFilePath = '/testfile.xlsx'
            String excelExt = 'xlsx'
            String excelFileName = 'testfile'
            int sizeOfExcelFile = 8800
            String excelFileId = UUID.randomUUID().toString()

            tempName = resourcesDirectory.getAbsolutePath() + excelFilePath
            String excelPathWithNoPercent20s = tempName.replaceAll('%20', ' ')
            excelBlobId = UUID.randomUUID().toString() + '_XLSX'
            uploadBlobFile(adminUser.projectId, excelBlobId, excelFileName, excelExt, excelPathWithNoPercent20s)
            excelOcrBlobId = UUID.randomUUID().toString() + '_OCR'
            uploadBlobFile(adminUser.projectId, excelOcrBlobId, excelFileName, excelExt, excelPathWithNoPercent20s)
            excelOriginalBlobId = UUID.randomUUID().toString()
            uploadBlobFile(adminUser.projectId, excelOriginalBlobId, excelFileName, excelExt, excelPathWithNoPercent20s)
            excelXodBlobId = UUID.randomUUID().toString() + '_XOD'
            uploadBlobFile(adminUser.projectId, excelXodBlobId, excelFileName, excelExt, excelPathWithNoPercent20s)

            // next, manually create the document in metadata
            excelDocumentMetadataId = createDocument(adminUser.projectId, fileroomId, jwtToken, excelBlobId, excelOcrBlobId, excelOriginalBlobId, excelXodBlobId, 'xlsx', 'txt')
        }

        if (createCopyFile) {

            def copyRequestBodyMap = [
                    "destinationId"             : fileroomId,
                    "inheritDestinationSecurity": true,
                    "publishingOptions"         : "KEEP_EXISTING",

                    "sourceIds"                 : [documentMetadataId]
            ]

            def getCopyMetadataResponse = coreMetadataClient.post(path: "/api/projects/${testProjectId}/metadata/v2/copy",
                    headers: headers,
                    contentType: ContentType.APPLICATION_JSON,
                    body: copyRequestBodyMap
            )

            assert getCopyMetadataResponse.status == HttpStatus.OK.value()

            copiedDocumentMetadataId = getCopyMetadataResponse.data.results[0].get("destinationId")
            assert copiedDocumentMetadataId != null
        }
    }

    private void uploadBlobFile(String projectId, String blobId, String fileName, String fileExt, String pathWithNoPercent20s) {
        String authHeader = 'Bearer ' + jwtToken

        def uploadResponse = fileStoreClient.uploadFile(authHeader, blobId, projectId, new FileInputStream(new File(pathWithNoPercent20s)))
        assert uploadResponse.status == HttpStatus.CREATED.value()
    }

    private String createDocument(String projectId, String fileroomId, String tokenForCreate, String pdfcBlobId, String ocrBlobId, String originalBlobId, String xodBlobId, String primaryFileExtension, String secondFileExtension) {

        def contentCreateMap = buildContentCreateRequestBody(projectId, pdfcBlobId, ocrBlobId, originalBlobId, xodBlobId, primaryFileExtension, secondFileExtension)

        def contentBody = new ArrayList<>()
        contentBody.add(contentCreateMap)

        // first, just do a create directly into doc-metadata
        def coreResponse = coreMetadataClient.post(path: "/api/projects/${projectId}/metadata/v2/${sandBoxID}",
                headers: headers,
                contentType: ContentType.APPLICATION_JSON,
                body: [content: contentBody, groupId: pdfcBlobId])

        coreResponse.status == HttpStatus.CREATED.value()
        def responseContent = coreResponse.data.get('content')

        // grab the id as the documentMetadataId
        String tempDocumentMetadataId = responseContent.get(0).get('id')

        // call test set to done endpoint
        def coreUpdateResponse = coreMetadataClient.put(path: "/api/projects/${projectId}/metadata/${tempDocumentMetadataId}/processing/testOnly/setToDone",
                headers: headers,
                contentType: ContentType.APPLICATION_JSON
        )

        coreUpdateResponse.status == HttpStatus.OK.value()

        return tempDocumentMetadataId
    }

    private Map createFileroom(String token, String projectId, String name, Map extraValues) {
        createFileroom(token, projectId, name, UUID.randomUUID().toString(), extraValues)
    }

    private Map createFileroom(String token, String projectId, String name, String groupId, Map extraValues) {
        Map contentBody = [
                name  : name + ' - ' + UUID.randomUUID(),
                type  : 'FILEROOM',
                active: true
        ]
        contentBody.putAll(extraValues)

        def response = coreMetadataClient.post(
                path: "/api/projects/${projectId}/metadata/v2/",
                contentType: 'application/json;charset=UTF-8',
                headers: ['Authorization': "Bearer ${token}", 'Content-Type': 'application/json'],
                body: [content: [contentBody], groupId: groupId])

        assert response.status == 201
        return response.data.content[0]
    }

    private void createFolder(final String token, final String projectId, final String parentId) {
        def name = "contracts"
        def defaultBody = [
                name  : name,
                type  : 'FOLDER',
                active: true
        ]
        Map createBody = [
                content: [defaultBody]
        ]

        def folderResponse = coreMetadataClient.post(
                path: "/api/projects/${projectId}/metadata/v2/${parentId ?: ''}",
                contentType: 'application/json;charset=UTF-8',
                headers: ['Authorization': "Bearer ${token}", 'Content-Type': 'application/json'],
                body: createBody
        )
        folderResponse.status == 201
    }

    private Map buildContentCreateRequestBody(String projectId, String pdfcBlobId, String ocrBlobId, String originalBlobId, String xodBlobId, String primaryFileExtension, String secondFileExtension) {
        def contentBody = [
                name        : 'document - ' + UUID.randomUUID(),
                type        : 'DOCUMENT',
                projectId   : projectId,
                extension   : 'pdf',
                fileSize    : sizeOfFile,
                downloadOnly: 'false',
                status      : 'PROCESSING',
                pageCount   : pageCount,
                baseBlobId  : originalBlobId,
                blobDetails : [
                        [
                                processName: 'ORIGINAL',
                                blobId     : originalBlobId,
                                fileSize   : sizeOfFile,
                                pageCount  : pageCount,
                                fileType   : primaryFileExtension
                        ],
                        [
                                processName: 'PDFC',
                                blobId     : pdfcBlobId,
                                fileSize   : sizeOfFile,
                                pageCount  : pageCount,
                                fileType   : primaryFileExtension
                        ],
                        [
                                processName: 'XOD',
                                blobId     : xodBlobId,
                                fileSize   : sizeOfFile,
                                pageCount  : pageCount,
                                fileType   : primaryFileExtension
                        ],
                        [
                                processName: 'OCR',
                                blobId     : ocrBlobId,
                                fileSize   : sizeOfFile,
                                pageCount  : pageCount,
                                fileType   : secondFileExtension
                        ]
                ]
        ]

        return contentBody
    }

    private buildUpdateMetadataDetailsBody() {
        def contentBody = [
                status: 'DONE'
        ]

        return contentBody
    }

    private buildManifestRequestBody(String fileName, String fileExt, String fileId, int sizeOfFile) {
        [entries        :
                 [
                         [type        : 'DOCUMENT',
                          relativePath: "${fileName}.${fileExt}" as String,
                          fileId      : fileId,
                          size        : sizeOfFile
                         ]
                 ],
         inheritSecurity: true,
         published      : false
        ]
    }

    Map getCustomHeader(String token, String hitId = null) {
        ['Authorization'  : "Bearer ${token}",
         'Content-Type'   : 'application/json',
         'X-CLIENT_HIT_ID': "acc-spec-${hitId == null ? UUID.randomUUID().toString() : hitId}"]
    }

    @Override
    public <T extends AbstractAcceptanceSpecConfigurationDefault> T getConfig() {
        return acceptanceSpecConfiguration
    }

    def getCachedToken(TestUser testUser, String projectId) {
        tokenCache.getToken(testUser, projectId)
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
}
