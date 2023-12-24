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
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.core.svnstorage;

import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.internal.preferences.Base64;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.SVNTeamPlugin;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.ISVNCredentialsPrompt;
import org.eclipse.team.svn.core.connector.SVNDepth;
import org.eclipse.team.svn.core.connector.SVNEntryInfo;
import org.eclipse.team.svn.core.connector.SVNEntryRevisionReference;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.connector.ssl.SSLServerCertificateFailures;
import org.eclipse.team.svn.core.connector.ssl.SSLServerCertificateInfo;
import org.eclipse.team.svn.core.extension.CoreExtensionsManager;
import org.eclipse.team.svn.core.extension.options.IOptionProvider;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.ActivityCancelledException;
import org.eclipse.team.svn.core.operation.LoggedOperation;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.operation.UnreportableException;
import org.eclipse.team.svn.core.resource.IRepositoryContainer;
import org.eclipse.team.svn.core.resource.IRepositoryFile;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IRepositoryRoot;
import org.eclipse.team.svn.core.resource.IRevisionLink;
import org.eclipse.team.svn.core.resource.SSHSettings;
import org.eclipse.team.svn.core.resource.SSLSettings;
import org.eclipse.team.svn.core.resource.events.IRepositoryLocationStateListener;
import org.eclipse.team.svn.core.resource.events.ISSHSettingsStateListener;
import org.eclipse.team.svn.core.resource.events.ISSLSettingsStateListener;
import org.eclipse.team.svn.core.utility.ProgressMonitorUtility;
import org.eclipse.team.svn.core.utility.SVNURLStreamHandler;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * SVN based representation of IRepositoryLocation
 * 
 * @author Alexander Gurov
 */
