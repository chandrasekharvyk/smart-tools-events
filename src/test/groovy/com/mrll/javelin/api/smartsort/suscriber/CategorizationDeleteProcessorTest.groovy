package com.mrll.javelin.api.smartsort.suscriber

import com.mrll.javelin.api.smarttools.exception.FatalSmartSortException
import com.mrll.javelin.api.smarttools.model.delegates.MetadataUpdateEvent
import com.mrll.javelin.api.smarttools.model.delegates.ProcessingAction
import com.mrll.javelin.api.smarttools.model.delegates.UpdateType
import com.mrll.javelin.api.smarttools.service.CategorizationService
import com.mrll.javelin.api.smarttools.suscriber.CategorizationDeleteProcessor
import spock.lang.Specification
import spock.lang.Subject

class CategorizationDeleteProcessorTest extends Specification {

    @Subject
    static CategorizationDeleteProcessor categorizationDeleteProcessor

    def 'Message successfully processed'() {
        given:
        def message = new MetadataUpdateEvent(projectId: 'project_id_1', updateType: UpdateType.DONE, processingType: ProcessingAction.DOC_REPLACE, metadataIds: ['m1'])
        CategorizationService categorizationService = Mock()
        categorizationDeleteProcessor = new CategorizationDeleteProcessor(categorizationService)

        when:
        categorizationDeleteProcessor.processMessage(message)

        then:
        1 * categorizationService.deleteDocCategory(_)

        notThrown FatalSmartSortException
    }

}
