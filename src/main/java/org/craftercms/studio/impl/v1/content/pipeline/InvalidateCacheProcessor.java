/*
 * Crafter Studio Web-content authoring solution
 * Copyright (C) 2007-2014 Crafter Software Corporation.
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
 */
package org.craftercms.studio.impl.v1.content.pipeline;

import org.craftercms.studio.api.v1.content.pipeline.PipelineContent;
import org.craftercms.studio.api.v1.exception.ContentProcessException;
import org.craftercms.studio.api.v1.log.Logger;
import org.craftercms.studio.api.v1.log.LoggerFactory;
import org.craftercms.studio.api.v1.to.ResultTO;

/**
 * Invalidate cache of the content being processed
 * TODO: currently this is only being used for assets. should refactor the code to use for form contents as well
 *
 * @author hyanghee
 * @author Dejan Brkic
 *
 */
public class InvalidateCacheProcessor extends BaseContentProcessor {

    private static final Logger logger = LoggerFactory.getLogger(InvalidateCacheProcessor.class);

    public static final String NAME = "InvalidateCacheProcessor";



    /**
     * default constructor
     */
    public InvalidateCacheProcessor() {
        super(NAME);
    }

    /**
     * constructor that sets the process name
     *
     * @param name
     */
    public InvalidateCacheProcessor(String name) {
        super(name);
    }

    public void process(PipelineContent content, ResultTO result) throws ContentProcessException {
        result.setInvalidateCache(true);
        /** Disabled **/
        /*
        String site = content.getProperty(DmConstants.KEY_SITE);
        String folderPath = content.getProperty(DmConstants.KEY_FOLDER_PATH);
        String fileName = content.getProperty(DmConstants.KEY_FILE_NAME);
        String path = (folderPath.endsWith("/")) ? folderPath + fileName : folderPath + "/" + fileName;
        String fullPath = _dmContentService.getContentFullPath(site, path);
        _cache.invalidate(Scope.DM_CONTENT_ITEM, fullPath);
        */
    }
}
