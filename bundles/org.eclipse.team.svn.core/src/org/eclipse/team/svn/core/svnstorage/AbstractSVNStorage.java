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
 *    Alexander Gurov (Polarion Software) - initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.core.svnstorage;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.eclipse.core.internal.preferences.Base64;
import org.eclipse.core.internal.utils.UniversalUniqueIdentifier;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.equinox.security.storage.EncodingUtils;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.SVNTeamPlugin;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.connector.SVNRevision.Kind;
import org.eclipse.team.svn.core.operation.LoggedOperation;
import org.eclipse.team.svn.core.resource.IRepositoryContainer;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryLocation.LocationReferenceTypeEnum;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IRepositoryRoot;
import org.eclipse.team.svn.core.resource.IRevisionLink;
import org.eclipse.team.svn.core.resource.ISVNStorage;
import org.eclipse.team.svn.core.resource.SSHSettings;
import org.eclipse.team.svn.core.resource.SSLSettings;
import org.eclipse.team.svn.core.svnstorage.events.IRepositoriesStateChangedListener;
import org.eclipse.team.svn.core.svnstorage.events.IRevisionPropertyChangeListener;
import org.eclipse.team.svn.core.svnstorage.events.RepositoriesStateChangedEvent;
import org.eclipse.team.svn.core.svnstorage.events.RevisonPropertyChangeEvent;
import org.eclipse.team.svn.core.utility.SVNUtility;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Basic IRemoteStorage implementation
 * 
 * @author Alexander Gurov
 */
public abstract class AbstractSVNStorage implements ISVNStorage {

	/**
	 * Top secure preferences node to cache SVN information
	 */
	private static final String SVN_SECURE_NAME_SEGMENT = "/SVN/"; //$NON-NLS-1$

	protected static final String IPREF_STATE_INFO_FILE = "internal.stateInfoFile";

	protected static final String IPREF_REPO_NODE_NAME = "internal.repoNodeName";

	protected static final String IPREF_AUTH_NODE_NAME = "internal.authNodeName";

	protected File stateInfoFile;

	protected String repositoriesPreferencesNode;

	protected IPreferenceChangeListener repoPrefChangeListener;

	protected SVNCachedProxyCredentialsManager proxyCredentialsManager;

	protected IRepositoryLocation[] repositories;

	protected List<IRepositoriesStateChangedListener> repositoriesStateChangedListeners;

	protected ArrayList<IRevisionPropertyChangeListener> revPropChangeListeners;

	protected String migrateFromAuthDBPreferenceNode;

	protected boolean noStoredAuthentication;

	protected class RepositoryPreferenceChangeListener implements IPreferenceChangeListener {
		@Override
		public void preferenceChange(PreferenceChangeEvent event) {
			HashSet<IRepositoryLocation> readLocations = new HashSet<>(
					Arrays.asList(repositories));
			IRepositoryLocation location = AbstractSVNStorage.this.newRepositoryLocation((String) event.getNewValue());
			readLocations.add(location);
			repositories = readLocations.toArray(new IRepositoryLocation[readLocations.size()]);
			try {
				((IEclipsePreferences) event.getSource()).flush();
			} catch (BackingStoreException e) {
				LoggedOperation.reportError("preferenceChange", e); //$NON-NLS-1$
			}
			fireRepositoriesStateChanged(
					new RepositoriesStateChangedEvent(location, RepositoriesStateChangedEvent.ADDED));
		}
	}

	public AbstractSVNStorage() {
		repositories = new IRepositoryLocation[0];
		repositoriesStateChangedListeners = new ArrayList<>();
		revPropChangeListeners = new ArrayList<>();
	}

	@Override
	public void dispose() {
		// synchronization is innecessary
		IRepositoryLocation[] locations = repositories;
		if (locations != null) {
			for (IRepositoryLocation location : locations) {
				location.dispose();
			}
		}
	}

