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
 *    Thomas Champagne - Bug 217561 : additional date formats for label decorations
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.preferences;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.eclipse.compare.internal.TabFolderLayout;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.preference.IPreferenceStore;
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
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.svn.core.SVNTeamPlugin;
import org.eclipse.team.svn.core.extension.CoreExtensionsManager;
import org.eclipse.team.svn.core.extension.factory.ISVNConnectorFactory;
import org.eclipse.team.svn.core.extension.factory.SVNConnectorHelper;
import org.eclipse.team.svn.core.resource.events.ResourceStatesChangedEvent;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.synchronize.AbstractSVNSubscriber;
import org.eclipse.team.svn.core.synchronize.UpdateSubscriber;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.verifier.AbstractVerifierProxy;
import org.eclipse.team.svn.ui.verifier.CompositeVerifier;
import org.eclipse.team.svn.ui.verifier.DateFormatVerifier;
import org.eclipse.team.svn.ui.verifier.ExistingResourceVerifier;
import org.eclipse.team.svn.ui.verifier.IntegerFieldVerifier;
import org.eclipse.team.svn.ui.verifier.NonEmptyFieldVerifier;
import org.eclipse.team.svn.ui.verifier.ResourceNameVerifier;
import org.eclipse.ui.PlatformUI;

/**
 * Main SVN Team preferences page
 * 
 * @author Alexander Gurov
 */
public class SVNTeamPreferencesPage extends AbstractSVNTeamPreferencesPage {
	protected String head;

	protected String branches;

	protected String tags;

	protected boolean showExternals;

	protected boolean simpleShare;

	protected boolean fastReport;

	protected boolean enableModelSync;

	protected boolean pagingEnable;

	protected boolean connectToCompareWith;

	protected int pageSize;

	protected int dateFormat;

	protected String dateFormatCustom;

	protected boolean mailReporterEnabled;

	protected boolean mailReporterErrorsEnabled;

	protected boolean commitSelectNewResources;

	protected boolean commitSelectDerivedResources;

	protected boolean notSelectMissingResources;

	protected boolean useSubversionExternalsBehaviour;

	protected boolean showSelectedResourcesInSyncPane;

	protected boolean treatReplacementAsEdit;

	protected String svnConnector;

	protected ISVNConnectorFactory[] factories;

	protected boolean useJavaHLMerge;

	protected boolean includeMergedRevisions;

	protected boolean checkoutUsingDotProjectName;

	protected boolean checkoutRespectProjectStructure;

	protected boolean checkoutUseDefaultLocation;

	protected String checkoutSpecifiedLocation;

	protected boolean branchTagConsiderStructure;

	protected boolean forceExternalsFreeze;

	protected boolean computeKeywordsValues;

	protected boolean enableAutoShare;

	protected boolean caseInsensitiveSorting;

	protected String consultChangeSets;

	protected boolean ignoreExternals;

	protected Text headField;

	protected Text branchesField;

	protected Text tagsField;

	protected Button showExternalsButton;

	protected Combo svnConnectorField;

	protected Button useInteractiveMergeButton;

	protected Button includeMergedRevisionsButton;

	protected Button fastReportButton;

	protected Button enableModelSyncButton;

	protected Button enablePagingButton;

	protected Button connectToCompareWithButton;

	protected Text pageSizeField;

	protected Combo dateFormatField;

	protected Text dateFormatCustomField;

	protected Button mailReporterEnabledButton;

	protected Button mailReporterErrorsEnabledButton;

	protected Button btnResourceSelectionNew;

	protected Button btnResourceSelectionDerived;

	protected Button btnResourceNotSelectionMissing;

	protected Button btnResourceSelectionExternal;

	protected Button btnResourceSelectionPresentation;

	protected Button btnResourceSelectionTreatAsEdit;

	protected Button checkoutUsingDotProjectNameButton;

	protected Button checkoutRespectProjectStructureButton;

	protected Button browse;

	protected Button useDefaultLocationButton;

	protected Text locationField;

	protected Button branchTagConsiderStructureButton;

	protected Button branchTagManualUrlEditButton;

	protected Button computeKeywordsValuesButton;

	protected Button enableAutoShareButton;

	protected Button caseInsensitiveSortingButton;

	protected Button forceExternalsFreezeButton;

	protected Button consultCSAlwaysButton;

	protected Button consultCSNeverButton;

	protected Button consultCSPromptButton;

	protected Button ignoreExternalsButton;

	protected Button checkForConnectorsButton;

	public SVNTeamPreferencesPage() {
	}

