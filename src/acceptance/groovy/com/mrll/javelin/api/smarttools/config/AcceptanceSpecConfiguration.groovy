package com.mrll.javelin.api.smarttools.config

import com.mrll.javelin.common.test.client.CloudFoundryHelper
import com.mrll.javelin.common.test.config.AbstractAcceptanceSpecConfigurationDefault
import com.mrll.javelin.common.test.config.TokenServiceCredentials

class AcceptanceSpecConfiguration extends AbstractAcceptanceSpecConfigurationDefault{
    String targetProjectService
    String projectId
    String coreMetadataService
    String smartSortServiceClient
    static String targetEnv
    String redactionServiceUrl
    String projectCompositeServiceUrl
    String azureblobdownloadServiceUrl
    String azureblobdeleteServiceUrl
    String dataCenter
    //TestUser adminUser
    TokenServiceCredentials adminUser
    TokenServiceCredentials reviewerUser
    MQConfigs mqConfigs
    String rabbitUri
    String redactedTermsSearchServiceUrl

    TokenServiceCredentials internalTechUser

    Map configByEnvironment = [
            local : [
                    dataCenter : 'USA',
                    internalTechUser: new TokenServiceCredentials(email: 'dummyone@datasite.com', password: 'Password1!'),
                    reviewerUser: new TokenServiceCredentials(
                            email: 'smartsortrev@datasite.mailinator.com',
                            password: 'Password1!',
                            projectId: '5b73265ef164b70013a30a63'
                    ),
                    adminUser   : new TokenServiceCredentials(
                            email: 'smartsortadm@datasite.mailinator.com',
                            password: 'Password1!',
                            projectId: '5e83514b026f316f64def8d9'
                    ),
                    mq          : new MQConfigs(
                            metadataEventsExchange: 'pr-ds1-metadata-exchange',
                            smartSortQueue: 'pr-smart-sort-queue',
                            smartSortRoutingKey: 'done',
                            metadataCategorizationExchange: 'pr-smart-sort-categorization-exchange',
                            metadataCategorizationQueue: 'pr-smart-sort-categorization-queue',
                            metadataCategorizationRoutingKey: 'smart-sort-categorization-key',
                            smartSortDeleteQueue:"pr-smart-sort-delete-queue",
                            smartSortDeleteKey:"delete",
                            smartCategorizationResultExchange:'pr-ds1-smartsort-result-exchange',
                            smartCategorizationResultTestQueue:'pr-ds1-smartsort-result-queue'
                    ),
                    treeProjectId: '59a85c47cfe6a20013fdb7ba',
                    projectId    : '5ba159af3df7650013e3d763',
                    billingProjectId : '5b74826ace00de000e667062'
            ],
            pr    : [
                    dataCenter : 'USA',
                    internalTechUser: new TokenServiceCredentials(email: 'dummyone@datasite.com', password: 'Password1!'),
                    reviewerUser: new TokenServiceCredentials(
                            email: 'smartsortrev@datasite.mailinator.com',
                            password: 'Password1!',
                            projectId: '5b73265ef164b70013a30a63'
                    ),
                    adminUser   : new TokenServiceCredentials(
                            email: 'smartsortadm@datasite.mailinator.com',
                            password: 'Password1!',
                            projectId: '5e83514b026f316f64def8d9'
                    ),
                    mq          : new MQConfigs(
                            metadataEventsExchange: 'pr-ds1-metadata-exchange',
                            smartSortQueue: 'pr-smart-sort-queue',
                            smartSortRoutingKey: 'done',
                            metadataCategorizationExchange: 'pr-smart-sort-categorization-exchange',
                            metadataCategorizationQueue: 'pr-smart-sort-categorization-queue',
                            metadataCategorizationRoutingKey: 'smart-sort-categorization-key',
                            smartSortDeleteQueue:"pr-smart-sort-delete-queue",
                            smartSortDeleteKey:"delete",
                            smartCategorizationResultExchange:'pr-ds1-smartsort-result-exchange',
                            smartCategorizationResultTestQueue:'pr-ds1-smartsort-result-queue'
                    ),

            ],
            dev   : [
                    dataCenter : 'USA',
                    internalTechUser: new TokenServiceCredentials(email: 'dummyone@datasite.com', password: 'Password1!'),
                    reviewerUser: new TokenServiceCredentials(
                            email: 'smartsortrev@datasite.mailinator.com',
                            password: 'Password1!',
                            projectId: '5b73265ef164b70013a30a63'
                    ),
                    adminUser   : new TokenServiceCredentials(
                            email: 'smartsortadm@datasite.mailinator.com',
                            password: 'Password1!',
                            projectId: '5e83514b026f316f64def8d9'
                    ),
                    mq          : new MQConfigs(
                            metadataEventsExchange: 'pr-ds1-metadata-exchange',
                            smartSortQueue: 'pr-smart-sort-queue',
                            smartSortRoutingKey: 'done',
                            metadataCategorizationExchange: 'pr-smart-sort-categorization-exchange',
                            metadataCategorizationQueue: 'pr-smart-sort-categorization-queue',
                            metadataCategorizationRoutingKey: 'smart-sort-categorization-key',
                            smartSortDeleteQueue:"pr-smart-sort-delete-queue",
                            smartSortDeleteKey:"delete",
                            smartCategorizationResultExchange:'pr-ds1-smartsort-result-exchange',
                            smartCategorizationResultTestQueue:'pr-ds1-smartsort-result-queue'
                    ),
            ],
            stage : [
                    dataCenter : 'USA',
                    internalTechUser: new TokenServiceCredentials(email: 'dummyone@datasite.com', password: 'Password1!'),
                    reviewerUser: new TokenServiceCredentials(
                            email: 'smartsortrev@datasite.mailinator.com',
                            password: 'Password1!',
                            projectId: '5c477343d0796d000e77feee'
                    ),
                    adminUser   : new TokenServiceCredentials(
                            email: 'smartsortadm@datasite.mailinator.com',
                            password: 'Password1!',
                            projectId: '5e834e4845137753ec3ddad4'
                    ),
                    mq          : new MQConfigs(
                            metadataEventsExchange: 'pr-ds1-metadata-exchange',
                            smartSortQueue: 'pr-smart-sort-queue',
                            smartSortRoutingKey: 'done',
                            metadataCategorizationExchange: 'pr-smart-sort-categorization-exchange',
                            metadataCategorizationQueue: 'pr-smart-sort-categorization-queue',
                            metadataCategorizationRoutingKey: 'smart-sort-categorization-key',
                            smartSortDeleteQueue:"pr-smart-sort-delete-queue",
                            smartSortDeleteKey:"delete",
                            smartCategorizationResultExchange:'pr-ds1-smartsort-result-exchange',
                            smartCategorizationResultTestQueue:'pr-ds1-smartsort-result-queue'
                    ),
            ],
            produs: [
                    dataCenter : 'USA',
                    internalTechUser: new TokenServiceCredentials(email: 'dummyone@datasite.com', password: 'Password1!'),
                    reviewerUser: new TokenServiceCredentials(
                            email: 'smartsortrev@datasite.mailinator.com',
                            password: 'Password1!',
                            projectId: '5c4774de3c0df500140305c3'
                    ),
                    adminUser   : new TokenServiceCredentials(
                            email: 'smartsortadm@datasite.mailinator.com',
                            password: 'Password1!',
                            projectId: '5e835215148e8d613f3faaa4'
                    ),
                    mq          : new MQConfigs(
                            metadataEventsExchange: 'pr-ds1-metadata-exchange',
                            smartSortQueue: 'pr-smart-sort-queue',
                            smartSortRoutingKey: 'done',
                            metadataCategorizationExchange: 'pr-smart-sort-categorization-exchange',
                            metadataCategorizationQueue: 'pr-smart-sort-categorization-queue',
                            metadataCategorizationRoutingKey: 'smart-sort-categorization-key',
                            smartSortDeleteQueue:"pr-smart-sort-delete-queue",
                            smartSortDeleteKey:"delete",
                            smartCategorizationResultExchange:'pr-ds1-smartsort-result-exchange',
                            smartCategorizationResultTestQueue:'pr-ds1-smartsort-result-queue'
                    ),
            ],

            prodeu: [
                    dataCenter : 'DEU',
                    internalTechUser: new TokenServiceCredentials(email: 'dummyone@datasite.com', password: 'Password1!'),
                    reviewerUser: new TokenServiceCredentials(
                            email: 'smartsortrev@datasite.mailinator.com',
                            password: 'Password1!',
                            projectId: '5c4774de3c0df500140305c3'
                    ),
                    adminUser   : new TokenServiceCredentials(
                            email: 'smartsortadm@datasite.mailinator.com',
                            password: 'Password1!',
                            projectId: '5e8352fce9b7247949184fc9'
                    ),
                    mq          : new MQConfigs(
                            metadataEventsExchange: 'pr-ds1-metadata-exchange',
                            smartSortQueue: 'pr-smart-sort-queue',
                            smartSortRoutingKey: 'done',
                            metadataCategorizationExchange: 'pr-smart-sort-categorization-exchange',
                            metadataCategorizationQueue: 'pr-smart-sort-categorization-queue',
                            metadataCategorizationRoutingKey: 'smart-sort-categorization-key',
                            smartSortDeleteQueue:"pr-smart-sort-delete-queue",
                            smartSortDeleteKey:"delete",
                            smartCategorizationResultExchange:'pr-ds1-smartsort-result-exchange',
                            smartCategorizationResultTestQueue:'pr-ds1-smartsort-result-queue'
                    ),
            ]
    ]

