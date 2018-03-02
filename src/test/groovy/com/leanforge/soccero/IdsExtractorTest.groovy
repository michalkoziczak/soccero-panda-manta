package com.leanforge.soccero

import spock.lang.Specification

class IdsExtractorTest extends Specification {
    def "should extract slack ids"() {
        given:
        def message = '<@ID1> <@ID2> <@ID3> <@ID4>'

        when:
        def ids = IdsExtractor.INSTANCE.extractIds(message)

        then:
        ids == ['ID1', 'ID2', 'ID3', 'ID4']
    }
}
