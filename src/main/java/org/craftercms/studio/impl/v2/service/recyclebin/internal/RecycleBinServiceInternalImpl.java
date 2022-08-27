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

package org.craftercms.studio.impl.v2.service.recyclebin.internal;

import org.craftercms.studio.api.v1.exception.ServiceLayerException;
import org.craftercms.studio.api.v2.repository.ContentRepository;
import org.craftercms.studio.api.v2.service.recyclebin.RecycleBinService;

import java.beans.ConstructorProperties;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static java.lang.String.format;
import static org.craftercms.studio.api.v1.constant.DmConstants.ROOT_PATTERN_RECYCLE_BIN;

public class RecycleBinServiceInternalImpl implements RecycleBinService {
    private static final String RECYCLE_BIN_PACKAGE_DATE_FORMAT = "yyyy%sMM%sdd%sHHmmssSSS";
    protected final ContentRepository contentRepository;
    private final SimpleDateFormat packagePathDateFormat;

    @ConstructorProperties({"contentRepository"})
    public RecycleBinServiceInternalImpl(final ContentRepository contentRepository) {
        this.contentRepository = contentRepository;
        packagePathDateFormat = new SimpleDateFormat(format(RECYCLE_BIN_PACKAGE_DATE_FORMAT, File.separator, File.separator, File.separator));
    }

    @Override
    public long recycle(final String siteId, final List<String> paths, final String comment) throws ServiceLayerException {
        String deletePackagePath = generateRecycleBinPackagePath(new Date());
        String recycleBinPath = ROOT_PATTERN_RECYCLE_BIN + deletePackagePath;
        // TODO: Process all XMLs to update timestamps
        // TODO: Create manifest.xml
        String commitID = this.contentRepository.recycleItems(siteId, paths, recycleBinPath);
        // TODO: populate recycle_bin tables
        // TODO: update item states and commitID
        return 1234;
    }

    protected String generateRecycleBinPackagePath(Date date) {
        // A string like 2022/08/25/184412978-0c5a
        return format("%s-%s", packagePathDateFormat.format(date), UUID.randomUUID().toString().substring(0, 4).toLowerCase());
    }
}