	@Override
	public void reconfigureLocations() {
		// synchronization is innecessary
		IRepositoryLocation[] locations = repositories;
		if (locations != null) {
			for (IRepositoryLocation location : locations) {
				location.reconfigure();
			}
		}
	}

	public void addRepositoriesStateChangedListener(IRepositoriesStateChangedListener listener) {
		synchronized (repositoriesStateChangedListeners) {
			repositoriesStateChangedListeners.add(listener);
		}
	}

	public void removeRepositoriesStateChangedListener(IRepositoriesStateChangedListener listener) {
		synchronized (repositoriesStateChangedListeners) {
			repositoriesStateChangedListeners.remove(listener);
		}
	}

	public void fireRepositoriesStateChanged(RepositoriesStateChangedEvent event) {
		synchronized (repositoriesStateChangedListeners) {
			for (IRepositoriesStateChangedListener listener : repositoriesStateChangedListeners) {
				listener.repositoriesStateChanged(event);
			}
		}
	}

	public void addRevisionPropertyChangeListener(IRevisionPropertyChangeListener listener) {
		synchronized (revPropChangeListeners) {
			revPropChangeListeners.add(listener);
		}
	}

	public void fireRevisionPropertyChangeEvent(RevisonPropertyChangeEvent event) {
		synchronized (revPropChangeListeners) {
			for (IRevisionPropertyChangeListener current : revPropChangeListeners) {
				current.revisionPropertyChanged(event);
			}
		}
	}

	public void removeRevisionPropertyChangeListener(IRevisionPropertyChangeListener listener) {
		synchronized (revPropChangeListeners) {
			revPropChangeListeners.remove(listener);
		}
	}

	/**
	 * Return the preferences node whose child nodes are the repositories, stored in preferences.
	 * 
	 * @return preferences node
	 */
	public static IEclipsePreferences getRepositoriesPreferences(String prefNode) {
		return (IEclipsePreferences) SVNTeamPlugin.instance().getPreferences().node(prefNode);
	}

	@Override
	public IRepositoryLocation[] getRepositoryLocations() {
		return repositories;
	}

	@Override
	public IRepositoryLocation getRepositoryLocation(String id) {
		for (IRepositoryLocation element : repositories) {
			if (element.getId().equals(id)) {
				return element;
			}
		}
		return null;
	}

	@Override
	public IRepositoryLocation newRepositoryLocation() {
		return new SVNRepositoryLocation(new UniversalUniqueIdentifier().toString());
	}

	@Override
	public void copyRepositoryLocation(IRepositoryLocation to, IRepositoryLocation from) {
		to.setStructureEnabled(from.isStructureEnabled());
		to.setBranchesLocation(from.getUserInputBranches());
		to.setTagsLocation(from.getUserInputTags());
		to.setTrunkLocation(from.getUserInputTrunk());

		to.setUrl(from.getUrlAsIs());
		to.setLabel(from.getLabel());

		to.setUsername(from.getUsername());
		to.setPassword(from.getPassword());
		to.setPasswordSaved(from.isPasswordSaved());

		SSHSettings sshOriginal = from.getSSHSettings();
		SSHSettings sshNew = to.getSSHSettings();
		sshNew.setPassPhrase(sshOriginal.getPassPhrase());
		sshNew.setPassPhraseSaved(sshOriginal.isPassPhraseSaved());
		sshNew.setPort(sshOriginal.getPort());
		sshNew.setPrivateKeyPath(sshOriginal.getPrivateKeyPath());
		sshNew.setUseKeyFile(sshOriginal.isUseKeyFile());

		SSLSettings sslOriginal = from.getSSLSettings();
		SSLSettings sslNew = to.getSSLSettings();
		sslNew.setAuthenticationEnabled(sslOriginal.isAuthenticationEnabled());
		sslNew.setCertificatePath(sslOriginal.getCertificatePath());
		sslNew.setPassPhrase(sslOriginal.getPassPhrase());
		sslNew.setPassPhraseSaved(sslOriginal.isPassPhraseSaved());

		if (from instanceof SVNRepositoryLocation && to instanceof SVNRepositoryLocation) {
			SVNRepositoryLocation tmpFrom = (SVNRepositoryLocation) from;
			SVNRepositoryLocation tmpTo = (SVNRepositoryLocation) to;
			tmpTo.repositoryRootUrl = tmpFrom.repositoryRootUrl;
			tmpTo.repositoryUUID = tmpFrom.repositoryUUID;

			tmpTo.getAdditionalRealms().clear();
			for (String realm : from.getRealms()) {
				IRepositoryLocation target = this.newRepositoryLocation();
				IRepositoryLocation source = from.getLocationForRealm(realm);
				copyRepositoryLocation(target, source);
				to.addRealm(realm, target);
			}
		}
	}

