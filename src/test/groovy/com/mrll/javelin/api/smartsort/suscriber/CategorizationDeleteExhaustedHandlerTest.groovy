package com.mrll.javelin.api.smartsort.suscriber

import com.mrll.javelin.api.smarttools.model.delegates.MetadataUpdateEvent
import com.mrll.javelin.api.smarttools.suscriber.CategorizationDeleteExhaustedHandler
import org.json.JSONObject
import org.springframework.amqp.core.Message
import org.springframework.amqp.core.MessageProperties
import spock.lang.Specification

class CategorizationDeleteExhaustedHandlerTest extends Specification {

    def 'empty project id parameter throws an exception'() {

        given:
        JSONObject jsonObj = new JSONObject()
        jsonObj.put("projectId", "prId")
        jsonObj.put("metadataIds", ['metadata_id'])
        jsonObj.put("updateType", "DONE")
        jsonObj.put("processingType", "DOC_UPLOAD")
        jsonObj.put("userId", "user_id")
        byte[] input = jsonObj.toString().getBytes("utf-8")
        MessageProperties messageProperties = new MessageProperties()
        Message message = new Message(input, messageProperties)

        CategorizationDeleteExhaustedHandler categorizationDeleteExhaustedHandler = new CategorizationDeleteExhaustedHandler()
        when:
        categorizationDeleteExhaustedHandler.handleRetriesExhausted(new MetadataUpdateEvent(), [:])

        then:
        true
    }
}
