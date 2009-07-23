/*******************************************************************************
 * Copyright (c) 2009 Tasktop Technologies, Polarion Software and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.svn.core.discovery.model;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IBundleGroup;
import org.eclipse.core.runtime.IBundleGroupProvider;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.discovery.util.WebUtil;
import org.eclipse.team.svn.core.operation.LoggedOperation;
import org.osgi.framework.Bundle;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;

/**
 * A means of discovering connectors.
 * 
 * @author David Green
 * @author Igor Burilo
 */
public class ConnectorDiscovery {

	private List<DiscoveryConnector> connectors = Collections.emptyList();

	private List<DiscoveryCategory> categories = Collections.emptyList();

	private List<DiscoveryConnector> filteredConnectors = Collections.emptyList();

	private final List<AbstractDiscoveryStrategy> discoveryStrategies = new ArrayList<AbstractDiscoveryStrategy>();

	private Dictionary<Object, Object> environment = System.getProperties();

	private boolean verifyUpdateSiteAvailability = false;

	private Map<String, Version> featureToVersion = null;

	public ConnectorDiscovery() {
	}

	/**
	 * get the discovery strategies to use.
	 */
	public List<AbstractDiscoveryStrategy> getDiscoveryStrategies() {
		return discoveryStrategies;
	}

	/**
	 * Initialize this by performing discovery. Discovery may take a long time as it involves network access.
	 * PRECONDITION: must add at least one {@link #getDiscoveryStrategies() discovery strategy} prior to calling.
	 */
	public void performDiscovery(IProgressMonitor monitor) throws CoreException {
		if (discoveryStrategies.isEmpty()) {
			throw new IllegalStateException();
		}
		connectors = new ArrayList<DiscoveryConnector>();
		filteredConnectors = new ArrayList<DiscoveryConnector>();
		categories = new ArrayList<DiscoveryCategory>();

		final int totalTicks = 100000;
		final int discoveryTicks = totalTicks - (totalTicks / 10);
		final int filterTicks = totalTicks - discoveryTicks;
		monitor.beginTask(SVNMessages.ConnectorDiscovery_task_discovering_connectors, totalTicks);
		try {
			for (AbstractDiscoveryStrategy discoveryStrategy : discoveryStrategies) {
				discoveryStrategy.setCategories(categories);
				discoveryStrategy.setConnectors(connectors);
				discoveryStrategy.performDiscovery(new SubProgressMonitor(monitor, discoveryTicks
						/ discoveryStrategies.size()));
			}

			filterDescriptors();
			if (verifyUpdateSiteAvailability) {
				verifySiteAvailability(new SubProgressMonitor(monitor, filterTicks));
			}
			connectCategoriesToDescriptors();
		} finally {
			monitor.done();
		}
	}

	/**
	 * get the top-level categories
	 * 
	 * @return the categories, or an empty list if there are none.
	 */
	public List<DiscoveryCategory> getCategories() {
		return categories;
	}

	/**
	 * get the connectors that were discovered and not filtered
	 * 
	 * @return the connectors, or an empty list if there are none.
	 */
	public List<DiscoveryConnector> getConnectors() {
		return connectors;
	}

	/**
	 * get the connectors that were discovered but filtered
	 * 
	 * @return the filtered connectors, or an empty list if there were none.
	 */
	public List<DiscoveryConnector> getFilteredConnectors() {
		return filteredConnectors;
	}

	/**
	 * The environment used to resolve {@link ConnectorDescriptor#getPlatformFilter() platform filters}. Defaults to the
	 * current environment.
	 */
	public Dictionary<Object, Object> getEnvironment() {
		return environment;
	}

	/**
	 * The environment used to resolve {@link ConnectorDescriptor#getPlatformFilter() platform filters}. Defaults to the
	 * current environment.
	 */
	public void setEnvironment(Dictionary<Object, Object> environment) {
		if (environment == null) {
			throw new IllegalArgumentException();
		}
		this.environment = environment;
	}