	@Override
	public IRepositoryLocation newRepositoryLocation(String reference) {
		if (reference == null) {
			return this.newRepositoryLocation();
		}
		String[] parts = reference.split(";"); //$NON-NLS-1$
		if (parts.length == 0 || parts[0].length() == 0) {
			return this.newRepositoryLocation();
		}
		IRepositoryLocation location = getRepositoryLocation(parts[0]);
		if (location != null) {
			return location;
		}
		String id = parts[0].trim();
		location = new SVNRepositoryLocation(id.length() > 0 ? id : new UniversalUniqueIdentifier().toString());
		location.setTrunkLocation(""); //$NON-NLS-1$
		location.setTagsLocation(""); //$NON-NLS-1$
		location.setBranchesLocation(""); //$NON-NLS-1$
		location.setAuthorName(""); //$NON-NLS-1$
		location.fillLocationFromReference(parts);
		return location;
	}

	/*
	 * see IRepositoryLocation comments why we need LocationReferenceTypeEnum parameter
	 */
	@Override
	public String repositoryLocationAsReference(IRepositoryLocation location,
			LocationReferenceTypeEnum locationReferenceType) {
		return location.asReference(locationReferenceType);
	}

	@Override
	public synchronized void addRepositoryLocation(IRepositoryLocation location) {
		List<IRepositoryLocation> tmp = new ArrayList<>(Arrays.asList(repositories));
		if (!tmp.contains(location)) {
			tmp.add(location);
			repositories = tmp.toArray(new IRepositoryLocation[tmp.size()]);
		}
		fireRepositoriesStateChanged(
				new RepositoriesStateChangedEvent(location, RepositoriesStateChangedEvent.ADDED));
	}

	@Override
	public synchronized void removeRepositoryLocation(IRepositoryLocation location) {
		List<IRepositoryLocation> tmp = new ArrayList<>(Arrays.asList(repositories));
		removeAuthInfoForLocation(location, ""); //$NON-NLS-1$
		String[] realms = location.getRealms().toArray(new String[0]);
		for (String realm : realms) {
			removeAuthInfoForLocation(location, realm);
		}
		if (tmp.remove(location)) {
			repositories = tmp.toArray(new IRepositoryLocation[tmp.size()]);
		}
		fireRepositoriesStateChanged(
				new RepositoriesStateChangedEvent(location, RepositoriesStateChangedEvent.REMOVED));
	}

	public SVNCachedProxyCredentialsManager getProxyCredentialsManager() {
		return proxyCredentialsManager;
	}

	@Override
	public synchronized void saveConfiguration() throws Exception {
		saveLocations();
	}

	public byte[] revisionLinkAsBytes(IRevisionLink link, boolean saveRevisionLinksComments) {
		String str = repositoryResourceAsString(link.getRepositoryResource());
		if (str != null && saveRevisionLinksComments) {
			str += ";" + new String(Base64.encode(link.getComment().getBytes())); //$NON-NLS-1$
		}
		return str != null ? str.getBytes() : null;
	}