	@Override
	protected void saveValues(IPreferenceStore store) {
		SVNTeamPreferences.setRepositoryString(store, SVNTeamPreferences.REPOSITORY_HEAD_NAME, head);
		SVNTeamPreferences.setRepositoryString(store, SVNTeamPreferences.REPOSITORY_BRANCHES_NAME, branches);
		SVNTeamPreferences.setRepositoryString(store, SVNTeamPreferences.REPOSITORY_TAGS_NAME, tags);
		SVNTeamPreferences.setRepositoryBoolean(store, SVNTeamPreferences.BRANCH_TAG_CONSIDER_STRUCTURE_NAME,
				branchTagConsiderStructure);
		SVNTeamPreferences.setRepositoryBoolean(store, SVNTeamPreferences.REPOSITORY_FORCE_EXTERNALS_FREEZE_NAME,
				forceExternalsFreeze);
		SVNTeamPreferences.setRepositoryBoolean(store, SVNTeamPreferences.REPOSITORY_SHOW_EXTERNALS_NAME,
				showExternals);
		SVNTeamPreferences.setRepositoryBoolean(store, SVNTeamPreferences.REPOSITORY_SIMPLE_SHARE_NAME, simpleShare);

		AbstractSVNSubscriber.setSynchInfoContigous(fastReport);
		SVNTeamPreferences.setSynchronizeBoolean(store, SVNTeamPreferences.ENABLE_MODEL_SYNC_NAME, enableModelSync);

		SVNTeamPreferences.setHistoryInt(store, SVNTeamPreferences.HISTORY_PAGE_SIZE_NAME, pageSize);
		SVNTeamPreferences.setHistoryBoolean(store, SVNTeamPreferences.HISTORY_PAGING_ENABLE_NAME, pagingEnable);
		SVNTeamPreferences.setHistoryBoolean(store, SVNTeamPreferences.HISTORY_CONNECT_TO_COMPARE_WITH_NAME,
				connectToCompareWith);

		SVNTeamPreferences.setDateFormatInt(store, SVNTeamPreferences.DATE_FORMAT_NAME, dateFormat);
		SVNTeamPreferences.setDateFormatString(store, SVNTeamPreferences.DATE_FORMAT_CUSTOM_NAME, dateFormatCustom);

		SVNTeamPreferences.setConsultChangeSetsInCommit(store, SVNTeamPreferences.CONSULT_CHANGE_SETS_IN_COMMIT,
				consultChangeSets);

		SVNTeamPreferences.setMailReporterBoolean(store, SVNTeamPreferences.MAILREPORTER_ENABLED_NAME,
				mailReporterEnabled);
		SVNTeamPreferences.setMailReporterBoolean(store, SVNTeamPreferences.MAILREPORTER_ERRORS_ENABLED_NAME,
				mailReporterErrorsEnabled);

		SVNTeamPreferences.setBehaviourBoolean(store, SVNTeamPreferences.BEHAVIOUR_COMMIT_SELECT_NEW_RESOURCES_NAME,
				commitSelectNewResources);
		if (commitSelectDerivedResources != SVNTeamPreferences.getBehaviourBoolean(store,
				SVNTeamPreferences.BEHAVIOUR_COMMIT_SELECT_DERIVED_RESOURCES_NAME)) {
			SVNRemoteStorage.instance()
					.fireResourceStatesChangedEvent(new ResourceStatesChangedEvent(UpdateSubscriber.instance().roots(),
							IResource.DEPTH_INFINITE, ResourceStatesChangedEvent.CHANGED_NODES));
		}
		SVNTeamPreferences.setBehaviourBoolean(store, SVNTeamPreferences.BEHAVIOUR_COMMIT_SELECT_DERIVED_RESOURCES_NAME,
				commitSelectDerivedResources);
		SVNTeamPreferences.setBehaviourBoolean(store, SVNTeamPreferences.BEHAVIOUR_DO_NOT_SELECT_MISSING_RESOURCES_NAME,
				notSelectMissingResources);
		SVNTeamPreferences.setBehaviourBoolean(store, SVNTeamPreferences.BEHAVIOUR_DO_NOT_SELECT_EXTERNALS_NAME,
				useSubversionExternalsBehaviour);
		SVNTeamPreferences.setBehaviourBoolean(store,
				SVNTeamPreferences.BEHAVIOUR_SHOW_SELECTED_RESOURCES_IN_SYNC_PANE_NAME,
				showSelectedResourcesInSyncPane);
		SVNTeamPreferences.setBehaviourBoolean(store, SVNTeamPreferences.BEHAVIOUR_TREAT_REPLACEMENT_AS_EDIT_NAME,
				treatReplacementAsEdit);
		SVNTeamPreferences.setBehaviourBoolean(store, SVNTeamPreferences.BEHAVIOUR_ENABLE_AUTO_SHARE_NAME,
				enableAutoShare);
		SVNTeamPreferences.setBehaviourBoolean(store, SVNTeamPreferences.BEHAVIOUR_COMPUTE_KEYWORDS_NAME,
				computeKeywordsValues);
		SVNTeamPreferences.setBehaviourBoolean(store, SVNTeamPreferences.BEHAVIOUR_CASE_INSENSITIVE_TABLE_SORTING_NAME,
				caseInsensitiveSorting);
		SVNTeamPreferences.setBehaviourBoolean(store, SVNTeamPreferences.BEHAVIOUR_IGNORE_EXTERNALS_NAME,
				ignoreExternals);

		String oldId = CoreExtensionsManager.instance().getSVNConnectorFactory().getId();
		if (!oldId.equals(svnConnector)) {
			SVNTeamPreferences.setCoreString(store, SVNTeamPlugin.CORE_SVNCLIENT_NAME, svnConnector);
			// destroy all cached proxies
			SVNRemoteStorage.instance().dispose();
		}

		SVNTeamPreferences.setMergeBoolean(store, SVNTeamPreferences.MERGE_USE_JAVAHL_NAME, useJavaHLMerge);
		SVNTeamPreferences.setMergeBoolean(store, SVNTeamPreferences.MERGE_INCLUDE_MERGED_NAME, includeMergedRevisions);

		SVNTeamPreferences.setCheckoutBoolean(store, SVNTeamPreferences.CHECKOUT_USE_DOT_PROJECT_NAME,
				checkoutUsingDotProjectName);
		SVNTeamPreferences.setCheckoutBoolean(store, SVNTeamPreferences.CHECKOUT_RESPECT_PROJECT_STRUCTURE_NAME,
				checkoutRespectProjectStructure);
		SVNTeamPreferences.setCheckoutBoolean(store, SVNTeamPreferences.CHECKOUT_USE_DEFAULT_LOCATION_NAME,
				checkoutUseDefaultLocation);
		SVNTeamPreferences.setCheckoutString(store, SVNTeamPreferences.CHECKOUT_SPECIFIED_LOCATION_NAME,
				checkoutSpecifiedLocation);
	}

	@Override
	protected void loadDefaultValues(IPreferenceStore store) {
		head = SVNTeamPreferences.REPOSITORY_HEAD_DEFAULT;
		branches = SVNTeamPreferences.REPOSITORY_BRANCHES_DEFAULT;
		tags = SVNTeamPreferences.REPOSITORY_TAGS_DEFAULT;
		showExternals = SVNTeamPreferences.REPOSITORY_SHOW_EXTERNALS_DEFAULT;

		fastReport = Boolean.parseBoolean(AbstractSVNSubscriber.CONTIGOUS_REPORT_DEFAULT);
		enableModelSync = SVNTeamPreferences.ENABLE_MODEL_SYNC_DEFAULT;

		pagingEnable = SVNTeamPreferences.HISTORY_PAGING_ENABLE_DEFAULT;
		pageSize = SVNTeamPreferences.HISTORY_PAGE_SIZE_DEFAULT;
		connectToCompareWith = SVNTeamPreferences.HISTORY_CONNECT_TO_COMPARE_WITH_DEFAULT;

		dateFormat = SVNTeamPreferences.DATE_FORMAT_DEFAULT;
		dateFormatCustom = SVNTeamPreferences.DATE_FORMAT_CUSTOM_DEFAULT;

		consultChangeSets = SVNTeamPreferences.CONSULT_CHANGE_SETS_IN_COMMIT_DEFAULT;

		mailReporterEnabled = SVNTeamPreferences.MAILREPORTER_ENABLED_DEFAULT;
		mailReporterErrorsEnabled = SVNTeamPreferences.MAILREPORTER_ERRORS_ENABLED_DEFAULT;

		commitSelectNewResources = SVNTeamPreferences.BEHAVIOUR_COMMIT_SELECT_NEW_RESOURCES_DEFAULT;
		commitSelectDerivedResources = SVNTeamPreferences.BEHAVIOUR_COMMIT_SELECT_DERIVED_RESOURCES_DEFAULT;
		notSelectMissingResources = SVNTeamPreferences.BEHAVIOUR_DO_NOT_SELECT_MISSING_RESOURCES_NAME_DEFAULT;
		useSubversionExternalsBehaviour = SVNTeamPreferences.BEHAVIOUR_DO_NOT_SELECT_EXTERNAL_DEFAULT;
		showSelectedResourcesInSyncPane = SVNTeamPreferences.BEHAVIOUR_SHOW_SELECTED_RESOURCES_IN_SYNC_PANE_DEFAULT;
		treatReplacementAsEdit = SVNTeamPreferences.BEHAVIOUR_TREAT_REPLACEMENT_AS_EDIT_DEFAULT;

		useJavaHLMerge = SVNTeamPreferences.MERGE_USE_JAVAHL_DEFAULT;
		includeMergedRevisions = SVNTeamPreferences.MERGE_INCLUDE_MERGED_DEFAULT;

		checkoutUsingDotProjectName = SVNTeamPreferences.CHECKOUT_USE_DOT_PROJECT_DEFAULT;
		checkoutRespectProjectStructure = SVNTeamPreferences.CHECKOUT_RESPECT_PROJECT_STRUCTURE_DEFAULT;
		checkoutUseDefaultLocation = SVNTeamPreferences.CHECKOUT_USE_DEFAULT_LOCATION_DEFAULT;
		checkoutSpecifiedLocation = ResourcesPlugin.getWorkspace().getRoot().getLocation().toString();

		branchTagConsiderStructure = SVNTeamPreferences.BRANCH_TAG_CONSIDER_STRUCTURE_DEFAULT;
		forceExternalsFreeze = SVNTeamPreferences.REPOSITORY_FORCE_EXTERNALS_FREEZE_DEFAULT;

		computeKeywordsValues = SVNTeamPreferences.BEHAVIOUR_COMPUTE_KEYWORDS_DEFAULT;

		enableAutoShare = SVNTeamPreferences.BEHAVIOUR_ENABLE_AUTO_SHARE_DEFAULT;

		caseInsensitiveSorting = SVNTeamPreferences.BEHAVIOUR_CASE_INSENSITIVE_TABLE_SORTING_DEFAULT;

		ignoreExternals = SVNTeamPreferences.BEHAVIOUR_IGNORE_EXTERNALS_DEFAULT;

		svnConnector = SVNTeamPreferences.CORE_SVNCONNECTOR_DEFAULT;
	}

