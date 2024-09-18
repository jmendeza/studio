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

package org.craftercms.studio.impl.v2.service.publish;

import org.craftercms.commons.security.permissions.DefaultPermission;
import org.craftercms.commons.security.permissions.annotations.HasPermission;
import org.craftercms.studio.api.v1.exception.ServiceLayerException;
import org.craftercms.studio.api.v1.exception.SiteNotFoundException;
import org.craftercms.studio.api.v1.exception.security.AuthenticationException;
import org.craftercms.studio.api.v2.annotation.RequireSiteExists;
import org.craftercms.studio.api.v2.annotation.RequireSiteReady;
import org.craftercms.studio.api.v2.annotation.SiteId;
import org.craftercms.studio.api.v2.dal.publish.PublishItem;
import org.craftercms.studio.api.v2.dal.publish.PublishPackage;
import org.craftercms.studio.api.v2.exception.publish.PublishPackageNotFoundException;
import org.craftercms.studio.api.v2.security.HasAnyPermissions;
import org.craftercms.studio.api.v2.service.publish.PublishService;
import org.craftercms.studio.model.publish.PublishingTarget;
import org.craftercms.studio.model.rest.dashboard.DashboardPublishingPackage;
import org.craftercms.studio.model.rest.publish.PublishPackageDetails;
import org.craftercms.studio.permissions.CompositePermission;

import java.io.IOException;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;

import static org.craftercms.studio.permissions.StudioPermissionsConstants.*;
import static org.springframework.util.CollectionUtils.isEmpty;

@RequireSiteReady
public class PublishServiceImpl implements PublishService {

    private PublishService publishServiceInternal;

    @Override
    @RequireSiteExists
    @HasPermission(type = DefaultPermission.class, action = PERMISSION_GET_PUBLISHING_QUEUE)
    public int getPublishingPackagesCount(@SiteId String siteId, String environment,
                                          String path, Long states,
                                          final Collection<PublishPackage.ApprovalState> approvalStates) throws SiteNotFoundException {
        return publishServiceInternal.getPublishingPackagesCount(siteId, environment, path, states, approvalStates);
    }

    @Override
    @RequireSiteExists
    @HasPermission(type = DefaultPermission.class, action = PERMISSION_GET_PUBLISHING_QUEUE)
    public Collection<PublishPackage> getPublishingPackages(@SiteId String siteId,
                                                                    String environment, String path, Long states,
                                                                    final Collection<PublishPackage.ApprovalState> approvalStates,
                                                                    int offset, int limit) throws SiteNotFoundException {
        return publishServiceInternal.getPublishingPackages(siteId, environment, path, states, approvalStates, offset, limit);
    }

    @Override
    @RequireSiteExists
    @HasPermission(type = DefaultPermission.class, action = PERMISSION_GET_PUBLISHING_QUEUE)
    public PublishPackageDetails getPublishingPackageDetails(@SiteId String siteId,
                                                             long packageId) throws SiteNotFoundException, PublishPackageNotFoundException {
        PublishPackageDetails packageDetails = publishServiceInternal.getPublishingPackageDetails(siteId, packageId);
        if (packageDetails == null || isEmpty(packageDetails.getItems())) {
            throw new PublishPackageNotFoundException(siteId, packageId);
        }

        return packageDetails;
    }

    @Override
    @RequireSiteReady
    @HasPermission(type = CompositePermission.class, action = PERMISSION_PUBLISH)
    public long publish(@SiteId String siteId, String publishingTarget, List<PublishRequestPath> paths,
                        List<String> commitIds, Instant schedule, String comment, boolean submitAll)
            throws AuthenticationException, ServiceLayerException {
        return publishServiceInternal.publish(siteId, publishingTarget, paths, commitIds, schedule, comment, submitAll);
    }

    @Override
    @RequireSiteReady
    @HasPermission(type = CompositePermission.class, action = PERMISSION_CONTENT_READ)
    public long requestPublish(@SiteId String siteId, String publishingTarget, List<PublishRequestPath> paths,
                               List<String> commitIds, Instant schedule, String comment, boolean submitAll)
            throws AuthenticationException, ServiceLayerException {
        return publishServiceInternal.requestPublish(siteId, publishingTarget, paths, commitIds, schedule, comment, submitAll);
    }

    @Override
    @RequireSiteExists
    @HasPermission(type = CompositePermission.class, action = PERMISSION_CONTENT_READ)
    public int getPublishingItemsScheduledCount(@SiteId String siteId, String publishingTarget, String approver,
                                                ZonedDateTime dateFrom, ZonedDateTime dateTo, List<String> systemTypes) {
        return publishServiceInternal.getPublishingItemsScheduledCount(siteId, publishingTarget, approver, dateFrom, dateTo, systemTypes);
    }

