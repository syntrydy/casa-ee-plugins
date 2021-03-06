package org.gluu.casa.plugins.accounts.service.enrollment;

import org.gluu.casa.core.ldap.IdentityPerson;
import org.gluu.casa.misc.Utils;
import org.gluu.casa.plugins.accounts.pojo.Provider;

import java.util.*;

/**
 * @author jgomer
 */
public class SamlEnrollmentManager extends AbstractEnrollmentManager {

    private static final String OXEXTERNALUID_PREFIX = "passport-saml:";

    public SamlEnrollmentManager(Provider provider) {
        super(provider);
    }

    public String getUid(IdentityPerson p, boolean linked) {

        List<String> list = Utils.listfromArray(linked ? p.getOxExternalUid() : p.getOxUnlinkedExternalUids());
        for (String externalUid : list) {
            if (externalUid.startsWith(OXEXTERNALUID_PREFIX)) {

                int i = externalUid.lastIndexOf(":");
                if (i > OXEXTERNALUID_PREFIX.length() && i < externalUid.length() - 1) {
                    String providerName = externalUid.substring(OXEXTERNALUID_PREFIX.length(), i);
                    if (provider.getName().equals(providerName)) {
                        return externalUid.substring(i+1);
                    }
                }
            }
        }
        return null;

    }

    public boolean link(IdentityPerson p, String externalId) {

        List<String> list = new ArrayList<>(Utils.listfromArray(p.getOxExternalUid()));
        list.add(getFormatedAttributeVal(externalId));

        logger.info("Linked accounts for {} will be {}", p.getUid(), list);
        p.setOxExternalUid(list.toArray(new String[0]));
        return updatePerson(p);

    }

    public boolean remove(IdentityPerson p) {
        removeProvider(p);
        return updatePerson(p);
    }

    public boolean unlink(IdentityPerson p) {

        String uid = removeProvider(p);
        if (uid == null) {
            return false;
        }

        List<String> list = new ArrayList<>(Utils.listfromArray(p.getOxUnlinkedExternalUids()));
        list.add(getFormatedAttributeVal(uid));
        p.setOxUnlinkedExternalUids(list.toArray(new String[0]));
        return updatePerson(p);

    }

    public boolean enable(IdentityPerson p) {

        String uid = removeProvider(p);
        if (uid == null) {
            return false;
        }

        List<String> list = new ArrayList<>(Utils.listfromArray(p.getOxExternalUid()));
        list.add(getFormatedAttributeVal(uid));
        p.setOxExternalUid(list.toArray(new String[0]));
        return updatePerson(p);

    }

    private String removeProvider(IdentityPerson p) {

        String externalUid = null;
        String pattern = String.format("%s%s:",OXEXTERNALUID_PREFIX, provider.getName());

        Set<String> externalUids = new HashSet<>(Utils.listfromArray(p.getOxExternalUid()));
        Set<String> unlinkedUIds = new HashSet<>(Utils.listfromArray(p.getOxUnlinkedExternalUids()));

        for (String str : externalUids) {
            if (str.startsWith(pattern)) {
                externalUid = str.substring(pattern.length());
                break;
            }
        }

        for (String str : unlinkedUIds) {
            if (str.startsWith(pattern)) {
                externalUid = str.substring(pattern.length());
                break;
            }
        }

        if (externalUid != null) {
            String str = getFormatedAttributeVal(externalUid);
            externalUids.remove(str);
            unlinkedUIds.remove(str);
        }

        p.setOxExternalUid(externalUids.toArray(new String[0]));
        p.setOxUnlinkedExternalUids(unlinkedUIds.toArray(new String[0]));
        return externalUid;

    }

    public boolean isAssigned(String uid) {
        IdentityPerson p = new IdentityPerson();
        p.setOxExternalUid(getFormatedAttributeVal(uid));
        return ldapService.find(p, IdentityPerson.class, ldapService.getPeopleDn()).size() > 0;
    }

    private String getFormatedAttributeVal(String uid) {
        return String.format("passport-saml:%s:%s", provider.getName(), uid);
    }

}