	@Override
	protected void loadValues(IPreferenceStore store) {
		head = SVNTeamPreferences.getRepositoryString(store, SVNTeamPreferences.REPOSITORY_HEAD_NAME);
		branches = SVNTeamPreferences.getRepositoryString(store, SVNTeamPreferences.REPOSITORY_BRANCHES_NAME);
		tags = SVNTeamPreferences.getRepositoryString(store, SVNTeamPreferences.REPOSITORY_TAGS_NAME);
		showExternals = SVNTeamPreferences.getRepositoryBoolean(store,
				SVNTeamPreferences.REPOSITORY_SHOW_EXTERNALS_NAME);
		simpleShare = SVNTeamPreferences.getRepositoryBoolean(store, SVNTeamPreferences.REPOSITORY_SIMPLE_SHARE_NAME);

		fastReport = AbstractSVNSubscriber.getSynchInfoContigous();
		enableModelSync = SVNTeamPreferences.getSynchronizeBoolean(store, SVNTeamPreferences.ENABLE_MODEL_SYNC_NAME);

		connectToCompareWith = SVNTeamPreferences.getHistoryBoolean(store,
				SVNTeamPreferences.HISTORY_CONNECT_TO_COMPARE_WITH_NAME);
		pagingEnable = SVNTeamPreferences.getHistoryBoolean(store, SVNTeamPreferences.HISTORY_PAGING_ENABLE_NAME);
		pageSize = SVNTeamPreferences.getHistoryInt(store, SVNTeamPreferences.HISTORY_PAGE_SIZE_NAME);

		dateFormat = SVNTeamPreferences.getDateFormatInt(store, SVNTeamPreferences.DATE_FORMAT_NAME);
		dateFormatCustom = SVNTeamPreferences.getDateFormatString(store, SVNTeamPreferences.DATE_FORMAT_CUSTOM_NAME);

		consultChangeSets = SVNTeamPreferences.getConsultChangeSetsInCommit(store,
				SVNTeamPreferences.CONSULT_CHANGE_SETS_IN_COMMIT);

		mailReporterEnabled = SVNTeamPreferences.getMailReporterBoolean(store,
				SVNTeamPreferences.MAILREPORTER_ENABLED_NAME);
		mailReporterErrorsEnabled = SVNTeamPreferences.getMailReporterBoolean(store,
				SVNTeamPreferences.MAILREPORTER_ERRORS_ENABLED_NAME);

		commitSelectNewResources = SVNTeamPreferences.getBehaviourBoolean(store,
				SVNTeamPreferences.BEHAVIOUR_COMMIT_SELECT_NEW_RESOURCES_NAME);
		commitSelectDerivedResources = SVNTeamPreferences.getBehaviourBoolean(store,
				SVNTeamPreferences.BEHAVIOUR_COMMIT_SELECT_DERIVED_RESOURCES_NAME);
		notSelectMissingResources = SVNTeamPreferences.getBehaviourBoolean(store,
				SVNTeamPreferences.BEHAVIOUR_DO_NOT_SELECT_MISSING_RESOURCES_NAME);
		useSubversionExternalsBehaviour = SVNTeamPreferences.getBehaviourBoolean(store,
				SVNTeamPreferences.BEHAVIOUR_DO_NOT_SELECT_EXTERNALS_NAME);
		showSelectedResourcesInSyncPane = SVNTeamPreferences.getBehaviourBoolean(store,
				SVNTeamPreferences.BEHAVIOUR_SHOW_SELECTED_RESOURCES_IN_SYNC_PANE_NAME);
		treatReplacementAsEdit = SVNTeamPreferences.getBehaviourBoolean(store,
				SVNTeamPreferences.BEHAVIOUR_TREAT_REPLACEMENT_AS_EDIT_NAME);
		enableAutoShare = SVNTeamPreferences.getBehaviourBoolean(store,
				SVNTeamPreferences.BEHAVIOUR_ENABLE_AUTO_SHARE_NAME);
		computeKeywordsValues = SVNTeamPreferences.getBehaviourBoolean(store,
				SVNTeamPreferences.BEHAVIOUR_COMPUTE_KEYWORDS_NAME);
		caseInsensitiveSorting = SVNTeamPreferences.getBehaviourBoolean(store,
				SVNTeamPreferences.BEHAVIOUR_CASE_INSENSITIVE_TABLE_SORTING_NAME);
		ignoreExternals = SVNTeamPreferences.getBehaviourBoolean(store,
				SVNTeamPreferences.BEHAVIOUR_IGNORE_EXTERNALS_NAME);

		useJavaHLMerge = SVNTeamPreferences.getMergeBoolean(store, SVNTeamPreferences.MERGE_USE_JAVAHL_NAME);
		includeMergedRevisions = SVNTeamPreferences.getMergeBoolean(store,
				SVNTeamPreferences.MERGE_INCLUDE_MERGED_NAME);

		checkoutUsingDotProjectName = SVNTeamPreferences.getCheckoutBoolean(store,
				SVNTeamPreferences.CHECKOUT_USE_DOT_PROJECT_NAME);
		checkoutRespectProjectStructure = SVNTeamPreferences.getCheckoutBoolean(store,
				SVNTeamPreferences.CHECKOUT_RESPECT_PROJECT_STRUCTURE_NAME);
		checkoutUseDefaultLocation = SVNTeamPreferences.getCheckoutBoolean(store,
				SVNTeamPreferences.CHECKOUT_USE_DEFAULT_LOCATION_NAME);
		checkoutSpecifiedLocation = SVNTeamPreferences.getCheckoutString(store,
				SVNTeamPreferences.CHECKOUT_SPECIFIED_LOCATION_NAME);

		branchTagConsiderStructure = SVNTeamPreferences.getRepositoryBoolean(store,
				SVNTeamPreferences.BRANCH_TAG_CONSIDER_STRUCTURE_NAME);
		forceExternalsFreeze = SVNTeamPreferences.getRepositoryBoolean(store,
				SVNTeamPreferences.REPOSITORY_FORCE_EXTERNALS_FREEZE_NAME);

		//Client specified in preferences currently may be uninstalled. So, request real used connector instead of saved.
		svnConnector = CoreExtensionsManager.instance().getSVNConnectorFactory().getId();
	}