	@Override
	public byte[] repositoryResourceAsBytes(IRepositoryResource resource) {
		String str = repositoryResourceAsString(resource);
		return str != null ? str.getBytes() : null;
	}

	protected String repositoryResourceAsString(IRepositoryResource resource) {
		if (resource == null) {
			return null;
		}
		String retVal = new String(Base64.encode(String.valueOf(resource instanceof IRepositoryContainer).getBytes()))
				+ ";" + //$NON-NLS-1$
				resource.getRepositoryLocation().getId() + ";" + //$NON-NLS-1$
				new String(Base64.encode(resource.getUrl().getBytes())) + ";" + //$NON-NLS-1$
				String.valueOf(resource.getSelectedRevision().getKind().id) + ";" + //$NON-NLS-1$
				convertRevisionToString(resource.getSelectedRevision()) + ";" + //$NON-NLS-1$
				String.valueOf(IRepositoryRoot.KIND_ROOT) + ";" + //$NON-NLS-1$
				String.valueOf(resource.getPegRevision().getKind().id) + ";" + //$NON-NLS-1$
				convertRevisionToString(resource.getPegRevision());
		return retVal;
	}

	protected SVNRevision convertToRevision(int revisionKind, long revNum, boolean isPegRevision) {
		SVNRevision revision;
		if (revisionKind == Kind.NUMBER.id) {
			if (revNum == SVNRevision.INVALID_REVISION_NUMBER) {
				revision = isPegRevision ? null : SVNRevision.INVALID_REVISION;
			} else {
				revision = SVNRevision.fromNumber(revNum);
			}
		} else if (revisionKind == Kind.DATE.id) {
			revision = SVNRevision.fromDate(revNum);
		} else {
			revision = SVNRevision.fromKind(SVNRevision.Kind.fromId(revisionKind));
		}
		return revision;
	}

	protected String convertRevisionToString(SVNRevision revision) {
		String strRevision;
		if (revision.getKind() == Kind.NUMBER) {
			strRevision = String.valueOf(((SVNRevision.Number) revision).getNumber());
		} else if (revision.getKind() == Kind.DATE) {
			strRevision = String.valueOf(((SVNRevision.Date) revision).getDate());
		} else {
			strRevision = "0"; //$NON-NLS-1$
		}
		return strRevision;
	}

	@Override
	public IRepositoryResource repositoryResourceFromBytes(byte[] bytes) {
		return this.repositoryResourceFromBytes(bytes, null);
	}

	public IRevisionLink revisionLinkFromBytes(byte[] bytes, IRepositoryLocation location) {
		IRepositoryResource resource = this.repositoryResourceFromBytes(bytes, location);
		if (resource != null) {
			String[] data = new String(bytes).split(";"); //$NON-NLS-1$
			String comment = null;
			if (data.length > 8) {
				comment = new String(Base64.decode(data[8].getBytes()));
			}
			IRevisionLink link = SVNUtility.createRevisionLink(resource);
			link.setComment(comment);
			return link;
		}
		return null;
	}

