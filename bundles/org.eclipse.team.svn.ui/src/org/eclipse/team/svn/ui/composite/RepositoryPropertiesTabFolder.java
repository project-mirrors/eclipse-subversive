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
 *    Sergiy Logvin - Initial API and implementation
 *    Sergey Vasilchenko - [patch] bug fix: Trunk, branches and tags default values are not stored if they are disabled while creating new location
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.composite;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;

import org.eclipse.compare.internal.TabFolderLayout;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.svn.core.connector.ISVNCredentialsPrompt;
import org.eclipse.team.svn.core.extension.CoreExtensionsManager;
import org.eclipse.team.svn.core.extension.factory.ISVNConnectorFactory;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.SSHSettings;
import org.eclipse.team.svn.core.resource.SSLSettings;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.preferences.SVNTeamPreferences;
import org.eclipse.team.svn.ui.utility.UserInputHistory;
import org.eclipse.team.svn.ui.verifier.AbstractFormattedVerifier;
import org.eclipse.team.svn.ui.verifier.AbstractVerifier;
import org.eclipse.team.svn.ui.verifier.AbstractVerifierProxy;
import org.eclipse.team.svn.ui.verifier.IValidationManager;
import org.eclipse.ui.dialogs.PreferencesUtil;

/**
 * Repository properties tab folder
 *
 * @author Sergiy Logvin
 */
public class RepositoryPropertiesTabFolder extends Composite implements IPropertiesPanel, ISecurityInfoProvider {

	protected static final String AUTHOR_HISTORY_NAME = "authorName"; //$NON-NLS-1$

	protected RepositoryPropertiesComposite repositoryPropertiesPanel;

	protected SSHComposite sshComposite;

	protected SSLComposite sslComposite;

	protected RepositoryRootsComposite rootsComposite;

	protected Composite parent;

	protected IRepositoryLocation repositoryLocation;

	protected int style;

	protected IValidationManager validationManager;

	protected Button validateButton;

	protected Button resetChangesButton;

	protected boolean validateOnFinish;

	protected boolean forceDisableRoots;

	protected boolean createNew;

	protected Combo cachedRealms;

	protected boolean isAuthorNameEnabled;

	protected Combo authorInput;

	protected Button authorEnabled;

	protected UserInputHistory authorNameHistory;

	protected TabItem sshTab;

	protected TabItem sslTab;

	protected Composite unavailableSSHComposite;

	protected Composite unavailableProxyComposite;

	protected IRepositoryLocation backup;

	public RepositoryPropertiesTabFolder(Composite parent, int style, IValidationManager validationManager,
			IRepositoryLocation repositoryLocation) {
		super(parent, style);
		this.parent = parent;
		this.style = style;
		this.validationManager = validationManager;
		this.repositoryLocation = repositoryLocation;
		createNew = repositoryLocation == null;
		if (createNew) {
			this.repositoryLocation = SVNRemoteStorage.instance().newRepositoryLocation();
		} else {
			backup = SVNRemoteStorage.instance().newRepositoryLocation();
			SVNRemoteStorage.instance().copyRepositoryLocation(backup, this.repositoryLocation);
		}
	}

	protected String sipUsername;

	protected String sipPassword;

	protected boolean sipIsPasswordSaved;

	protected SSHSettings sipSSHSettings;

	protected SSLSettings sipSSLSettings;

	@Override
	public String getUsername() {
		return sipUsername = repositoryPropertiesPanel.getUsernameDirect();
	}

	@Override
	public void setUsername(String username) {
		sipUsername = username;
	}

	@Override
	public String getPassword() {
		return sipPassword = repositoryPropertiesPanel.getPasswordDirect();
	}

	@Override
	public void setPassword(String password) {
		sipPassword = password;
	}

	@Override
	public boolean isPasswordSaved() {
		return sipIsPasswordSaved = repositoryPropertiesPanel.getPasswordSavedDirect();
	}

	@Override
	public void setPasswordSaved(boolean saved) {
		sipIsPasswordSaved = saved;
	}

	@Override
	public SSLSettings getSSLSettings() {
		return sipSSLSettings = sslComposite.getSSLSettingsDirect();
	}

	@Override
	public SSHSettings getSSHSettings() {
		return sipSSHSettings = sshComposite.getSSHSettingsDirect();
	}