	/**
	 * indicate if update site availability should be verified. The default is false.
	 * 
	 * @see DiscoveryConnector#getAvailable()
	 * @see #verifySiteAvailability(IProgressMonitor)
	 */
	public boolean isVerifyUpdateSiteAvailability() {
		return verifyUpdateSiteAvailability;
	}

	/**
	 * indicate if update site availability should be verified. The default is false.
	 * 
	 * @see DiscoveryConnector#getAvailable()
	 * @see #verifySiteAvailability(IProgressMonitor)
	 */
	public void setVerifyUpdateSiteAvailability(boolean verifyUpdateSiteAvailability) {
		this.verifyUpdateSiteAvailability = verifyUpdateSiteAvailability;
	}

	/**
	 * <em>not for general use: public for testing purposes only</em> A map of installed features to their version. Used
	 * to resolve {@link ConnectorDescriptor#getFeatureFilter() feature filters}.
	 */
	public Map<String, Version> getFeatureToVersion() {
		return featureToVersion;
	}

	/**
	 * <em>not for general use: public for testing purposes only</em> A map of installed features to their version. Used
	 * to resolve {@link ConnectorDescriptor#getFeatureFilter() feature filters}.
	 */
	public void setFeatureToVersion(Map<String, Version> featureToVersion) {
		this.featureToVersion = featureToVersion;
	}

	private void connectCategoriesToDescriptors() {
		Map<String, DiscoveryCategory> idToCategory = new HashMap<String, DiscoveryCategory>();
		for (DiscoveryCategory category : categories) {
			DiscoveryCategory previous = idToCategory.put(category.getId(), category);
			if (previous != null) {
				String errMessage = SVNMessages.format(
						SVNMessages.ConnectorDiscovery_duplicate_category_id, new Object[] { category.getId(),
								category.getSource().getId(), previous.getSource().getId() });
				LoggedOperation.reportError(this.getClass().getName(), new Exception(errMessage));			
			}
		}

		for (DiscoveryConnector connector : connectors) {
			DiscoveryCategory category = idToCategory.get(connector.getCategoryId());
			if (category != null) {
				category.getConnectors().add(connector);
				connector.setCategory(category);
			} else {
				String errMessage = SVNMessages.format(
						SVNMessages.ConnectorDiscovery_bundle_references_unknown_category, new Object[] {
								connector.getCategoryId(), connector.getId(), connector.getSource().getId() });
				LoggedOperation.reportError(this.getClass().getName(), new Exception(errMessage));
			}
		}
	}

	/**
	 * eliminate any connectors whose {@link ConnectorDescriptor#getPlatformFilter() platform filters} don't match
	 */
	private void filterDescriptors() {
		for (DiscoveryConnector connector : new ArrayList<DiscoveryConnector>(connectors)) {
			if (connector.getPlatformFilter() != null && connector.getPlatformFilter().trim().length() > 0) {
				boolean match = false;
				try {
					Filter filter = FrameworkUtil.createFilter(connector.getPlatformFilter());
					match = filter.match(environment);
				} catch (InvalidSyntaxException e) {
					String errMessage = SVNMessages.format(
							SVNMessages.ConnectorDiscovery_illegal_filter_syntax, new Object[] {
									connector.getPlatformFilter(), connector.getId(), connector.getSource().getId() });
					LoggedOperation.reportError(this.getClass().getName(), new Exception(errMessage, e));					
				}
				if (!match) {
					connectors.remove(connector);
					filteredConnectors.add(connector);
				}
			}
			for (FeatureFilter featureFilter : connector.getFeatureFilter()) {
				if (featureToVersion == null) {
					featureToVersion = computeFeatureToVersion();
				}
				boolean match = false;
				Version version = featureToVersion.get(featureFilter.getFeatureId());
				if (version != null) {
					VersionRange versionRange = new VersionRange(featureFilter.getVersion());
					if (versionRange.isIncluded(version)) {
						match = true;
					}
				}
				if (!match) {
					connectors.remove(connector);
					filteredConnectors.add(connector);
					break;
				}
			}
		}
	}

