/*
 * Copyright (C) 2007-2018 Crafter Software Corporation. All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.craftercms.studio.impl.v2.upgrade.pipeline;

import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.craftercms.commons.config.YamlConfiguration;
import org.craftercms.studio.api.v2.exception.UpgradeException;
import org.craftercms.studio.api.v2.upgrade.UpgradeOperation;
import org.craftercms.studio.api.v2.upgrade.UpgradePipeline;
import org.craftercms.studio.api.v2.upgrade.UpgradePipelineFactory;
import org.craftercms.studio.api.v2.upgrade.VersionProvider;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;

import static org.craftercms.studio.api.v2.upgrade.UpgradeConstants.CONFIG_KEY_OPERATIONS;
import static org.craftercms.studio.api.v2.upgrade.UpgradeConstants.CONFIG_KEY_TYPE;
import static org.craftercms.studio.api.v2.upgrade.UpgradeConstants.CONFIG_KEY_VERSION;

/**
 * Default implementation of {@link UpgradePipelineFactory}.
 * @author joseross
 */
public class DefaultUpgradePipelineFactoryImpl implements UpgradePipelineFactory, ApplicationContextAware {

    /**
     * Path of the configuration file.
     */
    protected Resource configurationFile;

    /**
     * The application context.
     */
    protected ApplicationContext appContext;

    /**
     * Prefix used in the configuration file.
     */
    protected String pipelinePrefix;

    /**
     * Name used in the configuration file.
     */
    protected String pipelineName;

    public DefaultUpgradePipelineFactoryImpl() {
        //Default constructor
    }

    public DefaultUpgradePipelineFactoryImpl(final String pipelineName) {
        this.pipelineName = pipelineName;
    }

    @Required
    public void setPipelinePrefix(final String pipelinePrefix) {
        this.pipelinePrefix = pipelinePrefix;
    }

    public void setPipelineName(final String pipelineName) {
        this.pipelineName = pipelineName;
    }

    @Required
    public void setConfigurationFile(final Resource configurationFile) {
        this.configurationFile = configurationFile;
    }

    public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
        appContext = applicationContext;
    }

    protected HierarchicalConfiguration loadUpgradeConfiguration() throws UpgradeException {
        YamlConfiguration configuration = new YamlConfiguration();
        try (InputStream is = configurationFile.getInputStream()) {
            configuration.read(is);
        } catch (Exception e) {
            throw  new UpgradeException("Error reading configuration file", e);
        }
        return configuration;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public UpgradePipeline getPipeline(VersionProvider versionProvider) throws UpgradeException {
        String currentVersion = versionProvider.getCurrentVersion();
        List<UpgradeOperation> operations = new LinkedList<>();
        HierarchicalConfiguration config = loadUpgradeConfiguration();
        List<HierarchicalConfiguration> pipeline = config.configurationsAt(pipelinePrefix + "." + pipelineName);

        boolean versionFound = false;
        for(HierarchicalConfiguration release : pipeline) {
            String version = release.getString(CONFIG_KEY_VERSION);
            if(!version.equals(currentVersion)) {
                if(versionFound) {
                    List<HierarchicalConfiguration> operationsConfig = release.configurationsAt(CONFIG_KEY_OPERATIONS);
                    operationsConfig.forEach(operationConfig -> {
                        UpgradeOperation operation =
                            appContext.getBean(operationConfig.getString(CONFIG_KEY_TYPE), UpgradeOperation.class);
                        operation.init(version, operationConfig);
                        operations.add(operation);
                    });
                }
            } else {
                versionFound = true;
            }
        }
        return new DefaultUpgradePipelineImpl(pipelineName, operations);
    }

}