	@Override
	public void commit() {
		repositoryPropertiesPanel.setUsernameDirect(sipUsername);
		repositoryPropertiesPanel.setPasswordDirect(sipPassword);
		repositoryPropertiesPanel.setPasswordSavedDirect(sipIsPasswordSaved);
		sshComposite.setSSHSettingsDirect(sipSSHSettings);
		sslComposite.setSSLSettingsDirect(sipSSLSettings);
	}

	@Override
	public void initialize() {
		GridLayout layout = new GridLayout();
		layout.verticalSpacing = 7;
		setLayout(layout);
		TabFolder tabFolder = new TabFolder(this, SWT.NONE);
		tabFolder.setLayout(new TabFolderLayout());
		tabFolder.setLayoutData(new GridData(GridData.FILL_BOTH));

		TabItem tabItem = new TabItem(tabFolder, SWT.NONE);
		tabItem.setText(SVNUIMessages.RepositoryPropertiesTabFolder_General);
		tabItem.setControl(createRepositoryPropertiesPanel(tabFolder));

		tabItem = new TabItem(tabFolder, SWT.NONE);
		tabItem.setText(SVNUIMessages.RepositoryPropertiesTabFolder_Advanced);
		Composite rootsComposite = createRepositoryRootsComposite(tabFolder);
		GridData data = new GridData();
		data.verticalIndent = 10;
		authorEnabled = new Button(rootsComposite, SWT.CHECK);
		authorEnabled.setText(SVNUIMessages.NewRepositoryLocationWizard_OverrideAuthor);
		authorEnabled.setSelection(repositoryLocation.isAuthorNameEnabled());
		authorEnabled.setLayoutData(data);
		String name = repositoryLocation.getAuthorName() == null ? "" : repositoryLocation.getAuthorName(); //$NON-NLS-1$
		data = new GridData(GridData.FILL_HORIZONTAL);
		authorInput = new Combo(rootsComposite, SWT.BORDER);
		authorNameHistory = new UserInputHistory(RepositoryPropertiesTabFolder.AUTHOR_HISTORY_NAME);
		authorInput.setVisibleItemCount(authorNameHistory.getDepth());
		authorInput.setItems(authorNameHistory.getHistory());
		authorInput.setText(name);
		authorInput.setEnabled(repositoryLocation.isAuthorNameEnabled());
		authorInput.setLayoutData(data);
		AbstractFormattedVerifier verifier = new AbstractFormattedVerifier("AuthorNameVerifier") { //$NON-NLS-1$
			@Override
			protected String getErrorMessageImpl(Control input) {
				if (getText(input).equals("")) { //$NON-NLS-1$
					return SVNUIMessages.NewRepositoryLocationWizard_AuthorName_Verifier;
				}
				return null;
			}

			@Override
			protected String getWarningMessageImpl(Control input) {
				return null;
			}
		};
		validationManager.attachTo(authorInput, new AbstractVerifierProxy(verifier) {
			@Override
			protected boolean isVerificationEnabled(Control input) {
				return authorEnabled.getSelection();
			}
		});
		authorEnabled.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				authorInput.setEnabled(((Button) event.widget).getSelection());
				validationManager.validateContent();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});

		Group proxyGroup = new Group(rootsComposite, SWT.NONE);
		GridLayout proxyGroupLayout = new GridLayout(1, true);
		proxyGroupLayout.marginHeight = 5;
		proxyGroupLayout.marginWidth = 5;
		proxyGroup.setLayout(proxyGroupLayout);
		proxyGroup.setText(SVNUIMessages.RepositoryPropertiesTabFolder_Proxy);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.verticalIndent = 10;
		proxyGroup.setLayoutData(data);

		Link proxyLink = new Link(proxyGroup, SWT.WRAP);
		proxyLink.setText(SVNUIMessages.RepositoryPropertiesTabFolder_Proxy_Link);
		proxyLink.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String pageId = "org.eclipse.ui.net.NetPreferences"; //$NON-NLS-1$
				PreferencesUtil.createPreferenceDialogOn(null, pageId, new String[] { pageId }, null).open();
			}
		});
		data = new GridData(GridData.FILL_BOTH);
		data.widthHint = 200;
		data.heightHint = DefaultDialog.convertHeightInCharsToPixels(this, 3);
		proxyLink.setLayoutData(data);

		tabItem.setControl(rootsComposite);
		unavailableSSHComposite = createUnavailableComposite(tabFolder);
		unavailableSSHComposite.setVisible(false);
		createSSHHostComposite(tabFolder);
		sshComposite.setVisible(false);
		sshTab = new TabItem(tabFolder, SWT.NONE);
		sshTab.setText(SVNUIMessages.RepositoryPropertiesTabFolder_SSHSettings);
		if ((CoreExtensionsManager.instance().getSVNConnectorFactory().getSupportedFeatures()
				& ISVNConnectorFactory.OptionalFeatures.SSH_SETTINGS) != 0) {
			sshTab.setControl(sshComposite);
		} else {
			sshTab.setControl(unavailableSSHComposite);
		}

		createSSLHostComposite(tabFolder);
		sslComposite.setVisible(false);
		sslTab = new TabItem(tabFolder, SWT.NONE);
		sslTab.setText(SVNUIMessages.RepositoryPropertiesTabFolder_SSLSettings);
		sslTab.setControl(sslComposite);

		unavailableProxyComposite = createUnavailableComposite(tabFolder);
		unavailableProxyComposite.setVisible(false);

		Composite bottomPart = new Composite(this, SWT.NONE);
		layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.numColumns = 3;
		bottomPart.setLayout(layout);
		data = new GridData(GridData.FILL_HORIZONTAL);
		bottomPart.setLayoutData(data);

		Composite realmsComposite = new Composite(bottomPart, SWT.NONE);
		layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		layout.numColumns = 3;
		realmsComposite.setLayout(layout);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 3;
		realmsComposite.setLayoutData(data);

		Label label = new Label(realmsComposite, SWT.NONE);
		data = new GridData();
		label.setLayoutData(data);
		label.setText(SVNUIMessages.RepositoryPropertiesTabFolder_ShowFor);

		cachedRealms = new Combo(realmsComposite, SWT.BORDER | SWT.READ_ONLY);
		final Button deleteRealm = new Button(realmsComposite, SWT.PUSH);

		data = new GridData(GridData.FILL_HORIZONTAL);
		cachedRealms.setLayoutData(data);
		final ArrayList itemSet = new ArrayList();
		itemSet.add(ISVNCredentialsPrompt.ROOT_LOCATION);
		itemSet.addAll(repositoryLocation.getRealms());
		cachedRealms.setItems((String[]) itemSet.toArray(new String[itemSet.size()]));
		cachedRealms.select(0);
		cachedRealms.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				deleteRealm.setEnabled(cachedRealms.getSelectionIndex() != 0);
				RepositoryPropertiesTabFolder.this.realmSelectionChanged(false);
			}
		});

		ImageDescriptor imgDescr = SVNTeamUIPlugin.instance().getImageDescriptor("icons/common/delete_realm.gif"); //$NON-NLS-1$
		deleteRealm.setImage(imgDescr.createImage());
		data = new GridData();
		data.heightHint = cachedRealms.getTextHeight() + 2;
		deleteRealm.setLayoutData(data);
		deleteRealm.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int idx = cachedRealms.getSelectionIndex();
				if (idx != 0) {
					String item = cachedRealms.getItem(idx);
					itemSet.remove(item);
					cachedRealms.setItems((String[]) itemSet.toArray(new String[itemSet.size()]));
					cachedRealms.select(idx - 1);
					RepositoryPropertiesTabFolder.this.realmSelectionChanged(false);
				}
				boolean enabled = cachedRealms.getItems().length > 1;
				((Button) e.widget).setEnabled(enabled);
				cachedRealms.setEnabled(enabled);
				idx = cachedRealms.getSelectionIndex();
				if (idx == 0) {
					((Button) e.widget).setEnabled(false);
				}
			}
		});
		deleteRealm.setEnabled(false);
		RepositoryPropertiesTabFolder.this.cachedRealms.setEnabled(itemSet.size() > 1);

		validateButton = new Button(bottomPart, SWT.CHECK);
		data = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		validateButton.setLayoutData(data);
		validateButton.setText(SVNUIMessages.RepositoryPropertiesTabFolder_ValidateOnFinish);
		validateButton.setSelection(true);

		Text empty = new Text(bottomPart, SWT.READ_ONLY);
		data = new GridData(GridData.FILL_HORIZONTAL);
		empty.setLayoutData(data);
		empty.setVisible(false);

		resetChangesButton = new Button(bottomPart, SWT.PUSH);
		data = new GridData(GridData.HORIZONTAL_ALIGN_END);
		resetChangesButton.setText(SVNUIMessages.RepositoryPropertiesTabFolder_ResetChanges);
		int widthHint = DefaultDialog.computeButtonWidth(resetChangesButton);
		data.widthHint = widthHint;
		resetChangesButton.setLayoutData(data);
		resetChangesButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				RepositoryPropertiesTabFolder.this.resetChanges();
				validationManager.validateContent();
			}
		});

		if ((repositoryLocation.getUsername() == null || repositoryLocation.getUsername().length() == 0)
				&& repositoryLocation.getRealms().size() > 0) {
			cachedRealms.select(1);
			deleteRealm.setEnabled(true);
			realmSelectionChanged(true);
		}
	}

	/*
	 * We need isFirstTime flag in order not to load values from controls to models for the first time
	 * as initial control values are default.
	 */
	protected void realmSelectionChanged(boolean isFirstTime) {
		IRepositoryLocation location = repositoryLocation;
		int idx = cachedRealms.getSelectionIndex();
		if (idx != 0) {
			location = location.getLocationForRealm(cachedRealms.getItem(idx));
		}

		if (!isFirstTime) {
			repositoryPropertiesPanel.saveChanges();
		}
		repositoryPropertiesPanel.setCredentialsInput(location, this);
		repositoryPropertiesPanel.resetChanges();
		if ((CoreExtensionsManager.instance().getSVNConnectorFactory().getSupportedFeatures()
				& ISVNConnectorFactory.OptionalFeatures.SSH_SETTINGS) != 0) {
			if (!isFirstTime) {
				sshComposite.saveChanges();
			}
			sshComposite.setCredentialsInput(location.getSSHSettings());
			sshComposite.resetChanges();
		}
		if (!isFirstTime) {
			sslComposite.saveChanges();
		}
		sslComposite.setCredentialsInput(location.getSSLSettings());
		sslComposite.resetChanges();
	}

	protected Composite createRepositoryPropertiesPanel(Composite tabFolder) {
		repositoryPropertiesPanel = new RepositoryPropertiesComposite(tabFolder, style, validationManager);
		repositoryPropertiesPanel.setRepositoryLocation(repositoryLocation,
				createNew ? null : repositoryLocation.getRepositoryRootUrl(), this);
		repositoryPropertiesPanel.initialize();

		return repositoryPropertiesPanel;
	}

	protected Composite createSSHHostComposite(Composite tabFolder) {
		sshComposite = new SSHComposite(tabFolder, style, validationManager);
		sshComposite.setCredentialsInput(repositoryLocation.getSSHSettings());
		sshComposite.initialize();

		return sshComposite;
	}

	protected Composite createSSLHostComposite(Composite tabFolder) {
		sslComposite = new SSLComposite(tabFolder, style, validationManager);
		sslComposite.setCredentialsInput(repositoryLocation.getSSLSettings());
		sslComposite.initialize();

		return sslComposite;
	}

	protected Composite createRepositoryRootsComposite(Composite tabFolder) {
		rootsComposite = new RepositoryRootsComposite(tabFolder, style, validationManager);
		rootsComposite.setStructureEnabled(repositoryLocation.isStructureEnabled());

		rootsComposite.setTrunkLocation(repositoryLocation.getUserInputTrunk());
		rootsComposite.setBranchesLocation(repositoryLocation.getUserInputBranches());
		rootsComposite.setTagsLocation(repositoryLocation.getUserInputTags());

		rootsComposite.setCreateLocation(createNew);
		rootsComposite.initialize();

		return rootsComposite;
	}

	protected Composite createUnavailableComposite(Composite tabFolder) {
		Composite composite = new Composite(tabFolder, SWT.NONE);
		GridLayout layout = new GridLayout();
		composite.setLayout(layout);

		Label label = new Label(composite, SWT.WRAP);
		label.setText(SVNUIMessages.RepositoryPropertiesTabFolder_UnavailableMessage);

		Link link = new Link(composite, SWT.NONE);
		link.setText(SVNUIMessages.RepositoryPropertiesTabFolder_LinkToPreferences);
		link.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				RepositoryPropertiesTabFolder.this.handleLinkSelection();
			}
		});

		return composite;
	}

	public void handleLinkSelection() {
		boolean sshWasAllowed = (CoreExtensionsManager.instance().getSVNConnectorFactory().getSupportedFeatures()
				& ISVNConnectorFactory.OptionalFeatures.SSH_SETTINGS) != 0;

		String pageId = "org.eclipse.team.svn.ui.SVNTeamPreferences"; //$NON-NLS-1$
		PreferencesUtil.createPreferenceDialogOn(null, pageId, new String[] { pageId }, null).open();

		boolean sshAllowed = (CoreExtensionsManager.instance().getSVNConnectorFactory().getSupportedFeatures()
				& ISVNConnectorFactory.OptionalFeatures.SSH_SETTINGS) != 0;

		updateTabContent(sshWasAllowed, sshAllowed, sshTab, sshComposite, unavailableSSHComposite);
	}

	public void updateTabContent(boolean wasAvailable, boolean isAvailable, TabItem tab,
			AbstractDynamicComposite availableComposite, Composite unavailableComposite) {
		if (isAvailable) {
			if (!wasAvailable) {
				availableComposite.restoreAppearance();
				tab.setControl(availableComposite);
			}
		} else if (wasAvailable) {
			availableComposite.saveAppearance();
			tab.setControl(unavailableComposite);
			availableComposite.revalidateContent();
		}
	}

	public IRepositoryLocation getRepositoryLocation() {
		return repositoryLocation;
	}

	public String getLocationUrl() {
		return repositoryPropertiesPanel.getLocationUrl();
	}

	public boolean isStructureEnabled() {
		return rootsComposite.isStructureEnabled();
	}

	public boolean isValidateOnFinishRequested() {
		return validateOnFinish;
	}

	@Override
	public void saveChanges() {

		repositoryPropertiesPanel.saveChanges();
		if ((CoreExtensionsManager.instance().getSVNConnectorFactory().getSupportedFeatures()
				& ISVNConnectorFactory.OptionalFeatures.SSH_SETTINGS) != 0) {
			sshComposite.saveChanges();
		}
		sslComposite.saveChanges();
		rootsComposite.saveChanges();

		repositoryLocation.setAuthorName(authorInput.getText());
		repositoryLocation.setAuthorNameEnabled(authorEnabled.getSelection());
		authorNameHistory.addLine(authorInput.getText());

		IPreferenceStore store = SVNTeamUIPlugin.instance().getPreferenceStore();
		boolean enabled = rootsComposite.isStructureEnabled();
		if (enabled) {
			repositoryLocation.setTrunkLocation(rootsComposite.getTrunkLocation());
			repositoryLocation.setBranchesLocation(rootsComposite.getBranchesLocation());
			repositoryLocation.setTagsLocation(rootsComposite.getTagsLocation());
		} else if (createNew) {
			repositoryLocation.setTrunkLocation(
					SVNTeamPreferences.getRepositoryString(store, SVNTeamPreferences.REPOSITORY_HEAD_NAME));
			repositoryLocation.setBranchesLocation(
					SVNTeamPreferences.getRepositoryString(store, SVNTeamPreferences.REPOSITORY_BRANCHES_NAME));
			repositoryLocation.setTagsLocation(
					SVNTeamPreferences.getRepositoryString(store, SVNTeamPreferences.REPOSITORY_TAGS_NAME));
		}
		repositoryLocation.setStructureEnabled(enabled);

		HashSet realms = new HashSet(Arrays.asList(cachedRealms.getItems()));
		for (Iterator<String> it = repositoryLocation.getRealms().iterator(); it.hasNext();) {
			String current = it.next();
			if (!realms.contains(current)) {
				it.remove();
				SVNRemoteStorage.instance().removeAuthInfoForLocation(repositoryLocation, current);
			}
		}
		validateOnFinish = validateButton.getSelection();
	}

	@Override
	public void resetChanges() {
		repositoryPropertiesPanel.resetChanges();
		if ((CoreExtensionsManager.instance().getSVNConnectorFactory().getSupportedFeatures()
				& ISVNConnectorFactory.OptionalFeatures.SSH_SETTINGS) != 0) {
			sshComposite.resetChanges();
		}
		sslComposite.resetChanges();
		rootsComposite.resetChanges();
	}

	@Override
	public void cancelChanges() {
		if (!createNew) {
			SVNRemoteStorage.instance().copyRepositoryLocation(repositoryLocation, backup);
		}
	}

	public void setForceDisableRoots(boolean forceDisableRoots, AbstractVerifier verifier) {
		if (rootsComposite != null) {
			rootsComposite.setForceDisableRoots(forceDisableRoots);
		}
		if (repositoryPropertiesPanel != null) {
			repositoryPropertiesPanel.defineUrlVerifier(verifier);
		}
	}

}
