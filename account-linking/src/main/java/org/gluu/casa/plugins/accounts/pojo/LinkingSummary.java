package org.gluu.casa.plugins.accounts.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * @author jgomer
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LinkingSummary {

    private String provider;
    private String uid;
    private String errorMessage;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

}
