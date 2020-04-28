package com.mrll.javelin.api.smarttools.config

import com.google.common.base.MoreObjects

class TestUser {
    String password
    String email
    String contentGroupId
    String serviceId
    String projectId

    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        TestUser testUser = (TestUser) o

        if (contentGroupId != testUser.contentGroupId) return false
        if (email != testUser.email) return false
        if (password != testUser.password) return false
        if (serviceId != testUser.serviceId) return false
        if (projectId != testUser.projectId) return false

        return true
    }

    int hashCode() {
        int result
        result = (password != null ? password.hashCode() : 0)
        result = 31 * result + (email != null ? email.hashCode() : 0)
        result = 31 * result + (contentGroupId != null ? contentGroupId.hashCode() : 0)
        result = 31 * result + (serviceId != null ? serviceId.hashCode() : 0)
        result = 31 * result + (projectId != null ? projectId.hashCode() : 0)
        return result
    }

    @Override
    String toString() {
        return MoreObjects.toStringHelper(this)
                .add("password", password)
                .add("email", email)
                .add("contentGroupId", contentGroupId)
                .add("serviceId", serviceId)
        .add('projectId',projectId)
                .toString();
    }
}