	public IRepositoryResource repositoryResourceFromBytes(byte[] bytes, IRepositoryLocation location) {
		if (bytes == null) {
			return null;
		}
		String[] data = new String(bytes).split(";"); //$NON-NLS-1$
		boolean isFolder = false;
		boolean base64Label = false;
		if ("true".equals(data[0])) { //$NON-NLS-1$
			isFolder = true;
		} else if (!"false".equals(data[0])) { //$NON-NLS-1$
			isFolder = "true".equals(new String(Base64.decode(data[0].getBytes()))); //$NON-NLS-1$
			base64Label = true;
		}
		if (location == null) {
			location = getRepositoryLocation(data[1]);
		}
		if (location == null) {
			return null;
		}
		long revNum = Long.parseLong(data[4]);
		int revisionKind;
		try {
			revisionKind = Integer.parseInt(data[3]);
		} catch (NumberFormatException ex) { // in order to prevent crashing on improperly stored data (see bug 465812)
			revisionKind = revNum > 0 ? SVNRevision.Kind.NUMBER.id : SVNRevision.Kind.HEAD.id;
		}
		SVNRevision selectedRevision = convertToRevision(revisionKind, revNum, false);
		SVNRevision pegRevision = null;
		if (data.length > 6) {
			long pegNum = Long.parseLong(data[7]);
			int pegKind;
			try {
				pegKind = Integer.parseInt(data[6]);
			} catch (NumberFormatException ex) { // in order to prevent crashing on improperly stored data (see bug 465812)
				pegKind = pegNum > 0 ? SVNRevision.Kind.NUMBER.id : SVNRevision.Kind.HEAD.id;
			}
			pegRevision = convertToRevision(pegKind, pegNum, true);
		}

		String urlPart = base64Label ? new String(Base64.decode(data[2].getBytes())) : data[2];
		try {
			SVNUtility.getSVNUrl(urlPart);
		} catch (MalformedURLException e) {
			// old-style partial url
			String prefix = AbstractSVNStorage.getRootPrefix(location, Integer.parseInt(data[5]));
			urlPart = prefix + urlPart;
		}

		location = wrapLocationIfRequired(location, urlPart, !isFolder);

		IRepositoryResource retVal = isFolder
				? (IRepositoryResource) location.asRepositoryContainer(urlPart, false)
				: location.asRepositoryFile(urlPart, false);
		retVal.setSelectedRevision(selectedRevision);
		retVal.setPegRevision(pegRevision);
		return retVal;
	}

	protected abstract IRepositoryLocation wrapLocationIfRequired(IRepositoryLocation location, String url,
			boolean isFile);

	protected static String getRootPrefix(IRepositoryLocation location, int rootKind) {
		switch (rootKind) {
			case IRepositoryRoot.KIND_ROOT: {
				return location.getRepositoryRootUrl();
			}
			case IRepositoryRoot.KIND_LOCATION_ROOT: {
				return location.getUrl();
			}
			case IRepositoryRoot.KIND_TRUNK: {
				return location.isStructureEnabled()
						? location.getUrl() + "/" + location.getTrunkLocation() //$NON-NLS-1$
						: location.getUrl();
			}
			case IRepositoryRoot.KIND_BRANCHES: {
				return location.isStructureEnabled()
						? location.getUrl() + "/" + location.getBranchesLocation() //$NON-NLS-1$
						: location.getUrl();
			}
			case IRepositoryRoot.KIND_TAGS: {
				return location.isStructureEnabled()
						? location.getUrl() + "/" + location.getTagsLocation() //$NON-NLS-1$
						: location.getUrl();
			}
		}
		return null;
	}

	@Override
	public void initialize(IPath stateInfoLocation) throws Exception {
		HashMap preferences = new HashMap();
		preferences.put(ISVNStorage.PREF_STATE_INFO_LOCATION, stateInfoLocation);
		this.initialize(preferences);
	}

