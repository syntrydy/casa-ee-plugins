package org.gluu.casa.plugins.accounts.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.gluu.casa.core.ldap.oxCustomScript;
import org.gluu.casa.misc.Utils;
import org.gluu.casa.plugins.accounts.ldap.ExternalIdentityPerson;
import org.gluu.casa.plugins.accounts.pojo.LinkingSummary;
import org.gluu.casa.plugins.accounts.pojo.PassportScriptProperties;
import org.gluu.casa.plugins.accounts.pojo.PendingLinks;
import org.gluu.casa.plugins.accounts.pojo.ProviderType;
import org.gluu.casa.service.ILdapService;
import org.gluu.casa.service.ISessionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxauth.model.common.WebKeyStorage;
import org.xdi.oxauth.model.configuration.AppConfiguration;
import org.xdi.oxauth.model.crypto.CryptoProviderFactory;
import org.xdi.oxauth.model.jwt.Jwt;
import org.zkoss.util.resource.Labels;
import org.zkoss.web.servlet.http.Encodes;

import javax.ws.rs.*;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author jgomer
 */
@Path("/idp-linking")
public class LinkingService {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private ObjectMapper mapper = new ObjectMapper();

    private ILdapService ldapService;

    private Map<ProviderType, PassportScriptProperties> passportProperties;

    @Context
    private UriInfo uriInfo;

    public LinkingService() {

        try {
            logger.info("Creating an instance of LinkingService");
            mapper = new ObjectMapper();
            ldapService = Utils.managedBean(ILdapService.class);

            passportProperties = new HashMap<>();
            for (ProviderType pt : ProviderType.values()) {

                PassportScriptProperties psp = new PassportScriptProperties();
                oxCustomScript script = new oxCustomScript();
                script.setDisplayName(pt.getAcr());

                List<oxCustomScript> list = ldapService.find(script, oxCustomScript.class, ldapService.getCustomScriptsDn());
                script = list.size() > 0  ? list.get(0) : null;

                if (script != null) {
                    Map<String, String> props = Utils.scriptConfigPropertiesAsMap(script);
                    psp.setKeyStoreFile(props.get("key_store_file"));
                    psp.setKeyStorePassword(props.get("key_store_password"));

                    int i = Utils.firstTrue(Arrays.asList(props.get("generic_local_attributes_list").split(",\\s*")), "uid"::equals);
                    psp.setRemoteUserNameAttribute(i >= 0 ? props.get("generic_remote_attributes_list").split(",\\s*")[i] : "id");

                    passportProperties.put(pt, psp);
                }
            }

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            logger.warn("Service for linking external identities may not work properly");
        }

    }

    @GET
    public Response processError(@QueryParam("failure") String msg) throws Exception {
        logger.warn("An error occurred: {}", msg);
        return Response.serverError().entity(msg).build();
    }

    @POST
    @Path("{provider}")
    public Response doLink(@FormParam("user") String userJwt, @PathParam("provider") String provider) throws Exception {

        LinkingSummary summary = new LinkingSummary();
        String msg = null;

        ISessionContext sessionContext = Utils.managedBean(ISessionContext.class);
        String userId = sessionContext.getLoggedUser().getId();
        logger.info("Linking provider {} to user {} ...", provider, userId);

        try {
            if (PendingLinks.contains(userId, provider)) {
                PassportScriptProperties psp = passportProperties.get(
                        AvailableProviders.get().stream().filter(p -> p.getName().equals(provider)).findFirst().get().getType());

                Jwt jwt = validateJWT(userJwt, psp);
                if (jwt != null) {
                    logger.info("user profile JWT validated successfully\n{}", jwt);
                    String profile = jwt.getClaims().getClaimAsString("data");

                    String remoteUserNameAttribute = psp.getRemoteUserNameAttribute();
                    String uid = mapper.readTree(profile).get(remoteUserNameAttribute).asText();

                    //Verify it's not already enrolled by someone
                    if (!alreadyAssigned(provider, uid)) {
                        summary.setProvider(provider);
                        summary.setUid(uid);
                    } else {
                        msg = Labels.getLabel("sociallogin.link_result.already_taken", new String[]{uid, provider});
                        logger.warn(msg);
                    }
                } else {
                    msg = Labels.getLabel("sociallogin.link_result.validation_failed");
                    logger.error(msg);
                }
            } else {
                msg = Labels.getLabel("sociallogin.link_result.unexpected_provider", new String[]{provider});
                logger.warn(msg);
            }
        } catch (Exception e) {
            msg = e.getMessage();
            logger.error(msg, e);
        }
        if (msg != null) {
            summary.setErrorMessage(msg);
        }

        //Removes the /idp-linking/{provider} portion
        String url = uriInfo.getAbsolutePath().toString();
        url+= "/../../account-linking-result.zul?provider=" + Encodes.encodeURIComponent(provider);
        URI uri = new URL(url.replaceFirst("/rest", "")).toURI();

        PendingLinks.add(userId, provider, summary);
        logger.debug("Redirecting to {}", uri.toString());
        return Response.seeOther(uri).build();

    }

    private Jwt validateJWT(String encodedJWT, PassportScriptProperties properties) {

        try {
            //Verify JWT
            Jwt jwt = Jwt.parse(encodedJWT);
            AppConfiguration appCfg = new AppConfiguration();
            appCfg.setWebKeysStorage(WebKeyStorage.KEYSTORE);
            appCfg.setKeyStoreFile(properties.getKeyStoreFile());
            appCfg.setKeyStoreSecret(properties.getKeyStorePassword());

            return CryptoProviderFactory.getCryptoProvider(appCfg).verifySignature(jwt.getSigningInput(), jwt.getEncodedSignature(),
                    jwt.getHeader().getKeyId(), null, null, jwt.getHeader().getAlgorithm()) ? jwt : null;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return null;
        }

    }

    private boolean alreadyAssigned(String provider, String uid) {

        String value = String.format("passport-%s:%s", provider, uid);
        ExternalIdentityPerson p = new ExternalIdentityPerson();
        p.setOxExternalUid(value);
        return ldapService.find(p, ExternalIdentityPerson.class, ldapService.getPeopleDn()).size() > 0;

    }

}
