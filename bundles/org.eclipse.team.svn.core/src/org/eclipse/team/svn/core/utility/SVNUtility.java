/*******************************************************************************
 * Copyright (c) 2005, 2023 Polarion Software and others.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Alexander Gurov - Initial API and implementation
 *    Gabor Liptak - Speedup Pattern's usage
 *    Andrey Loskutov - Performance improvements for SVNUtility
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.core.utility;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.internal.preferences.Base64;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.Team;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.PathForURL;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.SVNTeamPlugin;
import org.eclipse.team.svn.core.SVNTeamProvider;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.ISVNNotificationCallback;
import org.eclipse.team.svn.core.connector.ISVNProgressMonitor;
import org.eclipse.team.svn.core.connector.ISVNPropertyCallback.Pair;
import org.eclipse.team.svn.core.connector.SVNChangeStatus;
import org.eclipse.team.svn.core.connector.SVNConnectorException;
import org.eclipse.team.svn.core.connector.SVNDepth;
import org.eclipse.team.svn.core.connector.SVNDiffStatus;
import org.eclipse.team.svn.core.connector.SVNEntry;
import org.eclipse.team.svn.core.connector.SVNEntry.Kind;
import org.eclipse.team.svn.core.connector.SVNEntryInfo;
import org.eclipse.team.svn.core.connector.SVNEntryReference;
import org.eclipse.team.svn.core.connector.SVNEntryRevisionReference;
import org.eclipse.team.svn.core.connector.SVNEntryStatus;
import org.eclipse.team.svn.core.connector.SVNLogEntry;
import org.eclipse.team.svn.core.connector.SVNProperty;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.connector.SVNRevisionRange;
import org.eclipse.team.svn.core.connector.ssl.SSLServerCertificateInfo;
import org.eclipse.team.svn.core.extension.CoreExtensionsManager;
import org.eclipse.team.svn.core.extension.factory.ISVNConnectorFactory;
import org.eclipse.team.svn.core.extension.options.IIgnoreRecommendations;
import org.eclipse.team.svn.core.extension.options.IOptionProvider;
import org.eclipse.team.svn.core.operation.SVNNullProgressMonitor;
import org.eclipse.team.svn.core.operation.UnreportableException;
import org.eclipse.team.svn.core.operation.file.SVNFileStorage;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.resource.IRepositoryContainer;
import org.eclipse.team.svn.core.resource.IRepositoryFile;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IRepositoryRoot;
import org.eclipse.team.svn.core.resource.IRevisionLink;
import org.eclipse.team.svn.core.resource.SSLSettings;
import org.eclipse.team.svn.core.svnstorage.SVNCachedProxyCredentialsManager;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.svnstorage.SVNRevisionLink;

/**
 * SVN Utility functions
 * 
 * @author Alexander Gurov
 */
public final class SVNUtility {
	private static String svnFolderName = null;

	public static String formatSSLFingerprint(byte[] fingerprint) {
		String retVal = "";
		for (byte data : fingerprint) {
			String part = String.format("%02x", data);
			retVal += retVal.length() > 0 ? ":" + part : part;
		}
		return retVal;
	}