	@Override
	public void initialize(Map<String, Object> preferences) throws Exception {
		Boolean noStoredAuthObj = (Boolean) preferences.get(ISVNStorage.PREF_NO_STORED_AUTHENTICATION);
		noStoredAuthentication = noStoredAuthObj != null ? noStoredAuthObj : false;

		IPath stateInfoLocation = (IPath) preferences.get(ISVNStorage.PREF_STATE_INFO_LOCATION);
		String infoFileName = (String) preferences.get(AbstractSVNStorage.IPREF_STATE_INFO_FILE);
		String repositoriesPreferencesNode = (String) preferences.get(AbstractSVNStorage.IPREF_REPO_NODE_NAME);
		String migrateFromAuthDBPreferenceNode = (String) preferences.get(AbstractSVNStorage.IPREF_AUTH_NODE_NAME);

		stateInfoFile = stateInfoLocation.append(infoFileName).toFile();
		IProxyService proxyService = SVNTeamPlugin.instance().getProxyService();
		proxyCredentialsManager = new SVNCachedProxyCredentialsManager(proxyService);

		proxyService.addProxyChangeListener(event -> {
			IProxyData[] newDatas = event.getChangedProxyData();
			for (IProxyData current : newDatas) {
				if (current.isRequiresAuthentication()) {
					proxyCredentialsManager.setPassword(current.getPassword());
					proxyCredentialsManager.setUsername(current.getUserId());
					break;
				}
			}
			AbstractSVNStorage.this.dispose();
		});

		//set flag whether we migrated from Authorization Database
		this.migrateFromAuthDBPreferenceNode = migrateFromAuthDBPreferenceNode;
		IEclipsePreferences migratePref = (IEclipsePreferences) SVNTeamPlugin.instance()
				.getPreferences()
				.node(this.migrateFromAuthDBPreferenceNode);

		this.repositoriesPreferencesNode = repositoriesPreferencesNode;
		repoPrefChangeListener = new RepositoryPreferenceChangeListener();
		IEclipsePreferences repositoryPreferences = AbstractSVNStorage
				.getRepositoriesPreferences(this.repositoriesPreferencesNode);
		repositoryPreferences.addPreferenceChangeListener(repoPrefChangeListener);
		// if the file exists, we should convert the data and delete the file.
		if (stateInfoFile.exists()) {
			try {
				loadLocationsFromFile();
				saveLocations();
			} catch (Exception ex) {
				LoggedOperation.reportError(SVNMessages.getErrorString("Error_LoadLocationsFromFile"), ex); //$NON-NLS-1$
			} finally {
				stateInfoFile.delete();
			}
		} else {
			try {
				loadLocations();
			} catch (Exception ex) {
				LoggedOperation.reportError(SVNMessages.getErrorString("Error_LoadLocations"), ex); //$NON-NLS-1$
			}
		}
	}

	protected void saveLocations() throws Exception {
		IEclipsePreferences repositoryPreferences = AbstractSVNStorage
				.getRepositoriesPreferences(repositoriesPreferencesNode);
		repositoryPreferences.removePreferenceChangeListener(repoPrefChangeListener);
		repositoryPreferences.clear();
		for (IRepositoryLocation current : repositories) {
			repositoryPreferences.put(current.getId(),
					repositoryLocationAsReference(current, LocationReferenceTypeEnum.ALL));
			saveAuthInfo(current, ""); //$NON-NLS-1$
			String[] realms = current.getRealms().toArray(new String[0]);
			for (String realm : realms) {
				saveAuthInfo(current, realm);
			}
		}

		repositoryPreferences.flush();
		SVNTeamPlugin.instance().savePreferences();
		repositoryPreferences.addPreferenceChangeListener(repoPrefChangeListener);
	}

	/*
	 * Using location id in node name means that secure preferences can't be
	 * used in another workspace
	 */
	protected ISecurePreferences getSVNNodeForSecurePreferences(IRepositoryLocation location, String realm) {
		ISecurePreferences preferences = SecurePreferencesFactory.getDefault();
		if (preferences == null) {
			return null;
		}
		String urlPart = location.getUrlAsIs() + ":" + location.getId(); //$NON-NLS-1$
		if (!"".equals(realm)) { //$NON-NLS-1$
			urlPart += ":" + realm; //$NON-NLS-1$
		}
		String path = AbstractSVNStorage.SVN_SECURE_NAME_SEGMENT + EncodingUtils.encodeSlashes(urlPart);
		try {
			return preferences.node(path);
		} catch (IllegalArgumentException e) {
			return null; // invalid path
		}
	}