public class SVNRepositoryLocation extends SVNRepositoryBase
		implements IRepositoryLocation, ISSHSettingsStateListener, ISSLSettingsStateListener, Serializable {
	private static final int PROXY_CACHE_SIZE = 5;

	private static final long serialVersionUID = -5820937379741639580L;

	public static final int DEFAULT_CONNECTION = 0;

	public static final int SSH_CONNECTION = 1;

	public static final int SSL_CONNECTION = 2;

	public static final int PROXY_CONNECTION = 3;

	protected String id;

	protected String label;

	protected String trunk;

	protected String branches;

	protected String tags;

	protected boolean trunkEnabled;

	protected String username;

	protected String repositoryRootUrl;

	protected String repositoryUUID;

	// Base64 encoded to hide in the serialized content
	protected String password;

	protected boolean passwordSaved;

	private transient String passwordTemporary;

	private List<byte[]> serializedRevisionLinks;

	private SSLSettings sslSettings;

	private SSHSettings sshSettings;

	private transient List<ISVNConnector> proxyCache;

	private transient HashSet<ISVNConnector> usedProxies;

	private transient HashMap<Thread, ProxyHolder> thread2Proxy;

	private transient IRevisionLink[] revisionLinks;

	protected transient boolean trustSiteDefined;

	protected transient ISVNCredentialsPrompt.Answer trustSite;

	protected transient int proxyConfigurationState;

	protected boolean authorNameEnabled;

	protected String authorName;

	private Map<String, IRepositoryLocation> additionalRealms;

	//used for differed in time realms initialization
	private transient List<String> rawRealms = new ArrayList<>();

	private transient List<IRepositoryLocationStateListener> changeListeners = new ArrayList<>();

	private transient Integer lazyInitLock = 0;

	private transient Integer proxyManagerLock = 0;

	private transient Integer repositoryRootLock = 0;

	private transient Integer authInitLock = 0;

	/*
	 * Used for differed in time authentication retrieving.
	 * We need it in order to avoid deadlocks in case of loading
	 * authentication info initiated from Plugin#start.
	 */
	protected transient boolean isRetrieveAuthInfo;

	public SVNRepositoryLocation() {
		super(null);
	}

	public SVNRepositoryLocation(String id) {
		super(null);
		this.id = id;
	}

	@Override
	public void addStateListener(IRepositoryLocationStateListener listener) {
		synchronized (changeListeners) {
			changeListeners.add(listener);
		}
	}

	@Override
	public void removeStateListener(IRepositoryLocationStateListener listener) {
		synchronized (changeListeners) {
			changeListeners.remove(listener);
		}
	}

	@Override
	public String asReference(LocationReferenceTypeEnum locationReferenceType) {
		String reference = id;
		reference += ";" + getUrlAsIs(); //$NON-NLS-1$
		if (locationReferenceType == LocationReferenceTypeEnum.ONLY_REQUIRED_DATA) {
			return reference;
		}
		reference += ";" + getLabel(); //$NON-NLS-1$
		reference += ";" + getBranchesLocation(); //$NON-NLS-1$
		reference += ";" + getTagsLocation(); //$NON-NLS-1$
		reference += ";" + getTrunkLocation(); //$NON-NLS-1$
		reference += ";" + trunkEnabled; //$NON-NLS-1$
		reference += ";" + (repositoryUUID == null ? "" : repositoryUUID); //$NON-NLS-1$ //$NON-NLS-2$
		reference += ";" + (repositoryRootUrl == null ? "" : repositoryRootUrl); //$NON-NLS-1$ //$NON-NLS-2$
		reference += ";" + getAuthorName(); //$NON-NLS-1$
		reference += ";" + authorNameEnabled + ";"; //$NON-NLS-1$ //$NON-NLS-2$
		String[] realms = getRealms().toArray(new String[0]);
		for (int i = 0; i < realms.length; i++) {
			if (i < realms.length - 1) {
				reference += realms[i] + "^"; //$NON-NLS-1$
			} else {
				reference += realms[i];
			}
		}
		reference += ";"; //$NON-NLS-1$

		IRevisionLink[] revisionLinks = getRevisionLinks();
		for (int i = 0; i < revisionLinks.length; i++) {
			String base64revLink = new String(Base64.encode(SVNRemoteStorage.instance()
					.revisionLinkAsBytes(revisionLinks[i],
							locationReferenceType != LocationReferenceTypeEnum.WITHOUT_REVISION_COMMENTS)));
			if (i < revisionLinks.length - 1) {
				reference += base64revLink + "^"; //$NON-NLS-1$
			} else {
				reference += base64revLink;
			}
		}

		//write this line only for compatibility issues, previously it contained ssh port
		reference += ";" + 0; //$NON-NLS-1$

		return reference;
	}

	@Override
	public void fillLocationFromReference(String[] referenceParts) {
		boolean containRevisionLinks = false;
		switch (referenceParts.length) {
			case 14:
				//check ssh port for compatibility issues
				int sshPort = Integer.parseInt(referenceParts[13]);
				if (sshPort != 0) {
					this.getSSHSettings().setPort(sshPort);
				}
			case 13:
				if (!referenceParts[12].equals("")) { //$NON-NLS-1$
					containRevisionLinks = true;
				}
			case 12:
				if (!referenceParts[11].equals("")) { //$NON-NLS-1$
					rawRealms.addAll(Arrays.asList(referenceParts[11].split("\\^"))); //$NON-NLS-1$
				}
			case 11:
				setAuthorNameEnabled(referenceParts[10].equals("true")); //$NON-NLS-1$
			case 10:
				setAuthorName(referenceParts[9].trim());
			case 9:
				repositoryRootUrl = referenceParts[8].trim().equals("") ? null : referenceParts[8].trim(); //$NON-NLS-1$
			case 8:
				repositoryUUID = referenceParts[7].trim().equals("") ? null : referenceParts[7].trim(); //$NON-NLS-1$
			case 7:
				setStructureEnabled(referenceParts[6].equals("true")); //$NON-NLS-1$
			case 6:
				setTrunkLocation(referenceParts[5].trim());
			case 5:
				setTagsLocation(referenceParts[4].trim());
			case 4:
				setBranchesLocation(referenceParts[3].trim());
			case 3:
				String label = referenceParts[2].trim();
				if (label.length() > 0) {
					setLabel(label);
				}
			case 2:
				setUrl(referenceParts[1].trim());
			case 1:
		}
		if (label == null || label.length() == 0) {
			label = url;
		}
		if (containRevisionLinks) {
			String[] revLinks = referenceParts[12].split("\\^"); //$NON-NLS-1$
			for (String revLink : revLinks) {
				addRevisionLink(
						SVNRemoteStorage.instance().revisionLinkFromBytes(Base64.decode(revLink.getBytes()), this));
			}
		}
		setRetrieveAuthInfo(true);
	}

	@Override
	public Collection<String> getRealms() {
		return this.getAdditionalRealms().keySet();
	}

	@Override
	public void addRealm(String realm, IRepositoryLocation location) {
		this.getAdditionalRealms(false).put(realm, location);
		fireRealmAdded(realm, location);
	}

	@Override
	public void removeRealm(String realm) {
		fireRealmRemoved(realm);
		this.getAdditionalRealms().remove(realm);
	}

	@Override
	public Collection<IRepositoryLocation> getRealmLocations() {
		return this.getAdditionalRealms().values();
	}

	@Override
	public IRepositoryLocation getLocationForRealm(String realm) {
		return this.getAdditionalRealms().get(realm);
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public String getName() {
		return getUrl();
	}

	@Override
	public String getUrlAsIs() {
		return super.getUrl();
	}

	@Override
	public String getUrl() {
		return getUrlImpl(super.getUrl());
	}

	@Override
	public String getLabel() {
		return label == null ? getUrl() : label;
	}

	@Override
	public String getRepositoryRootUrl() {
		this.fetchRepoInfo();
		return repositoryRootUrl == null ? getUrl() : repositoryRootUrl;
	}

	@Override
	public String getRepositoryUUID() {
		this.fetchRepoInfo();
		return repositoryUUID;
	}

	@Override
	public IRepositoryRoot getRepositoryRoot() {
		return new SVNRepositoryRoot(this);
	}

	@Override
	public IRepositoryRoot getRoot() {
		return new SVNRepositoryLocationRoot(this);
	}

	@Override
	public boolean isStructureEnabled() {
		return trunkEnabled;
	}

	@Override
	public void setStructureEnabled(boolean structureEnabled) {
		boolean oldValue = trunkEnabled;
		trunkEnabled = structureEnabled;
		fireChanged(IRepositoryLocationStateListener.STRUCTURE_ENABLED, Boolean.valueOf(oldValue),
				Boolean.valueOf(trunkEnabled));
	}

	@Override
	public String getUserInputTrunk() {
		return trunk == null ? "" : trunk; //$NON-NLS-1$
	}

	@Override
	public String getUserInputTags() {
		return tags == null ? "" : tags; //$NON-NLS-1$
	}

	@Override
	public String getUserInputBranches() {
		return branches == null ? "" : branches; //$NON-NLS-1$
	}

	@Override
	public String getTrunkLocation() {
		return trunk == null || !isStructureEnabled() ? "" : trunk; //$NON-NLS-1$
	}

	@Override
	public String getBranchesLocation() {
		return branches == null || !isStructureEnabled() ? "" : branches; //$NON-NLS-1$
	}

	@Override
	public String getTagsLocation() {
		return tags == null || !isStructureEnabled() ? "" : tags; //$NON-NLS-1$
	}

	@Override
	public boolean isAuthorNameEnabled() {
		return authorNameEnabled;
	}

	@Override
	public String getAuthorName() {
		return authorName == null ? "" : authorName; //$NON-NLS-1$
	}

	@Override
	public IRepositoryContainer asRepositoryContainer(String url, boolean allowsNull) {
		return SVNRepositoryLocation.asRepositoryContainer(this, url, allowsNull);
	}

	@Override
	public IRepositoryFile asRepositoryFile(String url, boolean allowsNull) {
		return SVNRepositoryLocation.asRepositoryFile(this, url, allowsNull);
	}

	public static IRepositoryContainer asRepositoryContainer(IRepositoryLocation location, String url,
			boolean allowsNull) {
		if (!SVNRepositoryLocation.isArgumentsCorrect(location, url, allowsNull)) {
			return null;
		}

		IPath urlPath = SVNUtility.createPathForSVNUrl(url);
		String name = urlPath.lastSegment();

		if (name != null && location.isStructureEnabled()) {
			boolean regularFolder = false;
			if (name.equals(location.getTrunkLocation()) || name.equals(location.getTagsLocation())
					|| name.equals(location.getBranchesLocation())) {
				IPath tPath = SVNUtility.createPathForSVNUrl(url);
				String tName = null;
				while ((tPath = tPath.removeLastSegments(1)) != null && !tPath.isEmpty()
						&& (tName = tPath.lastSegment()) != null) {
					if (tName.equals(location.getTrunkLocation()) || tName.equals(location.getBranchesLocation())
							|| tName.equals(location.getTagsLocation())) {
						regularFolder = true;
						break;
					}
				}
			}
			if (!regularFolder) {
				if (name.equals(location.getTrunkLocation())) {
					return new SVNRepositoryTrunk(location, url, SVNRevision.HEAD);
				}
				if (name.equals(location.getTagsLocation())) {
					return new SVNRepositoryTags(location, url, SVNRevision.HEAD);
				}
				if (name.equals(location.getBranchesLocation())) {
					return new SVNRepositoryBranches(location, url, SVNRevision.HEAD);
				}
			}
		}
		IPath locationUrl = SVNUtility.createPathForSVNUrl(location.getUrl());
		if (urlPath.equals(locationUrl)) {
			return location.getRoot();
		}
		if (locationUrl.isPrefixOf(urlPath)) {
			// do not access repository root if it is not required
			return new SVNRepositoryFolder(location, url, SVNRevision.HEAD);
		}
		if (urlPath.equals(SVNUtility.createPathForSVNUrl(location.getRepositoryRootUrl()))) {
			return location.getRepositoryRoot();
		}
		return new SVNRepositoryFolder(location, url, SVNRevision.HEAD);
	}

	public static IRepositoryFile asRepositoryFile(IRepositoryLocation location, String url, boolean allowsNull) {
		if (!SVNRepositoryLocation.isArgumentsCorrect(location, url, allowsNull)) {
			return null;
		}
		return new SVNRepositoryFile(location, url, SVNRevision.HEAD);
	}

	@Override
	public String getUsername() {
		checkAuthInfo();
		return username;
	}

	@Override
	public String getPassword() {
		checkAuthInfo();
		return passwordSaved ? SVNUtility.base64Decode(password) : SVNUtility.base64Decode(passwordTemporary);
	}

	@Override
	public boolean isPasswordSaved() {
		checkAuthInfo();
		return passwordSaved;
	}

	@Override
	public IRevisionLink[] getRevisionLinks() {
		synchronized (lazyInitLock) {
			if (revisionLinks == null) {
				List<byte[]> serialized = getSerializedRevisionLinks();
				revisionLinks = new IRevisionLink[serialized.size()];
				for (int i = 0; i < revisionLinks.length; i++) {
					byte[] data = serialized.get(i);
					revisionLinks[i] = SVNRemoteStorage.instance().revisionLinkFromBytes(data, this);
				}
			}
			return revisionLinks;
		}
	}

	@Override
	public void addRevisionLink(IRevisionLink link) {
		synchronized (lazyInitLock) {
			IRevisionLink[] links = getRevisionLinks();
			int idx = -1;
			for (int i = 0; i < links.length; i++) {
				if (links[i].equals(link) && links[i].getRepositoryResource()
						.getSelectedRevision()
						.equals(link.getRepositoryResource().getSelectedRevision())) {
					idx = i;
					break;
				}
			}
			if (idx == -1) {
				List<byte[]> serialized = getSerializedRevisionLinks();
				serialized.add(SVNRemoteStorage.instance().revisionLinkAsBytes(link, true));
				revisionLinks = null;
			}
		}
		fireRevisionLinkAdded(link);
	}

	@Override
	public void removeRevisionLink(IRevisionLink link) {
		fireRevisionLinkRemoved(link);
		synchronized (lazyInitLock) {
			IRevisionLink[] links = getRevisionLinks();
			int idx = -1;
			for (int i = 0; i < links.length; i++) {
				if (links[i].equals(link) && links[i].getRepositoryResource()
						.getSelectedRevision()
						.equals(link.getRepositoryResource().getSelectedRevision())) {
					idx = i;
					break;
				}
			}
			if (idx != -1) {
				List<byte[]> serialized = getSerializedRevisionLinks();
				serialized.remove(idx);
				revisionLinks = null;
			}
		}
	}

	@Override
	public void setLabel(String label) {
		String oldValue = this.label;
		this.label = label;
		fireChanged(IRepositoryLocationStateListener.LABEL, oldValue, this.label);
	}

	@Override
	public void setUrl(String url) {
		String oldValue = this.url;

		String oldRootUrl = getRepositoryRootUrl();
		IRevisionLink[] oldLinks = getRevisionLinks();
		List<byte[]> serialized = getSerializedRevisionLinks();

		this.url = url;

		if (oldRootUrl != null
				&& !SVNUtility.createPathForSVNUrl(oldRootUrl).isPrefixOf(SVNUtility.createPathForSVNUrl(getUrl()))) {
			repositoryRootUrl = null;
			repositoryUUID = null;

			if (oldLinks.length > 0) {
				String newRootUrl = getRepositoryRootUrl();
				synchronized (lazyInitLock) {
					for (int i = 0; i < oldLinks.length; i++) {
						String linkUrl = oldLinks[i].getRepositoryResource().getUrl();
						int idx = linkUrl.indexOf(oldRootUrl);
						if (idx == -1) {
							serialized.set(i, null);
						} else {
							linkUrl = newRootUrl + linkUrl.substring(idx + oldRootUrl.length());
							IRepositoryResource tmpResource = oldLinks[i] instanceof IRepositoryFile
									? (IRepositoryResource) this.asRepositoryFile(linkUrl, false)
									: this.asRepositoryContainer(linkUrl, false);
							IRevisionLink link = SVNUtility.createRevisionLink(tmpResource);
							link.getRepositoryResource()
									.setPegRevision(oldLinks[i].getRepositoryResource().getPegRevision());
							link.getRepositoryResource()
									.setSelectedRevision(oldLinks[i].getRepositoryResource().getSelectedRevision());
							link.setComment(oldLinks[i].getComment());
							serialized.set(i, SVNRemoteStorage.instance().revisionLinkAsBytes(link, true));
						}
					}
					for (Iterator<byte[]> it = serialized.iterator(); it.hasNext();) {
						if (it.next() == null) {
							it.remove();
						}
					}
					revisionLinks = null;
				}
			}
		}
		fireChanged(IRepositoryLocationStateListener.URL, oldValue, this.url);
	}

	@Override
	public void setTrunkLocation(String location) {
		String oldValue = trunk;
		trunk = location;
		fireChanged(IRepositoryLocationStateListener.TRUNK_LOCATION, oldValue, trunk);
	}

	@Override
	public void setBranchesLocation(String location) {
		String oldValue = branches;
		branches = location;
		fireChanged(IRepositoryLocationStateListener.BRANCHES_LOCATION, oldValue, branches);
	}

	@Override
	public void setTagsLocation(String location) {
		String oldValue = tags;
		tags = location;
		fireChanged(IRepositoryLocationStateListener.TAGS_LOCATION, oldValue, tags);
	}

	@Override
	public void setUsername(String username) {
		String oldValue = this.username;
		this.username = username;
		fireChanged(IRepositoryLocationStateListener.USERNAME, oldValue, this.username);
	}

	@Override
	public void setAuthorNameEnabled(boolean isEnabled) {
		boolean oldValue = authorNameEnabled;
		authorNameEnabled = isEnabled;
		fireChanged(IRepositoryLocationStateListener.AUTHOR_NAME_ENABLED, Boolean.valueOf(oldValue),
				Boolean.valueOf(authorNameEnabled));
	}

	@Override
	public void setAuthorName(String name) {
		String oldValue = authorName;
		authorName = name;
		fireChanged(IRepositoryLocationStateListener.AUTHOR_NAME, oldValue, authorName);
	}

	@Override
	public void setPassword(String password) {
		String oldValue = !passwordSaved ? passwordTemporary : this.password;
		oldValue = oldValue != null ? SVNUtility.base64Decode(oldValue) : oldValue;
		if (!passwordSaved) {
			passwordTemporary = SVNUtility.base64Encode(password);
		} else {
			this.password = SVNUtility.base64Encode(password);
		}
		fireChanged(IRepositoryLocationStateListener.PASSWORD, oldValue, password);
	}

	@Override
	public void setPasswordSaved(boolean saved) {
		if (passwordSaved == saved) {
			return;
		}
		boolean oldValue = passwordSaved;
		passwordSaved = saved;
		if (!saved) {
			passwordTemporary = password;
			password = null;
		} else {
			password = passwordTemporary;
		}
		fireChanged(IRepositoryLocationStateListener.PASSWORD_SAVED, Boolean.valueOf(oldValue),
				Boolean.valueOf(passwordSaved));
	}

	@Override
	public ISVNConnector acquireSVNProxy() {
		ISVNConnector retVal = null;
		boolean isNew = false;
		synchronized (proxyManagerLock) {
			try {
				// initialize proxy cache, usedProxies list and thread2Proxy map
				List<ISVNConnector> cache = getProxyCache();

				// make the method reenterable: the same thread must use the same proxy. Access from call-backs must be controlled by programmer
				Thread current = Thread.currentThread();
				ProxyHolder holder = thread2Proxy.get(current);
				if (holder != null) {
					holder.referenceCounter++;
					return holder.proxy;
				}

				if (proxyConfigurationState == 1) {
					try {
						proxyManagerLock.wait();
					} catch (InterruptedException ex) {
					}
					if (proxyConfigurationState != 2) {
						throw new ActivityCancelledException(
								SVNMessages.getErrorString("Error_AuthenticationCancelled")); //$NON-NLS-1$
					}
				} else if (proxyConfigurationState == 0) {
					proxyConfigurationState = 1;
				}

				if (cache.size() == 0) {
					retVal = newProxyInstance();
					isNew = true;
				} else {
					retVal = cache.remove(0);
				}

				usedProxies.add(retVal);
				thread2Proxy.put(current, new ProxyHolder(retVal));
			} catch (RuntimeException e) {
				proxyConfigurationState = 0;
				proxyManagerLock.notifyAll();
				throw e;
			} catch (Throwable e) {
				proxyConfigurationState = 0;
				proxyManagerLock.notifyAll();
				throw new RuntimeException(e);
			}
		}
		if (isNew) { // configure a new proxy later in order to avoid recursive deadlocks when there is a misconfiguration of some sort
			SVNUtility.configureProxy(retVal, this);
		}
		fireProxyAcquired(retVal);
		return retVal;
	}

	@Override
	public void releaseSVNProxy(ISVNConnector proxy) {
		fireProxyDisposed(proxy);
		synchronized (proxyManagerLock) {
			List<ISVNConnector> proxies = getProxyCache();

			Thread current = Thread.currentThread();
			ProxyHolder holder = thread2Proxy.get(current);

			if (--holder.referenceCounter > 0) {
				return;
			}

			thread2Proxy.remove(current);
			// Proxy should be always removed from the usedProxies list. So, do it first.
			if (!usedProxies.remove(proxy) || proxies.size() >= SVNRepositoryLocation.PROXY_CACHE_SIZE) {
				// The function code is sensitive to exceptions. So, disallow error reporting in that case.
				try {
					proxy.dispose();
				} catch (Throwable ex) {
				}
			} else {
				proxies.add(proxy);
			}

			if (proxyConfigurationState == 1) {
				proxyConfigurationState = 2;
			}
			proxyManagerLock.notifyAll();
		}
	}

	@Override
	public void reconfigure() {
		synchronized (proxyManagerLock) {
			proxyConfigurationState = 0;
			reconfigureImpl();
		}
	}

	@Override
	public void dispose() {
		synchronized (proxyManagerLock) {
			reconfigureProxies(proxy -> {
				// When exiting Eclipse IDE connector plug-in's can be stopped before Core. So, disallow error reporting in that case.
				try {
					proxy.dispose();
				} catch (Throwable ex) {
				}
			});
			getProxyCache().clear();
		}
	}

	@Override
	public SSLSettings getSSLSettings() {
		return this.getSSLSettings(true);
	}

	public SSLSettings getSSLSettings(boolean isCheckAuthInfo) {
		if (isCheckAuthInfo) {
			checkAuthInfo();
		}
		synchronized (lazyInitLock) {
			if (sslSettings == null) {
				sslSettings = new SSLSettings();
			}
			return sslSettings;
		}
	}

	public SSHSettings getSSHSettings(boolean isCheckAuthInfo) {
		if (isCheckAuthInfo) {
			checkAuthInfo();
		}
		synchronized (lazyInitLock) {
			if (sshSettings == null) {
				sshSettings = new SSHSettings(this);
			}
			return sshSettings;
		}
	}

	@Override
	public SSHSettings getSSHSettings() {
		return this.getSSHSettings(true);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj != null && obj instanceof IRepositoryLocation) {
			IRepositoryLocation other = (IRepositoryLocation) obj;
			return getId().equals(other.getId());
		}
		return false;
	}

	protected static boolean isArgumentsCorrect(IRepositoryLocation location, String url, boolean allowsNull)
			throws IllegalArgumentException {
		if (url == null) {
			throw new IllegalArgumentException(SVNMessages.getErrorString("Error_NullURL")); //$NON-NLS-1$
		}
		IPath repoPath = SVNUtility.createPathForSVNUrl(location.getUrl());
		IPath urlPath = SVNUtility.createPathForSVNUrl(url);
		// do not access repository root URL if it is not required
		if (!repoPath.isPrefixOf(urlPath)) {
			IPath rootPath = SVNUtility.createPathForSVNUrl(location.getRepositoryRootUrl());
			if (!rootPath.isPrefixOf(urlPath)) {
				if (!allowsNull) {
					if (!urlPath.isPrefixOf(rootPath)) {
						String message = SVNMessages.formatErrorString("Error_NotRelatedURL", //$NON-NLS-1$
								new String[] { url, rootPath.toString() });
						throw new IllegalArgumentException(message);
					}
					String message = SVNMessages.formatErrorString("Error_ShorterURL", //$NON-NLS-1$
							new String[] { url, rootPath.toString() });
					throw new UnreportableException(message);
				}
				return false;
			}
		}
		return true;
	}

	protected void fetchRepoInfo() {
		synchronized (repositoryRootLock) {
			if (repositoryRootUrl == null && url != null && SVNUtility.isValidSVNURL(getUrl())) {
				String[] values = SVNRepositoryLocation.fetchRepoInfo(this, false);
				repositoryRootUrl = values[0];
				repositoryUUID = values[1];
				if (repositoryUUID != null) {
					SVNTeamPlugin.instance().setLocationsDirty(true);
				}
			}
		}
	}

	public static String[] fetchRepoInfo(final IRepositoryLocation location, final boolean usePrompt) {
		// completely disallow error reporting
		final String[] retVal = new String[2];
		ProgressMonitorUtility
				.doTaskExternal(new AbstractActionOperation("Operation_FetchRepositoryRoot", SVNMessages.class) { //$NON-NLS-1$
					@Override
					protected void runImpl(IProgressMonitor monitor) throws Exception {
						ISVNConnector proxy = CoreExtensionsManager.instance()
								.getSVNConnectorFactory()
								.createConnector();
						SVNUtility.configureProxy(proxy, location);

						if (usePrompt) {
							IOptionProvider optionProvider = SVNTeamPlugin.instance().getOptionProvider();
							ISVNCredentialsPrompt externalPrompt = optionProvider.getCredentialsPrompt();
							if (externalPrompt != null) {
								proxy.setPrompt(new BaseCredentialsPromptWrapper(externalPrompt, location));
							}
						} else {
							proxy.setPrompt(new BaseCredentialsPromptWrapper(null, location));
						}

						SVNEntryInfo[] infos = null;
						String url = location.getUrl();
						try {
							infos = SVNUtility.info(proxy,
									new SVNEntryRevisionReference(SVNUtility.encodeURL(url), SVNRevision.HEAD,
											SVNRevision.HEAD),
									SVNDepth.EMPTY, new SVNProgressMonitor(this, monitor, null));
						} finally {
							proxy.dispose();
						}
						if (infos != null && infos.length > 0 && infos[0] != null) {
							retVal[0] = SVNUtility.decodeURL(infos[0].reposRootUrl);
							if (!SVNUtility.createPathForSVNUrl(retVal[0])
									.isPrefixOf(SVNUtility.createPathForSVNUrl(url))) {
								// different host name could be returned by server side
								SVNURLStreamHandler userUrl = SVNUtility.getSVNUrlStreamHandler(url);
								SVNURLStreamHandler returnedURL = SVNUtility.getSVNUrlStreamHandler(retVal[0]);
								returnedURL.setHost(userUrl.getURL().getHost());
								retVal[0] = returnedURL.getURL().toExternalForm();
							}
							retVal[1] = infos[0].reposUUID;
						}
					}
				}, new NullProgressMonitor(), operation -> operation);
		return retVal;
	}

	protected void reconfigureImpl() {
		reconfigureProxies(proxy -> SVNUtility.configureProxy(proxy, SVNRepositoryLocation.this));
	}

	protected void reconfigureProxies(IProxyVisitor visitor) {
		visitProxies(visitor);
		usedProxies.clear();
	}

	protected void visitProxies(IProxyVisitor visitor) {
		for (ISVNConnector proxy : getProxyCache()) {
			visitor.visit(proxy);
		}
	}

	protected List<ISVNConnector> getProxyCache() {
		if (proxyCache == null) {
			proxyCache = new ArrayList<>();
			usedProxies = new HashSet<>();
			thread2Proxy = new HashMap<>();
		}
		return proxyCache;
	}

	protected ISVNConnector newProxyInstance() {
		IOptionProvider optionProvider = SVNTeamPlugin.instance().getOptionProvider();
		ISVNConnector proxy = CoreExtensionsManager.instance().getSVNConnectorFactory().createConnector();

		ISVNCredentialsPrompt externalPrompt = optionProvider.getCredentialsPrompt();
		if (externalPrompt != null) {
			proxy.setPrompt(new CredentialsPromptWrapper(externalPrompt));
		}

		return proxy;
	}

	protected List<byte[]> getSerializedRevisionLinks() {
		if (serializedRevisionLinks == null) {
			serializedRevisionLinks = new ArrayList<>();
		}
		return serializedRevisionLinks;
	}

	protected String getUrlImpl(String url) {
		if (url == null) {
			return null;
		}
		try {
			url = SVNUtility.decodeURL(url);
		} catch (IllegalArgumentException ex) {
			// the URL is not encoded
		}
		return SVNUtility.normalizeURL(url);
	}

	protected Map<String, IRepositoryLocation> getAdditionalRealms() {
		return this.getAdditionalRealms(true);
	}

	protected Map<String, IRepositoryLocation> getAdditionalRealms(boolean isCheckAuthInfo) {
		if (isCheckAuthInfo) {
			checkAuthInfo();
		}
		synchronized (this) {
			if (additionalRealms == null) {
				additionalRealms = new LinkedHashMap<>();
			}
		}
		return additionalRealms;
	}

	protected void fireChanged(String field, Object oldValue, Object newValue) {
		for (IRepositoryLocationStateListener listener : getStateListeners()) {
			listener.changed(this, field, oldValue, newValue);
		}
	}

	@Override
	public void sslChanged(IRepositoryLocation where, String field, Object oldValue, Object newValue) {
		for (IRepositoryLocationStateListener listener : getStateListeners()) {
			listener.sslChanged(this, field, oldValue, newValue);
		}
	}

	@Override
	public void sshChanged(IRepositoryLocation where, String field, Object oldValue, Object newValue) {
		for (IRepositoryLocationStateListener listener : getStateListeners()) {
			listener.sshChanged(this, field, oldValue, newValue);
		}
	}

	protected void fireRealmAdded(String realm, IRepositoryLocation location) {
		for (IRepositoryLocationStateListener listener : getStateListeners()) {
			listener.realmAdded(this, realm, location);
		}
	}

	protected void fireRealmRemoved(String realm) {
		for (IRepositoryLocationStateListener listener : getStateListeners()) {
			listener.realmRemoved(this, realm);
		}
	}

	protected void fireRevisionLinkAdded(IRevisionLink link) {
		for (IRepositoryLocationStateListener listener : getStateListeners()) {
			listener.revisionLinkAdded(this, link);
		}
	}

	protected void fireRevisionLinkRemoved(IRevisionLink link) {
		for (IRepositoryLocationStateListener listener : getStateListeners()) {
			listener.revisionLinkRemoved(this, link);
		}
	}

	protected void fireProxyAcquired(ISVNConnector proxy) {
		for (IRepositoryLocationStateListener listener : getStateListeners()) {
			listener.proxyAcquired(this, proxy);
		}
	}

	protected void fireProxyDisposed(ISVNConnector proxy) {
		for (IRepositoryLocationStateListener listener : getStateListeners()) {
			listener.proxyDisposed(this, proxy);
		}
	}

	protected IRepositoryLocationStateListener[] getStateListeners() {
		synchronized (changeListeners) {
			return changeListeners.toArray(new IRepositoryLocationStateListener[changeListeners.size()]);
		}
	}

	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
	}

	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		lazyInitLock = 0;
		proxyManagerLock = 0;
		repositoryRootLock = 0;
	}

	protected interface IProxyVisitor {
		void visit(ISVNConnector proxy);
	}

	public static class BaseCredentialsPromptWrapper implements ISVNCredentialsPrompt {
		protected ISVNCredentialsPrompt prompt;

		protected String tryRealm;

		protected String threadName;

		protected IRepositoryLocation realmLocation;

		protected IRepositoryLocation location;

		public BaseCredentialsPromptWrapper(ISVNCredentialsPrompt prompt, IRepositoryLocation location) {
			this.prompt = prompt == null ? ISVNCredentialsPrompt.DEFAULT_PROMPT : prompt;
			this.location = location;
		}

		@Override
		public Answer askTrustSSLServer(Object context, SSLServerCertificateFailures failures,
				SSLServerCertificateInfo info, boolean allowPermanently) {
			return prompt.askTrustSSLServer(location, failures, info, allowPermanently);
		}

		@Override
		public String getRealmToSave() {
			return prompt.getRealmToSave();
		}

		@Override
		public String getProxyHost() {
			return prompt.getProxyHost();
		}

		@Override
		public String getProxyPassword() {
			return prompt.getProxyPassword();
		}

		@Override
		public int getProxyPort() {
			return prompt.getProxyPort();
		}

		@Override
		public String getProxyUserName() {
			return prompt.getProxyUserName();
		}

		@Override
		public int getSSHPort() {
			return realmLocation != null ? realmLocation.getSSHSettings().getPort() : prompt.getSSHPort();
		}

		@Override
		public String getSSHPrivateKeyPassphrase() {
			return realmLocation != null
					? realmLocation.getSSHSettings().getPassPhrase()
					: prompt.getSSHPrivateKeyPassphrase();
		}

		@Override
		public String getSSHPrivateKeyPath() {
			return realmLocation != null
					? realmLocation.getSSHSettings().getPrivateKeyPath()
					: prompt.getSSHPrivateKeyPath();
		}

		@Override
		public String getSSLClientCertPassword() {
			return realmLocation != null
					? realmLocation.getSSLSettings().getPassPhrase()
					: prompt.getSSLClientCertPassword();
		}

		@Override
		public String getSSLClientCertPath() {
			return realmLocation != null
					? realmLocation.getSSLSettings().getCertificatePath()
					: prompt.getSSLClientCertPath();
		}

		@Override
		public String getUsername() {
			return realmLocation != null ? realmLocation.getUsername() : prompt.getUsername();
		}

		@Override
		public String getPassword() {
			return realmLocation != null ? realmLocation.getPassword() : prompt.getPassword();
		}

		@Override
		public boolean isProxyAuthenticationEnabled() {
			return prompt.isProxyAuthenticationEnabled();
		}

		@Override
		public boolean isProxyEnabled() {
			return prompt.isProxyEnabled();
		}

		@Override
		public boolean isSSHPrivateKeyPassphraseSaved() {
			return prompt.isSSHPrivateKeyPassphraseSaved();
		}

		@Override
		public boolean isSSHPublicKeySelected() {
			return prompt.isSSHPublicKeySelected();
		}

		@Override
		public boolean isSSLAuthenticationEnabled() {
			return prompt.isSSLAuthenticationEnabled();
		}

		@Override
		public boolean isSSLSavePassphrase() {
			return prompt.isSSLSavePassphrase();
		}

		@Override
		public boolean isSaveCredentialsEnabled() {
			return prompt.isSaveCredentialsEnabled();
		}

		@Override
		public boolean isSaveProxyPassword() {
			return prompt.isSaveProxyPassword();
		}

		@Override
		public boolean promptProxy(Object context) {
			boolean retVal = prompt.promptProxy(location);
			checkForSave(retVal, SVNRepositoryLocation.PROXY_CONNECTION);
			return retVal;
		}

		@Override
		public boolean prompt(Object context, String realm) {
			if (tryCachedRealm(realm)) {
				return true;
			}
			boolean retVal = prompt.prompt(location, realm);
			checkForSave(retVal, SVNRepositoryLocation.DEFAULT_CONNECTION);
			return retVal;
		}

		@Override
		public boolean promptSSH(Object context, String realm) {
			if (tryCachedRealm(realm)) {
				return true;
			}
			boolean retVal = prompt.promptSSH(location, realm);
			checkForSave(retVal, SVNRepositoryLocation.SSH_CONNECTION);
			return retVal;
		}

		@Override
		public boolean promptSSL(Object context, String realm) {
			if (tryCachedRealm(realm)) {
				return true;
			}
			boolean retVal = prompt.promptSSL(location, realm);
			checkForSave(retVal, SVNRepositoryLocation.SSL_CONNECTION);
			return retVal;
		}

		protected boolean tryCachedRealm(String realm) {
			/*
			 * There shouldn't be any checks on emptiness of the basic authentication password, since there might be other than basic authentication access method used (SSH key-file, for example).
			 */
			String threadName = Thread.currentThread().getName();
			if (tryRealm == null || !tryRealm.equals(realm) || !threadName.equals(this.threadName)) {
				realmLocation = location.getLocationForRealm(realm);
				if (realmLocation != null && realmLocation.getUsername() != null
						&& !realmLocation.getUsername().equals("")) {
					tryRealm = realm;
					this.threadName = threadName;
					return true;
				}
				if (realmLocation == null
						&& CoreExtensionsManager.instance().getSVNConnectorFactory().getId().indexOf("svnkit") != -1) {
					String protocol = "file"; //$NON-NLS-1$
					try {
						URL url = SVNUtility.getSVNUrl(location.getUrl());
						protocol = url.getProtocol();
						if (protocol.equals("file")) { //$NON-NLS-1$
							realmLocation = location;
							tryRealm = realm;
							this.threadName = threadName;
							return true;
						}
					} catch (MalformedURLException ex) {
						//skip
					}
				}
			} else {
				tryRealm = null;
				realmLocation = null;
				this.threadName = null;
			}
			return false;
		}

		protected void checkForSave(boolean retVal, int connectionType) {
			if (retVal) {
				IRepositoryLocation location = this.location;
				String realmToSave = getRealmToSave();
				if (!ISVNCredentialsPrompt.ROOT_LOCATION.equals(realmToSave)) {
					location = this.location.getLocationForRealm(realmToSave);
					if (location == null) {
						location = SVNRemoteStorage.instance().newRepositoryLocation();
						SVNRemoteStorage.instance().copyRepositoryLocation(location, this.location);
						this.location.addRealm(realmToSave, location);
					}
				}
				checkForSaveImpl(location, retVal, connectionType);
			}
		}

		protected void checkForSaveImpl(IRepositoryLocation location, boolean retVal, int connectionType) {
			location.setUsername(prompt.getUsername());
			location.setPassword(prompt.getPassword());
			location.setPasswordSaved(prompt.isSaveCredentialsEnabled());
			SVNTeamPlugin.instance().setLocationsDirty(true);

			if (connectionType == SVNRepositoryLocation.SSH_CONNECTION) {
				SSHSettings settings = location.getSSHSettings();
				settings.setPort(prompt.getSSHPort());
				settings.setUseKeyFile(prompt.isSSHPublicKeySelected());
				if (settings.isUseKeyFile()) {
					settings.setPrivateKeyPath(prompt.getSSHPrivateKeyPath());
					settings.setPassPhraseSaved(prompt.isSSHPrivateKeyPassphraseSaved());
					settings.setPassPhrase(prompt.getSSHPrivateKeyPassphrase());
				}
			}
			if (connectionType == SVNRepositoryLocation.SSL_CONNECTION) {
				SSLSettings settings = location.getSSLSettings();
				settings.setAuthenticationEnabled(prompt.isSSLAuthenticationEnabled());
				if (settings.isAuthenticationEnabled()) {
					settings.setCertificatePath(prompt.getSSLClientCertPath());
					settings.setPassPhrase(prompt.getSSLClientCertPassword());
					settings.setPassPhraseSaved(prompt.isSSLSavePassphrase());
				}
			}
		}

	}

	protected class CredentialsPromptWrapper extends BaseCredentialsPromptWrapper {
		public CredentialsPromptWrapper(ISVNCredentialsPrompt prompt) {
			super(prompt, SVNRepositoryLocation.this);
		}

		@Override
		public Answer askTrustSSLServer(Object context, SSLServerCertificateFailures failures,
				SSLServerCertificateInfo info, boolean allowPermanently) {
			if (!trustSiteDefined) {
				trustSite = super.askTrustSSLServer(SVNRepositoryLocation.this, failures, info, allowPermanently);
				if (trustSite != ISVNCredentialsPrompt.Answer.REJECT) {
					trustSiteDefined = true;
				} else {
					proxyConfigurationState = 0;
				}
				return trustSite;
			}
			return trustSite;
		}

		@Override
		protected void checkForSave(boolean retVal, int connectionType) {
			synchronized (proxyManagerLock) {
				if (retVal) {
					super.checkForSave(retVal, connectionType);
				} else {
					proxyConfigurationState = 0;
				}
				reconfigureImpl();
			}
		}

	}

	protected static class ProxyHolder {
		public final ISVNConnector proxy;

		public int referenceCounter;

		public ProxyHolder(ISVNConnector proxy) {
			this.proxy = proxy;
			referenceCounter = 1;
		}
	}

	@Override
	public boolean isPasswordSavedForRealm(String realm) {
		IRepositoryLocation locationForRealm = this.getAdditionalRealms().get(realm);
		if (locationForRealm != null && locationForRealm.isPasswordSaved()) {
			return true;
		}
		return false;
	}

	protected void setRetrieveAuthInfo(boolean isRetrieveAuthInfo) {
		synchronized (authInitLock) {
			this.isRetrieveAuthInfo = isRetrieveAuthInfo;
		}
	}

	private void checkAuthInfo() {
		synchronized (authInitLock) {
			if (isRetrieveAuthInfo) {
				try {
					SVNRemoteStorage.instance().loadAuthInfo(this, ""); //$NON-NLS-1$
					for (String realm : rawRealms) {
						SVNRemoteStorage.instance().loadAuthInfo(this, realm);
					}
				} catch (Exception ex) {
					LoggedOperation.reportError("fillLocationFromReference", ex); //$NON-NLS-1$
				} finally {
					isRetrieveAuthInfo = false;
				}
			}
		}
	}

}