	@Override
	protected void initializeControls() {
		headField.setText(head);
		branchesField.setText(branches);
		tagsField.setText(tags);
		showExternalsButton.setSelection(showExternals);

		fastReportButton.setSelection(fastReport);
		enableModelSyncButton.setSelection(enableModelSync);

		pageSizeField.setText(String.valueOf(pageSize));
		enablePagingButton.setSelection(pagingEnable);
		pageSizeField.setEnabled(pagingEnable);
		connectToCompareWithButton.setSelection(connectToCompareWith);

		dateFormatField.select(dateFormat);
		dateFormatCustomField.setEnabled(dateFormat == SVNTeamPreferences.DATE_FORMAT_MODE_CUSTOM);
		setDateFormatValue();

		consultCSAlwaysButton
				.setSelection(SVNTeamPreferences.CONSULT_CHANGE_SETS_IN_COMMIT_ALWAYS.equals(consultChangeSets));
		consultCSNeverButton
				.setSelection(SVNTeamPreferences.CONSULT_CHANGE_SETS_IN_COMMIT_NEVER.equals(consultChangeSets));
		consultCSPromptButton
				.setSelection(SVNTeamPreferences.CONSULT_CHANGE_SETS_IN_COMMIT_PROMPT.equals(consultChangeSets));

		mailReporterEnabledButton.setSelection(mailReporterEnabled);
		mailReporterErrorsEnabledButton.setSelection(mailReporterErrorsEnabled);

		btnResourceSelectionNew.setSelection(commitSelectNewResources);
		btnResourceSelectionDerived.setSelection(commitSelectDerivedResources);
		btnResourceNotSelectionMissing.setSelection(notSelectMissingResources);
		btnResourceSelectionExternal.setSelection(useSubversionExternalsBehaviour);
		btnResourceSelectionPresentation.setSelection(showSelectedResourcesInSyncPane);
		btnResourceSelectionTreatAsEdit.setSelection(treatReplacementAsEdit);

		List<ISVNConnectorFactory> factoriesList = Arrays.asList(factories);
		svnConnectorField.select(
				factoriesList.indexOf(CoreExtensionsManager.instance().getSVNConnectorFactory(svnConnector)));

		initializeClientSettings();

		checkoutUsingDotProjectNameButton.setSelection(checkoutUsingDotProjectName);
		checkoutRespectProjectStructureButton.setSelection(checkoutRespectProjectStructure);
		useDefaultLocationButton.setSelection(checkoutUseDefaultLocation);
		locationField.setText(checkoutSpecifiedLocation);
		locationField.setEnabled(!checkoutUseDefaultLocation);
		browse.setEnabled(!checkoutUseDefaultLocation);

		branchTagConsiderStructureButton.setSelection(branchTagConsiderStructure);
		forceExternalsFreezeButton.setSelection(forceExternalsFreeze);

		branchTagManualUrlEditButton.setSelection(!branchTagConsiderStructure);

		computeKeywordsValuesButton.setSelection(computeKeywordsValues);

		enableAutoShareButton.setSelection(enableAutoShare);

		caseInsensitiveSortingButton.setSelection(caseInsensitiveSorting);

		ignoreExternalsButton.setSelection(ignoreExternals);
	}

	protected void initializeClientSettings() {
		boolean isSVN15CompatibleConnector = CoreExtensionsManager.instance()
				.getSVNConnectorFactory(svnConnector)
				.getSVNAPIVersion() >= ISVNConnectorFactory.APICompatibility.SVNAPI_1_5_x;
		useInteractiveMergeButton.setSelection(!useJavaHLMerge);
		includeMergedRevisionsButton.setSelection(includeMergedRevisions);
		includeMergedRevisionsButton.setEnabled(isSVN15CompatibleConnector);
	}

	@Override
	protected Control createContentsImpl(Composite parent) {
		TabFolder tabFolder = new TabFolder(parent, SWT.NONE);
		tabFolder.setLayout(new TabFolderLayout());
		tabFolder.setLayoutData(new GridData(GridData.FILL_BOTH));

		TabItem tabItem = new TabItem(tabFolder, SWT.NONE);
		tabItem.setText(SVNUIMessages.MainPreferencePage_generalTabName);
		tabItem.setControl(createGeneralPage(tabFolder));

		tabItem = new TabItem(tabFolder, SWT.NONE);
		tabItem.setText(SVNUIMessages.MainPreferencePage_svnConnectorTabName);
		tabItem.setControl(createSVNConnectorsPage(tabFolder));

		tabItem = new TabItem(tabFolder, SWT.NONE);
		tabItem.setText(SVNUIMessages.MainPreferencePage_repositoryTabName);
		tabItem.setControl(createRepositorySettingsPage(tabFolder));

		tabItem = new TabItem(tabFolder, SWT.NONE);
		tabItem.setText(SVNUIMessages.MainPreferencePage_viewSettingsTabName);
		tabItem.setControl(createViewSettingsPage(tabFolder));

		tabItem = new TabItem(tabFolder, SWT.NONE);
		tabItem.setText(SVNUIMessages.MainPreferencePage_mailReporterGroupName);
		tabItem.setControl(createErrorReportingSettingsPage(tabFolder));

//		Setting context help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "org.eclipse.team.svn.help.SVNPreferencesContext"); //$NON-NLS-1$

		return tabFolder;
	}

