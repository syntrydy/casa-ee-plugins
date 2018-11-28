package org.gluu.casa.plugins.accounts.vm;

import org.codehaus.jackson.map.ObjectMapper;
import org.gluu.casa.misc.Utils;
import org.gluu.casa.misc.WebUtils;
import org.gluu.casa.service.ILdapService;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.bind.annotation.Init;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.select.annotation.WireVariable;

/**
 * @author jgomer
 */
public class SiteRedirectViewModel {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @WireVariable
    private ILdapService ldapService;

    private ObjectMapper mapper = new ObjectMapper();

    private String serverUrl;

    private String text;

    public String getText() {
        return text;
    }

    @Init
    public void init() {

        logger.debug("Initializing ViewModel");
        String provider = WebUtils.getQueryParam("provider");
        text = Labels.getLabel("sociallogin.link_redirect_failed", new String[]{provider});

        //One might check in pendingLinks if the userId/provider pair exists, but it may not be set yet when this code runs
        if (Utils.isNotEmpty(provider) ) {
            serverUrl = ldapService.getIssuerUrl();
            String url = getRedirectUrl(provider);

            if (url != null) {
                WebUtils.execRedirect(url);
            }
        }

    }

    private String getRedirectUrl(String provider) {
        String token = getPassportToken();
        return Utils.isEmpty(token) ? null : String.format("%s/passport/casa/%s/%s", serverUrl, provider, token);
    }

    private String getPassportToken() {

        try {
            ResteasyClient client = new ResteasyClientBuilder().build();
            String url = String.format("%s/passport/token", serverUrl);
            logger.info("Requesting token at {}", url);

            ResteasyWebTarget target = client.target(url);
            String data = target.request().get(String.class);
            return mapper.readTree(data).get("token_").asText();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return null;
        }

    }

}
