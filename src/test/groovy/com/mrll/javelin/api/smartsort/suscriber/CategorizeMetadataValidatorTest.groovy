package com.mrll.javelin.api.smartsort.suscriber

import com.mrll.javelin.api.smarttools.model.delegates.MetadataUpdateEvent
import com.mrll.javelin.api.smarttools.model.delegates.ProcessingAction
import com.mrll.javelin.api.smarttools.model.delegates.UpdateType
import com.mrll.javelin.api.smarttools.suscriber.CategorizeMetadataValidator
import spock.lang.Specification
import spock.lang.Subject

class CategorizeMetadataValidatorTest extends Specification {

    @Subject
    CategorizeMetadataValidator categorizeMetadataValidator = new CategorizeMetadataValidator()

    def 'all message properties are valid'() {

        given:
        def message = new MetadataUpdateEvent(projectId: 'project_id_1', updateType: UpdateType.DONE, processingType: ProcessingAction.DOC_REPLACE, metadataIds: ['m1'])

        when:
        categorizeMetadataValidator.validateMessage(message)

        then:
        notThrown RuntimeException
    }

    def 'missing project id parameter throws an exception'() {

        given:
        def message = new MetadataUpdateEvent( updateType: UpdateType.DONE, metadataIds: ['m1'])


        when:
        categorizeMetadataValidator.validateMessage(message)

        then:
        thrown RuntimeException
    }

    def 'empty project id parameter throws an exception'() {

        given:
        def message = new MetadataUpdateEvent( projectId: '', updateType: UpdateType.DONE, processingType: ProcessingAction.DOC_REPLACE, metadataIds: ['m1'])


        when:
        categorizeMetadataValidator.validateMessage(message)

        then:
        thrown RuntimeException
    }

    def 'missing metadata ids parameter throws an exception'() {

        given:
        def message = new MetadataUpdateEvent(projectId: 'project_id_1', updateType: UpdateType.DONE, processingType: ProcessingAction.DOC_REPLACE)


        when:
        categorizeMetadataValidator.validateMessage(message)

        then:
        thrown RuntimeException
    }
}
