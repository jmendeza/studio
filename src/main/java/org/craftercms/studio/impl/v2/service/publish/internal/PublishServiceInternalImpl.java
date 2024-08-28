/*
 * Copyright (C) 2007-2024 Crafter Software Corporation. All Rights Reserved.
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

package org.craftercms.studio.impl.v2.service.publish.internal;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.StringUtils;
import org.craftercms.commons.security.permissions.annotations.ProtectedResourceId;
import org.craftercms.studio.api.v1.constant.DmConstants;
import org.craftercms.studio.api.v1.exception.ServiceLayerException;
import org.craftercms.studio.api.v1.exception.SiteNotFoundException;
import org.craftercms.studio.api.v1.exception.security.AuthenticationException;
import org.craftercms.studio.api.v1.exception.security.UserNotFoundException;
import org.craftercms.studio.api.v1.service.GeneralLockService;
import org.craftercms.studio.api.v1.service.configuration.ServicesConfig;
import org.craftercms.studio.api.v1.to.ContentItemTO;
import org.craftercms.studio.api.v2.annotation.SiteId;
import org.craftercms.studio.api.v2.dal.*;
import org.craftercms.studio.api.v2.dal.publish.*;
import org.craftercms.studio.api.v2.dal.publish.PublishPackage.PackageType;
import org.craftercms.studio.api.v2.event.publish.RequestPublishEvent;
import org.craftercms.studio.api.v2.event.workflow.WorkflowEvent;
import org.craftercms.studio.api.v2.exception.InvalidParametersException;
import org.craftercms.studio.api.v2.exception.publish.PublishPackagesNotFoundException;
import org.craftercms.studio.api.v2.repository.GitContentRepository;
import org.craftercms.studio.api.v2.service.audit.internal.ActivityStreamServiceInternal;
import org.craftercms.studio.api.v2.service.audit.internal.AuditServiceInternal;
import org.craftercms.studio.api.v2.service.dependency.DependencyService;
import org.craftercms.studio.api.v2.service.item.internal.ItemServiceInternal;
import org.craftercms.studio.api.v2.service.publish.PublishService;
import org.craftercms.studio.api.v2.service.security.SecurityService;
import org.craftercms.studio.api.v2.service.security.internal.UserServiceInternal;
import org.craftercms.studio.api.v2.service.site.SitesService;
import org.craftercms.studio.impl.v2.utils.DateUtils;
import org.craftercms.studio.impl.v2.utils.StudioUtils;
import org.craftercms.studio.model.publish.PublishingTarget;
import org.craftercms.studio.model.rest.dashboard.DashboardPublishingPackage;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Predicate;

import static java.lang.String.format;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.*;
import static org.apache.commons.collections4.CollectionUtils.union;
import static org.apache.commons.lang3.ArrayUtils.contains;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;
import static org.apache.tika.io.FilenameUtils.getName;
import static org.craftercms.studio.api.v2.dal.AuditLogConstants.*;
import static org.craftercms.studio.api.v2.dal.publish.PublishDAO.ACTIVE_APPROVAL_STATES;
import static org.craftercms.studio.api.v2.dal.publish.PublishItem.Action.ADD;
import static org.craftercms.studio.api.v2.dal.publish.PublishPackage.ApprovalState.APPROVED;
import static org.craftercms.studio.api.v2.dal.publish.PublishPackage.ApprovalState.SUBMITTED;
import static org.craftercms.studio.api.v2.dal.publish.PublishPackage.PackageType.*;
import static org.craftercms.studio.api.v2.event.workflow.WorkflowEvent.WorkFlowEventType.APPROVE;
import static org.craftercms.studio.api.v2.event.workflow.WorkflowEvent.WorkFlowEventType.SUBMIT;
import static org.craftercms.studio.api.v2.utils.StudioUtils.getPublishPackageLockKey;
import static org.craftercms.studio.impl.v1.repository.git.GitContentRepositoryConstants.IGNORE_FILES;
import static org.craftercms.studio.impl.v2.utils.DateUtils.formatDateIso;
import static org.craftercms.studio.permissions.PermissionResolverImpl.SITE_ID_RESOURCE_ID;
import static org.springframework.util.CollectionUtils.isEmpty;

public class PublishServiceInternalImpl implements PublishService, ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(PublishServiceInternalImpl.class);

    private GitContentRepository contentRepository;
    private RetryingDatabaseOperationFacade retryingDatabaseOperationFacade;
    private StudioUtils studioUtils;

    protected ItemServiceInternal itemServiceInternal;

    protected ApplicationContext applicationContext;
    private ServicesConfig servicesConfig;
    private UserServiceInternal userServiceInternal;
    private AuditServiceInternal auditServiceInternal;
    private DependencyService dependencyServiceInternal;
    private PublishDAO publishDao;
    private ItemTargetDAO itemTargetDao;
    private SitesService siteService;
    private GeneralLockService generalLockService;
    private ActivityStreamServiceInternal activityStreamServiceInternal;
    private SecurityService securityService;


    @Override
    public int getPublishingPackagesTotal(String siteId, String environment, String path, List<String> states) {
        // TODO: implement for new publishing system
        return 0;
//        return publishRequestDao.getPublishingPackagesTotal(siteId, environment, path, states);
    }

    @Override
    public List<PublishingPackage> getPublishingPackages(String siteId, String environment, String path,
                                                         List<String> states, int offset, int limit) {
        // TODO: implement for new publishing system
        return emptyList();
//        return publishRequestDao.getPublishingPackages(siteId, environment, path, states, offset, limit);
    }

    @Override
    public PublishingPackageDetails getPublishingPackageDetails(String siteId, String packageId) {
        // TODO: implement for new publishing system
        return null;
//        List<PublishRequest> publishingRequests = publishRequestDao.getPublishingPackageDetails(siteId, packageId);
//        PublishingPackageDetails publishingPackageDetails = new PublishingPackageDetails();
//        List<PublishingPackageDetails.PublishingPackageItem> packageItems = new ArrayList<>();
//        if (CollectionUtils.isNotEmpty(publishingRequests)) {
//            PublishRequest pr = publishingRequests.get(0);
//            publishingPackageDetails.setSiteId(pr.getSite());
//            publishingPackageDetails.setPackageId(pr.getPackageId());
//            publishingPackageDetails.setEnvironment(pr.getEnvironment());
//            publishingPackageDetails.setState(pr.getState());
//            publishingPackageDetails.setScheduledDate(pr.getScheduledDate());
//            publishingPackageDetails.setUser(pr.getUser());
//            publishingPackageDetails.setComment(pr.getSubmissionComment());
//        }
//        for (PublishRequest publishRequest : publishingRequests) {
//            PublishingPackageDetails.PublishingPackageItem item = new PublishingPackageDetails.PublishingPackageItem();
//            item.setPath(publishRequest.getPath());
//            item.setContentTypeClass(publishRequest.getContentTypeClass());
//            packageItems.add(item);
//        }
//        publishingPackageDetails.setItems(packageItems);
//        return publishingPackageDetails;
    }

    @Override
    public void cancelPublishingPackages(final String siteId, final Collection<Long> packageIds)
            throws ServiceLayerException, UserNotFoundException {
        Site site = siteService.getSite(siteId);
        String username = securityService.getCurrentUser();
        User user = userServiceInternal.getUserByIdOrUsername(-1, username);

        Collection<PublishPackage> packages = publishDao.getByIds(site.getId(), packageIds);
        if (packages.size() != packageIds.size()) {
            List<Long> missingPackages = packageIds.stream()
                    .filter(id -> packages.stream().noneMatch(p -> id.equals(p.getId())))
                    .toList();
            throw new PublishPackagesNotFoundException(siteId, missingPackages);
        }

        for (Long packageId : packageIds) {
            String packageLockKey = getPublishPackageLockKey(packageId);
            generalLockService.lock(packageLockKey);
            try {
                PublishPackage publishPackage = publishDao.getById(site.getId(), packageId);
                if (publishPackage.getPackageState() == PublishPackage.PackageState.CANCELLED.value) {
                    logger.warn("Package '{}' for site '{}' is already cancelled", packageId, siteId);
                    continue;
                }
                publishDao.cancelPackageById(site.getId(), packageId, servicesConfig.getLiveEnvironment(siteId));
                createCancelPackageAuditLogEntry(publishPackage, ORIGIN_API, username);

                activityStreamServiceInternal.insertActivity(site.getId(), user.getId(),
                        OPERATION_CANCEL_PUBLISHING_PACKAGE, DateUtils.getCurrentTime(), null, String.valueOf(packageId));
                applicationContext.publishEvent(new WorkflowEvent(siteId, packageId, WorkflowEvent.WorkFlowEventType.CANCEL));
            } finally {
                generalLockService.unlock(packageLockKey);
            }
        }
    }

    /**
     * Audit package cancellation
     *
     * @param publishPackage the package being cancelled
     * @param origin         the origin of the action (API vs GIT)
     * @param username       the username of the user who cancelled the package
     */
    private void createCancelPackageAuditLogEntry(final PublishPackage publishPackage, final String origin, final String username) {
        AuditLog auditLog = auditServiceInternal.createAuditLogEntry();
        auditLog.setOrigin(origin);
        auditLog.setOperation(OPERATION_CANCEL_PUBLISHING_PACKAGE);
        auditLog.setActorId(username);
        auditLog.setSiteId(publishPackage.getSiteId());
        auditLog.setPrimaryTargetId(String.valueOf(publishPackage.getId()));
        auditLog.setPrimaryTargetType(TARGET_TYPE_PUBLISHING_PACKAGE);
        auditLog.setPrimaryTargetValue(String.valueOf(publishPackage.getId()));
        auditServiceInternal.insertAuditLog(auditLog);
    }

    @Override
    public int getPublishingHistoryDetailTotalItems(String siteId, String packageId) {
        // TODO: implement for new publishing system
        return 0;
//        return publishRequestDao.getPublishingHistoryDetailTotalItems(siteId, packageId);
    }

    // TODO: implement for new publishing system
