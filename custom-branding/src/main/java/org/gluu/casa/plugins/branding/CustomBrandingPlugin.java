package org.gluu.casa.plugins.branding;

import org.gluu.casa.misc.Utils;
import org.gluu.casa.service.IBrandingManager;
import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The plugin for custom branding Gluu Casa.
 * @author jgomer
 */
public class CustomBrandingPlugin extends Plugin {

    private Logger logger = LoggerFactory.getLogger(getClass());

    public CustomBrandingPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    //TODO: see issue #6
    public void delete() {

        try {
            IBrandingManager brandingManager = Utils.managedBean(IBrandingManager.class);
            brandingManager.factoryReset();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

    }

}
