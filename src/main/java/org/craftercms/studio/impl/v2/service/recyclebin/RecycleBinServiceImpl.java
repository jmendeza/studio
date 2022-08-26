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

package org.craftercms.studio.impl.v2.service.recyclebin;

import org.craftercms.commons.security.permissions.annotations.HasPermission;
import org.craftercms.commons.security.permissions.annotations.ProtectedResourceId;
import org.craftercms.studio.api.v1.exception.ServiceLayerException;
import org.craftercms.studio.api.v2.service.recyclebin.RecycleBinService;
import org.craftercms.studio.permissions.CompositePermission;

import java.beans.ConstructorProperties;
import java.util.List;

import static org.craftercms.studio.permissions.CompositePermissionResolverImpl.PATH_LIST_RESOURCE_ID;
import static org.craftercms.studio.permissions.PermissionResolverImpl.SITE_ID_RESOURCE_ID;
import static org.craftercms.studio.permissions.StudioPermissionsConstants.PERMISSION_CONTENT_DELETE;

public class RecycleBinServiceImpl implements RecycleBinService {

    protected RecycleBinService recycleBinServiceInternal;

    @ConstructorProperties({"recycleBinServiceInternal"})
    public RecycleBinServiceImpl(final RecycleBinService recycleBinServiceInternal) {
        this.recycleBinServiceInternal = recycleBinServiceInternal;
    }

    @Override
    @HasPermission(type = CompositePermission.class, action = PERMISSION_CONTENT_DELETE)
    public long recycle(@ProtectedResourceId(SITE_ID_RESOURCE_ID) final String siteId,
                        @ProtectedResourceId(PATH_LIST_RESOURCE_ID) final List<String> paths,
                        final String comment) throws ServiceLayerException {
        return recycleBinServiceInternal.recycle(siteId, paths, comment);
    }
}
