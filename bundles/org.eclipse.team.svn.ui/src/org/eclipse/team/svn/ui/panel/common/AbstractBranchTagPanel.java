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

package org.eclipse.team.svn.ui.panel.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IRepositoryRoot;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.SVNUtility;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.composite.CommentComposite;
import org.eclipse.team.svn.ui.composite.ResourceSelectionComposite;
import org.eclipse.team.svn.ui.composite.RevisionComposite;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.panel.AbstractDialogPanel;
import org.eclipse.team.svn.ui.panel.participant.BasePaneParticipant;
import org.eclipse.team.svn.ui.panel.participant.PaneParticipantHelper;
import org.eclipse.team.svn.ui.panel.participant.PaneParticipantHelper.PaneVerifier;
import org.eclipse.team.svn.ui.preferences.SVNTeamPreferences;
import org.eclipse.team.svn.ui.synchronize.AbstractSynchronizeActionGroup;
import org.eclipse.team.svn.ui.utility.UserInputHistory;
import org.eclipse.team.svn.ui.verifier.AbsolutePathVerifier;
import org.eclipse.team.svn.ui.verifier.AbstractVerifier;
import org.eclipse.team.svn.ui.verifier.CompositeVerifier;
import org.eclipse.team.svn.ui.verifier.NonEmptyFieldVerifier;
import org.eclipse.team.svn.ui.verifier.URLVerifier;
import org.eclipse.team.ui.synchronize.ResourceScope;

/**
 * Abstract Branch/Tag panel
 * 
 * @author Alexander Gurov
 */
public abstract class AbstractBranchTagPanel extends AbstractDialogPanel {
	protected Button startWithCheck;

	protected Button freezeExternalsCheck;

	protected Combo destinationCombo;

	protected Combo branchingModeCombo;

	protected UserInputHistory resourceNameHistory;

	protected CommentComposite comment;

	protected String destinationUrl;

	protected IRepositoryRoot root;

	protected String nationalizationId;

	protected boolean startsWith;

	protected boolean freezeExternals;

	protected Set existingNodesNamesSet;

	protected boolean considerStructure;

	protected String historyName;

	protected int creationMode;

	protected ResourceSelectionComposite resourceSelection;

	protected IResource[] newResources;

	protected boolean disableSwitch;

	//used for revision selection for repository resources
	protected IRepositoryResource[] selectedRemoteResources;

	protected RevisionComposite revisionComposite;

	protected PaneParticipantHelper paneParticipantHelper;

	/*
	 * As participant pane is not always present, we need to use this flag
	 * (instead of paneParticipantHelper.isParticipantPane())
	 * to determine whether we can work with pane or not.
	 * (E.g. participant pane is not present if there are no new resources)
	 */
	protected boolean hasParticipantPane;

	public AbstractBranchTagPanel(IRepositoryRoot root, boolean showStartsWith, Set existingNames,
			String nationalizationId, String historyName, IRepositoryResource[] selectedRemoteResources) {
		this(root, showStartsWith, existingNames, nationalizationId, historyName, new IResource[0],
				selectedRemoteResources);
	}

	public AbstractBranchTagPanel(IRepositoryRoot root, boolean showStartsWith, Set existingNames,
			String nationalizationId, String historyName, IResource[] resources,
			IRepositoryResource[] selectedRemoteResources) {
		this.nationalizationId = nationalizationId;
		this.historyName = historyName;
		this.selectedRemoteResources = selectedRemoteResources;

		newResources = FileUtility.getResourcesRecursive(resources, IStateFilter.SF_NEW, IResource.DEPTH_INFINITE);
		disableSwitch = FileUtility.checkForResourcesPresence(resources, new IStateFilter.AbstractStateFilter() {
			@Override
			protected boolean acceptImpl(ILocalResource local, IResource resource, String state, int mask) {
				return state == IStateFilter.ST_ADDED;
			}

			@Override
			protected boolean allowsRecursionImpl(ILocalResource local, IResource resource, String state, int mask) {
				return true;
			}
		}, IResource.DEPTH_INFINITE);

		dialogTitle = SVNUIMessages.getString(this.nationalizationId + "_Title"); //$NON-NLS-1$
		dialogDescription = SVNUIMessages.getString(this.nationalizationId + "_Description"); //$NON-NLS-1$
		if (SVNTeamPreferences.getRepositoryBoolean(SVNTeamUIPlugin.instance().getPreferenceStore(),
				SVNTeamPreferences.BRANCH_TAG_CONSIDER_STRUCTURE_NAME)) {
			defaultMessage = SVNUIMessages.getString(this.nationalizationId + "_MessageAuto"); //$NON-NLS-1$
		} else {
			defaultMessage = SVNUIMessages.getString(this.nationalizationId + "_Message"); //$NON-NLS-1$
		}
		if (!showStartsWith) {
			defaultMessage += " " + SVNUIMessages.AbstractBranchTagPanel_Message; //$NON-NLS-1$
		}

		existingNodesNamesSet = existingNames;
		this.root = root;
		startsWith = showStartsWith;
		considerStructure = root.getRepositoryLocation().isStructureEnabled()
				&& SVNTeamPreferences.getRepositoryBoolean(SVNTeamUIPlugin.instance().getPreferenceStore(),
						SVNTeamPreferences.BRANCH_TAG_CONSIDER_STRUCTURE_NAME);

		paneParticipantHelper = new PaneParticipantHelper();
	}

