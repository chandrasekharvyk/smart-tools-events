package com.mrll.javelin.api.smartsort.messagingproxyfilter

import com.mrll.javelin.api.smarttools.messagingproxyfilter.EventProxyFilterMessageFactory
import com.mrll.javelin.api.smarttools.messagingproxyfilter.EventProxyFilterMessageFactory
import org.json.JSONArray
import org.json.JSONObject
import org.springframework.amqp.core.Message
import org.springframework.amqp.core.MessageProperties
import spock.lang.Specification
import spock.lang.Subject

class EventProxyFilterMessageFactoryTest extends Specification {

    @Subject
    EventProxyFilterMessageFactory eventProxyFilterMessageFactory = new EventProxyFilterMessageFactory()

    def 'Successful message creation'() {

        given: 'valid messaging event'
        def metadataUpdateEventMsg
        JSONObject jsonObj = new JSONObject()
        jsonObj.put("projectId", "prId")
        jsonObj.put("metadataIds", new JSONArray(["metadata_id"]))
        jsonObj.put("updateType", "DONE")
        jsonObj.put("processingType", "DOC_UPLOAD")
        jsonObj.put("userId", "user_id")
        byte[] input = jsonObj.toString().getBytes("utf-8")
        MessageProperties messageProperties = new MessageProperties()
        Message message = new Message(input, messageProperties)

        when:
        metadataUpdateEventMsg = eventProxyFilterMessageFactory.createMessage(message)

        then:
        metadataUpdateEventMsg.userId == 'user_id'
        metadataUpdateEventMsg.metadataIds.get(0) == 'metadata_id'
        metadataUpdateEventMsg.updateType == 'DONE'
        metadataUpdateEventMsg.processingType == 'DOC_UPLOAD'
    }

    def 'Failed to create message'() {

        given:
        MessageProperties messageProperties = new MessageProperties()
        Message message = new Message(null, messageProperties)

        when:
        eventProxyFilterMessageFactory.createMessage(message)

        then:
        thrown RuntimeException
    }

}
