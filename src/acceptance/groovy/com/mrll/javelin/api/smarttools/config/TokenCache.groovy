package com.mrll.javelin.api.smarttools.config

import com.mrll.javelin.common.test.config.JwtEngineerotron
import com.mrll.javelin.common.test.config.TokenServiceCredentials

class TokenCache {
    AcceptanceSpecConfiguration configuration
    JwtEngineerotron jwtEngineerotron
    Map<TestUser, Map<String, String>> tokens = new HashMap<>()

    TokenCache(AcceptanceSpecConfiguration acceptanceSpecConfiguration) {
        configuration = acceptanceSpecConfiguration
        jwtEngineerotron = new JwtEngineerotron(configuration)
    }

    def getToken(TokenServiceCredentials user, String projectId) {
        Map<String, String> projectIdTokenCache = tokens.get(user, [:])

        String token = projectIdTokenCache[projectId]
        if (token == null) {
            token = jwtEngineerotron.generateToken(user, projectId)
            projectIdTokenCache[projectId] = token
        }

        return token
    }
}