//    public List<PublishRequest> getPublishingHistoryDetail(String siteId, String packageId, int offset, int limit) {
//        return publishRequestDao.getPublishingHistoryDetail(siteId, packageId, offset, limit);
//    }

    public List<DeploymentHistoryItem> getDeploymentHistory(String siteId, List<String> environments,
                                                            ZonedDateTime fromDate, ZonedDateTime toDate,
                                                            String filterType, int numberOfItems) {
        // TODO: implement for new publishing system
        return emptyList();
    }

    @Override
    public List<DeploymentHistoryGroup> getDeploymentHistory(String siteId, int daysFromToday, int numberOfItems,
                                                             String filterType) throws ServiceLayerException, UserNotFoundException {
        ZonedDateTime toDate = DateUtils.getCurrentTime();
        ZonedDateTime fromDate = toDate.minusDays(daysFromToday);
        List<String> environments = studioUtils.getEnvironmentNames(siteId);
        List<DeploymentHistoryItem> deploymentHistoryItems = getDeploymentHistory(siteId,
                environments, fromDate, toDate, filterType, numberOfItems);
        List<DeploymentHistoryGroup> groups = new ArrayList<>();

        if (deploymentHistoryItems == null) {
            return groups;
        }
        int count = 0;
        Map<String, Set<String>> processedItems = new HashMap<>();
        for (int index = 0; index < deploymentHistoryItems.size() && count < numberOfItems; index++) {
            DeploymentHistoryItem entry = deploymentHistoryItems.get(index);
            String env = entry.getEnvironment();
            if (!processedItems.containsKey(env)) {
                processedItems.put(env, new HashSet<>());
            }
            if (!processedItems.get(env).contains(entry.getPath())) {
                ContentItemTO deployedItem = studioUtils.getContentItemForDashboard(entry.getSite(), entry.getPath());
                if (deployedItem != null) {
                    deployedItem.eventDate = entry.getDeploymentDate();
                    deployedItem.endpoint = entry.getTarget();
                    User user = userServiceInternal.getUserByIdOrUsername(-1, entry.getUser());
                    deployedItem.setUser(user.getUsername());
                    deployedItem.setUserFirstName(user.getFirstName());
                    deployedItem.setUserLastName(user.getLastName());
                    deployedItem.setEndpoint(entry.getEnvironment());
                    String deployedLabel = formatDateIso(entry.getDeploymentDate().truncatedTo(DAYS));
                    if (groups.size() > 0) {
                        DeploymentHistoryGroup group = groups.get(groups.size() - 1);
                        String lastDeployedLabel = group.getInternalName();
                        if (lastDeployedLabel.equals(deployedLabel)) {
                            // add to the last task if it is deployed on the same day
                            group.setNumOfChildren(group.getNumOfChildren() + 1);
                            group.getChildren().add(deployedItem);
                        } else {
                            groups.add(createDeploymentHistoryGroup(deployedLabel, deployedItem));
                        }
                    } else {
                        groups.add(createDeploymentHistoryGroup(deployedLabel, deployedItem));
                    }
                    processedItems.get(env).add(entry.getPath());
                }
            }
        }
        return groups;
    }

    private DeploymentHistoryGroup createDeploymentHistoryGroup(String deployedLabel, ContentItemTO item) {
        // otherwise just add as the last task
        DeploymentHistoryGroup group = new DeploymentHistoryGroup();
        group.setInternalName(deployedLabel);
        List<ContentItemTO> taskItems = group.getChildren();
        if (taskItems == null) {
            taskItems = new ArrayList<>();
            group.setChildren(taskItems);
        }
        taskItems.add(item);
        group.setNumOfChildren(taskItems.size());
        return group;
    }

    @Override
    public List<PublishingTarget> getAvailablePublishingTargets(@SiteId String siteId) {
        var availablePublishingTargets = new ArrayList<PublishingTarget>();
        var liveTarget = new PublishingTarget();
        liveTarget.setName(servicesConfig.getLiveEnvironment(siteId));
        availablePublishingTargets.add(liveTarget);
        if (servicesConfig.isStagingEnvironmentEnabled(siteId)) {
            var stagingTarget = new PublishingTarget();
            stagingTarget.setName(servicesConfig.getStagingEnvironment(siteId));
            availablePublishingTargets.add(stagingTarget);
        }
        return availablePublishingTargets;
    }

    @Override
    public boolean isSitePublished(@ProtectedResourceId(SITE_ID_RESOURCE_ID) String siteId) {
        // Site is published if PUBLISHED repo exists
        return contentRepository.publishedRepositoryExists(siteId);
    }

    @Override
    public int getPublishingItemsScheduledTotal(String siteId, String publishingTarget, String approver,
                                                ZonedDateTime dateFrom, ZonedDateTime dateTo, List<String> systemTypes) {
        // TODO: implement for new publishing system
        return 0;
//        return publishRequestDao
//                .getPublishingItemsScheduledTotal(siteId, publishingTarget, approver, READY_FOR_LIVE, dateFrom, dateTo, systemTypes)
//                .orElse(0);
    }

    // TODO: implement for new publishing system