	protected Control createSVNConnectorsPage(Composite parent) {
		GridLayout layout = null;
		GridData data = null;

		Composite composite = new Composite(parent, SWT.NONE);
		layout = new GridLayout();
		layout.numColumns = 2;
		composite.setLayout(layout);
		data = new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(data);

		Label label = new Label(composite, SWT.WRAP);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = 450;
		data.horizontalSpan = 2;
		label.setLayoutData(data);
		label.setText(SVNUIMessages.MainPreferencePage_svnConnectorPrompt);

		label = new Label(composite, SWT.NONE);
		data = new GridData();
		label.setLayoutData(data);
		label.setText(SVNUIMessages.MainPreferencePage_svnConnector);

		svnConnectorField = new Combo(composite, SWT.READ_ONLY);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = 100;
		svnConnectorField.setLayoutData(data);
		Collection<?> fullSet = CoreExtensionsManager.instance().getAccessibleClients();
		factories = fullSet.toArray(new ISVNConnectorFactory[fullSet.size()]);
		Arrays.sort(factories, (o1, o2) -> ((ISVNConnectorFactory) o1).getName().compareTo(((ISVNConnectorFactory) o2).getName()));
		String[] items = new String[fullSet.size()];
		for (int i = 0; i < items.length; i++) {
			items[i] = SVNConnectorHelper.getConnectorName(factories[i]);
		}
		svnConnectorField.setItems(items);
		svnConnectorField.setVisibleItemCount(items.length);
		svnConnectorField.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				svnConnector = factories[svnConnectorField.getSelectionIndex()].getId();
				SVNTeamPreferencesPage.this.initializeClientSettings();
			}
		});

		if (CoreExtensionsManager.instance().getAccessibleClients().isEmpty()) {
			checkForConnectorsButton = new Button(composite, SWT.PUSH);
			checkForConnectorsButton.setText(SVNUIMessages.Button_CheckForConnectors);
			data = new GridData(GridData.FILL_HORIZONTAL);
			data.widthHint = DefaultDialog.computeButtonWidth(checkForConnectorsButton);
			data.grabExcessHorizontalSpace = true;
			data.horizontalSpan = 2;
			data.horizontalAlignment = SWT.RIGHT;
			checkForConnectorsButton.setLayoutData(data);
			checkForConnectorsButton.setEnabled(true);
			checkForConnectorsButton.addListener(SWT.Selection, event -> SVNTeamUIPlugin.instance().discoveryConnectors());
		}

		// Merge settings group
		Group group = new Group(composite, SWT.NONE);
		group.setLayout(new GridLayout());
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		group.setLayoutData(data);
		group.setText(SVNUIMessages.MainPreferencePage_mergeGroupName);

		label = new Label(group, SWT.WRAP);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = 450;
		label.setLayoutData(data);
		label.setText(SVNUIMessages.MainPreferencePage_mergePrompt);

		useInteractiveMergeButton = new Button(group, SWT.CHECK);
		data = new GridData();
		useInteractiveMergeButton.setLayoutData(data);
		useInteractiveMergeButton.setText(SVNUIMessages.MainPreferencePage_mergeUseInteractiveMerge);
		useInteractiveMergeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				useJavaHLMerge = !useInteractiveMergeButton.getSelection();
			}
		});

		label = new Label(group, SWT.SEPARATOR | SWT.HORIZONTAL);
		label.setVisible(false);

		label = new Label(group, SWT.WRAP);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = 450;
		label.setLayoutData(data);
		label.setText(SVNUIMessages.MainPreferencePage_includeMergedRevisionsLabel);

		includeMergedRevisionsButton = new Button(group, SWT.CHECK);
		data = new GridData();
		includeMergedRevisionsButton.setLayoutData(data);
		includeMergedRevisionsButton.setText(SVNUIMessages.MainPreferencePage_includeMergedRevisions);
		includeMergedRevisionsButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				includeMergedRevisions = includeMergedRevisionsButton.getSelection();
			}
		});

		return composite;
	}

	protected Control createGeneralPage(Composite parent) {
		GridLayout layout = null;
		GridData data = null;

		Composite composite = new Composite(parent, SWT.NONE);
		layout = new GridLayout();
		composite.setLayout(layout);
		data = new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(data);

		// Commit settings group
		Group group = new Group(composite, SWT.NONE);
		group.setLayout(new GridLayout());
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		group.setText(SVNUIMessages.MainPreferencePage_resourceSelectionGroupName);

		Label label = new Label(group, SWT.WRAP);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = 450;
		label.setLayoutData(data);
		label.setText(SVNUIMessages.MainPreferencePage_resourceSelectionPrompt);

		btnResourceSelectionNew = new Button(group, SWT.CHECK);
		data = new GridData();
		btnResourceSelectionNew.setLayoutData(data);
		btnResourceSelectionNew.setText(SVNUIMessages.MainPreferencePage_resourceSelectionNew);
		btnResourceSelectionNew.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				commitSelectNewResources = btnResourceSelectionNew.getSelection();
			}
		});

		btnResourceSelectionDerived = new Button(group, SWT.CHECK);
		data = new GridData();
		btnResourceSelectionDerived.setLayoutData(data);
		btnResourceSelectionDerived.setText(SVNUIMessages.MainPreferencePage_resourceSelectionDerived);
		btnResourceSelectionDerived.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				commitSelectDerivedResources = btnResourceSelectionDerived.getSelection();
			}
		});

		btnResourceNotSelectionMissing = new Button(group, SWT.CHECK);
		data = new GridData();
		btnResourceNotSelectionMissing.setLayoutData(data);
		btnResourceNotSelectionMissing.setText(SVNUIMessages.MainPreferencePage_resourceSelectionMissing);
		btnResourceNotSelectionMissing.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				notSelectMissingResources = btnResourceNotSelectionMissing.getSelection();
			}
		});

		btnResourceSelectionExternal = new Button(group, SWT.CHECK);
		data = new GridData();
		btnResourceSelectionExternal.setLayoutData(data);
		btnResourceSelectionExternal.setText(SVNUIMessages.MainPreferencePage_resourceSelectionExternal);
		btnResourceSelectionExternal.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				useSubversionExternalsBehaviour = btnResourceSelectionExternal.getSelection();
			}
		});

		btnResourceSelectionPresentation = new Button(group, SWT.CHECK);
		data = new GridData();
		btnResourceSelectionPresentation.setLayoutData(data);
		btnResourceSelectionPresentation.setText(SVNUIMessages.MainPreferencePage_resourceSelectionPresentation);
		btnResourceSelectionPresentation.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				showSelectedResourcesInSyncPane = btnResourceSelectionPresentation.getSelection();
			}
		});

		btnResourceSelectionTreatAsEdit = new Button(group, SWT.CHECK);
		data = new GridData();
		btnResourceSelectionTreatAsEdit.setLayoutData(data);
		btnResourceSelectionTreatAsEdit.setText(SVNUIMessages.MainPreferencePage_resourceSelectionTreatAsEdit);
		btnResourceSelectionTreatAsEdit.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				treatReplacementAsEdit = btnResourceSelectionTreatAsEdit.getSelection();
			}
		});

		// Project share settings group
		group = new Group(composite, SWT.NONE);
		group.setLayout(new GridLayout());
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		group.setText(SVNUIMessages.MainPreferencePage_shareGroupName);

		label = new Label(group, SWT.WRAP);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = 450;
		label.setLayoutData(data);
		label.setText(SVNUIMessages.MainPreferencePage_sharePrompt);

		enableAutoShareButton = new Button(group, SWT.CHECK);
		data = new GridData();
		enableAutoShareButton.setLayoutData(data);
		enableAutoShareButton.setText(SVNUIMessages.MainPreferencePage_shareEnableAuto);
		enableAutoShareButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				enableAutoShare = enableAutoShareButton.getSelection();
			}
		});

		// Keywords property settings group
		group = new Group(composite, SWT.NONE);
		group.setLayout(new GridLayout());
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		group.setText(SVNUIMessages.MainPreferencePage_keywordsGroupName);

		label = new Label(group, SWT.WRAP);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = 450;
		label.setLayoutData(data);
		label.setText(SVNUIMessages.MainPreferencePage_keywordsPrompt);

		computeKeywordsValuesButton = new Button(group, SWT.CHECK);
		data = new GridData();
		computeKeywordsValuesButton.setLayoutData(data);
		computeKeywordsValuesButton.setText(SVNUIMessages.MainPreferencePage_keywordsComputeRecursively);
		computeKeywordsValuesButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				computeKeywordsValues = computeKeywordsValuesButton.getSelection();
			}
		});

		// Table sorting settings group
		group = new Group(composite, SWT.NONE);
		group.setLayout(new GridLayout());
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		group.setText(SVNUIMessages.MainPreferencePage_tableSortingGroupName);

		label = new Label(group, SWT.WRAP);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = 450;
		label.setLayoutData(data);
		label.setText(SVNUIMessages.MainPreferencePage_tableSortingPrompt);

		caseInsensitiveSortingButton = new Button(group, SWT.CHECK);
		data = new GridData();
		caseInsensitiveSortingButton.setLayoutData(data);
		caseInsensitiveSortingButton.setText(SVNUIMessages.MainPreferencePage_tableSortingCaseInsensitive);
		caseInsensitiveSortingButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				caseInsensitiveSorting = caseInsensitiveSortingButton.getSelection();
			}
		});

		//svn:externals settings group
		group = new Group(composite, SWT.NONE);
		group.setLayout(new GridLayout());
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		group.setText(SVNUIMessages.MainPreferencePage_externalsGroupName);

		label = new Label(group, SWT.WRAP);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = 450;
		label.setLayoutData(data);
		label.setText(SVNUIMessages.MainPreferencePage_externalsGroupPrompt);

		ignoreExternalsButton = new Button(group, SWT.CHECK);
		data = new GridData();
		ignoreExternalsButton.setLayoutData(data);
		ignoreExternalsButton.setText(SVNUIMessages.MainPreferencePage_ignoreExternals);
		ignoreExternalsButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ignoreExternals = ignoreExternalsButton.getSelection();
			}
		});

		return composite;
	}

	protected Control createViewSettingsPage(Composite parent) {
		GridLayout layout = null;
		GridData data = null;

		Composite composite = new Composite(parent, SWT.NONE);
		layout = new GridLayout();
		composite.setLayout(layout);
		data = new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(data);

		Group synchViewGroup = new Group(composite, SWT.NONE);
		layout = new GridLayout();
		synchViewGroup.setLayout(layout);
		data = new GridData(GridData.FILL_HORIZONTAL);
		synchViewGroup.setLayoutData(data);

		Group consultChangeSetsGroup = new Group(composite, SWT.FILL);
		layout = new GridLayout();
		consultChangeSetsGroup.setLayout(layout);
		data = new GridData(GridData.FILL_HORIZONTAL);
		consultChangeSetsGroup.setLayoutData(data);

		Group historyViewGroup = new Group(composite, SWT.NONE);
		layout = new GridLayout();
		layout.numColumns = 2;
		historyViewGroup.setLayout(layout);
		data = new GridData(GridData.FILL_HORIZONTAL);
		historyViewGroup.setLayoutData(data);

		Group dateFormatGroup = new Group(composite, SWT.NONE);
		layout = new GridLayout();
		layout.numColumns = 2;
		dateFormatGroup.setLayout(layout);
		data = new GridData(GridData.FILL_HORIZONTAL);
		dateFormatGroup.setLayoutData(data);

		//Synchronize View group
		synchViewGroup.setText(SVNUIMessages.MainPreferencePage_synchronizeGroupName);

		Label label = new Label(synchViewGroup, SWT.NULL);
		label.setLayoutData(new GridData());
		label.setText(SVNUIMessages.MainPreferencePage_synchronizePrompt);

		fastReportButton = new Button(synchViewGroup, SWT.CHECK);
		data = new GridData();
		fastReportButton.setLayoutData(data);
		fastReportButton.setText(SVNUIMessages.MainPreferencePage_synchronizeFastReportName);
		fastReportButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				fastReport = fastReportButton.getSelection();
			}
		});

		//show models
		enableModelSyncButton = new Button(synchViewGroup, SWT.CHECK);
		data = new GridData();
		enableModelSyncButton.setLayoutData(data);
		enableModelSyncButton.setText(SVNUIMessages.MainPreferencePage_allowModelsName);
		enableModelSyncButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				enableModelSync = enableModelSyncButton.getSelection();
			}
		});

		//Consult change sets group
		createConsultChangeSets(consultChangeSetsGroup);

		//History View group
		historyViewGroup.setText(SVNUIMessages.MainPreferencePage_historyGroupName);

		label = new Label(historyViewGroup, SWT.WRAP);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		label.setLayoutData(data);
		label.setText(SVNUIMessages.MainPreferencePage_historyPrompt);

		enablePagingButton = new Button(historyViewGroup, SWT.CHECK);
		data = new GridData();
		enablePagingButton.setLayoutData(data);
		String labelText = SVNUIMessages.MainPreferencePage_historyEnablePaging;
		enablePagingButton.setText(labelText);
		enablePagingButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (enablePagingButton.getSelection()) {
					pageSizeField.setEnabled(true);
					int tmpPageSize = Integer.parseInt(pageSizeField.getText());
					pageSize = tmpPageSize == 0 ? SVNTeamPreferences.HISTORY_PAGE_SIZE_DEFAULT : tmpPageSize;
					pageSizeField.setText(String.valueOf(pageSize));
					pagingEnable = true;
				} else {
					pagingEnable = false;
					pageSizeField.setEnabled(false);
				}
			}
		});

		pageSizeField = new Text(historyViewGroup, SWT.SINGLE | SWT.BORDER);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.grabExcessHorizontalSpace = true;
		pageSizeField.setLayoutData(data);
		CompositeVerifier verifier = new CompositeVerifier();
		verifier.add(new NonEmptyFieldVerifier(labelText));
		verifier.add(new IntegerFieldVerifier(labelText, true));
		attachTo(pageSizeField, verifier);
		pageSizeField.addModifyListener(e -> {
			try {
				pageSize = Integer.parseInt(pageSizeField.getText());
			} catch (Exception ex) {

			}
		});

		connectToCompareWithButton = new Button(historyViewGroup, SWT.CHECK);
		data = new GridData();
		data.horizontalSpan = 2;
		connectToCompareWithButton.setLayoutData(data);
		labelText = SVNUIMessages.MainPreferencePage_historyConnectToCompareWith;
		connectToCompareWithButton.setText(labelText);
		connectToCompareWithButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				connectToCompareWith = ((Button) e.widget).getSelection();
			}
		});

		// Date format group
		dateFormatGroup.setText(SVNUIMessages.MainPreferencePage_dateFormatGroupName);

		label = new Label(dateFormatGroup, SWT.WRAP);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		label.setLayoutData(data);
		labelText = SVNUIMessages.MainPreferencePage_dateFormatPrompt;
		label.setText(labelText);

		dateFormatField = new Combo(dateFormatGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
		String[] itemsDateFormat = new String[4];
		itemsDateFormat[SVNTeamPreferences.DATE_FORMAT_MODE_SHORT] = SVNUIMessages.MainPreferencePage_dateFormatShort;
		itemsDateFormat[SVNTeamPreferences.DATE_FORMAT_MODE_MEDIUM] = SVNUIMessages.MainPreferencePage_dateFormatMedium;
		itemsDateFormat[SVNTeamPreferences.DATE_FORMAT_MODE_LONG] = SVNUIMessages.MainPreferencePage_dateFormatLong;
		itemsDateFormat[SVNTeamPreferences.DATE_FORMAT_MODE_CUSTOM] = SVNUIMessages.MainPreferencePage_dateFormatCustom;
		dateFormatField.setItems(itemsDateFormat);

		data = new GridData();
		data.widthHint = 100;
		dateFormatField.setLayoutData(data);
		dateFormatField.addListener(SWT.Selection, event -> {
			dateFormat = dateFormatField.getSelectionIndex();
			if (dateFormat == SVNTeamPreferences.DATE_FORMAT_MODE_CUSTOM) {
				dateFormatCustomField.setEnabled(true);
				dateFormatCustomField.setFocus();
			} else {
				dateFormatCustomField.setEnabled(false);
			}
			SVNTeamPreferencesPage.this.setDateFormatValue();
			SVNTeamPreferencesPage.this.validateContent();
		});

		dateFormatCustomField = new Text(dateFormatGroup, SWT.SINGLE | SWT.BORDER);
		data = new GridData(GridData.FILL_HORIZONTAL);
		dateFormatCustomField.setLayoutData(data);
		verifier = new CompositeVerifier();
		verifier.add(new NonEmptyFieldVerifier(labelText));
		verifier.add(new DateFormatVerifier(labelText));
		attachTo(dateFormatCustomField, new AbstractVerifierProxy(verifier) {
			@Override
			protected boolean isVerificationEnabled(Control input) {
				return dateFormatField.getSelectionIndex() == SVNTeamPreferences.DATE_FORMAT_MODE_CUSTOM;
			}
		});
		dateFormatCustomField.addModifyListener(e -> {
			if (dateFormat == SVNTeamPreferences.DATE_FORMAT_MODE_CUSTOM) {
				dateFormatCustom = dateFormatCustomField.getText();
			}
		});

		return composite;
	}

	protected void setDateFormatValue() {
		if (dateFormat == SVNTeamPreferences.DATE_FORMAT_MODE_CUSTOM) {
			dateFormatCustomField.setText(dateFormatCustom);
			return;
		}

		//set example date
		DateFormat dateTimeFormat;
		if (dateFormat == SVNTeamPreferences.DATE_FORMAT_MODE_SHORT) {
			dateTimeFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault());
		} else if (dateFormat == SVNTeamPreferences.DATE_FORMAT_MODE_MEDIUM) {
			dateTimeFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, Locale.getDefault());
		} else if (dateFormat == SVNTeamPreferences.DATE_FORMAT_MODE_LONG) {
			dateTimeFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, Locale.getDefault());
		} else {
			dateTimeFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault());
		}

		Date exampleDate;
		try {
			exampleDate = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse("2010-01-02 15:10:12"); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (ParseException e) {
			exampleDate = new Date();
		}
		String strDate = dateTimeFormat.format(exampleDate);
		dateFormatCustomField.setText(strDate);
	}

	protected void createConsultChangeSets(Group consultChangeSetsGroup) {
		Listener changeSetsSelectionListener = event -> {
			if (consultCSAlwaysButton.getSelection()) {
				consultChangeSets = SVNTeamPreferences.CONSULT_CHANGE_SETS_IN_COMMIT_ALWAYS;
			} else if (consultCSNeverButton.getSelection()) {
				consultChangeSets = SVNTeamPreferences.CONSULT_CHANGE_SETS_IN_COMMIT_NEVER;
			} else if (consultCSPromptButton.getSelection()) {
				consultChangeSets = SVNTeamPreferences.CONSULT_CHANGE_SETS_IN_COMMIT_PROMPT;
			}
		};

		consultChangeSetsGroup.setText(SVNUIMessages.MainPreferencePage_consultChangeSetsGroupName);
		GridLayout layout = (GridLayout) consultChangeSetsGroup.getLayout();
		layout.numColumns = 3;
		layout.horizontalSpacing = 40;
		GridData data = (GridData) consultChangeSetsGroup.getLayoutData();
		data.horizontalSpan = 2;
		data.grabExcessVerticalSpace = false;

		consultCSAlwaysButton = new Button(consultChangeSetsGroup, SWT.RADIO);
		data = new GridData();
		consultCSAlwaysButton.setLayoutData(data);
		consultCSAlwaysButton.setText(SVNUIMessages.MainPreferencePage_consultChangeSetsAlways);
		consultCSAlwaysButton.addListener(SWT.Selection, changeSetsSelectionListener);

		consultCSNeverButton = new Button(consultChangeSetsGroup, SWT.RADIO);
		data = new GridData();
		consultCSNeverButton.setLayoutData(data);
		consultCSNeverButton.setText(SVNUIMessages.MainPreferencePage_consultChangeSetsNever);
		consultCSNeverButton.addListener(SWT.Selection, changeSetsSelectionListener);

		consultCSPromptButton = new Button(consultChangeSetsGroup, SWT.RADIO);
		data = new GridData();
		consultCSPromptButton.setLayoutData(data);
		consultCSPromptButton.setText(SVNUIMessages.MainPreferencePage_consultChangeSetsPrompt);
		consultCSPromptButton.addListener(SWT.Selection, changeSetsSelectionListener);
	}

	protected Control createRepositorySettingsPage(Composite parent) {
		GridLayout layout = null;
		GridData data = null;

		Composite composite = new Composite(parent, SWT.NONE);
		layout = new GridLayout();
		composite.setLayout(layout);
		data = new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(data);

		Group group = new Group(composite, SWT.NONE);
		layout = new GridLayout();
		layout.numColumns = 6;
		group.setLayout(layout);
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		group.setText(SVNUIMessages.MainPreferencePage_structureGroupName);

		Label label = new Label(group, SWT.NULL);
		data = new GridData();
		data.horizontalSpan = 6;
		label.setLayoutData(data);
		label.setText(SVNUIMessages.MainPreferencePage_repositoryPrompt);

		label = new Label(group, SWT.NULL);
		data = new GridData();
		label.setLayoutData(data);
		String labelText = SVNUIMessages.MainPreferencePage_repositoryHeadName;
		label.setText(labelText);

		headField = new Text(group, SWT.SINGLE | SWT.BORDER);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.grabExcessHorizontalSpace = true;
		headField.setLayoutData(data);
		CompositeVerifier verifier = new CompositeVerifier();
		verifier.add(new NonEmptyFieldVerifier(labelText));
		verifier.add(new ResourceNameVerifier(labelText, false));
		attachTo(headField, verifier);
		headField.addModifyListener(e -> head = headField.getText());

		label = new Label(group, SWT.NULL);
		label.setLayoutData(new GridData());
		labelText = SVNUIMessages.MainPreferencePage_repositoryBranchesName;
		label.setText(labelText);

		branchesField = new Text(group, SWT.SINGLE | SWT.BORDER);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.grabExcessHorizontalSpace = true;
		branchesField.setLayoutData(data);
		verifier = new CompositeVerifier();
		verifier.add(new NonEmptyFieldVerifier(labelText));
		verifier.add(new ResourceNameVerifier(labelText, false));
		attachTo(branchesField, verifier);
		branchesField.addModifyListener(e -> branches = branchesField.getText());

		label = new Label(group, SWT.NULL);
		label.setLayoutData(new GridData());
		labelText = SVNUIMessages.MainPreferencePage_repositoryTagsName;
		label.setText(labelText);

		tagsField = new Text(group, SWT.SINGLE | SWT.BORDER);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.grabExcessHorizontalSpace = true;
		tagsField.setLayoutData(data);
		verifier = new CompositeVerifier();
		verifier.add(new NonEmptyFieldVerifier(labelText));
		verifier.add(new ResourceNameVerifier(labelText, false));
		attachTo(tagsField, verifier);
		tagsField.addModifyListener(e -> tags = tagsField.getText());

		showExternalsButton = new Button(group, SWT.CHECK);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 6;
		showExternalsButton.setLayoutData(data);
		showExternalsButton.setText(SVNUIMessages.MainPreferencePage_showExternals);
		showExternalsButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				showExternals = ((Button) e.widget).getSelection();
			}
		});

		// Checkout settings group
		group = new Group(composite, SWT.NONE);
		layout = new GridLayout();
		layout.numColumns = 2;
		group.setLayout(layout);
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		group.setText(SVNUIMessages.MainPreferencePage_checkoutGroupName);

		label = new Label(group, SWT.WRAP);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = 450;
		data.horizontalSpan = 2;
		label.setLayoutData(data);
		label.setText(SVNUIMessages.MainPreferencePage_checkoutPrompt);

		checkoutUsingDotProjectNameButton = new Button(group, SWT.CHECK);
		data = new GridData();
		data.horizontalSpan = 2;
		checkoutUsingDotProjectNameButton.setLayoutData(data);
		checkoutUsingDotProjectNameButton.setText(SVNUIMessages.MainPreferencePage_checkoutUsingDotProjectName);
		checkoutUsingDotProjectNameButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				checkoutUsingDotProjectName = checkoutUsingDotProjectNameButton.getSelection();
			}
		});

		checkoutRespectProjectStructureButton = new Button(group, SWT.CHECK);
		data = new GridData();
		data.horizontalSpan = 2;
		checkoutRespectProjectStructureButton.setLayoutData(data);
		checkoutRespectProjectStructureButton.setText(SVNUIMessages.MainPreferencePage_checkoutRespectProjectStructure);
		checkoutRespectProjectStructureButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				checkoutRespectProjectStructure = checkoutRespectProjectStructureButton.getSelection();
			}
		});

		useDefaultLocationButton = new Button(group, SWT.CHECK);
		data = new GridData();
		data.horizontalSpan = 2;
		useDefaultLocationButton.setLayoutData(data);
		useDefaultLocationButton.setSelection(true);
		useDefaultLocationButton.setText(SVNUIMessages.ProjectLocationSelectionPage_UseDefaultLocation);
		useDefaultLocationButton.addListener(SWT.Selection, event -> {
			checkoutUseDefaultLocation = useDefaultLocationButton.getSelection();
			locationField.setEnabled(!checkoutUseDefaultLocation);
			browse.setEnabled(!checkoutUseDefaultLocation);
			SVNTeamPreferencesPage.this.validateContent();
		});

		locationField = new Text(group, SWT.SINGLE | SWT.BORDER);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = 300;
		locationField.setLayoutData(data);
		locationField.setEnabled(false);
		verifier = new CompositeVerifier();
		verifier.add(new NonEmptyFieldVerifier(SVNUIMessages.ProjectLocationSelectionPage_Location_Verifier));
		verifier.add(new ExistingResourceVerifier(SVNUIMessages.ProjectLocationSelectionPage_Location_Verifier, false));
		attachTo(locationField, new AbstractVerifierProxy(verifier) {
			@Override
			protected boolean isVerificationEnabled(Control input) {
				return !useDefaultLocationButton.getSelection();
			}
		});
		locationField.addModifyListener(e -> checkoutSpecifiedLocation = locationField.getText());

		browse = new Button(group, SWT.PUSH);
		browse.setText(SVNUIMessages.Button_Browse);
		data = new GridData();
		data.widthHint = DefaultDialog.computeButtonWidth(browse);
		browse.setLayoutData(data);
		browse.setEnabled(false);
		browse.addListener(SWT.Selection, event -> {
			DirectoryDialog fileDialog = new DirectoryDialog(getShell());
			fileDialog.setFilterPath(locationField.getText());
			String res = fileDialog.open();
			if (res != null) {
				locationField.setText(res);
			}
		});