	protected void saveAuthInfo(IRepositoryLocation location, String realm) throws Exception {
		if (!noStoredAuthentication) {
			ISecurePreferences node = getSVNNodeForSecurePreferences(location, realm);
			if (node != null) {
				try {
					IRepositoryLocation tmp = realm.equals("") ? location : location.getLocationForRealm(realm); //$NON-NLS-1$
					boolean toStorePass = tmp.isPasswordSaved();

					//store normal password settings
					node.put("username", tmp.getUsername(), false); //$NON-NLS-1$
					if (toStorePass) {
						node.put("password", tmp.getPassword(), true); //$NON-NLS-1$
					} else {
						node.remove("password"); //$NON-NLS-1$
					}
					node.putBoolean("password_saved", toStorePass, false); //$NON-NLS-1$

					//store SSH settings
					SSHSettings sshSettings = tmp.getSSHSettings();
					boolean useKeyFile = sshSettings.isUseKeyFile();
					node.putBoolean("ssh_use_key", useKeyFile, false); //$NON-NLS-1$
					boolean savePassphrase = sshSettings.isPassPhraseSaved();
					node.putBoolean("ssh_passphrase_saved", useKeyFile ? savePassphrase : false, false); //$NON-NLS-1$
					node.put("ssh_key", useKeyFile ? sshSettings.getPrivateKeyPath() : "", false); //$NON-NLS-1$ //$NON-NLS-2$
					if (useKeyFile && savePassphrase) {
						node.put("ssh_passprase", sshSettings.getPassPhrase(), true); //$NON-NLS-1$
					} else {
						node.remove("ssh_passprase"); //$NON-NLS-1$
					}
					node.putInt("ssh_port", sshSettings.getPort(), false);

					//store SSL settings
					SSLSettings sslSettings = tmp.getSSLSettings();
					boolean clientAuthEnabled = sslSettings.isAuthenticationEnabled();
					savePassphrase = sslSettings.isPassPhraseSaved();
					node.putBoolean("ssl_enabled", clientAuthEnabled, false); //$NON-NLS-1$
					node.put("ssl_certificate", clientAuthEnabled ? sslSettings.getCertificatePath() : "", false); //$NON-NLS-1$ //$NON-NLS-2$
					node.putBoolean("ssl_passphrase_saved", clientAuthEnabled ? savePassphrase : false, false); //$NON-NLS-1$
					if (clientAuthEnabled && savePassphrase) {
						node.put("ssl_passphrase", sslSettings.getPassPhrase(), true); //$NON-NLS-1$
					} else {
						node.remove("ssl_passphrase"); //$NON-NLS-1$
					}
				} catch (StorageException e) {
					LoggedOperation.reportError(SVNMessages.getErrorString("Error_SaveAutherizationInfo"), e); //$NON-NLS-1$
				}
			}
		}
	}

	protected void loadAuthInfo(IRepositoryLocation location, String realm) throws Exception {
		loadAuthInfoFromSecureStorage(location, realm);
	}

