/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.core.security.action.saml;

import java.io.IOException;

import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

/**
 * Response to an IdP-initiated SAML {@code &lt;LogoutRequest&gt;}
 */
public final class SamlInvalidateSessionResponse extends ActionResponse {

    private String realmName;
    private int count;
    private String redirectUrl;

    public SamlInvalidateSessionResponse() {
    }

    public SamlInvalidateSessionResponse(String realmName, int count, String redirectUrl) {
        this.realmName = realmName;
        this.count = count;
        this.redirectUrl = redirectUrl;
    }

    public String getRealmName() {
        return realmName;
    }

    public int getCount() {
        return count;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeString(realmName);
        out.writeInt(count);
        out.writeString(redirectUrl);
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        realmName = in.readString();
        count = in.readInt();
        redirectUrl = in.readString();
    }

}