//		Branch/tag settings group
		group = new Group(composite, SWT.NONE);
		group.setLayout(new GridLayout());
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		group.setText(SVNUIMessages.MainPreferencePage_branchTagGroupName);

		label = new Label(group, SWT.WRAP);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = 450;
		label.setLayoutData(data);
		label.setText(SVNUIMessages.MainPreferencePage_branchTagPrompt);

		SelectionListener listener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				branchTagConsiderStructure = branchTagConsiderStructureButton.getSelection();
			}
		};
		branchTagConsiderStructureButton = new Button(group, SWT.RADIO);
		branchTagConsiderStructureButton.setLayoutData(new GridData());
		branchTagConsiderStructureButton.setText(SVNUIMessages.MainPreferencePage_branchTagConsiderStructureLabel);
		branchTagConsiderStructureButton.addSelectionListener(listener);

		branchTagManualUrlEditButton = new Button(group, SWT.RADIO);
		branchTagManualUrlEditButton.setLayoutData(new GridData());
		branchTagManualUrlEditButton.setText(SVNUIMessages.MainPreferencePage_branchTagManualLabel);
		branchTagManualUrlEditButton.addSelectionListener(listener);

		label = new Label(group, SWT.HORIZONTAL | SWT.SEPARATOR);
		data = new GridData(GridData.FILL_HORIZONTAL);
		label.setLayoutData(data);

		forceExternalsFreezeButton = new Button(group, SWT.CHECK);
		data = new GridData(GridData.FILL_HORIZONTAL);
		forceExternalsFreezeButton.setLayoutData(data);
		forceExternalsFreezeButton.setText(SVNUIMessages.MainPreferencePage_forceFreezeExternals);
		forceExternalsFreezeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				forceExternalsFreeze = ((Button) e.widget).getSelection();
			}
		});

		return composite;
	}

	protected Control createErrorReportingSettingsPage(Composite parent) {
		GridLayout layout = null;
		GridData data = null;

		Composite composite = new Composite(parent, SWT.NONE);
		layout = new GridLayout();
		composite.setLayout(layout);
		data = new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(data);

		Label label = new Label(composite, SWT.WRAP);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = 450;
		label.setLayoutData(data);
		label.setText(SVNUIMessages.MainPreferencePage_mailReporterPrompt);

		mailReporterEnabledButton = new Button(composite, SWT.CHECK);
		data = new GridData();
		mailReporterEnabledButton.setLayoutData(data);
		mailReporterEnabledButton.setText(SVNUIMessages.MainPreferencePage_mailReporterEnabledName);
		mailReporterEnabledButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				mailReporterEnabled = mailReporterEnabledButton.getSelection();
			}
		});

		mailReporterErrorsEnabledButton = new Button(composite, SWT.CHECK);
		data = new GridData();
		mailReporterErrorsEnabledButton.setLayoutData(data);
		mailReporterErrorsEnabledButton.setText(SVNUIMessages.MainPreferencePage_mailReporterErrorsEnabledName);
		mailReporterErrorsEnabledButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				mailReporterErrorsEnabled = mailReporterErrorsEnabledButton.getSelection();
			}
		});

		return composite;
	}

}