	protected void loadAuthInfoFromSecureStorage(IRepositoryLocation location, String realm) throws Exception {
		if (!noStoredAuthentication) {
			try {
				ISecurePreferences node = getSVNNodeForSecurePreferences(location, realm);
				if (node != null) {
					IRepositoryLocation tmp;
					boolean toAddRealm = !realm.equals(""); //$NON-NLS-1$
					if (toAddRealm) {
						tmp = this.newRepositoryLocation();
					} else {
						tmp = location;
					}

					//recover normal password settings
					tmp.setPasswordSaved(node.getBoolean("password_saved", false)); //$NON-NLS-1$
					tmp.setUsername(node.get("username", "")); //$NON-NLS-1$ //$NON-NLS-2$
					tmp.setPassword(node.get("password", "")); //$NON-NLS-1$ //$NON-NLS-2$

					//recover SSH settings
					SSHSettings sshSettings = tmp instanceof SVNRepositoryLocation
							? ((SVNRepositoryLocation) tmp).getSSHSettings(false)
							: tmp.getSSHSettings();
					sshSettings.setUseKeyFile(node.getBoolean("ssh_use_key", false)); //$NON-NLS-1$
					sshSettings.setPrivateKeyPath(node.get("ssh_key", "")); //$NON-NLS-1$ //$NON-NLS-2$
					sshSettings.setPassPhraseSaved(node.getBoolean("ssh_passphrase_saved", false)); //$NON-NLS-1$
					sshSettings.setPassPhrase(node.get("ssh_passprase", "")); //$NON-NLS-1$ //$NON-NLS-2$
					int defaultPort = sshSettings.getPort();
					defaultPort = defaultPort != 0 ? defaultPort : SSHSettings.SSH_PORT_DEFAULT;
					sshSettings.setPort(node.getInt("ssh_port", defaultPort));

					//recover SSL settings
					SSLSettings sslSettings = tmp instanceof SVNRepositoryLocation
							? ((SVNRepositoryLocation) tmp).getSSLSettings(false)
							: tmp.getSSLSettings();
					sslSettings.setAuthenticationEnabled(node.getBoolean("ssl_enabled", false)); //$NON-NLS-1$
					sslSettings.setCertificatePath(node.get("ssl_certificate", "")); //$NON-NLS-1$ //$NON-NLS-2$
					sslSettings.setPassPhraseSaved(node.getBoolean("ssl_passphrase_saved", false)); //$NON-NLS-1$
					sslSettings.setPassPhrase(node.get("ssl_passphrase", "")); //$NON-NLS-1$ //$NON-NLS-2$

					//if realm, add it to realms
					if (toAddRealm) {
						location.addRealm(realm, tmp);
					}
				}
			} catch (StorageException e) {
				LoggedOperation.reportError(SVNMessages.getErrorString("Error_LoadAuthorizationInfo"), e); //$NON-NLS-1$
			}
		}
	}

	public void removeAuthInfoForLocation(IRepositoryLocation location, String realm) {
		if (!noStoredAuthentication) {
			ISecurePreferences node = getSVNNodeForSecurePreferences(location, realm);
			if (node == null) {
				return;
			}
			try {
				node.clear();
				node.flush(); // save immediately
			} catch (IllegalStateException | IOException e) {
				LoggedOperation.reportError(SVNMessages.getErrorString("Error_RemoveAuthorizationInfo"), e); //$NON-NLS-1$
			}
		}
	}

	protected void loadLocations() throws Exception {
		IEclipsePreferences repositoryPreferences = AbstractSVNStorage
				.getRepositoriesPreferences(repositoriesPreferencesNode);
		String[] keys = repositoryPreferences.keys();
		ArrayList<IRepositoryLocation> readLocations = new ArrayList<>();
		for (String current : keys) {
			IRepositoryLocation location = this.newRepositoryLocation(repositoryPreferences.get(current, null));
			readLocations.add(location);
		}
		repositories = readLocations.toArray(new IRepositoryLocation[readLocations.size()]);
	}

	/**
	 * Loads locations info from a file. Left for compatibility with earlier versions - to have a probability to read locations and convert
	 * to preferences. Used for compatibility with the versions of Subversive, where the locations info was stored in files.
	 * 
	 * @throws Exception
	 */
	protected void loadLocationsFromFile() throws Exception {
		List<IRepositoryLocation> tmp = new ArrayList<>(Arrays.asList(repositories));
		ObjectInputStream stream = null;
		try {
			stream = new ObjectInputStream(new FileInputStream(stateInfoFile));

			// stream.available() does not provide any EOF information
			while (true) {
				SVNRepositoryLocation obj = (SVNRepositoryLocation) stream.readObject();
				if (!tmp.contains(obj)) {
					tmp.add(obj);
				}
			}
		} catch (EOFException ex) {
			// EOF, do nothing
		} finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (Exception ex) {
				}
			}
		}
		repositories = tmp.toArray(new IRepositoryLocation[tmp.size()]);
	}

}
