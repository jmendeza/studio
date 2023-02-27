/*
 * Copyright (C) 2007-2023 Crafter Software Corporation. All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as published by
 * the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.craftercms.studio.model.rest.content;

import org.craftercms.commons.validation.annotations.param.EsapiValidatedParam;
import org.craftercms.commons.validation.annotations.param.ValidSiteId;
import org.craftercms.commons.validation.annotations.param.ValidateSecurePathParam;

import javax.validation.constraints.NotEmpty;

import static org.craftercms.commons.validation.annotations.param.EsapiValidationType.CONTENT_PATH_WRITE;

public class UnlockItemByPathRequest {

    @NotEmpty
    @ValidSiteId
    private String siteId;
    @NotEmpty
    @ValidateSecurePathParam
    @EsapiValidatedParam(type = CONTENT_PATH_WRITE)
    private String path;

    public String getSiteId() {
        return siteId;
    }

    public void setSiteId(String siteId) {
        this.siteId = siteId;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