	private Map<String, Version> computeFeatureToVersion() {
		Map<String, Version> featureToVersion = new HashMap<String, Version>();
		for (IBundleGroupProvider provider : Platform.getBundleGroupProviders()) {
			for (IBundleGroup bundleGroup : provider.getBundleGroups()) {
				for (Bundle bundle : bundleGroup.getBundles()) {
					featureToVersion.put(bundle.getSymbolicName(), bundle.getVersion());
				}
			}
		}
		return featureToVersion;
	}

	/**
	 * Determine update site availability. This may be performed automatically as part of discovery when
	 * {@link #isVerifyUpdateSiteAvailability()} is true, or it may be invoked later by calling this method.
	 */
	public void verifySiteAvailability(IProgressMonitor monitor) {
		// NOTE: we don't put java.net.URLs in the map since it involves DNS activity when
		//       computing the hash code.
		Map<String, Collection<DiscoveryConnector>> urlToDescriptors = new HashMap<String, Collection<DiscoveryConnector>>();

		for (DiscoveryConnector descriptor : connectors) {
			String url = descriptor.getSiteUrl();
			if (!url.endsWith("/")) { //$NON-NLS-1$
				url += "/"; //$NON-NLS-1$
			}
			Collection<DiscoveryConnector> collection = urlToDescriptors.get(url);
			if (collection == null) {
				collection = new ArrayList<DiscoveryConnector>();
				urlToDescriptors.put(url, collection);
			}
			collection.add(descriptor);
		}
		final int totalTicks = urlToDescriptors.size();
		monitor.beginTask(SVNMessages.ConnectorDiscovery_task_verifyingAvailability, totalTicks);
		try {
			if (!urlToDescriptors.isEmpty()) {
				ExecutorService executorService = Executors.newFixedThreadPool(Math.min(urlToDescriptors.size(), 4));
				try {
					List<Future<VerifyUpdateSiteJob>> futures = new ArrayList<Future<VerifyUpdateSiteJob>>(
							urlToDescriptors.size());
					for (String url : urlToDescriptors.keySet()) {
						futures.add(executorService.submit(new VerifyUpdateSiteJob(url)));
					}
					for (Future<VerifyUpdateSiteJob> jobFuture : futures) {
						try {
							for (;;) {
								try {
									VerifyUpdateSiteJob job = jobFuture.get(1L, TimeUnit.SECONDS);

									Collection<DiscoveryConnector> descriptors = urlToDescriptors.get(job.url);
									for (DiscoveryConnector descriptor : descriptors) {
										descriptor.setAvailable(job.ok);
									}
									break;
								} catch (TimeoutException e) {
									if (monitor.isCanceled()) {
										return;
									}
								}
							}
						} catch (InterruptedException e) {
							monitor.setCanceled(true);
							return;
						} catch (ExecutionException e) {
							if (e.getCause() instanceof OperationCanceledException) {
								monitor.setCanceled(true);
								return;
							}												
							LoggedOperation.reportError(this.getClass().getName(), e.getCause());
						}
						monitor.worked(1);
					}
				} finally {
					executorService.shutdownNow();
				}
			}
		} finally {
			monitor.done();
		}
	}

	private static class VerifyUpdateSiteJob implements Callable<VerifyUpdateSiteJob> {

		private final String url;

		private boolean ok = false;

		public VerifyUpdateSiteJob(String url) {
			this.url = url;
		}

		public VerifyUpdateSiteJob call() throws Exception {
			URL baseUrl = new URL(this.url);
			URL[] locations = new URL[] { new URL(baseUrl, "content.jar"), new URL(baseUrl, "content.xml"), new URL(baseUrl, "site.xml") }; //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
			ok = WebUtil.verifyAvailability(locations, true, new NullProgressMonitor());
			return this;
		}
	}

	public void dispose() {
		for (final AbstractDiscoveryStrategy strategy : discoveryStrategies) {
			SafeRunner.run(new ISafeRunnable() {

				public void run() throws Exception {
					strategy.dispose();
				}

				public void handleException(Throwable exception) {
					String errMessage = SVNMessages.ConnectorDiscovery_exception_disposing + strategy.getClass().getName();
					LoggedOperation.reportError(this.getClass().getName(), new Exception(errMessage, exception));
				}
			});
		}
	}
}