	public static String formatSSLValid(Date validFrom, Date validTo) {
		DateFormat df = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH); //$NON-NLS-1$
		return "from " + df.format(validFrom) + " until " + df.format(validTo); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public static SSLServerCertificateInfo decodeCertificateData(Map<String, String> map) throws ParseException {
		String serverURL = map.get("serverURL") != null ? map.get("serverURL") : ""; //$NON-NLS-1$
		String issuer = map.get("issuer"); //$NON-NLS-1$
		String subject = map.get("subject"); //$NON-NLS-1$
		String[] parts = map.get("fingerprint") == null ? new String[0] : map.get("fingerprint").split(":"); //$NON-NLS-1$ //$NON-NLS-2$
		byte[] fingerprint = new byte[parts.length];
		for (int k = 0; k < parts.length; k++) {
			try {
				fingerprint[k] = (byte) Integer.parseInt(parts[k], 16);
			} catch (NumberFormatException ex) {
				throw new ParseException(parts[k], 0);
			}
		}
		String valid = map.get("valid"); //$NON-NLS-1$
		long validFrom = 0, validTo = 0;
		//Tue Oct 22 15:00:01 EEST 2013
		if (valid != null) {
			DateFormat df = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH); //$NON-NLS-1$
			String fromStr = valid.substring(5, valid.indexOf("until") - 1); //$NON-NLS-1$
			String toStr = valid.substring(valid.indexOf("until") + 6); //$NON-NLS-1$
			validFrom = df.parse(fromStr).getTime();
			validTo = df.parse(toStr).getTime();
		}
		return new SSLServerCertificateInfo(subject, issuer, validFrom, validTo, fingerprint, Arrays.asList(serverURL),
				null);
	}

	public static Map<String, String> splitCertificateString(String message) {
		HashMap<String, String> retVal = new HashMap<>();
		String[] baseLines = message.split("\n"); //$NON-NLS-1$
		boolean infoMessagePart = false;
		String infoMessage = null;
		for (String baseLine : baseLines) {
			int idx1 = baseLine.indexOf("https:"); //$NON-NLS-1$
			if (idx1 != -1) {
				String serverURL = baseLine.substring(idx1).trim();
				serverURL = serverURL.substring(0, serverURL.length() - 2);
				retVal.put("serverURL", serverURL); //$NON-NLS-1$
				infoMessagePart = true;
			} else if (infoMessagePart) {
				if (baseLine.endsWith(":")) {
					infoMessagePart = false;
				} else {
					infoMessage = infoMessage == null ? baseLine : infoMessage + "\n" + baseLine; //$NON-NLS-1$
				}
			} else {
				int idx = baseLine.indexOf(':');
				String key = baseLine.substring(0, idx).replaceFirst("\\s*-\\s*", "").trim(); //$NON-NLS-1$
				String value = baseLine.substring(idx + 1).trim();
				if ("Subject".equals(key)) { //$NON-NLS-1$
					retVal.put("subject", value); //$NON-NLS-1$
				} else if ("Valid".equals(key)) { //$NON-NLS-1$
					retVal.put("valid", value); //$NON-NLS-1$
				} else if ("Issuer".equals(key)) { //$NON-NLS-1$
					retVal.put("issuer", value); //$NON-NLS-1$
				} else if ("Fingerprint".equals(key)) { //$NON-NLS-1$
					retVal.put("fingerprint", value); //$NON-NLS-1$
				}
			}
		}
		retVal.put("infoMessage", infoMessage); //$NON-NLS-1$
		return retVal;
	}

	public static boolean isPriorToSVN17() {
		return CoreExtensionsManager.instance()
				.getSVNConnectorFactory()
				.getSVNAPIVersion() < ISVNConnectorFactory.APICompatibility.SVNAPI_1_7_x;
	}

	public static IRepositoryResource asRepositoryResource(String url, boolean isFolder) {
		if (!SVNUtility.isValidSVNURL(url)) {
			return null;
		}
		IRepositoryRoot[] roots = SVNUtility.findRoots(url, true);
		IRepositoryResource retVal = null;
		if (roots.length > 0) {
			retVal = isFolder ? roots[0].asRepositoryContainer(url, false) : roots[0].asRepositoryFile(url, false);
		} else {
			IRepositoryLocation location = SVNRemoteStorage.instance().newRepositoryLocation();
			SVNUtility.initializeRepositoryLocation(location, url);
			retVal = isFolder ? location.asRepositoryContainer(url, false) : location.asRepositoryFile(url, false);
		}
		return retVal;
	}

	public static void initializeRepositoryLocation(IRepositoryLocation location, String url) {
		location.setStructureEnabled(true);
		location.setTrunkLocation(
				CoreExtensionsManager.instance().getOptionProvider().getString(IOptionProvider.DEFAULT_TRUNK_NAME));
		location.setBranchesLocation(
				CoreExtensionsManager.instance().getOptionProvider().getString(IOptionProvider.DEFAULT_BRANCHES_NAME));
		location.setTagsLocation(
				CoreExtensionsManager.instance().getOptionProvider().getString(IOptionProvider.DEFAULT_TAGS_NAME));
		IPath urlPath = SVNUtility.createPathForSVNUrl(url);
		if (urlPath.lastSegment().equals(location.getTrunkLocation())) {
			url = urlPath.removeLastSegments(1).toString();
		}
		location.setUrl(url);
	}

	public static IRepositoryResource getCopiedFrom(IResource resource) {
		return SVNUtility.getCopiedFrom(SVNRemoteStorage.instance().asLocalResource(resource));
	}

	public static IRepositoryResource getCopiedFrom(ILocalResource local) {
		if (local.isCopied()) {
			IResource resource = local.getResource();
			IRepositoryLocation location = SVNRemoteStorage.instance().getRepositoryLocation(resource);
			ISVNConnector proxy = location.acquireSVNProxy();
			final String path = FileUtility.getWorkingCopyPath(resource);
			SVNEntryInfo[] st = null;
			try {
				st = SVNUtility.info(proxy, new SVNEntryRevisionReference(path), SVNDepth.EMPTY,
						new SVNNullProgressMonitor());
			} catch (SVNConnectorException ex) {
				return null;
			} finally {
				location.releaseSVNProxy(proxy);
			}

			if (st[0] != null) {
				String url = st[0].copyFromUrl;
				if (url == null) {
					IResource parent = resource.getParent();
					if (parent != null && parent.getType() != IResource.ROOT) {
						IRepositoryResource tmp = SVNUtility.getCopiedFrom(parent);
						if (tmp != null) {
							url = tmp.getUrl() + "/" + resource.getName(); //$NON-NLS-1$
						}
					}
				} else {
					url = SVNUtility.decodeURL(url);
				}
				IRepositoryResource retVal = SVNRemoteStorage.instance()
						.asRepositoryResource(location, url, resource.getType() == IResource.FILE);
				retVal.setSelectedRevision(
						SVNRevision.fromNumber(st[0].copyFromRevision == SVNRevision.INVALID_REVISION_NUMBER
						? st[0].revision
								: st[0].copyFromRevision));
				return retVal;
			}
		}
		return null;
	}

	public static Map<String, SVNEntryRevisionReference> parseSVNExternalsProperty(String property,
			IRepositoryResource propertyHolder) {
		Map<String, SVNEntryRevisionReference> retVal = new HashMap<>();

		SVNExternalPropertyData[] externalsData = SVNExternalPropertyData.parse(property);
		for (SVNExternalPropertyData externalData : externalsData) {
			String url = externalData.url.trim();
			SVNRevision revision = null;
			SVNRevision pegRevision = null;

			try {
				if (externalData.revision != null) {
					revision = SVNRevision.fromString(externalData.revision);
				}
				if (externalData.pegRevision != null) {
					revision = SVNRevision.fromString(externalData.pegRevision);
				}
			} catch (Exception ex) {
				throw new UnreportableException("Malformed external, " + externalData.toString()); //$NON-NLS-1$
			}

			url = SVNUtility.replaceRelativeExternalParts(url, propertyHolder);

			try {
				url = SVNUtility.decodeURL(url);
			} catch (IllegalArgumentException ex) {
				// the URL is not encoded
				url = SVNUtility.normalizeURL(url);
			}

			retVal.put(externalData.localPath, new SVNEntryRevisionReference(url, pegRevision, revision));
		}

		return retVal;
	}

	public static String replaceRelativeExternalParts(String url, IRepositoryResource resource)
			throws UnreportableException {
		if (SVNUtility.isValidSVNURL(url)) {
			return url;
		}

		if (url.startsWith("^/")) { //$NON-NLS-1$
			url = resource.getRepositoryLocation().getRepositoryRoot().getUrl() + url.substring(1);
		} else if (url.startsWith("//")) { //$NON-NLS-1$
			try {
				String protocol = SVNUtility.getSVNUrl(resource.getUrl()).getProtocol();
				if (resource.getUrl().indexOf(":///") != -1) { //$NON-NLS-1$
					url = protocol + ":/" + url; //$NON-NLS-1$
				} else {
					url = protocol + ":" + url; //$NON-NLS-1$
				}
			} catch (MalformedURLException e) {
				// cannot be thrown
			}
		} else if (url.startsWith("/")) { //$NON-NLS-1$
			String prefix = resource.getUrl();
			int idx = prefix.lastIndexOf("//"); //$NON-NLS-1$
			idx = prefix.indexOf('/', idx + 2);
			url = prefix.substring(0, idx) + url;
		} else if (url.startsWith("../")) { //$NON-NLS-1$
			IRepositoryResource prefix = resource;
			while (url.startsWith("../")) { //$NON-NLS-1$
				url = url.substring(3);
				prefix = prefix.getParent();
				if (prefix == null) {
					throw new UnreportableException("Malformed url: " + url); //$NON-NLS-1$
				}
			}
			url = prefix.getUrl() + "/" + url; //$NON-NLS-1$
		} else {
			throw new UnreportableException("Malformed url: " + url); //$NON-NLS-1$
		}
		return url;
	}

	public static SVNEntryReference asEntryReference(String url) {
		if (url == null) {
			return null;
		}
		int idx = url.lastIndexOf('@');
		SVNRevision peg = null;
		if (idx != -1) {
			try {
				peg = SVNRevision.fromString(url.substring(idx + 1));
				url = url.substring(0, idx);
			} catch (IllegalArgumentException ex) {
				// it is not a revision at the end of the URL
			}
		}
		return new SVNEntryReference(url, peg);
	}

	public static boolean useSingleReferenceSignature(SVNEntryRevisionReference reference1,
			SVNEntryRevisionReference reference2) {
		SVNRevision.Kind kind1 = reference1.revision.getKind();
		SVNRevision.Kind kind2 = reference2.revision.getKind();
		if ((kind1 == SVNRevision.Kind.BASE || kind1 == SVNRevision.Kind.WORKING)
				&& (kind2 == SVNRevision.Kind.BASE || kind2 == SVNRevision.Kind.WORKING)) {
			return false;
		}
		return reference1.path.equals(reference2.path) && (reference1.pegRevision == reference2.pegRevision
				|| reference1.pegRevision != null && reference1.pegRevision.equals(reference2.pegRevision));
	}

	public static SVNEntryRevisionReference getEntryRevisionReference(IRepositoryResource resource) {
		return new SVNEntryRevisionReference(SVNUtility.encodeURL(resource.getUrl()), resource.getPegRevision(),
				resource.getSelectedRevision());
	}

	public static SVNEntryReference getEntryReference(IRepositoryResource resource) {
		return new SVNEntryReference(SVNUtility.encodeURL(resource.getUrl()), resource.getPegRevision());
	}

	public static SVNProperty[] properties(ISVNConnector proxy, SVNEntryRevisionReference reference, long options,
			ISVNProgressMonitor monitor) throws SVNConnectorException {
		final SVNProperty[][] retVal = new SVNProperty[1][];
		proxy.listProperties(reference, SVNDepth.EMPTY, null, options, (personalProps, inheritedProps) -> {
			ArrayList<SVNProperty> props = new ArrayList<>();
			Collections.addAll(props, personalProps.data);
			for (Pair iProps : inheritedProps) {
				Collections.addAll(props, iProps.data);
			}
			retVal[0] = personalProps.data;
		}, monitor);
		return retVal[0];
	}

	public static SVNChangeStatus[] status(ISVNConnector proxy, String path, SVNDepth depth, long options,
			ISVNProgressMonitor monitor) throws SVNConnectorException {
		final ArrayList<SVNChangeStatus> statuses = new ArrayList<>();
		proxy.status(path, depth, options, null, status -> statuses.add(status), monitor);

		for (Iterator<SVNChangeStatus> it = statuses.iterator(); it.hasNext() && !monitor.isActivityCancelled();) {
			final SVNChangeStatus svnChangeStatus = it.next();
			if (svnChangeStatus.hasConflict && svnChangeStatus.treeConflicts == null) {
				proxy.getInfo(new SVNEntryRevisionReference(svnChangeStatus.path), SVNDepth.EMPTY,
						ISVNConnector.Options.FETCH_ACTUAL_ONLY, null, info -> svnChangeStatus.setTreeConflicts(info.treeConflicts), monitor);
			}
		}

		return statuses.toArray(new SVNChangeStatus[statuses.size()]);
	}

	public static void diffStatus(ISVNConnector proxy, final Collection<SVNDiffStatus> statuses,
			SVNEntryRevisionReference reference1, SVNEntryRevisionReference reference2, SVNDepth depth, long options,
			ISVNProgressMonitor monitor) throws SVNConnectorException {
		proxy.diffStatusTwo(reference1, reference2, depth, options, null, status -> statuses.add(status), monitor);
	}

	public static void diffStatus(ISVNConnector proxy, final Collection<SVNDiffStatus> statuses,
			SVNEntryReference reference, SVNRevisionRange range, SVNDepth depth, long options,
			ISVNProgressMonitor monitor) throws SVNConnectorException {
		proxy.diffStatus(reference, range, depth, options, null, status -> statuses.add(status), monitor);
	}

	public static SVNEntry[] list(ISVNConnector proxy, SVNEntryRevisionReference reference, SVNDepth depth,
			int direntFields, long options, ISVNProgressMonitor monitor) throws SVNConnectorException {
		final ArrayList<SVNEntry> entries = new ArrayList<>();
		proxy.listEntries(reference, depth, direntFields, options, entry -> entries.add(entry), monitor);
		return entries.toArray(new SVNEntry[entries.size()]);
	}

	public static SVNLogEntry[] logEntries(ISVNConnector proxy, SVNEntryReference reference, SVNRevision revisionStart,
			SVNRevision revisionEnd, long options, String[] revProps, long limit, ISVNProgressMonitor monitor)
					throws SVNConnectorException {
		return SVNUtility.logEntries(proxy, reference,
				new SVNRevisionRange[] { new SVNRevisionRange(revisionStart, revisionEnd) }, options, revProps, limit,
				monitor);
	}

	public static SVNLogEntry[] logEntries(ISVNConnector proxy, SVNEntryReference reference,
			SVNRevisionRange[] revisionRanges, long options, String[] revProps, long limit, ISVNProgressMonitor monitor)
					throws SVNConnectorException {
		SVNLogEntryCallbackWithMergeInfo callback = new SVNLogEntryCallbackWithMergeInfo();
		proxy.listHistoryLog(reference, revisionRanges, revProps, limit, options, callback, monitor);
		return callback.getEntries();
	}

	public static SVNEntryInfo[] info(SVNEntryRevisionReference reference) {
		ISVNConnector proxy = CoreExtensionsManager.instance().getSVNConnectorFactory().createConnector();
		try {
			return SVNUtility.info(proxy, reference, SVNDepth.EMPTY, new SVNNullProgressMonitor());
		} catch (Exception ex) {
			return null;
		} finally {
			proxy.dispose();
		}
	}

	public static SVNEntryInfo[] info(ISVNConnector proxy, SVNEntryRevisionReference reference, SVNDepth depth,
			ISVNProgressMonitor monitor) throws SVNConnectorException {
		final ArrayList<SVNEntryInfo> infos = new ArrayList<>();
		proxy.getInfo(reference, depth, ISVNConnector.Options.FETCH_ACTUAL_ONLY, null, info -> infos.add(info), monitor);
		return infos.toArray(new SVNEntryInfo[infos.size()]);
	}

	public static SVNEntryRevisionReference convertRevisionReference(ISVNConnector proxy,
			SVNEntryRevisionReference entry, ISVNProgressMonitor monitor) throws SVNConnectorException {
		if (entry.revision != null && entry.pegRevision != null && !entry.revision.equals(entry.pegRevision)
				&& entry.revision.getKind() == SVNRevision.Kind.NUMBER) {
			SVNEntryInfo[] info = SVNUtility.info(proxy, entry, SVNDepth.EMPTY, monitor);
			if (info != null && info.length > 0 && info[0].url != null) {
				return new SVNEntryRevisionReference(info[0].url, entry.revision, entry.revision);
			}
		}
		return entry;
	}

	public static String getStatusText(String status) {
		if (status == null) {
			status = "NotExists"; //$NON-NLS-1$
		}
		return SVNMessages.getString("Status_" + status); //$NON-NLS-1$
	}

	public static IRepositoryRoot getTrunkLocation(IRepositoryResource resource) {
		return SVNUtility.getRootLocation(resource, resource.getRepositoryLocation().getTrunkLocation(),
				IRepositoryRoot.KIND_TRUNK);
	}

	public static IRepositoryRoot getBranchesLocation(IRepositoryResource resource) {
		return SVNUtility.getRootLocation(resource, resource.getRepositoryLocation().getBranchesLocation(),
				IRepositoryRoot.KIND_BRANCHES);
	}

	public static IRepositoryRoot getTagsLocation(IRepositoryResource resource) {
		return SVNUtility.getRootLocation(resource, resource.getRepositoryLocation().getTagsLocation(),
				IRepositoryRoot.KIND_TAGS);
	}

	public static IRepositoryContainer getProposedTrunk(IRepositoryLocation location) {
		return location.asRepositoryContainer(SVNUtility.getProposedTrunkLocation(location), false);
	}

	public static IRepositoryContainer getProposedBranches(IRepositoryLocation location) {
		return location.asRepositoryContainer(SVNUtility.getProposedBranchesLocation(location), false);
	}

	public static IRepositoryContainer getProposedTags(IRepositoryLocation location) {
		return location.asRepositoryContainer(SVNUtility.getProposedTagsLocation(location), false);
	}

	public static String getProposedTrunkLocation(IRepositoryLocation location) {
		String baseUrl = location.getUrl();
		return location.isStructureEnabled() ? baseUrl + "/" + location.getTrunkLocation() : baseUrl; //$NON-NLS-1$
	}

	public static String getProposedBranchesLocation(IRepositoryLocation location) {
		String baseUrl = location.getUrl();
		return location.isStructureEnabled() ? baseUrl + "/" + location.getBranchesLocation() : baseUrl; //$NON-NLS-1$
	}

	public static String getProposedTagsLocation(IRepositoryLocation location) {
		String baseUrl = location.getUrl();
		return location.isStructureEnabled() ? baseUrl + "/" + location.getTagsLocation() : baseUrl; //$NON-NLS-1$
	}

	public static IRepositoryRoot[] findRoots(String resourceUrl, boolean longestOnly) {
		if (!SVNUtility.isValidSVNURL(resourceUrl)) {
			return new IRepositoryRoot[0];
		}
		IPath url = SVNUtility.createPathForSVNUrl(resourceUrl);
		IRepositoryLocation[] locations = SVNRemoteStorage.instance().getRepositoryLocations();
		ArrayList<IRepositoryRoot> roots = new ArrayList<>();
		for (IRepositoryLocation location : locations) {
			IPath locationUrl = SVNUtility.createPathForSVNUrl(location.getUrl());
			if (url.segmentCount() < locationUrl.segmentCount() && !url.isPrefixOf(locationUrl)) {
				continue;
			}
			if (locationUrl.isPrefixOf(url) || // performance optimization: repository root URL detection [if is not cached] requires interaction with a remote host
					SVNUtility.createPathForSVNUrl(location.getRepositoryRootUrl()).isPrefixOf(url)) {
				SVNUtility.addRepositoryRoot(roots,
						(IRepositoryRoot) location.asRepositoryContainer(resourceUrl, false).getRoot(), longestOnly);
			}
		}
		IRepositoryRoot[] repositoryRoots = roots.toArray(new IRepositoryRoot[roots.size()]);
		if (!longestOnly) {
			Arrays.sort(repositoryRoots, Comparator.comparing(IRepositoryRoot::getUrl).reversed());
		}
		return repositoryRoots;
	}

	private static void addRepositoryRoot(List<IRepositoryRoot> container, IRepositoryRoot root, boolean longestOnly) {
		if (longestOnly && container.size() > 0) {
			int cnt = SVNUtility.createPathForSVNUrl(root.getUrl()).segmentCount();
			int cnt2 = SVNUtility.createPathForSVNUrl(container.get(0).getUrl()).segmentCount();
			if (cnt > cnt2) {
				container.clear();
				container.add(root);
			} else if (cnt == cnt2) {
				container.add(root);
			}
		} else {
			container.add(root);
		}
	}

	public static String getSVNFolderName() {
		if (SVNUtility.svnFolderName == null) {
			String name = FileUtility.getEnvironmentVariables().get("SVN_ASP_DOT_NET_HACK") != null ? "_svn" : ".svn"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			SVNUtility.svnFolderName = System.getProperty("javasvn.admindir", name); //$NON-NLS-1$
		}
		return SVNUtility.svnFolderName;
	}

	public static boolean hasSVNFolderInOrAbove(IResource resource) {
		IPath location = FileUtility.getResourcePath(resource);
		return location != null && SVNUtility.hasSVNFolderInOrAbove(location.toFile());
	}

	public static boolean hasSVNFolderInOrAbove(File node) {
		String svnFolderName = SVNUtility.getSVNFolderName();
		node = node.isFile() ? node.getParentFile() : node;

		do {
			if (new File(node, svnFolderName).exists()) {
				return true;
			}
		} while ((node = node.getParentFile()) != null);

		return false;
	}

	public static String getResourceParent(IRepositoryResource resource) {
		String parent = ""; //$NON-NLS-1$
		String url = resource.getUrl();
		String rootUrl = resource.getRoot().getUrl();
		if (url.equals(rootUrl)) {
			return ""; //$NON-NLS-1$
		}
		parent = url.substring(rootUrl.length(), url.length() - resource.getName().length() - 1);
		return parent;
	}

	public static IRepositoryResource copyOf(IRepositoryResource resource) {
		String url = resource.getUrl();
		return resource instanceof IRepositoryFile
				? (IRepositoryResource) resource.asRepositoryFile(url, false)
						: resource.asRepositoryContainer(url, false);
	}

	public static IRevisionLink createRevisionLink(IRepositoryResource resource) {
		return new SVNRevisionLink(resource);
	}

	public static IRepositoryResource[] makeResourceSet(IRepositoryResource upPoint, String relativeReference,
			boolean isFile) {
		String url = SVNUtility.normalizeURL(upPoint.getUrl() + "/" + relativeReference); //$NON-NLS-1$
		IRepositoryLocation location = upPoint.getRepositoryLocation();
		IRepositoryResource downPoint = isFile
				? (IRepositoryResource) location.asRepositoryFile(url, false)
						: location.asRepositoryContainer(url, false);
		downPoint.setPegRevision(upPoint.getPegRevision());
		downPoint.setSelectedRevision(upPoint.getSelectedRevision());
		return SVNUtility.makeResourceSet(upPoint, downPoint);
	}

	public static IRepositoryResource[] makeResourceSet(IRepositoryResource upPoint, IRepositoryResource downPoint) {
		ArrayList<IRepositoryResource> resourceSet = new ArrayList<>();
		while (downPoint != null && !downPoint.equals(upPoint)) {
			resourceSet.add(0, downPoint);
			downPoint = downPoint.getParent();
		}
		return resourceSet.toArray(new IRepositoryResource[resourceSet.size()]);
	}

	public static boolean isValidSVNURL(String url) {
		try {
			URL svnUrl = SVNUtility.getSVNUrl(url);
			String host = svnUrl.getHost();
			if (!host.matches("[a-zA-Z0-9_\\-]+(?:\\.[a-zA-Z0-9_\\-]+)*") && host.length() > 0 || //$NON-NLS-1$
					host.length() == 0 && !"file".equals(svnUrl.getProtocol())) { //$NON-NLS-1$
				return false;
			}
			return true;
		} catch (MalformedURLException e) {
			return false;
		}
	}

	public static URL getSVNUrl(String url) throws MalformedURLException {
		return SVNUtility.getSVNUrlStreamHandler(url).getURL();
	}

	public static SVNURLStreamHandler getSVNUrlStreamHandler(String url) throws MalformedURLException {
		SVNURLStreamHandler retVal = new SVNURLStreamHandler();
		new URL(null, url, retVal);
		return retVal;
	}

	public static String base64Encode(String data) {
		if (data == null) {
			return null;
		}
		return new String(Base64.encode(data.getBytes()));
	}

	public static String base64Decode(String encoded) {
		if (encoded == null) {
			return null;
		}
		return new String(Base64.decode(encoded.getBytes()));
	}

	public static void addSVNNotifyListener(ISVNConnector proxy, ISVNNotificationCallback listener) {
		ISVNNotificationCallback composite = proxy.getNotificationCallback();
		if (composite == null || !(composite instanceof SVNNotificationComposite)) {
			proxy.setNotificationCallback(composite = new SVNNotificationComposite());
		}
		((SVNNotificationComposite) composite).add(listener);
	}

	public static void removeSVNNotifyListener(ISVNConnector proxy, ISVNNotificationCallback listener) {
		ISVNNotificationCallback composite = proxy.getNotificationCallback();
		if (composite != null && composite instanceof SVNNotificationComposite) {
			((SVNNotificationComposite) composite).remove(listener);
		}
	}

	public static void reorder(SVNDiffStatus[] statuses, final boolean parent2Child) {
		/*
		 * Resources are reported in reverse for tree deletions and in averse for tree additions. Next, when we
		 * virtually have a replacement of trees, in actuality we would get two sequences reported: one for deletion and
		 * one for addition. The reason, I suspect is that there could be a node kind change with file replaced with folder and otherwise.
		 * So, we should ensure we take into account the actual action type when reordering the nodes and deletions should go first
		 * in case they're followed by additions of the nodes with the same paths.
		 */
		SVNDiffStatus[] tStatuses = new SVNDiffStatus[statuses.length];
		System.arraycopy(statuses, 0, tStatuses, 0, statuses.length);
		Arrays.sort(tStatuses, new Comparator<SVNDiffStatus>() {
			@Override
			public int compare(SVNDiffStatus d1, SVNDiffStatus d2) {
				int retVal = parent2Child ? d1.pathPrev.compareTo(d2.pathPrev) : d2.pathPrev.compareTo(d1.pathPrev);
				if (retVal == 0 && d1.textStatus != d2.textStatus) {
					if (d1.textStatus == SVNEntryStatus.Kind.DELETED) {
						return parent2Child ? -1 : 1;
					}
					if (d2.textStatus == SVNEntryStatus.Kind.DELETED) {
						return parent2Child ? 1 : -1;
					}
				}
				return retVal;
			}

			@Override
			public boolean equals(Object obj) {
				return false;
			}
		});
		for (int i = 0, k = 0; i < tStatuses.length; i++) {
			// skip if the node reference is already copied by [deletions shift]
			if (tStatuses[i] != null) {
				statuses[k] = tStatuses[i];
				k++;
				if (tStatuses[i].textStatus == SVNEntryStatus.Kind.DELETED) {
					for (int m = i + 1; m < tStatuses.length; m++) {
						// shift prefixed deletions next to their prefix
						if (tStatuses[m] != null && tStatuses[m].textStatus == SVNEntryStatus.Kind.DELETED
								&& tStatuses[m].pathPrev.indexOf(tStatuses[i].pathPrev) == 0) {
							statuses[k] = tStatuses[m];
							k++;
							tStatuses[m] = null;
						}
					}
				}
			}
		}
	}

	public static void reorder(SVNChangeStatus[] statuses, final boolean parent2Child) {
		Arrays.sort(statuses, new Comparator<SVNChangeStatus>() {
			@Override
			public int compare(SVNChangeStatus o1, SVNChangeStatus o2) {
				String s1 = o1 != null ? o1.path : ""; //$NON-NLS-1$
				String s2 = o2 != null ? o2.path : ""; //$NON-NLS-1$
				return parent2Child ? s1.compareTo(s2) : s2.compareTo(s1);
			}

			@Override
			public boolean equals(Object obj) {
				return false;
			}
		});
	}

	public static void reorder(IRepositoryResource[] resources, final boolean parent2Child) {
		Arrays.sort(resources, new Comparator<IRepositoryResource>() {
			@Override
			public int compare(IRepositoryResource o1, IRepositoryResource o2) {
				String s1 = o1.getUrl();
				String s2 = o2.getUrl();
				return parent2Child ? s1.compareTo(s2) : s2.compareTo(s1);
			}

			@Override
			public boolean equals(Object obj) {
				return false;
			}
		});
	}

	private static final byte[] uri_char_validity = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
			1, 0, 0, 1, 0, 0,

			/* 64 */
			1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 1, 0, 1, 1, 1,
			1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 1, 0,

			/* 128 */
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,

			/* 192 */
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, };

	public static boolean isHexDigit(char ch) {
		return Character.isDigit(ch) || Character.toUpperCase(ch) >= 'A' && Character.toUpperCase(ch) <= 'F';
	}

	private static int hexValue(char ch) {
		if (Character.isDigit(ch)) {
			return ch - '0';
		}
		ch = Character.toUpperCase(ch);
		return ch - 'A' + 0x0A;
	}

	public static String encodeURL(String url) {
		if (url == null) {
			return null;
		}
		url = SVNUtility.normalizeURL(url);
		int idx = url.startsWith("file:///") //$NON-NLS-1$
				? "file:///".length() //$NON-NLS-1$
						: url.startsWith("file://") ? url.indexOf("/", "file://".length()) + 1 : url.indexOf("://") + 3; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		idx = url.indexOf("/", idx); //$NON-NLS-1$
		if (idx == -1) {
			return url;
		}
		String retVal = url.substring(0, idx);
		String partToEncode = url.substring(idx);
		// user name should be never encoded
		idx = retVal.indexOf('@');
		if (idx != -1) {
			String protocol = retVal.substring(0, retVal.indexOf("://") + 3); //$NON-NLS-1$
			String serverPart = retVal.substring(idx);
			retVal = protocol + retVal.substring(protocol.length(), idx) + serverPart;
		}
		retVal += doEncode(partToEncode);
		return retVal;
	}

	public static String decodeURL(String url) {
		if (url == null) {
			return null;
		}
		url = SVNUtility.normalizeURL(url);
		int idx = url.startsWith("file:///") //$NON-NLS-1$
				? "file:///".length() //$NON-NLS-1$
						: url.startsWith("file://") ? url.indexOf("/", "file://".length()) + 1 : url.indexOf("://") + 3; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		idx = url.indexOf("/", idx); //$NON-NLS-1$
		if (idx == -1) {
			return url;
		}
		String retVal = url.substring(0, idx);
		String partToDecode = url.substring(idx);
		// user name should be never decoded
		idx = retVal.indexOf('@');
		if (idx != -1) {
			String protocol = retVal.substring(0, retVal.indexOf("://") + 3); //$NON-NLS-1$
			String serverPart = retVal.substring(idx);
			retVal = protocol + retVal.substring(protocol.length(), idx) + serverPart;
		}
		retVal += doDecode(partToDecode);
		return retVal;
	}

	protected static String doEncode(String src) {
		StringBuilder sb = null;
		byte[] bytes;
		try {
			bytes = src.getBytes("UTF-8"); //$NON-NLS-1$
		} catch (UnsupportedEncodingException e) {
			bytes = src.getBytes();
		}
		for (int i = 0; i < bytes.length; i++) {
			int index = bytes[i] & 0xFF;
			if (uri_char_validity[index] > 0) {
				if (sb != null) {
					sb.append((char) bytes[i]);
				}
				continue;
			}
			if (sb == null) {
				sb = new StringBuilder();
				try {
					sb.append(new String(bytes, 0, i, "UTF-8")); //$NON-NLS-1$
				} catch (UnsupportedEncodingException e) {
					sb.append(new String(bytes, 0, i));
				}
			}
			sb.append("%"); //$NON-NLS-1$

			sb.append(Character.toUpperCase(Character.forDigit((index & 0xF0) >> 4, 16)));
			sb.append(Character.toUpperCase(Character.forDigit(index & 0x0F, 16)));
		}
		return sb == null ? src : sb.toString();
	}

	protected static String doDecode(String src) {
		// this is string in ASCII-US encoding.
		boolean query = false;
		boolean decoded = false;
		int length = src.length();
		ByteArrayOutputStream bos = new ByteArrayOutputStream(length);
		for (int i = 0; i < length; i++) {
			byte ch = (byte) src.charAt(i);
			if (ch == '?') {
				query = true;
			} else if (ch == '+' && query) {
				ch = ' ';
			} else if (ch == '%' && i + 2 < length && isHexDigit(src.charAt(i + 1)) && isHexDigit(src.charAt(i + 2))) {
				ch = (byte) (hexValue(src.charAt(i + 1)) * 0x10 + hexValue(src.charAt(i + 2)));
				decoded = true;
				i += 2;
			} else {
				// if character is not URI-safe try to encode it.
			}
			bos.write(ch);
		}
		if (!decoded) {
			return src;
		}
		try {
			return new String(bos.toByteArray(), "UTF-8"); //$NON-NLS-1$
		} catch (UnsupportedEncodingException e) {
		}
		return src;
	}

	public static String normalizeURL(String url) {
		if (url == null) {
			return null;
		}

		int len = url.length();
		int st = 0;
		while (st < len && url.charAt(st) <= ' ') {
			st++;
		}
		url = url.substring(st);

		String prefix = ""; //$NON-NLS-1$
		final String[] knownPrefixes = { "http://", "https://", "svn://", "svn+ssh://", "file:///", //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$//$NON-NLS-4$//$NON-NLS-5$
				"file://", "^/", "../", "//", "/" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		for (String element : knownPrefixes) {
			if (url.startsWith(element)) {
				prefix = element;
				url = url.substring(element.length());
				break;
			}
		}

		StringTokenizer tokenizer = new StringTokenizer(PatternProvider.replaceAll(url, "([\\\\])+", "/"), "/", false); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		StringBuilder retVal = new StringBuilder();
		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			retVal.append(retVal.length() == 0 ? token : "/" + token); //$NON-NLS-1$
		}
		if (!"".equals(prefix)) { //$NON-NLS-1$
			retVal.insert(0, prefix);
		}
		return retVal.toString();
	}

	public static Exception validateRepositoryLocation(IRepositoryLocation location, ISVNProgressMonitor monitor) {
		ISVNConnector proxy = location.acquireSVNProxy();
		try {
			proxy.listEntries(new SVNEntryRevisionReference(SVNUtility.encodeURL(location.getUrl()), null, null),
					SVNDepth.EMPTY, SVNEntry.Fields.NONE, ISVNConnector.Options.NONE, entry -> {
					}, monitor);
		} catch (Exception e) {
			return e;
		} finally {
			location.releaseSVNProxy(proxy);
			location.dispose();
		}
		return null;
	}

	public static IProxyData getProxyData(String host, String type) {
		IProxyService proxyService = SVNTeamPlugin.instance().getProxyService();
		if (host != null && host.trim().length() > 0 && proxyService.isProxiesEnabled()) {
			try {
				URI uri = new URI(type, "//" + host, null); //$NON-NLS-1$
				IProxyData[] proxyDatas = proxyService.select(uri);
				return proxyDatas != null && proxyDatas.length > 0 ? proxyDatas[0] : null;
			} catch (URISyntaxException e) {
				// return null;
			}
		}
		return null;
	}

	public static void configureProxy(ISVNConnector proxy, IRepositoryLocation location) {
		proxy.setUsername(location.getUsername());
		proxy.setPassword(location.getPassword());

		SSLSettings sslSettings = location.getSSLSettings();
		String host = "localhost"; //$NON-NLS-1$
		String protocol = "file"; //$NON-NLS-1$
		try {
			URL url = SVNUtility.getSVNUrl(location.getUrl());
			protocol = url.getProtocol();
			if (!protocol.equals("file")) { //$NON-NLS-1$
				host = url.getHost();
			}
		} catch (MalformedURLException ex) {
			//skip
		}
		String proxyType = protocol.equals("https") ? IProxyData.HTTPS_PROXY_TYPE : IProxyData.HTTP_PROXY_TYPE; //$NON-NLS-1$
		SVNCachedProxyCredentialsManager proxyCredetialsManager = SVNRemoteStorage.instance()
				.getProxyCredentialsManager();
		IProxyData proxyData = SVNUtility.getProxyData(host, proxyType);
	}

	public static SVNChangeStatus getSVNInfoForNotConnected(IResource root) {
		IPath location = FileUtility.getResourcePath(root);
		IPath checkedPath = root.getType() == IResource.FILE ? location.removeLastSegments(1) : location;
		if (SVNUtility.isPriorToSVN17() && !checkedPath.append(SVNUtility.getSVNFolderName()).toFile().exists()) {
			return null;
		}
		ISVNConnector proxy = CoreExtensionsManager.instance().getSVNConnectorFactory().createConnector();
		try {
			SVNChangeStatus[] st = SVNUtility.status(proxy, location.toString(), SVNDepth.IMMEDIATES,
					ISVNConnector.Options.INCLUDE_UNCHANGED, new SVNNullProgressMonitor());
			if (st != null && st.length > 0) {
				SVNUtility.reorder(st, true);
				return st[0].url == null ? null : st[0];
			}
			return null;
		} catch (Exception ex) {
			return null;
		} finally {
			proxy.dispose();
		}
	}

	public static String getPropertyForNotConnected(IResource root, String propertyName) {
		String location = FileUtility.getWorkingCopyPath(root);
		ISVNConnector proxy = CoreExtensionsManager.instance().getSVNConnectorFactory().createConnector();
		try {
			SVNProperty data = proxy.getProperty(new SVNEntryRevisionReference(location, null, SVNRevision.WORKING),
					propertyName, null, new SVNNullProgressMonitor());
			return data == null ? null : data.value;
		} catch (Exception ex) {
			return null;
		} finally {
			proxy.dispose();
		}
	}

	public static boolean isIgnored(IResource resource) {
		if (FileUtility.isNotSupervised(resource) || resource.isDerived(IResource.CHECK_ANCESTORS)
				&& !CoreExtensionsManager.instance().getOptionProvider().is(IOptionProvider.COMMIT_DERIVED_ENABLED)
				|| Team.isIgnoredHint(resource) || SVNUtility.isMergeParts(resource)) {
			return true;
		}
		try {
			for (IIgnoreRecommendations ignore : CoreExtensionsManager.instance().getIgnoreRecommendations()) {
				if (ignore.isAcceptableNature(resource) && ignore.isIgnoreRecommended(resource)) {
					return true;
				}
			}
		} catch (CoreException ex) {
			throw new RuntimeException(ex);
		}
		return false;
	}

	public static Map<IProject, List<IResource>> splitWorkingCopies(IResource[] resources) {
		Map<IProject, List<IResource>> wc2Resources = new HashMap<>();

		ISVNConnector proxy = CoreExtensionsManager.instance().getSVNConnectorFactory().createConnector();
		try {
			Map<File, IProject> roots = new HashMap<>();
			for (IResource element : resources) {
				IProject wcRoot = element.getProject();
				File tFile = FileUtility.getResourcePath(wcRoot).toFile();
				Object[] realRoot = SVNUtility.getWCRoot(proxy, tFile, SVNUtility.getSVNInfo(tFile, proxy));
				IProject tRoot = roots.get(realRoot[0]);
				if (tRoot == null) {
					roots.put((File) realRoot[0], tRoot = wcRoot);
				}
				wcRoot = tRoot;

				List<IResource> wcResources = wc2Resources.get(wcRoot);
				if (wcResources == null) {
					wc2Resources.put(wcRoot, wcResources = new ArrayList<>());
				}
				wcResources.add(element);
			}
		} finally {
			proxy.dispose();
		}

		return wc2Resources;
	}

	public static Map splitWorkingCopies(File[] files) {
		Map wc2Resources = new HashMap();

		ISVNConnector proxy = CoreExtensionsManager.instance().getSVNConnectorFactory().createConnector();
		try {
			Map<File, SVNEntryInfo> file2info = new HashMap<>();
			for (File file : files) {
				file2info.put(file, SVNUtility.getSVNInfo(file, proxy));
			}

			ArrayList<File> restOfFiles = new ArrayList<>(Arrays.asList(files));
			while (restOfFiles.size() > 0) {
				File current = restOfFiles.get(0);
				SVNEntryInfo info = file2info.get(current);
				Object[] wcRoot = SVNUtility.getWCRoot(proxy, current, info);

				List wcResources = (List) wc2Resources.get(wcRoot[0]);
				if (wcResources == null) {
					wc2Resources.put(wcRoot[0], wcResources = new ArrayList());
				}

				IPath rootPath = new Path(((File) wcRoot[0]).getAbsolutePath());
				IPath rootInfoPath = SVNUtility.createPathForSVNUrl(((SVNEntryInfo) wcRoot[1]).url);
				for (Iterator it = restOfFiles.iterator(); it.hasNext();) {
					File checked = (File) it.next();
					if (rootPath.isPrefixOf(new Path(checked.getAbsolutePath()))) {
						if (rootInfoPath.isPrefixOf(SVNUtility.createPathForSVNUrl(file2info.get(checked).url))) {
							wcResources.add(checked);
							it.remove();
						}
					}
				}
			}
		} finally {
			proxy.dispose();
		}

		return wc2Resources;
	}

	private static Object[] getWCRoot(ISVNConnector proxy, File node, SVNEntryInfo info) {
		File oldRoot = node;
		SVNEntryInfo oldInfo = info;

		node = node.getParentFile();
		while (node != null) {
			SVNEntryInfo rootInfo = SVNUtility.getSVNInfo(node, proxy);
			if (rootInfo != null) {
				if (oldInfo == null) {
					oldInfo = rootInfo;
				} else if (!SVNUtility.createPathForSVNUrl(rootInfo.url)
						.isPrefixOf(SVNUtility.createPathForSVNUrl(oldInfo.url))) {
					return new Object[] { oldRoot, oldInfo };
				}
				oldRoot = node;
			} else if (oldInfo != null) {
				return new Object[] { oldRoot, oldInfo };
			}
			node = node.getParentFile();
		}

		if (oldInfo == null) {
			SVNUtility.getSVNInfo(oldRoot, proxy, true); //check for real reason
			// throw a generic exception otherwise
			String errMessage = SVNMessages.formatErrorString("Error_NonSVNPath", //$NON-NLS-1$
					new String[] { oldRoot.getAbsolutePath() });
			throw new RuntimeException(errMessage);
		}
		return new Object[] { oldRoot, oldInfo };
	}

	public static SVNEntryInfo getSVNInfo(File root) {
		ISVNConnector proxy = CoreExtensionsManager.instance().getSVNConnectorFactory().createConnector();
		try {
			return SVNUtility.getSVNInfo(root, proxy);
		} finally {
			proxy.dispose();
		}
	}

	public static SVNEntryInfo getSVNInfo(File root, ISVNConnector proxy) {
		return SVNUtility.getSVNInfo(root, proxy, false);
	}

	public static SVNEntryInfo getSVNInfo(File root, ISVNConnector proxy, boolean reportException) {
		if (root.exists()) {
			File svnMeta = root.isDirectory() ? root : root.getParentFile();
			svnMeta = new File(svnMeta.getAbsolutePath() + "/" + SVNUtility.getSVNFolderName()); //$NON-NLS-1$
			if (!SVNUtility.isPriorToSVN17() || svnMeta.exists()) {
				try {
					//NOTE WARNING! JavaHL always tries to access repository when revision is specified, even if the specified revision one of two local kinds: WORKING or BASE.
					//	so, just do not specify any revisions!
					SVNEntryInfo[] st = SVNUtility.info(proxy, new SVNEntryRevisionReference(root.getAbsolutePath()),
							SVNDepth.EMPTY, new SVNNullProgressMonitor());
					return st != null && st.length != 0 ? st[0] : null;
				} catch (Exception ex) {
					if (reportException) {
						throw new RuntimeException(ex);
					}
				}
			}
		}
		return null;
	}

	public static String[] asURLArray(IRepositoryResource[] resources, boolean encode) {
		String[] retVal = new String[resources.length];
		for (int i = 0; i < resources.length; i++) {
			retVal[i] = encode ? SVNUtility.encodeURL(resources[i].getUrl()) : resources[i].getUrl();
		}
		return retVal;
	}

	public static Map splitRepositoryLocations(IRepositoryResource[] resources) throws Exception {
		Map repository2Resources = new HashMap();
		for (IRepositoryResource element : resources) {
			IRepositoryLocation location = element.getRepositoryLocation();

			List tResources = (List) repository2Resources.get(location);
			if (tResources == null) {
				repository2Resources.put(location, tResources = new ArrayList());
			}
			tResources.add(element);
		}
		return SVNUtility.combineLocationsByUUID(repository2Resources);
	}

	public static Map splitRepositoryLocations(IResource[] resources) throws Exception {
		Map repository2Resources = new HashMap();
		SVNRemoteStorage storage = SVNRemoteStorage.instance();
		for (IResource element : resources) {
			IRepositoryLocation location = storage.getRepositoryLocation(element);

			List tResources = (List) repository2Resources.get(location);
			if (tResources == null) {
				repository2Resources.put(location, tResources = new ArrayList());
			}
			tResources.add(element);
		}
		return SVNUtility.combineLocationsByUUID(repository2Resources);
	}

	public static Map splitRepositoryLocations(File[] files) throws Exception {
		Map repository2Resources = new HashMap();
		for (File file : files) {
			IRepositoryResource resource = SVNFileStorage.instance().asRepositoryResource(file, false);
			IRepositoryLocation location = resource.getRepositoryLocation();

			List tResources = (List) repository2Resources.get(location);
			if (tResources == null) {
				repository2Resources.put(location, tResources = new ArrayList());
			}
			tResources.add(file);
		}
		return SVNUtility.combineLocationsByUUID(repository2Resources);
	}

	public static SVNEntry.Kind getNodeKind(String path, SVNEntry.Kind kind, boolean ignoreNone) {
		if (kind == Kind.DIR || kind == Kind.FILE || kind == Kind.SYMLINK) {
			return kind;
		} else if (kind == Kind.NONE || kind == Kind.UNKNOWN) {
			File f = new File(path);
			if (f.exists()) {
				try {
					IFileStore store = EFS.getStore(f.toURI());
					IFileInfo info = store.fetchInfo();
					if (info.getAttribute(EFS.ATTRIBUTE_SYMLINK)) {
						return Kind.SYMLINK;
					}
				} catch (CoreException e) {
					// uninterested
				}
				return f.isDirectory() ? Kind.DIR : Kind.FILE;
			}
			// ignore files absent in the WC base and WC working. But what is the reason why it is reported ?
			if (ignoreNone) {
				return Kind.NONE;
			}
		}
		String errMessage = BaseMessages.format("Error_UnrecognizedNodeKind", //$NON-NLS-1$
				new String[] { String.valueOf(kind), path });
		throw new RuntimeException(errMessage);
	}

	public static IRepositoryResource[] shrinkChildNodes(IRepositoryResource[] resources) {
		Set<IRepositoryResource> roots = new HashSet<>(Arrays.asList(resources));
		for (IRepositoryResource element : resources) {
			if (SVNUtility.hasRoots(roots, element)) {
				roots.remove(element);
			}
		}
		return roots.toArray(new IRepositoryResource[roots.size()]);
	}

	public static IRepositoryResource[] getCommonParents(IRepositoryResource[] resources) {
		Map<IRepositoryResource, ArrayList> byRepositoryRoots = new HashMap<>();
		for (IRepositoryResource element : resources) {
			IRepositoryResource root = element.getRoot();
			ArrayList tmp = byRepositoryRoots.get(root);
			if (tmp == null) {
				byRepositoryRoots.put(root, tmp = new ArrayList());
			}
			tmp.add(element);
		}
		HashSet<IRepositoryResource> container = new HashSet<>();
		for (ArrayList tmp : byRepositoryRoots.values()) {
			IRepositoryResource parent = SVNUtility
					.getCommonParent((IRepositoryResource[]) tmp.toArray(new IRepositoryResource[tmp.size()]));
			if (parent != null) {
				container.add(parent);
			}
		}
		return container.toArray(new IRepositoryResource[container.size()]);
	}

	public static String getAscendant(IRepositoryResource resource) {
		String pathUpToRoot = SVNUtility.getPathUpToRoot(resource);
		int idx = pathUpToRoot.indexOf('/');
		return idx == -1 ? pathUpToRoot : pathUpToRoot.substring(0, idx);
	}

	public static String getDescendant(IRepositoryResource resource) {
		String pathUpToRoot = SVNUtility.getPathUpToRoot(resource);
		int idx = pathUpToRoot.lastIndexOf('/');
		return idx == -1 ? pathUpToRoot : pathUpToRoot.substring(idx + 1);
	}

	public static String getPathUpToRoot(IRepositoryResource resource) {
		IRepositoryResource root = resource.getRoot();
		return root == resource ? resource.getName() : resource.getUrl().substring(root.getUrl().length() + 1);
	}

	/**
	 * Compares two revisions
	 * 
	 * @return: 0 if equal, -1 if less, 1 if more
	 */
	public static int compareRevisions(SVNRevision first, SVNRevision second, SVNEntryRevisionReference referenceFirst,
			SVNEntryRevisionReference referenceSecond, ISVNConnector proxy) throws SVNConnectorException {
		if (first.getKind() == SVNRevision.Kind.NUMBER && second.getKind() == SVNRevision.Kind.NUMBER) {
			SVNRevision.Number fromNumber = (SVNRevision.Number) first;
			SVNRevision.Number toNumber = (SVNRevision.Number) second;
			return fromNumber.getNumber() > toNumber.getNumber()
					? 1
							: fromNumber.getNumber() == toNumber.getNumber() ? 0 : -1;
		}
		SVNRevision.Date fromDate = null;
		SVNRevision.Date toDate = null;
		if (first.getKind() == SVNRevision.Kind.DATE) {
			fromDate = (SVNRevision.Date) first;
		} else {
			SVNEntryInfo[] entryInfo = SVNUtility.info(proxy, referenceFirst, SVNDepth.UNKNOWN,
					new SVNNullProgressMonitor());
			fromDate = SVNRevision.fromDate(entryInfo[0].lastChangedDate);
		}
		if (second.getKind() == SVNRevision.Kind.DATE) {
			toDate = (SVNRevision.Date) second;
		} else {
			SVNEntryInfo[] entryInfo = SVNUtility.info(proxy, referenceSecond, SVNDepth.UNKNOWN,
					new SVNNullProgressMonitor());
			toDate = SVNRevision.fromDate(entryInfo[0].lastChangedDate);
		}
		return fromDate.getDate() > toDate.getDate() ? 1 : fromDate.getDate() == toDate.getDate() ? 0 : -1;
	}

	private static final Pattern MERGE_PART = Pattern.compile("r\\d+"); //$NON-NLS-1$

	private static boolean isMergeParts(IResource resource) {
		String ext = resource.getFileExtension();
		return ext != null && SVNUtility.MERGE_PART.matcher(ext).matches();
	}

	private static Map combineLocationsByUUID(Map repository2Resources) throws Exception {
		Map locationUtility2Resources = new HashMap();
		for (Object element : repository2Resources.entrySet()) {
			Map.Entry entry = (Map.Entry) element;
			IRepositoryLocation location = (IRepositoryLocation) entry.getKey();
			List tResources = (List) entry.getValue();
			RepositoryLocationUtility locationUtility = new RepositoryLocationUtility(location);
			List tResources2 = (List) locationUtility2Resources.get(locationUtility);
			if (tResources2 == null) {
				locationUtility2Resources.put(locationUtility, tResources2 = new ArrayList());
			}
			tResources2.addAll(tResources);
		}
		repository2Resources.clear();
		for (Object element : locationUtility2Resources.entrySet()) {
			Map.Entry entry = (Map.Entry) element;
			RepositoryLocationUtility locationUtility = (RepositoryLocationUtility) entry.getKey();
			repository2Resources.put(locationUtility.location, entry.getValue());
		}
		return repository2Resources;
	}

	private static boolean hasRoots(Set<IRepositoryResource> roots, IRepositoryResource node) {
		while ((node = node.getParent()) != null) {
			if (roots.contains(node)) {
				return true;
			}
		}
		return false;
	}

	private static IRepositoryResource getCommonParent(IRepositoryResource[] resources) {
		if (resources == null || resources.length == 0) {
			return null;
		}
		IRepositoryResource base = resources[0].getParent();
		while (base != null) { // can be null for resources from different repositories
			int startsCnt = 0;
			IPath baseUrl = SVNUtility.createPathForSVNUrl(base.getUrl());
			for (IRepositoryResource element : resources) {
				if (baseUrl.isPrefixOf(SVNUtility.createPathForSVNUrl(element.getUrl()))) {
					startsCnt++;
				}
			}
			if (startsCnt == resources.length) {
				break;
			}
			base = base.getParent();
		}
		return base;
	}

	private static IRepositoryRoot getRootLocation(IRepositoryResource resource, String rootName, int kind) {
		IRepositoryLocation location = resource.getRepositoryLocation();
		IRepositoryRoot root = (IRepositoryRoot) resource.getRoot();
		if (!location.isStructureEnabled() || root.getName().equals(rootName)) {
			return root;
		}
		IRepositoryResource retVal = null;
		int rootKind = root.getKind();
		IRepositoryResource parent = root.getParent();
		if (rootKind == IRepositoryRoot.KIND_ROOT || parent == null /*repository and location root at the same time*/) {
			retVal = root.asRepositoryContainer(rootName, false);
		} else if (rootKind == IRepositoryRoot.KIND_LOCATION_ROOT) {
			IRepositoryRoot tmp = (IRepositoryRoot) parent.getRoot();
			if (root.getName().equals(location.getTrunkLocation()) || // the actual reason for bug #385530
					root.getName().equals(location.getBranchesLocation())
					|| root.getName().equals(location.getTagsLocation())) {
				retVal = parent.asRepositoryContainer(rootName, false);
			} else if (tmp.getKind() == IRepositoryRoot.KIND_ROOT) {
				retVal = root.asRepositoryContainer(rootName, false);
			}
			root = tmp;
		}
		if (retVal == null) {
			IRepositoryResource rootParent = root.getParent();
			retVal = rootParent.asRepositoryContainer(rootName, false);
		}
		if (!(retVal instanceof IRepositoryRoot) || ((IRepositoryRoot) retVal).getKind() != kind) {
			// check for issue conditions (see bug #385530)
			throw new RuntimeException("Resource " + resource.getUrl() + " rootName " + rootName + " detected root " //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
					+ String.valueOf(rootKind) + " " + root.getUrl() + " location URL " + location.getUrl() + " retVal " //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
					+ retVal.getUrl());
		}
		return (IRepositoryRoot) retVal;
	}

	/**
	 * @param resources
	 * @return projects which contain modifications on tag
	 */
	public static IProject[] getTagOperatedProjects(IResource[] resources) {
		Set<IProject> operatedProjects = new HashSet<>();
		for (IResource element : resources) {
			IProject project = element.getProject();
			if (project != null && !operatedProjects.contains(project)) {
				SVNTeamProvider provider = (SVNTeamProvider) RepositoryProvider.getProvider(project,
						SVNTeamPlugin.NATURE_ID);
				if (provider != null && provider.isVerifyTagOnCommit()
						&& ((IRepositoryRoot) SVNRemoteStorage.instance().asRepositoryResource(element).getRoot())
						.getKind() == IRepositoryRoot.KIND_TAGS) {
					operatedProjects.add(project);
				}
			}
		}
		return operatedProjects.toArray(new IProject[0]);
	}

	public static String getDepthArg(SVNDepth depth, long options) {
		//TODO move into SVNDepth
		String depthArg = (options & ISVNConnector.Options.DEPTH_IS_STICKY) != 0 ? " --set-depth " : " --depth "; //$NON-NLS-1$
		if (depth == SVNDepth.EMPTY) {
			return depthArg + "empty "; //$NON-NLS-1$
		}
		if (depth == SVNDepth.INFINITY) {
			return depthArg + "infinity"; //$NON-NLS-1$
		}
		if (depth == SVNDepth.IMMEDIATES) {
			return depthArg + "immediates "; //$NON-NLS-1$
		}
		if (depth == SVNDepth.UNKNOWN) {
			return ""; //$NON-NLS-1$
		}
		if (depth == SVNDepth.EXCLUDE) {
			return depthArg + "exclude "; //$NON-NLS-1$
		}
		return depthArg + "files "; //$NON-NLS-1$
	}

	public static String getIgnoreExternalsArg(long options) {
		//TODO remove later
		return ISVNConnector.Options.asCommandLine(options);
	}

	/**
	 * Work with svn externals, e.g. parses
	 *
	 * 
	 * @author Igor Burilo
	 */
	public static class SVNExternalPropertyData {
		public String localPath;

		public String url;

		public String revision;

		public String pegRevision;

		public boolean isNewFormat;

		public SVNExternalPropertyData(String localPath, String url, String pegRevision, String revision,
				boolean isNewFormat) {
			this.isNewFormat = isNewFormat;
			this.localPath = localPath;
			this.pegRevision = pegRevision;
			this.revision = revision;
			this.url = url;
		}

		private static String[] splitExternalOnParts(String str, boolean isCheckSpacesInLocalPath) {
			if (str == null) {
				return new String[0];
			}
			str = str.trim();

			if (!isCheckSpacesInLocalPath) {
				return str.trim().split("[\\t ]+"); //$NON-NLS-1$
			}

			//process that local path can be enclosed in quotes or contain the \ (backslash) character for escaping characters
			List<String> parts = new ArrayList<>();
			StringBuilder tmpString = new StringBuilder();
			boolean hasQuote = false;
			for (int i = 0; i < str.length(); i++) {
				char ch = str.charAt(i);
				if (ch == '\'' || ch == '"') {
					hasQuote = !hasQuote;
				} else if (ch == ' ' || ch == '\t') {
					if (hasQuote) {
						tmpString.append(ch);
					} else if (tmpString.length() > 0) {
						parts.add(tmpString.toString());
						tmpString.setLength(0);
					}
				} else if (ch == '\\' && i + 1 < str.length() - 1 && str.charAt(i + 1) == ' ') {
					//if space character is escaped with backslash(\) then doesn't separate string to parts here
					tmpString.append(' ');
					i++;
				} else {
					tmpString.append(ch);
				}
			}
			parts.add(tmpString.toString());
			return parts.toArray(new String[0]);
		}

		public static String serialize(SVNExternalPropertyData[] data) {
			String retVal = "";
			for (SVNExternalPropertyData entry : data) {
				retVal += entry.toString() + "\n";
			}
			return retVal;
		}

		/**
		 * Parse external property and return result in raw format, i.e. it doesn't process and encode url, it doesn't parse revisions etc.
		 * 
		 * @param property
		 * @return
		 */
		public static SVNExternalPropertyData[] parse(String property) {
			if (property == null) {
				return new SVNExternalPropertyData[0];
			}

			List<SVNExternalPropertyData> resList = new ArrayList<>();

			String[] externals = property.trim().split("[\\n|\\r\\n]+"); // it seems different clients have different behaviours wrt trailing whitespace.. so trim() to be safe //$NON-NLS-1$
			for (String external : externals) {
				if (external.startsWith("#")) { // commented externals, see bug #316114
					continue;
				}
				boolean isCheckSpacesInLocalPath = CoreExtensionsManager.instance()
						.getSVNConnectorFactory()
						.getSVNAPIVersion() >= ISVNConnectorFactory.APICompatibility.SVNAPI_1_6_x;
						String[] parts = SVNExternalPropertyData.splitExternalOnParts(external, isCheckSpacesInLocalPath);
						// 2 - name + URL
						// 3 - name + -rRevision + URL
						// 4 - name + -r + Revision + URL
						//or in SVN 1.5 format
						// 2 - URL@peg + name
						// 3 - -rRevision + URL@peg + name
						// 4 - -r + Revision + URL@peg + name
						if (parts.length < 2 || parts.length > 4) {
							throw new UnreportableException("Malformed external, " + parts.length + ", " + external); //$NON-NLS-1$//$NON-NLS-2$
						}

						String name = null;
						String url = null;
						String revision = null;
						String pegRevision = null;
						boolean isNewFormat = false;

						if (SVNUtility.isValidSVNURL(parts[parts.length - 1])) {
							isNewFormat = false;
							name = parts[0];

							url = parts[1];
							if (parts.length == 4) {
								revision = parts[2];
								url = parts[3];
							} else if (parts.length == 3) {
								revision = parts[1].substring(2);
								url = parts[2];
							}
						} else {
							isNewFormat = true;
							name = parts[parts.length - 1];

							url = parts[0];
							if (parts.length == 4) {
								revision = parts[1];
								url = parts[2];
							} else if (parts.length == 3) {
								revision = parts[0].substring(2);
								url = parts[1];
							}

							int idx = url.lastIndexOf('@');
							if (idx != -1) {
								pegRevision = url.substring(idx + 1);
								url = url.substring(0, idx);
							}
						}

						SVNExternalPropertyData data = new SVNExternalPropertyData(name, url, pegRevision, revision,
								isNewFormat);
						resList.add(data);
			}
			return resList.toArray(new SVNExternalPropertyData[0]);
		}

		@Override
		public String toString() {
			String localPath = this.localPath;
			if (localPath.contains(" ")) { //$NON-NLS-1$
				localPath = "\"" + localPath + "\""; //$NON-NLS-1$ //$NON-NLS-2$
			}
			StringBuilder res = new StringBuilder();
			if (isNewFormat) {
				//Example: -r12 http://svn.example.com/skin-maker@21 third-party/skins/toolkit
				if (revision != null) {
					res.append("-r").append(revision).append("\t"); //$NON-NLS-1$ //$NON-NLS-2$
				}
				res.append(url);
				if (pegRevision != null) {
					res.append("@").append(pegRevision); //$NON-NLS-1$
				}
				res.append("\t").append(localPath); //$NON-NLS-1$
			} else {
				//Example: third-party/skins -r148 http://svn.example.com/skinproj
				res.append(localPath).append("\t"); //$NON-NLS-1$
				if (revision != null) {
					res.append("-r").append(revision).append("\t"); //$NON-NLS-1$ //$NON-NLS-2$
				}
				res.append(url);
			}
			return res.toString();
		}
	}

	/**
	 * This method should be used instead of creating {@link Path} directly when you need to manipulate with URLs.
	 * 
	 * @param fullPath
	 * @return
	 */
	public static IPath createPathForSVNUrl(String fullPath) {
		return fullPath == null ? null : new PathForURL(fullPath, true);
	}

	private SVNUtility() {

	}
}
