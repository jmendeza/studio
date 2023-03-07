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

import org.apache.commons.lang3.StringUtils
import org.craftercms.studio.api.v1.exception.BlobNotFoundException
import org.craftercms.studio.api.v1.exception.ContentNotFoundException
import scripts.api.DependencyServices

def result = [:]
def site = request.getParameter("site_id")
def path = request.getParameter("path")

/** Validate Parameters */
def invalidParams = false
def paramsList = []

// site_id
try {
    if (StringUtils.isEmpty(site)) {
        site = request.getParameter("site")
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
    try {
        def context = DependencyServices.createContext(applicationContext, request)
        result = DependencyServices.getDependantItems(context, site, path)
    } catch (BlobNotFoundException | ContentNotFoundException e) {
        response.setStatus(404)
        result.message = e.message
    }
}
return result