	public SVNRevision getRevisionForRemoteResources() {
		return revisionComposite != null ? revisionComposite.getSelectedRevision() : null;
	}

	public IResource[] getSelectedResources() {
		if (hasParticipantPane) {
			return paneParticipantHelper.getSelectedResources();
		} else {
			return resourceSelection == null ? new IResource[0] : resourceSelection.getSelectedResources();
		}
	}

	public IResource[] getNotSelectedResources() {
		if (hasParticipantPane) {
			return paneParticipantHelper.getNotSelectedResources();
		} else {
			return resourceSelection == null ? new IResource[0] : resourceSelection.getNotSelectedResources();
		}
	}

	public IResource[] getTreatAsEdits() {
		return paneParticipantHelper.isParticipantPane() ? new IResource[0] : resourceSelection.getTreatAsEdits();
	}

	public boolean isFreezeExternals() {
		return freezeExternals;
	}

	public String getMessage() {
		return comment.getMessage();
	}

	public int getCreationMode() {
		return creationMode;
	}

	public IRepositoryResource getDestination() {
		destinationUrl = destinationUrl.trim();
		while (destinationUrl.endsWith("/") || destinationUrl.endsWith("\\")) { //$NON-NLS-1$ //$NON-NLS-2$
			destinationUrl = destinationUrl.substring(0, destinationUrl.length() - 1);
		}
		return root.getRepositoryLocation().asRepositoryContainer(destinationUrl, false);
	}

	public boolean isStartWithSelected() {
		return startsWith;
	}

	@Override
	public Point getPrefferedSizeImpl() {
		return new Point(newResources != null && newResources.length > 0 ? 625 : 525, SWT.DEFAULT);
	}

	@Override
	public void postInit() {
		super.postInit();
		comment.postInit(manager);
		if (hasParticipantPane) {
			paneParticipantHelper.expandPaneTree();
		}
	}