    @Override
    @RequireSiteExists
    @HasPermission(type = CompositePermission.class, action = PERMISSION_CONTENT_READ)
    public int getPublishingHistoryCount(@SiteId String siteId, String publishingTarget, String approver,
                                         Instant dateFrom, Instant dateTo) {
        return publishServiceInternal.getPublishingHistoryCount(siteId, publishingTarget, approver, dateFrom, dateTo);
    }

    @Override
    @RequireSiteExists
    @HasPermission(type = CompositePermission.class, action = PERMISSION_CONTENT_READ)
    public int getPublishItemsCount(@SiteId String siteId, long publishingPackageId) {
        return publishServiceInternal.getPublishItemsCount(siteId, publishingPackageId);
    }

    @Override
    @RequireSiteExists
    @HasPermission(type = CompositePermission.class, action = PERMISSION_CONTENT_READ)
    public Collection<DashboardPublishingPackage> getPublishingHistory(@SiteId String siteId, String publishingTarget, String approver,
                                                                       Instant dateFrom, Instant dateTo, int offset, int limit) {
        return publishServiceInternal.getPublishingHistory(siteId, publishingTarget, approver, dateFrom, dateTo, offset, limit);
    }

    @Override
    @RequireSiteExists
    @HasPermission(type = CompositePermission.class, action = PERMISSION_CONTENT_READ)
    public int getNumberOfPublishes(@SiteId String siteId, int days) {
        return publishServiceInternal.getNumberOfPublishes(siteId, days);
    }

    @Override
    @RequireSiteExists
    @HasPermission(type = DefaultPermission.class, action = PERMISSION_CONTENT_READ)
    public int getNumberOfPublishedItemsByAction(@SiteId String siteId, int days, PublishItem.Action action) {
        return publishServiceInternal.getNumberOfPublishedItemsByAction(siteId, days, action);
    }

    @Override
    @RequireSiteExists
    @HasPermission(type = DefaultPermission.class, action = PERMISSION_CONTENT_READ)
    public PublishDependenciesResult getPublishDependencies(@SiteId String siteId, String publishingTarget, Collection<PublishRequestPath> paths,
                                                            Collection<String> commitIds)
            throws ServiceLayerException, IOException {
        return publishServiceInternal.getPublishDependencies(siteId, publishingTarget, paths, commitIds);
    }

    @Override
    @RequireSiteExists
    @HasPermission(type = DefaultPermission.class, action = PERMISSION_GET_PUBLISHING_QUEUE)
    public PublishPackage getReadyPackageForItem(@SiteId final String site, final String path, final boolean includeChildren) {
        return publishServiceInternal.getReadyPackageForItem(site, path, includeChildren);
    }

    @Override
    @RequireSiteExists
    @HasPermission(type = DefaultPermission.class, action = PERMISSION_GET_PUBLISHING_QUEUE)
    public Collection<PublishPackage> getActivePackagesForItems(@SiteId final String siteId, final Collection<String> paths,
                                                                final boolean includeChildren) {
        return publishServiceInternal.getActivePackagesForItems(siteId, paths, includeChildren);
    }

    @Override
    @RequireSiteExists
    @HasPermission(type = DefaultPermission.class, action = PERMISSION_CONTENT_DELETE)
    public long publishDelete(@SiteId String siteId, Collection<String> userRequestedPaths, Collection<String> dependencies, String comment) throws ServiceLayerException {
        return publishServiceInternal.publishDelete(siteId, userRequestedPaths, dependencies, comment);
    }

    @Override
    @RequireSiteExists
    @HasPermission(type = DefaultPermission.class, action = PERMISSION_GET_PUBLISHING_QUEUE)
    public PublishPackage getPackage(@SiteId String siteId, long packageId)
            throws PublishPackageNotFoundException, SiteNotFoundException {
        return publishServiceInternal.getPackage(siteId, packageId);
    }

    @Override
    @RequireSiteExists
    @HasPermission(type = DefaultPermission.class, action = PERMISSION_GET_PUBLISHING_QUEUE)
    public Collection<PublishItem> getPublishItems(@SiteId String siteId, final long packageId,
                                                   final int offset, final int limit)
            throws PublishPackageNotFoundException, SiteNotFoundException {
        return publishServiceInternal.getPublishItems(siteId, packageId, offset, limit);
    }

    @Override
    @RequireSiteExists
    @HasAnyPermissions(type = DefaultPermission.class, actions = {PERMISSION_PUBLISH, PERMISSION_CONTENT_READ})
    public List<PublishingTarget> getAvailablePublishingTargets(@SiteId String siteId) throws SiteNotFoundException {
        return publishServiceInternal.getAvailablePublishingTargets(siteId);
    }

    @Override
    @RequireSiteExists
    @HasPermission(type = DefaultPermission.class, action = PERMISSION_CONTENT_READ)
    public boolean isSitePublished(@SiteId String siteId) throws SiteNotFoundException {
        return publishServiceInternal.isSitePublished(siteId);
    }

    @SuppressWarnings("unused")
    public void setPublishServiceInternal(final PublishService publishServiceInternal) {
        this.publishServiceInternal = publishServiceInternal;
    }
}