//    @Override
//    public List<PublishRequest> getPublishingItemsScheduled(String siteId, String publishingTarget, String approver,
//                                                            ZonedDateTime dateFrom, ZonedDateTime dateTo,
//                                                            List<String> systemTypes, List<SortField> sortFields, int offset, int limit) {
//        return publishRequestDao.getPublishingItemsScheduled(siteId, publishingTarget, approver, READY_FOR_LIVE,
//                dateFrom, dateTo, systemTypes, DalUtils.mapSortFields(sortFields,PublishRequestDAO.SORT_FIELD_MAP), offset, limit);
//    }

    @Override
    public int getPublishingPackagesHistoryTotal(String siteId, String publishingTarget, String approver,
                                                 ZonedDateTime dateFrom, ZonedDateTime dateTo) {
        // TODO: implement for new publishing system
        return 0;
        // Need to check if null because of COUNT + GROUP BY
//        return publishRequestDao
//                .getPublishingPackagesHistoryTotal(siteId, publishingTarget, approver, COMPLETED, dateFrom, dateTo)
//                .orElse(0);
    }

    @Override
    public List<DashboardPublishingPackage> getPublishingPackagesHistory(String siteId, String publishingTarget,
                                                                         String approver, ZonedDateTime dateFrom,
                                                                         ZonedDateTime dateTo, int offset, int limit) {
        // TODO: implement for new publishing system
        return emptyList();
//        return publishRequestDao.getPublishingPackagesHistory(siteId, publishingTarget, approver, COMPLETED, dateFrom,
//                dateTo, offset, limit);
    }

    @Override
    public int getNumberOfPublishes(String siteId, int days) {
        // TODO: implement for new publishing system
        return 0;
//        return publishRequestDao.getNumberOfPublishes(siteId, days);
    }

    @Override
    public PublishDependenciesResult getPublishDependencies(final String siteId, final String publishingTarget,
                                                            final Collection<PublishRequestPath> publishRequestPaths,
                                                            final Collection<String> commitIds) throws ServiceLayerException, IOException {
        Site site = siteService.getSite(siteId);
        Set<String> corePackagePaths = new HashSet<>();
        corePackagePaths.addAll(publishRequestPaths.stream()
                .collect(teeing(
                        flatMapping(requestPath -> expandPublishRequestPath(site, requestPath).stream(), toSet()),
                        flatMapping(requestPath -> (requestPath.includeSoftDeps() ?
                                dependencyServiceInternal.getSoftDependencies(site.getSiteId(), Set.of(requestPath.path()))
                                : SetUtils.<String>emptySet()).stream(), toSet()),
                        SetUtils::union
                )));

        Map<Boolean, List<String>> commitOperations = contentRepository.validatePublishCommits(site.getSiteId(), commitIds).stream()
                .map(commitId -> contentRepository.getOperationsFromFirstParentDiff(site.getSiteId(), commitId))
                .flatMap(List::stream)
                .filter(getCommitRepoOperationsFilter(site))
                .collect(partitioningBy(op -> op.getAction() == RepoOperation.Action.DELETE,
                        mapping(RepoOperation::getPath, toList())));

        // Add non-delete operations
        corePackagePaths.addAll(commitOperations.get(false));

        Collection<String> deletedPaths = commitOperations.get(true);

        Collection<String> softDependencies = dependencyServiceInternal.getSoftDependencies(siteId, corePackagePaths);
        // Get hard deps of them all
        Collection<String> hardDependencies = dependencyServiceInternal.getHardDependencies(siteId, publishingTarget, union(corePackagePaths, softDependencies));
        return new PublishDependenciesResult(corePackagePaths, deletedPaths, hardDependencies, softDependencies);
    }

    @Override
    public PublishPackage getReadyPackageForItem(final String siteId, final String path, final boolean includeChildren) {
        return publishDao.getReadyPackageForItem(siteId, path, includeChildren);
    }

    @Override
    public Collection<PublishPackage> getActivePackagesForItems(final String siteId, final Collection<String> paths, final boolean includeChildren) {
        return publishDao.getItemPackagesByState(siteId, paths,
                PublishPackage.PackageState.READY.value + PublishPackage.PackageState.PROCESSING.value,
                ACTIVE_APPROVAL_STATES, includeChildren);
    }

    @Override
    @Transactional
    public long publishDelete(String siteId, Collection<String> userRequestedPaths, Collection<String> dependencies, String comment) throws ServiceLayerException {
        try {
            if (!contentRepository.publishedRepositoryExists(siteId)) {
                logger.warn("Site '{}' is not published, publish DELETE operations will be ignored", siteId);
                return 0;
            }
            String liveTarget = servicesConfig.getLiveEnvironment(siteId);
            PublishPackage publishPackage = createPackage(siteService.getSite(siteId), liveTarget, ITEM_LIST, false, null, comment);
            Collection<PublishItem> publishItems = createDeletePublishItems(siteId, userRequestedPaths, dependencies);
            if (CollectionUtils.isEmpty(publishItems)) {
                logger.debug("Deleted items are not published, nothing to do.");
                return 0;
            }
            retryingDatabaseOperationFacade.retry(() -> publishDao.insertPackageAndItems(publishPackage, publishItems, true));
            auditPublishSubmission(publishPackage, OPERATION_PUBLISH);

            applicationContext.publishEvent(new WorkflowEvent(siteId, publishPackage.getId(), APPROVE));
            notifyPublisher(publishPackage, siteService.getSite(siteId));
            return publishPackage.getId();
        } catch (Exception e) {
            String message = format("Failed to submit delete publish package for site '%s'", siteId);
            logger.error(message, e);
            throw new ServiceLayerException(message, e);
        }
    }

    private Collection<PublishItem> createDeletePublishItems(final String siteId, final Collection<String> userRequestedPaths,
                                                             final Collection<String> dependencies) throws SiteNotFoundException {
        Site site = siteService.getSite(siteId);
        Map<String, List<ItemTarget>> itemTargetsByPath = itemTargetDao.getItemTargetsByPath(site.getId(),
                union(userRequestedPaths, dependencies));

        Collection<PublishItem> publishItems = new ArrayList<>();
        publishItems.addAll(userRequestedPaths.stream()
                .map(path -> createPublishItem(path, PublishItem.Action.DELETE, true))
                .toList());

        publishItems.addAll(dependencies.stream()
                .map(path -> createPublishItem(path, PublishItem.Action.DELETE, false))
                .toList());

        publishItems.addAll(union(dependencies, userRequestedPaths).stream()
                .filter(path -> path.endsWith(DmConstants.SLASH_INDEX_FILE))
                .map(path -> StringUtils.removeEnd(path, DmConstants.SLASH_INDEX_FILE))
                .map(path -> createPublishItem(path, PublishItem.Action.DELETE, false))
                .toList());

        return publishItems.stream()
                .filter(item -> itemTargetsByPath.containsKey(item.getPath()))
                .peek(item -> setPreviousPaths(item, siteId, itemTargetsByPath.get(item.getPath())))
                .toList();
    }

    private void setPreviousPaths(final PublishItem item, final String siteId, final Collection<ItemTarget> itemTargets) {
        itemTargets.stream()
                .filter(itemTarget -> StringUtils.isNotEmpty(itemTarget.getPreviousPath()))
                .forEach(itemTarget -> {
                    boolean isLiveTarget = StringUtils.equals(servicesConfig.getLiveEnvironment(siteId), itemTarget.getTarget());
                    if (isLiveTarget) {
                        item.setLivePreviousPath(itemTarget.getPreviousPath());
                    } else {
                        item.setStagingPreviousPath(itemTarget.getPreviousPath());
                    }
                });
    }

    /**
     * Get common filter for commit repo operations to be included in publish dependencies
     * or actual publishing package submission
     */
    private Predicate<RepoOperation> getCommitRepoOperationsFilter(final Site site) {
        return op -> !contains(IGNORE_FILES, getName(op.getMoveToPath()))
                && !contains(IGNORE_FILES, getName(op.getPath()))
                // Ignore deletes if the path exists in current version
                && (op.getAction() != RepoOperation.Action.DELETE || !contentRepository.contentExists(site.getSiteId(), op.getPath()));
    }

    /**
     * Audit publish submission
     *
     * @param p         the publish package
     * @param operation the audit operation
     */
    private void auditPublishSubmission(final PublishPackage p, final String operation) {
        AuditLog auditLog = auditServiceInternal.createAuditLogEntry();
        auditLog.setOperation(operation);
        auditLog.setActorId(securityService.getCurrentUser());
        auditLog.setSiteId(p.getSiteId());
        auditLog.setPrimaryTargetId(String.valueOf(p.getId()));
        auditLog.setPrimaryTargetType(TARGET_TYPE_PUBLISHING_PACKAGE);
        auditLog.setPrimaryTargetValue(String.valueOf(p.getId()));

        AuditLogParameter commentParam = new AuditLogParameter();
        commentParam.setTargetId(TARGET_TYPE_SUBMISSION_COMMENT);
        commentParam.setTargetType(TARGET_TYPE_SUBMISSION_COMMENT);
        commentParam.setTargetValue(defaultIfEmpty(p.getSubmitterComment(), ""));

        auditLog.setParameters(List.of(commentParam));
        auditServiceInternal.insertAuditLog(auditLog);
    }

    /**
     * Create publish items from commit ids.
     * Extract operations from commit ids and create publish items.
     * Notice that delete operations will not be processed if new version of the path exists.
     */
    private void createPublishItemsFromCommitIds(final Site site, final Collection<String> commitIds,
                                                 final Map<String, PublishItem> publishItemsByPath)
            throws ServiceLayerException, IOException {
        if (isEmpty(commitIds)) {
            return;
        }
        // Validate and sort commits
        List<String> sortedCommits = contentRepository.validatePublishCommits(site.getSiteId(), commitIds);
        publishItemsByPath.putAll(
                sortedCommits.stream()
                        .map(commitId -> contentRepository.getOperationsFromFirstParentDiff(site.getSiteId(), commitId))
                        .flatMap(List::stream)
                        .filter(getCommitRepoOperationsFilter(site))
                        // TODO: review this: what if we are deleting a moved item? We should delete the old path, not the new
                        .map(op -> createPublishItem(op.getPath(),
                                op.getAction() == RepoOperation.Action.DELETE ? PublishItem.Action.DELETE : ADD, true))
                        .collect(toMap(PublishItem::getPath, item -> item)));
    }

    private PublishItem createPublishItem(final String path,
                                          final PublishItem.Action action, final boolean userRequested) {
        PublishItem publishItem = new PublishItem();
        publishItem.setAction(action);
        publishItem.setPath(path);
        publishItem.setUserRequested(userRequested);
        return publishItem;
    }

    /**
     * Create publish items from list of {@link PublishRequestPath}.
     * For each PublishRequest path:
     * - Add the path if it is not a folder
     * - Include soft deps if requested
     * - Include children if requested
     */
    private void createPublishItemsFromPaths(final Site site, final Collection<PublishRequestPath> publishRequestPaths,
                                             final Map<String, PublishItem> publishItemsByPath) {
        if (isEmpty(publishRequestPaths)) {
            return;
        }

        Set<String> allPaths = new HashSet<>();
        Set<String> softDepsPaths = new HashSet<>();
        for (PublishRequestPath publishRequestPath : publishRequestPaths) {
            Set<String> expandedPathList = expandPublishRequestPath(site, publishRequestPath);
            allPaths.addAll(expandedPathList);
            if (publishRequestPath.includeSoftDeps()) {
                softDepsPaths.addAll(expandedPathList);
            }
        }
        if(!softDepsPaths.isEmpty()) {
            allPaths.addAll(dependencyServiceInternal.getSoftDependencies(site.getSiteId(), softDepsPaths));
        }

        publishItemsByPath.putAll(
                allPaths.stream()
                        .filter(path -> !publishItemsByPath.containsKey(path))
                        .map(path -> createPublishItem(path, ADD, true))
                        .collect(toMap(PublishItem::getPath, item -> item)));
    }

    /**
     * Expand publish request path.
     * - Include the path itself if not a folder
     * - Include non-folder children if requested, recursively
     */
    private Set<String> expandPublishRequestPath(final Site site, final PublishRequestPath publishPath) {
        Set<String> paths = new HashSet<>();
        if (!contentRepository.isFolder(site.getSiteId(), publishPath.path())) {
            paths.add(publishPath.path());
        }
        if (publishPath.includeChildren()) {
            // Notice that we are publishing regardless of the item's state, consistent
            // with current behavior when publishing a live item directly
            paths.addAll(itemServiceInternal.getChildrenPaths(site.getId(), publishPath.path()));
        }
        return paths;
    }

    /**
     * Create publish items for hard dependencies.
     * For each non-delete PublishItem, get hard dependencies and add them to the publishItemsByPath map.
     */
    private void createPublishItemsForHardDeps(Site site, Map<String, PublishItem> publishItemsByPath) {
        Collection<String> paths = publishItemsByPath.keySet().stream()
                .filter(p -> publishItemsByPath.get(p).getAction() != PublishItem.Action.DELETE)
                .collect(toList());
        if (isEmpty(paths)) {
            return;
        }
        publishItemsByPath.putAll(
                dependencyServiceInternal.getHardDependencies(site.getSiteId(), paths).stream()
                        .filter(dep -> !publishItemsByPath.containsKey(dep))
                        .map(dep -> createPublishItem(dep, ADD, false))
                        .collect(toMap(PublishItem::getPath, item -> item)));
    }


    @Override
    public long publish(final String siteId, final String publishingTarget, final List<PublishRequestPath> paths,
                        final List<String> commitIds, final Instant schedule, final String comment, final boolean publishAll)
            throws ServiceLayerException, AuthenticationException {
        return routePackageSubmission(siteId, publishingTarget, paths, commitIds, schedule, comment, false, publishAll);
    }

    @Override
    public long requestPublish(final String siteId, final String publishingTarget, final List<PublishRequestPath> paths,
                               final List<String> commitIds, final Instant schedule, final String comment, final boolean publishAll)
            throws AuthenticationException, ServiceLayerException {
        return routePackageSubmission(siteId, publishingTarget, paths, commitIds, schedule, comment, true, publishAll);
    }

    /**
     * Routes the request to the appropriate method based on the site's publishing repo status.
     */
    private long routePackageSubmission(final String siteId, final String publishingTarget,
                                        final List<PublishRequestPath> paths, final List<String> commitIds,
                                        final Instant schedule, final String comment,
                                        final boolean requestApproval, final boolean publishAll)
            throws ServiceLayerException, AuthenticationException {
        Site site = siteService.getSite(siteId);
        String lockKey = org.craftercms.studio.api.v2.utils.StudioUtils.getSandboxRepoLockKey(site.getSiteId());
        generalLockService.lock(lockKey);
        try {
            if (!site.isSitePublishedRepoCreated()) {
                return buildInitialPublishPackage(site, publishingTarget, requestApproval, comment);
            }

            if (publishAll) {
                if (schedule != null) {
                    throw new InvalidParametersException("Failed to submit publishing package: Cannot schedule a publish all operation");
                }
                return buildPublishAllPackage(site, publishingTarget, requestApproval, comment);
            }

            return buildItemListPackage(site, publishingTarget,
                    paths, commitIds, requestApproval, schedule, comment);
        } finally {
            generalLockService.unlock(lockKey);
        }
    }

    @Override
    public void setApplicationContext(@NotNull final ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public void setContentRepository(final GitContentRepository contentRepository) {
        this.contentRepository = contentRepository;
    }

    public void setRetryingDatabaseOperationFacade(final RetryingDatabaseOperationFacade retryingDatabaseOperationFacade) {
        this.retryingDatabaseOperationFacade = retryingDatabaseOperationFacade;
    }

    public void setItemServiceInternal(final ItemServiceInternal itemServiceInternal) {
        this.itemServiceInternal = itemServiceInternal;
    }

    public void setServicesConfig(final ServicesConfig servicesConfig) {
        this.servicesConfig = servicesConfig;
    }

    @SuppressWarnings("unused")
    public void setStudioUtils(final StudioUtils studioUtils) {
        this.studioUtils = studioUtils;
    }

    public void setUserServiceInternal(final UserServiceInternal userServiceInternal) {
        this.userServiceInternal = userServiceInternal;
    }

    @SuppressWarnings("unused")
    public void setPublishDao(final PublishDAO publishDao) {
        this.publishDao = publishDao;
    }

    @SuppressWarnings("unused")
    public void setItemTargetDao(final ItemTargetDAO itemTargetDao) {
        this.itemTargetDao = itemTargetDao;
    }

    public void setSiteService(final SitesService siteService) {
        this.siteService = siteService;
    }

    public void setGeneralLockService(final GeneralLockService generalLockService) {
        this.generalLockService = generalLockService;
    }

    public void setAuditServiceInternal(final AuditServiceInternal auditServiceInternal) {
        this.auditServiceInternal = auditServiceInternal;
    }

    @SuppressWarnings("unused")
    public void setDependencyServiceInternal(final DependencyService dependencyServiceInternal) {
        this.dependencyServiceInternal = dependencyServiceInternal;
    }

    @SuppressWarnings("unused")
    public void setActivityStreamServiceInternal(final ActivityStreamServiceInternal activityStreamServiceInternal) {
        this.activityStreamServiceInternal = activityStreamServiceInternal;
    }

    @SuppressWarnings("unused")
    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }

    /**
     * Create a new publish package and populate it with the necessary information.
     *
     * @return the newly created publish package
     * @throws AuthenticationException if unable to find the current user
     */
    protected PublishPackage createPackage(final Site site,
                                           final String target,
                                           final PackageType packageType,
                                           final boolean requestApproval,
                                           final Instant schedule,
                                           final String comment) throws AuthenticationException {
        PublishPackage publishPackage = new PublishPackage();
        publishPackage.setPackageType(packageType);
        publishPackage.setSiteId(site.getId());
        publishPackage.setTarget(target);
        publishPackage.setSchedule(schedule);
        publishPackage.setSubmitterComment(comment);
        publishPackage.setSubmitterId(userServiceInternal.getCurrentUser().getId());
        publishPackage.setCommitId(site.getLastCommitId());
        publishPackage.setApprovalState(requestApproval ? SUBMITTED : APPROVED);
        return publishPackage;
    }

    /**
     * Template method to build a publish package and its items.
     *
     * @return the id of the created package
     */
    protected long buildPublishPackage(final Site site,
                                    final String target,
                                    final PackageType packageType,
                                    final Collection<PublishRequestPath> paths,
                                    final Collection<String> commitIds,
                                    final boolean requestApproval,
                                    final Instant schedule,
                                    final String comment,
                                    final GetPublishItems getPublishItemsFunction)
            throws ServiceLayerException, AuthenticationException {
        try {
            // Combine list of paths and list of commit changes
            Collection<PublishItem> publishItems = getPublishItemsFunction.get(site, paths, commitIds);

            // Create package
            PublishPackage publishPackage = createPackage(site, target, packageType, requestApproval, schedule, comment);

            boolean isLiveTarget = StringUtils.equals(servicesConfig.getLiveEnvironment(site.getSiteId()), target);
            retryingDatabaseOperationFacade.retry(() -> publishDao.insertPackageAndItems(publishPackage, publishItems, isLiveTarget));
            auditPublishSubmission(publishPackage, requestApproval ? OPERATION_REQUEST_PUBLISH : OPERATION_PUBLISH);

            applicationContext.publishEvent(new WorkflowEvent(site.getSiteId(), publishPackage.getId(), requestApproval ? SUBMIT : APPROVE));
            if (!requestApproval) {
                notifyPublisher(publishPackage, site);
            }
            return publishPackage.getId();
        } catch (IOException e) {
            logger.error("Failed to submit publish package: ", e);
            throw new ServiceLayerException("Failed to submit publish package", e);
        }
    }

    /**
     * Notify (if needed) the publisher to process the publish package.
     *
     * @param publishPackage the newly created publish package
     * @param site           the site
     */
    protected void notifyPublisher(final PublishPackage publishPackage, Site site) {
        if (publishPackage.getSchedule() == null) {
            applicationContext.publishEvent(
                    new RequestPublishEvent(site.getSiteId(), publishPackage.getId()));
        }
    }

    /**
     * Functional interface for a method that creates a collection of {@link PublishItem} objects
     * for a given request
     */
    @FunctionalInterface
    protected interface GetPublishItems {
        Collection<PublishItem> get(final Site site,
                                    final Collection<PublishRequestPath> paths,
                                    final Collection<String> commitIds)
                throws ServiceLayerException, IOException;
    }

    /**
     * Build an initial publish package for a site.
     * @param site the site
     * @param publishingTarget the publishing target
     * @param requestApproval whether to request approval
     * @param comment the comment
     * @return created package id
     */
    protected long buildInitialPublishPackage(Site site, String publishingTarget, boolean requestApproval, String comment)
            throws AuthenticationException, ServiceLayerException {
        return buildPublishPackage(site, publishingTarget, INITIAL_PUBLISH, emptyList(), emptyList(), requestApproval, null, comment, (s, p, c) -> emptyList());
    }

    /**
     * Create a collection of {@link PublishItem} objects for a publish all request.
     * @param site the site
     * @param paths the publish paths
     * @param commitIds the commit ids
     * @return the collection of publish items
     */
    @NotNull
    protected Collection<PublishItem> getPublishAllItems(Site site, Collection<PublishRequestPath> paths, Collection<String> commitIds) throws InvalidParametersException {
        List<PublishItem> publishItems = itemServiceInternal.getUnpublishedPaths(site.getId()).stream()
                .map(path -> createPublishItem(path, ADD, true))
                .toList();
        if (publishItems.isEmpty()) {
            throw new InvalidParametersException("Failed to submit publish package: No items to publish");
        }
        return publishItems;
    }

    /**
     * Build a publish all package for a site.
     *
     * @param site             the site
     * @param publishingTarget the publishing target
     * @param requestApproval  whether to request approval
     * @param comment          the comment
     * @return created package id
     */
    protected long buildPublishAllPackage(Site site, String publishingTarget, boolean requestApproval, String comment) throws AuthenticationException, ServiceLayerException {
        return buildPublishPackage(site, publishingTarget, PUBLISH_ALL, emptyList(), emptyList(), requestApproval, null, comment, this::getPublishAllItems);
    }

    /**
     * Create a collection of {@link PublishItem} objects for a list package request.
     *
     * @param site      the site
     * @param paths     the publish paths
     * @param commitIds the commit ids
     * @return the collection of publish items
     */
    @NotNull
    protected Collection<PublishItem> getItemListPackageItems(final Site site, final Collection<PublishRequestPath> paths,
                                                              final Collection<String> commitIds) throws ServiceLayerException, IOException {
        // Combine list of paths and list of commit changes
        Map<String, PublishItem> publishItemsByPath = new HashMap<>();
        createPublishItemsFromCommitIds(site, commitIds, publishItemsByPath);
        createPublishItemsFromPaths(site, paths, publishItemsByPath);
        createPublishItemsForHardDeps(site, publishItemsByPath);

        if (publishItemsByPath.isEmpty()) {
            throw new InvalidParametersException("Failed to submit publish package: No items to publish");
        }
        return publishItemsByPath.values();
    }

    /**
     * Build an item list package for a site.
     *
     * @param site            the site
     * @param target          the publishing target
     * @param paths           the publish paths
     * @param commitIds       the commit ids
     * @param requestApproval whether to request approval
     * @param schedule        the schedule
     * @param comment         the comment
     * @return created package id
     */
    protected long buildItemListPackage(Site site, String target, Collection<PublishRequestPath> paths, Collection<String> commitIds, boolean requestApproval, Instant schedule, String comment)
            throws ServiceLayerException, AuthenticationException {
        return buildPublishPackage(site, target, ITEM_LIST, paths, commitIds, requestApproval, schedule, comment, this::getItemListPackageItems);
    }

    /**
     * Cancel a package.
     * This method will cancel a package if its state is READY.
     * It will also update the state bits of the items in the package to cancel the workflow.
     *
     * @param siteId         the site id
     * @param publishPackage the package to cancel
     */
    private void cancelPackage(final String siteId, final PublishPackage publishPackage) {
        String packageLockKey = getPublishPackageLockKey(publishPackage.getId());
        generalLockService.lock(packageLockKey);
        try {
            if (publishPackage.getPackageState() != PublishPackage.PackageState.READY.value) {
                logger.debug("Package with id '{}' is not in READY state, it will not be cancelled", publishPackage.getId());
                return;
            }
            publishDao.cancelPackageById(publishPackage.getSiteId(), publishPackage.getId(), servicesConfig.getLiveEnvironment(siteId));
            createCancelPackageAuditLogEntry(publishPackage, ORIGIN_GIT, ACTOR_ID_GIT);
            applicationContext.publishEvent(new WorkflowEvent(siteId, publishPackage.getId(), WorkflowEvent.WorkFlowEventType.CANCEL));
        } finally {
            generalLockService.unlock(packageLockKey);
        }
    }

    @Override
    public void cancelAllPackagesForPath(final String siteId, final String path) {
        // Try to cancel ready packages
        Collection<PublishPackage> packages = publishDao.getReadyPackagesForItem(siteId, path);
        for (PublishPackage publishPackage : packages) {
            cancelPackage(siteId, publishPackage);
        }

        // Wait for processing package (if any) to complete
        PublishPackage processingPackage = publishDao.getPackageForItem(siteId, path, PublishPackage.PackageState.PROCESSING.value);
        if (processingPackage != null) {
            logger.debug("Package with id '{}' is in PROCESSING state, waiting for it to finish", processingPackage.getId());
            String packageLockKey = getPublishPackageLockKey(processingPackage.getId());
            generalLockService.lock(packageLockKey);
            try {
                logger.debug("Package with id '{}' has been released. Path '{}' is no longer in workflow", processingPackage.getId(), path);
            } finally {
                generalLockService.unlock(packageLockKey);
            }
        }
    }
}