    // *****
    // A list of project ids which shouldn't be used or deleted. This can be removed once the environment specific
    // project ids are removed.
    // *****
    def protectedProjects = [
            '5b73265ef164b70013a30a63',
            '5c477343d0796d000e77feee',
            '5c4774de3c0df500140305c3'
    ]

    AcceptanceSpecConfiguration() {
        super(8025)
        targetEnv = getSystemValue('TARGET_ENVIRONMENT', 'local')
        //------------- Set up block for acceptance test with RabbitMQ -------//
        if (targetEnvironment == 'local') {
            rabbitUri = 'amqp://localhost:5672'
            smartSortServiceClient = 'http://localhost:8025'
        } else {
            def pcfHelper = new CloudFoundryHelper()
            rabbitUri = pcfHelper.getRabbitUriFromPCF(targetService)
            println('**************** targetService : ' + targetService)

            smartSortServiceClient = buildServiceUrl('smart-sort-service')
        }

        println('**************** environmentKey : ' +  environmentKey)
        println('**************** targetService : ' + targetService)

        internalTechUser = configByEnvironment[environmentKey].internalTechUser
        reviewerUser = configByEnvironment[environmentKey].reviewerUser
        adminUser = configByEnvironment[environmentKey].adminUser
        mqConfigs = configByEnvironment[environmentKey].mq
        projectId = configByEnvironment[environmentKey].projectId
        dataCenter = configByEnvironment[environmentKey].dataCenter

        targetProjectService = buildServiceUrl('project-composite-service')
        azureblobdownloadServiceUrl = buildServiceUrl('azure-blob-download-service')
        azureblobdeleteServiceUrl = buildServiceUrl('azureblobdelete-service')
        coreMetadataService = buildServiceUrl('doc-metadata-service')
        redactionServiceUrl= buildServiceUrl('redaction-service')
        projectCompositeServiceUrl = buildServiceUrl('project-composite-service')
        redactedTermsSearchServiceUrl = buildServiceUrl('redacted-terms-search-service')
    }
}
