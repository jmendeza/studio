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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.craftercms.commons.validation.annotations.param.EsapiValidatedParam;
import org.craftercms.commons.validation.annotations.param.ValidateSecurePathParam;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;

import static org.craftercms.commons.validation.annotations.param.EsapiValidationType.HTTPURI;
import static org.craftercms.commons.validation.annotations.param.EsapiValidationType.SITE_ID;

/**
 * Holds data for the getSandboxItemsByPath request
 *
 * @author joseross
 * @since 4.0
 */
@JsonIgnoreProperties
public class GetSandboxItemsByPathRequestBody {

    @NotEmpty
    @EsapiValidatedParam(type = SITE_ID)
    private String siteId;
    @NotEmpty
    private List<@ValidateSecurePathParam @EsapiValidatedParam(type = HTTPURI) @NotEmpty String> paths;
    private boolean preferContent;

    public String getSiteId() {
        return siteId;
    }

    public void setSiteId(String siteId) {
        this.siteId = siteId;
    }

    public List<String> getPaths() {
        return paths;
    }

    public void setPaths(List<String> paths) {
        this.paths = paths;
    }

    public boolean isPreferContent() {
        return preferContent;
    }

    public void setPreferContent(boolean preferContent) {
        this.preferContent = preferContent;
    }
}
