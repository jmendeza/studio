/*
 * Copyright (C) 2007-2022 Crafter Software Corporation. All Rights Reserved.
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

/**
 * @author Dejan Brkic
 */

import org.apache.commons.lang3.StringUtils
import scripts.api.DeploymentServices

// extract parameters
def result = [:]
def sort = params.sort
def site = params.site
def ascending = params.ascending.toBoolean()
def filterType = params.filterType

/** Validate Parameters */
def invalidParams = false
def paramsList = []

// site_id
try {
    if (StringUtils.isEmpty(site)) {
        site = params.site
        if (StringUtils.isEmpty(site)) {
            invalidParams = true
            paramsList.add("site_id")
        }
    }
} catch (Exception e) {
    invalidParams = true
    paramsList.add("site_id")
}

if (invalidParams) {
    response.setStatus(400)
    result.message = "Invalid parameter(s): " + paramsList
} else {
    def context = DeploymentServices.createContext(applicationContext, request)
    def items = DeploymentServices.getScheduledItems(context, site, sort, ascending, "internalName", true, filterType)
    def total = 0
    for (task in items) {
        total += task.numOfChildren
    }
    result.total = total
    result.sortedBy = sort
    result.ascending = ascending
    result.documents = items
}
return result
