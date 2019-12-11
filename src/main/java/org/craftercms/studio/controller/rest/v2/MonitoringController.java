/*
 * Copyright (C) 2007-2019 Crafter Software Corporation. All Rights Reserved.
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

package org.craftercms.studio.controller.rest.v2;

import org.apache.commons.lang3.StringUtils;
import org.craftercms.commons.exceptions.InvalidManagementTokenException;
import org.craftercms.commons.monitoring.MemoryInfo;
import org.craftercms.commons.monitoring.StatusInfo;
import org.craftercms.commons.monitoring.VersionInfo;
import org.craftercms.commons.monitoring.rest.MonitoringRestControllerBase;
import org.craftercms.engine.util.logging.CircularQueueLogAppender;
import org.craftercms.studio.api.v1.service.security.SecurityService;
import org.craftercms.studio.api.v2.utils.StudioConfiguration;
import org.craftercms.studio.model.rest.ApiResponse;
import org.craftercms.studio.model.rest.ResultList;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

import static org.craftercms.engine.controller.rest.MonitoringController.LOG_URL;
import static org.craftercms.studio.api.v2.utils.StudioConfiguration.CONFIGURATION_MANAGEMENT_AUTHORIZATION_TOKEN;
import static org.craftercms.studio.controller.rest.v2.ResultConstants.RESULT_KEY_EVENTS;

/**
 * Rest controller to provide monitoring information
 * @author joseross
 */
@RestController
@RequestMapping("/api/2")
public class MonitoringController extends MonitoringRestControllerBase {

    private StudioConfiguration studioConfiguration;
    private SecurityService securityService;

    @GetMapping(ROOT_URL + MEMORY_URL)
    public MemoryInfo getCurrentMemory(@RequestParam(name = "token", required = false) String token)
            throws InvalidManagementTokenException {
        if (StringUtils.isNotEmpty(securityService.getCurrentUser()) ||
                (StringUtils.isNotEmpty(token) && StringUtils.equals(token, getConfiguredToken()))) {
            return MemoryInfo.getCurrentMemory();
        } else {
            throw new InvalidManagementTokenException("Management authorization failed, invalid token.");
        }
    }

    @GetMapping(ROOT_URL + STATUS_URL)
    public StatusInfo getCurrentStatus(@RequestParam(name = "token", required = false) String token)
            throws InvalidManagementTokenException {
        if (StringUtils.isNotEmpty(securityService.getCurrentUser()) ||
                (StringUtils.isNotEmpty(token) && StringUtils.equals(token, getConfiguredToken()))) {
            return StatusInfo.getCurrentStatus();
        } else {
            throw new InvalidManagementTokenException("Management authorization failed, invalid token.");
        }
    }

    @GetMapping(ROOT_URL + VERSION_URL)
    public VersionInfo getCurrentVersion(@RequestParam(name = "token", required = false) String token)
            throws InvalidManagementTokenException, IOException {
        if (StringUtils.isNotEmpty(securityService.getCurrentUser()) ||
                (StringUtils.isNotEmpty(token) && StringUtils.equals(token, getConfiguredToken()))) {
            return VersionInfo.getVersion(this.getClass());
        } else {
            throw new InvalidManagementTokenException("Management authorization failed, invalid token.");
        }
    }

    @GetMapping(ROOT_URL + LOG_URL)
    public ResultList<Map<String,Object>> getLogEvents(@RequestParam long since, @RequestParam String token)
            throws InvalidManagementTokenException {
        if (StringUtils.isNotEmpty(securityService.getCurrentUser()) ||
                (StringUtils.isNotEmpty(token) && StringUtils.equals(token, getConfiguredToken()))) {
            ResultList<Map<String, Object>> result = new ResultList<>();
            result.setResponse(ApiResponse.OK);
            result.setEntities(RESULT_KEY_EVENTS, CircularQueueLogAppender.getLoggedEvents("craftercms", since));
            return result;
        } else {
            throw new InvalidManagementTokenException("Management authorization failed, invalid token.");
        }
    }

    @Override
    protected String getConfiguredToken() {
        return studioConfiguration.getProperty(CONFIGURATION_MANAGEMENT_AUTHORIZATION_TOKEN);
    }

    public StudioConfiguration getStudioConfiguration() {
        return studioConfiguration;
    }

    public void setStudioConfiguration(StudioConfiguration studioConfiguration) {
        this.studioConfiguration = studioConfiguration;
    }

    public SecurityService getSecurityService() {
        return securityService;
    }

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }
}
