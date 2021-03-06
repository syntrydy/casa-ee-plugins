package org.gluu.casa.plugins.consent.ldap;

import com.unboundid.ldap.sdk.ReadOnlyEntry;
import com.unboundid.ldap.sdk.persist.FilterUsage;
import com.unboundid.ldap.sdk.persist.LDAPEntryField;
import com.unboundid.ldap.sdk.persist.LDAPField;
import com.unboundid.ldap.sdk.persist.LDAPObject;

import java.util.Optional;

/**
 * This class provides an implementation of an object that can be used to
 * represent oxAuthCustomScope objects in the directory.
 * It was generated by the generate-source-from-schema tool provided with the
 * UnboundID LDAP SDK for Java.  It may be customized as desired to better suit
 * your needs.
 */
@LDAPObject(structuralClass="oxAuthCustomScope",
        superiorClass="top")
public class Scope {

    // The field to use to hold a read-only copy of the associated entry.
    @LDAPEntryField
    private ReadOnlyEntry ldapEntry;

    // The field used for RDN attribute inum.
    @LDAPField(inRDN=true,
            filterUsage= FilterUsage.ALWAYS_ALLOWED,
            requiredForEncode=true)
    private String[] inum;

    // The field used for optional attribute description.
    @LDAPField
    private String[] description;

    // The field used for optional attribute displayName.
    @LDAPField
    private String displayName;

    /**
     * Retrieves the first value for the field associated with the
     * inum attribute, if present.
     *
     * @return  The first value for the field associated with the
     *          inum attribute, or
     *          {@code null} if that attribute was not present in the entry or
     *          does not have any values.
     */
    public String getInum()
    {
        if ((inum == null) ||
                (inum.length == 0))
        {
            return null;
        }
        else
        {
            return inum[0];
        }
    }

    /**
     * Retrieves the first value for the field associated with the
     * description attribute, if present.
     *
     * @return  The first value for the field associated with the
     *          description attribute, or
     *          {@code null} if that attribute was not present in the entry or
     *          does not have any values.
     */
    public String getDescription()
    {
        if ((description == null) ||
                (description.length == 0))
        {
            return null;
        }
        else
        {
            return description[0];
        }
    }

    /**
     * Retrieves the value for the field associated with the
     * displayName attribute, if present.
     *
     * @return  The value for the field associated with the
     *          displayName attribute, or
     *          {@code null} if the field does not have a value.
     */
    public String getDisplayName()
    {
        return displayName;
    }

    @Override
    public boolean equals(Object o) {
        boolean equal = o != null && o instanceof Scope;
        if (equal) {
            String otherName = Scope.class.cast(o).getDisplayName();
            equal = Optional.ofNullable(getDisplayName()).map(name -> name.equals(otherName)).orElse(otherName == null);
        }
        return equal;
    }

}