	@Override
	public void createControlsImpl(Composite parent) {
		GridData data = null;

		GridLayout layout = new GridLayout();
		Composite select = null;
		String substitutionUppercase = SVNUIMessages.getString(nationalizationId + "_NodeName"); //$NON-NLS-1$
		if (startsWith) {
			select = new Group(parent, SWT.NULL);
			layout.numColumns = 2;
			((Group) select).setText(considerStructure
					? substitutionUppercase
					: SVNUIMessages.getString(nationalizationId + "_Location_Group")); //$NON-NLS-1$
		} else {
			select = new Composite(parent, SWT.NONE);
			layout.marginHeight = 0;
			layout.marginWidth = 0;
			layout.numColumns = 3;
		}
		select.setLayout(layout);
		select.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		if (!startsWith) {
			Label description = new Label(select, SWT.NONE);
			data = new GridData();
			description.setLayoutData(data);
			description.setText(considerStructure
					? substitutionUppercase
					: SVNUIMessages.getString(nationalizationId + "_Location_Field")); //$NON-NLS-1$
		}
		createTopPart(select, substitutionUppercase);

		if (startsWith) {
			Composite inner = new Composite(select, SWT.NONE);
			data = new GridData(GridData.FILL_HORIZONTAL);
			inner.setLayoutData(data);
			layout = new GridLayout();
			layout.marginHeight = layout.marginWidth = 0;
			inner.setLayout(layout);

			startWithCheck = new Button(inner, SWT.CHECK);
			data = new GridData(GridData.FILL_HORIZONTAL);
			startWithCheck.setLayoutData(data);
			startWithCheck.setText(SVNUIMessages.getString(nationalizationId + "_StartsWith")); //$NON-NLS-1$
			startWithCheck.setSelection(false);
			startWithCheck.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					AbstractBranchTagPanel.this.validateContent();
				}
			});

			freezeExternalsCheck = new Button(inner, SWT.CHECK);
			data = new GridData(GridData.FILL_HORIZONTAL);
			freezeExternalsCheck.setLayoutData(data);
			freezeExternalsCheck.setText(SVNUIMessages.getString(nationalizationId + "_FreezeExternals")); //$NON-NLS-1$
			freezeExternalsCheck.setSelection(false);
		}

		//revision selection
		IRepositoryResource selectedRemoteResource = root;
		if (selectedRemoteResources.length == 1) {
			selectedRemoteResource = selectedRemoteResources[0];
		} else if (selectedRemoteResources.length > 1) {
			selectedRemoteResource = selectedRemoteResources[0].getRoot();
		} else if (root.getKind() != IRepositoryRoot.KIND_LOCATION_ROOT
				&& root.getKind() != IRepositoryRoot.KIND_ROOT) {
			selectedRemoteResource = root.getParent();
			selectedRemoteResource.setPegRevision(root.getPegRevision());
			selectedRemoteResource.setSelectedRevision(root.getSelectedRevision());
		}
		revisionComposite = new RevisionComposite(parent, this, false,
				new String[] { SVNUIMessages.RevisionComposite_Revision, SVNUIMessages.RevisionComposite_HeadRevision },
				SVNRevision.HEAD, false);
		layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		data = new GridData(GridData.FILL_HORIZONTAL);
		revisionComposite.setLayout(layout);
		revisionComposite.setLayoutData(data);
		revisionComposite.setSelectedResource(selectedRemoteResource);
		if (startsWith) {
			revisionComposite.setEnabled(SVNTeamPreferences.getDialogInt(
					SVNTeamUIPlugin.instance().getPreferenceStore(),
					SVNTeamPreferences.BRANCH_TAG_CREATION_MODE) == SVNTeamPreferences.CREATION_MODE_REPOSITORY);
		}

		SashForm splitter = new SashForm(parent, SWT.VERTICAL);
		data = new GridData(GridData.FILL_BOTH);
		splitter.setLayoutData(data);
		layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		layout.verticalSpacing = 3;
		splitter.setLayout(layout);

		Group group = new Group(splitter, SWT.NULL);
		group.setLayout(new GridLayout());
		data = new GridData(GridData.FILL_BOTH);
		group.setLayoutData(data);
		group.setText(SVNUIMessages.getString(nationalizationId + "_Comment")); //$NON-NLS-1$

		comment = new CommentComposite(group, this);
		data = new GridData(GridData.FILL_BOTH);
		comment.setLayoutData(data);

		if (startsWith && newResources != null && newResources.length > 0) {
			if (paneParticipantHelper.isParticipantPane()) {
				hasParticipantPane = true;
				paneParticipantHelper.init(createPaneParticipant());
				createPaneControls(splitter);
			} else {
				createResourceSelectionCompositeControls(splitter);
			}
			splitter.setWeights(new int[] { 1, 1 });
		} else {
			splitter.setWeights(new int[] { 1 });
		}
	}

	protected void createPaneControls(Composite parent) {
		Control paneControl = paneParticipantHelper.createChangesPage(parent);
		GridData data = new GridData(GridData.FILL_BOTH);
		paneControl.setLayoutData(data);

		paneParticipantHelper.initListeners();

		//add validator to pane
		attachTo(paneControl, new PanelPaneVerifier(paneParticipantHelper));
	}

	protected BasePaneParticipant createPaneParticipant() {
		return new BasePaneParticipant(new ResourceScope(newResources), this) {
			@Override
			protected Collection<AbstractSynchronizeActionGroup> getActionGroups() {
				Collection<AbstractSynchronizeActionGroup> actionGroups = new ArrayList<>();
				actionGroups.add(new BasePaneActionGroup(validationManager));
				return actionGroups;
			}
		};
	}

	protected void createResourceSelectionCompositeControls(Composite parent) {
		resourceSelection = new ResourceSelectionComposite(parent, SWT.NONE, newResources, true, true);
		GridData data = new GridData(GridData.FILL_BOTH);
		resourceSelection.setLayoutData(data);
		resourceSelection.addResourcesSelectionChangedListener(event -> AbstractBranchTagPanel.this.validateContent());
		attachTo(resourceSelection, new AbstractVerifier() {
			@Override
			protected String getWarningMessage(Control input) {
				IResource[] resources = resourceSelection.getSelectedResources();
				if ((resources != null && resources.length != 0 || disableSwitch) && startWithCheck.getSelection()) {
					return AbstractBranchTagPanel.this.defaultMessage + " " //$NON-NLS-1$
							+ SVNUIMessages.getString(nationalizationId + "_Warning"); //$NON-NLS-1$
				}
				return null;
			}

			@Override
			protected String getErrorMessage(Control input) {
				return null;
			}
		});
	}

	protected Composite createTopPart(Composite select, final String substitutionUppercase) {
		destinationUrl = root.getUrl();

		destinationCombo = new Combo(select, SWT.BORDER);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		destinationCombo.setLayoutData(data);

		Button browse = new Button(select, SWT.PUSH);
		browse.setText(SVNUIMessages.Button_Browse);
		data = new GridData();
		data.widthHint = DefaultDialog.computeButtonWidth(browse);
		browse.setLayoutData(data);

		if (startsWith) {
			branchingModeCombo = new Combo(select, SWT.BORDER | SWT.READ_ONLY);
			data = new GridData(GridData.FILL_HORIZONTAL);
			data.horizontalSpan = 2;
			branchingModeCombo.setLayoutData(data);
			branchingModeCombo.setItems(SVNUIMessages.AbstractBranchTagPanel_CreationMode_AsIs,
					SVNUIMessages.AbstractBranchTagPanel_CreationMode_CheckRevision,
					SVNUIMessages.AbstractBranchTagPanel_CreationMode_DoUpdate,
					SVNUIMessages.AbstractBranchTagPanel_CreationMode_Repository);
			branchingModeCombo.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					AbstractBranchTagPanel.this.creationModeChanged(branchingModeCombo.getSelectionIndex());
				}
			});
			branchingModeCombo.select(creationMode = SVNTeamPreferences.getDialogInt(
					SVNTeamUIPlugin.instance().getPreferenceStore(), SVNTeamPreferences.BRANCH_TAG_CREATION_MODE));
		}

		CompositeVerifier verifier = new CompositeVerifier();

		if (!considerStructure) {
			resourceNameHistory = new UserInputHistory(historyName);
			destinationCombo.setText(destinationUrl);
			String name = SVNUIMessages.getString(nationalizationId + "_Location_Verifier"); //$NON-NLS-1$
			verifier.add(new URLVerifier(name));
			verifier.add(new AbsolutePathVerifier(name));
			verifier.add(new AbstractVerifier() {
				@Override
				protected String getErrorMessage(Control input) {
					String url = root.getRepositoryLocation().getUrl();
					if (!destinationCombo.getText().startsWith(url)) {
						return BaseMessages.format(
								SVNUIMessages.getString(nationalizationId + "_Location_Verifier_DoesNotCorresponds"), //$NON-NLS-1$
								new String[] { destinationCombo.getText(), url });
					}
					if (startsWith) {
						if (!destinationCombo.getText().startsWith(root.getUrl())) {
							startWithCheck.setSelection(false);
							startWithCheck.setEnabled(false);
						} else {
							startWithCheck.setEnabled(true);
						}
					}
					if (root.getUrl().equals(SVNUtility.normalizeURL(destinationCombo.getText()))) {
						return SVNUIMessages.getString(
								nationalizationId + "_Location_Verifier_NoTagName"); //$NON-NLS-1$
					}
					return null;
				}

				@Override
				protected String getWarningMessage(Control input) {
					return null;
				}
			});
			browse.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					RepositoryTreePanel panel = new RepositoryTreePanel(SVNUIMessages.getString(
							nationalizationId + "_SelectionProposal"), //$NON-NLS-1$
							SVNUIMessages.RepositoryBrowsingPanel_Description,
							SVNUIMessages.RepositoryBrowsingPanel_Message, null, true, root.getRepositoryLocation(),
							false);
					DefaultDialog browser = new DefaultDialog(AbstractBranchTagPanel.this.manager.getShell(), panel);
					if (browser.open() == 0) {
						IRepositoryResource selected = panel.getSelectedResource();
						if (selected != null) {
							destinationCombo.setText(selected.getUrl());
						}
						AbstractBranchTagPanel.this.validateContent();
					}
				}
			});
		} else {
			resourceNameHistory = new UserInputHistory(historyName + "Name"); //$NON-NLS-1$

			String name = SVNUIMessages.getString(nationalizationId + "_NodeName_Verifier"); //$NON-NLS-1$
			verifier.add(new NonEmptyFieldVerifier(name) {
				@Override
				protected String getErrorMessageImpl(Control input) {
					String msg = super.getErrorMessageImpl(input);
					if (msg == null) {
						if (new Path(getText(input)).segmentCount() == 0) {
							return NonEmptyFieldVerifier.ERROR_MESSAGE;
						}
					}
					return msg;
				}
			});
			verifier.add(new AbsolutePathVerifier(name));
			verifier.add(new AbstractVerifier() {
				@Override
				protected String getErrorMessage(Control input) {
					return null;
				}

				@Override
				protected String getWarningMessage(Control input) {
					String name = destinationCombo.getText();
					if (existingNodesNamesSet != null && existingNodesNamesSet.contains(name)) {
						return BaseMessages.format(SVNUIMessages.getString(
								nationalizationId + "_NodeName_Verifier_Error_Exists"), //$NON-NLS-1$
								new String[] { name });
					}
					return null;
				}
			});
			browse.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					RepositoryTreePanel panel = new RepositoryTreePanel(SVNUIMessages.getString(
							nationalizationId + "_SelectionProposal"), //$NON-NLS-1$
							SVNUIMessages.RepositoryBrowsingPanel_Description,
							SVNUIMessages.RepositoryBrowsingPanel_Message, null, true, root.getRoot(), false);
					DefaultDialog browser = new DefaultDialog(AbstractBranchTagPanel.this.manager.getShell(), panel);
					if (browser.open() == 0) {
						IRepositoryResource selected = panel.getSelectedResource();
						if (selected != null) {
							destinationCombo.setText(selected.getUrl().substring(root.getUrl().length() + 1));
						}
						AbstractBranchTagPanel.this.validateContent();
					}
				}
			});
		}
		destinationCombo.setVisibleItemCount(resourceNameHistory.getDepth());
		destinationCombo.setItems(resourceNameHistory.getHistory());

		attachTo(destinationCombo, verifier);

		return select;
	}

	protected void creationModeChanged(int creationMode) {
		if (creationMode == SVNTeamPreferences.CREATION_MODE_REPOSITORY) {
			freezeExternalsCheck.setSelection(false);
		}
		freezeExternalsCheck.setEnabled(creationMode != SVNTeamPreferences.CREATION_MODE_REPOSITORY);
		revisionComposite.setEnabled(creationMode == SVNTeamPreferences.CREATION_MODE_REPOSITORY);
	}

	@Override
	protected void saveChangesImpl() {
		if (!considerStructure) {
			destinationUrl = destinationCombo.getText();
			resourceNameHistory.addLine(destinationUrl);
		} else {
			destinationUrl = destinationUrl + "/" + destinationCombo.getText(); //$NON-NLS-1$
			resourceNameHistory.addLine(destinationCombo.getText());
		}

		comment.saveChanges();
		if (startWithCheck != null) {
			startsWith = startWithCheck.getSelection();
			freezeExternals = freezeExternalsCheck.getSelection();
			SVNTeamPreferences.setDialogInt(SVNTeamUIPlugin.instance().getPreferenceStore(),
					SVNTeamPreferences.BRANCH_TAG_CREATION_MODE, creationMode = branchingModeCombo.getSelectionIndex());
		} else {
			startsWith = false;
			freezeExternals = false;
		}
	}

	@Override
	protected void cancelChangesImpl() {
		comment.cancelChanges();
	}

	@Override
	public void dispose() {
		super.dispose();
		if (hasParticipantPane) {
			paneParticipantHelper.dispose();
		}
	}

	/*
	 * Pane validator
	 */
	protected class PanelPaneVerifier extends PaneVerifier {

		public PanelPaneVerifier(PaneParticipantHelper paneParticipantHelper) {
			super(paneParticipantHelper);
		}

		@Override
		protected String getErrorMessage(Control input) {
			return null;
		}

		@Override
		protected String getWarningMessage(Control input) {
			IResource[] resourcesToProcess = paneParticipantHelper.getSelectedResources();

			if ((resourcesToProcess.length == 0 || disableSwitch) && startWithCheck.getSelection()) {
				return AbstractBranchTagPanel.this.defaultMessage + " " //$NON-NLS-1$
						+ SVNUIMessages.getString(nationalizationId + "_Warning"); //$NON-NLS-1$
			}
			return null;
		}
	}

}
