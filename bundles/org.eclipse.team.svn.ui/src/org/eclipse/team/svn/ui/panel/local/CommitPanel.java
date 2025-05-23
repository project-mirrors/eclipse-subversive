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
 *    Alessandro Nistico - [patch] Change Set's implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.panel.local;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.SVNProperty;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.extension.CoreExtensionsManager;
import org.eclipse.team.svn.core.extension.factory.ISVNConnectorFactory;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.local.AddToSVNIgnoreOperation;
import org.eclipse.team.svn.core.operation.local.CreatePatchOperation;
import org.eclipse.team.svn.core.operation.local.ExportOperation;
import org.eclipse.team.svn.core.operation.local.MarkAsMergedOperation;
import org.eclipse.team.svn.core.operation.local.RefreshResourcesOperation;
import org.eclipse.team.svn.core.operation.local.RestoreProjectMetaOperation;
import org.eclipse.team.svn.core.operation.local.SaveProjectMetaOperation;
import org.eclipse.team.svn.core.operation.local.management.CleanupOperation;
import org.eclipse.team.svn.core.operation.local.property.GetPropertiesOperation;
import org.eclipse.team.svn.core.operation.local.refactor.DeleteResourceOperation;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.ISVNStorage;
import org.eclipse.team.svn.core.resource.events.IResourceStatesListener;
import org.eclipse.team.svn.core.resource.events.ResourceStatesChangedEvent;
import org.eclipse.team.svn.core.svnstorage.ResourcesParentsProvider;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.ProgressMonitorUtility;
import org.eclipse.team.svn.core.utility.SVNUtility;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.action.local.AddToSVNIgnoreAction;
import org.eclipse.team.svn.ui.action.local.BranchTagAction;
import org.eclipse.team.svn.ui.action.local.CompareWithWorkingCopyAction;
import org.eclipse.team.svn.ui.action.local.ReplaceWithLatestRevisionAction;
import org.eclipse.team.svn.ui.action.local.ReplaceWithRevisionAction;
import org.eclipse.team.svn.ui.action.local.RevertAction;
import org.eclipse.team.svn.ui.composite.CommentComposite;
import org.eclipse.team.svn.ui.composite.ResourceSelectionComposite;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.dialog.DiscardConfirmationDialog;
import org.eclipse.team.svn.ui.event.IResourceSelectionChangeListener;
import org.eclipse.team.svn.ui.event.ResourceSelectionChangedEvent;
import org.eclipse.team.svn.ui.extension.factory.IModifiableCommentDialogPanel;
import org.eclipse.team.svn.ui.operation.CompareResourcesOperation;
import org.eclipse.team.svn.ui.operation.ShowConflictEditorOperation;
import org.eclipse.team.svn.ui.operation.ShowPostCommitErrorsOperation;
import org.eclipse.team.svn.ui.panel.common.CommentPanel;
import org.eclipse.team.svn.ui.panel.participant.CommitPaneParticipant;
import org.eclipse.team.svn.ui.panel.participant.CommitPaneParticipantHelper;
import org.eclipse.team.svn.ui.panel.participant.CommitPaneParticipantHelper.CommitPaneVerifier;
import org.eclipse.team.svn.ui.panel.remote.ComparePanel;
import org.eclipse.team.svn.ui.preferences.SVNTeamPreferences;
import org.eclipse.team.svn.ui.properties.bugtraq.BugtraqModel;
import org.eclipse.team.svn.ui.propfind.BugtraqPropFindVisitor;
import org.eclipse.team.svn.ui.propfind.CompositePropFindVisitor;
import org.eclipse.team.svn.ui.propfind.IPropFindVisitor;
import org.eclipse.team.svn.ui.propfind.LogTemplatesPropFindVisitor;
import org.eclipse.team.svn.ui.propfind.MaxLogWidthPropFindVisitor;
import org.eclipse.team.svn.ui.propfind.MinLockSizePropFindVisitor;
import org.eclipse.team.svn.ui.propfind.MinLogSizePropFindVisitor;
import org.eclipse.team.svn.ui.utility.LockProposeUtility;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;
import org.eclipse.team.svn.ui.verifier.AbstractVerifier;
import org.eclipse.team.ui.synchronize.ResourceScope;
import org.eclipse.ui.IWorkbenchActionConstants;

/**
 * Commit panel
 * 
 * @author Sergiy Logvin
 */
public class CommitPanel extends CommentPanel implements IModifiableCommentDialogPanel {
	public static final int MSG_COMMIT = 0;

	public static final int MSG_OVER_AND_COMMIT = 1;

	public static final int MAXIMUM_CHECKS_SIZE = 100;

	protected ResourceSelectionComposite selectionComposite;

	protected Button pasteNamesButton;

	protected CommitPaneParticipantHelper paneParticipantHelper;

	/*
	 * Difference between resources and userSelectedResources:
	 * Example: select src folder in Package Explorer
	 * 	resources: [L/SVNProject/src/existing/otherpack/CC.java, L/SVNProject/src/existing/ZA.java]
	 *  userSelectedResources: [F/SVNProject/src]; also it can be null
	 */
	protected IResource[] resources;

	protected IResource[] userSelectedResources;

	protected Button keepLocksButton;

	protected SashForm sForm;

	protected boolean keepLocks;

	protected List<IResourceSelectionChangeListener> changeListenerList;

	protected int minLogSize;

	protected int maxLogWidth;

	protected final String proposedComment;

	protected IResourceStatesListener resourceStatesListener;

	protected boolean resourcesChanged;

	public CommitPanel(IResource[] resources, int msgType) {
		this(resources, msgType, null);
	}

	public CommitPanel(IResource[] resources, int msgType, String proposedComment) {
		this(resources, null, msgType, proposedComment);
	}

	public CommitPanel(IResource[] resources, IResource[] userSelectedResources, int msgType, String proposedComment) {
		super(SVNUIMessages.CommitPanel_Title);
		this.proposedComment = proposedComment;
		this.resources = resources;
		if (msgType == CommitPanel.MSG_OVER_AND_COMMIT) {
			defaultMessage = SVNUIMessages.CommitPanel_Message;
			dialogDescription = SVNUIMessages.CommitPanel_Description;
		}
		changeListenerList = new ArrayList<>();
		this.userSelectedResources = userSelectedResources;

		paneParticipantHelper = new CommitPaneParticipantHelper();
	}

	@Override
	public void createControlsImpl(Composite parent) {
		GridData data = null;
		GridLayout layout = null;

		layout = (GridLayout) parent.getLayout();
		layout.marginHeight = 3;

		sForm = new SashForm(parent, SWT.VERTICAL);
		layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		layout.verticalSpacing = 3;
		sForm.setLayout(layout);
		data = new GridData(GridData.FILL_BOTH);
		data.heightHint = 400;
		sForm.setLayoutData(data);

		Composite composite = new Composite(sForm, SWT.NONE);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		layout = new GridLayout();
		layout.verticalSpacing = 4;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		composite.setLayout(layout);

		Group group = new Group(composite, SWT.NULL);
		layout = new GridLayout();
		group.setLayout(layout);
		data = new GridData(GridData.FILL_BOTH);
		group.setLayoutData(data);
		group.setText(SVNUIMessages.CommitPanel_Comment);

		CommitPanel.CollectPropertiesOperation op = new CollectPropertiesOperation(resources);
		UIMonitorUtility.doTaskNowDefault(op, true);

		bugtraqModel = op.getBugtraqModel();
		minLogSize = op.getMinLogSize();
		maxLogWidth = op.getMaxLogWidth();
		comment = new CommentComposite(group, proposedComment, this, op.getLogTemplates(), bugtraqModel, minLogSize,
				maxLogWidth);
		data = new GridData(GridData.FILL_BOTH);
		comment.setLayoutData(data);

		Composite middleComposite = new Composite(composite, SWT.NULL);
		data = new GridData(GridData.FILL_HORIZONTAL);
		layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 2;
		layout.numColumns = 2;
		middleComposite.setLayoutData(data);
		middleComposite.setLayout(layout);

		keepLocks = SVNTeamUIPlugin.instance()
				.getPreferenceStore()
				.getBoolean(SVNTeamPreferences.COMMIT_DIALOG_KEEP_LOCKS);
		keepLocksButton = new Button(middleComposite, SWT.CHECK);
		data = new GridData(GridData.BEGINNING | GridData.FILL_HORIZONTAL);
		keepLocksButton.setLayoutData(data);
		keepLocksButton.setText(SVNUIMessages.CommitPanel_KeepLocks);
		keepLocksButton.setSelection(false);
		keepLocksButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				keepLocks = keepLocksButton.getSelection();
			}
		});
		keepLocksButton.setSelection(keepLocks);

		pasteNamesButton = new Button(middleComposite, SWT.PUSH | SWT.END);
		data = new GridData();
		pasteNamesButton.setLayoutData(data);
		pasteNamesButton.setText(SVNUIMessages.CommitPanel_PasteNames_Button);
		pasteNamesButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				CommitPanel.this.pasteNames();
			}
		});

		Label separator = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
		separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		if (paneParticipantHelper.isParticipantPane()) {
			CommitPaneParticipant participant = new CommitPaneParticipant(new ResourceScope(resources), this);
			paneParticipantHelper.init(participant);
			createPaneControls();
		} else {
			createResourceSelectionCompositeControls();
		}
	}

	protected void createResourceSelectionCompositeControls() {
		selectionComposite = new ResourceSelectionComposite(sForm, SWT.NONE, resources, true, userSelectedResources,
				true);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.heightHint = 175;
		selectionComposite.setLayoutData(data);

		IPreferenceStore store = SVNTeamUIPlugin.instance().getPreferenceStore();
		int first = SVNTeamPreferences.getDialogInt(store, SVNTeamPreferences.COMMIT_DIALOG_WEIGHT_NAME);
		if (first < 20 || first > 80) // limit saved the value range to something acceptable, since there could be workspace crashes etc. (see bug 375038)
		{
			first = 50;
		}
		sForm.setWeights(new int[] { first, 100 - first });

		selectionComposite.addResourcesSelectionChangedListener(CommitPanel.this::fireResourcesSelectionChanged);
		attachTo(selectionComposite, new AbstractVerifier() {
			@Override
			protected String getErrorMessage(Control input) {
				IResource[] selection = CommitPanel.this.getSelectedResources();
				if (selection == null || selection.length == 0) {
					return SVNUIMessages.ResourceSelectionComposite_Verifier_Error;
				}
				if (FileUtility.checkForResourcesPresenceRecursive(selection, new IStateFilter.OrStateFilter(
						new IStateFilter[] { IStateFilter.SF_CONFLICTING, IStateFilter.SF_TREE_CONFLICTING }))) {
					return SVNUIMessages.CommitPanel_Conflicting_Error;
				}
				return null;
			}

			@Override
			protected String getWarningMessage(Control input) {
				String message = null;
				IResource[] selection = CommitPanel.this.getSelectedResources();
				if (selection != null && selection.length > 0) {
					message = CommitPanel.validateResources(selection);
				}
				return message;
			}
		});
		addContextMenu();
	}

	protected void createPaneControls() {
		Composite paneComposite = new Composite(sForm, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		paneComposite.setLayout(layout);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.grabExcessHorizontalSpace = true;
		data.grabExcessVerticalSpace = true;
		data.heightHint = 175;
		paneComposite.setLayoutData(data);

		IPreferenceStore store = SVNTeamUIPlugin.instance().getPreferenceStore();
		int first = SVNTeamPreferences.getDialogInt(store, SVNTeamPreferences.COMMIT_DIALOG_WEIGHT_NAME);
		sForm.setWeights(new int[] { first, 100 - first });

		Control paneControl = paneParticipantHelper.createChangesPage(paneComposite);
		data = new GridData(GridData.FILL_BOTH);
		paneControl.setLayoutData(data);

		paneParticipantHelper.initListeners();

		//add validator to pane
		attachTo(paneComposite, new CommitPaneVerifier(paneParticipantHelper));
	}

	@Override
	public String getHelpId() {
		return "org.eclipse.team.svn.help.commitDialogContext"; //$NON-NLS-1$
	}

	@Override
	public void postInit() {
		super.postInit();

		resourceStatesListener = CommitPanel.this::updateResources;
		SVNRemoteStorage.instance()
				.addResourceStatesListener(ResourceStatesChangedEvent.class, CommitPanel.this.resourceStatesListener);

		if (paneParticipantHelper.isParticipantPane()) {
			paneParticipantHelper.expandPaneTree();
		}
	}

	@Override
	protected void saveChangesImpl() {
		super.saveChangesImpl();
		savePreferences();
	}

	@Override
	protected void cancelChangesImpl() {
		super.cancelChangesImpl();
		savePreferences();
	}

	@Override
	public boolean canClose() {
		final boolean[] commit = { true };
		if (bugtraqModel != null && bugtraqModel.getMessage() != null && bugtraqModel.isWarnIfNoIssue()
				&& comment.getBugID() != null && comment.getBugID().trim().length() == 0) {
			UIMonitorUtility.getDisplay().syncExec(() -> {
				MessageDialog dlg = new MessageDialog(
						UIMonitorUtility.getShell(), SVNUIMessages.CommitPanel_NoBugId_Title, null,
						SVNUIMessages.CommitPanel_NoBugId_Message, MessageDialog.WARNING,
						new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL }, 0);
				commit[0] = dlg.open() == 0;
			});
		}
		return commit[0];
	}

	public void pasteNames() {
		if (!paneParticipantHelper.isParticipantPane()) {
			List<IResource> selectedResources = selectionComposite.getCurrentSelection();
			this.pasteNames(selectedResources.toArray(new IResource[0]));
		} else {
			ISelection selection = paneParticipantHelper.getSyncPageConfiguration()
					.getSite()
					.getSelectionProvider()
					.getSelection();
			if (selection instanceof IStructuredSelection) {
				IStructuredSelection strSelection = (IStructuredSelection) selection;
				Object[] elements = strSelection.toArray();
				IResource[] resources = Utils.getResources(elements);
				this.pasteNames(resources);
			}
		}
	}

	protected void pasteNames(IResource[] resources) {
		if (resources.length > 0) {
			String namesString = ""; //$NON-NLS-1$
			for (IResource resource : resources) {
				namesString += resource.getName() + "\n"; //$NON-NLS-1$
			}
			comment.insertText(namesString);
		}
	}

	//relates to resource selection composite
	protected void addContextMenu() {
		final TableViewer tableViewer = selectionComposite.getTableViewer();
		MenuManager menuMgr = new MenuManager();
		Menu menu = menuMgr.createContextMenu(tableViewer.getTable());
		menuMgr.addMenuListener(manager -> {
			manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
			final IStructuredSelection tSelection = (IStructuredSelection) tableViewer.getSelection();
			final IResource[] selectedResources = (IResource[]) tSelection.toList()
					.toArray(new IResource[tSelection.size()]);
			Action tAction = null;

			//paste selected names action
			manager.add(tAction = new Action(SVNUIMessages.CommitPanel_PasteNames_Action) {
				@Override
				public void run() {
					CommitPanel.this.pasteNames();
				}
			});
			tAction.setEnabled(tSelection.size() > 0);

			//Create Patch File action
			manager.add(tAction = new Action(SVNUIMessages.CreatePatchCommand_label) {
				@Override
				public void run() {
					FileDialog dlg = new FileDialog(UIMonitorUtility.getShell(), SWT.PRIMARY_MODAL | SWT.SAVE);
					dlg.setText(SVNUIMessages.SelectPatchFilePage_SavePatchAs);
					dlg.setFileName(selectedResources[0].getName() + ".patch"); //$NON-NLS-1$
					dlg.setFilterExtensions(new String[] { "patch", "*.*" }); //$NON-NLS-1$ //$NON-NLS-2$
					String file = dlg.open();
					if (file != null) {
						CreatePatchOperation mainOp = new CreatePatchOperation(
								new IResource[] { selectedResources[0] }, file, true, true, true, true);
						UIMonitorUtility.doTaskNowDefault(mainOp, false);
					}
				}
			});
			tAction.setEnabled(tSelection.size() == 1 && FileUtility.checkForResourcesPresence(selectedResources,
					IStateFilter.SF_VERSIONED, IResource.DEPTH_ZERO));

			//Create Branch action
			manager.add(tAction = new Action(SVNUIMessages.BranchAction_label) {
				@Override
				public void run() {
					IResource[] resources = FileUtility.getResourcesRecursive(selectedResources,
							IStateFilter.SF_EXCLUDE_DELETED, IResource.DEPTH_INFINITE);
					IActionOperation op = BranchTagAction.getBranchTagOperation(UIMonitorUtility.getShell(),
							BranchTagAction.BRANCH_ACTION, resources);
					if (op != null) {
						UIMonitorUtility.doTaskNowDefault(op, true);
					}
				}
			});
			tAction.setEnabled(tSelection.size() > 0 && FileUtility.checkForResourcesPresence(selectedResources,
					IStateFilter.SF_EXCLUDE_DELETED, IResource.DEPTH_ZERO));
			tAction.setImageDescriptor(
					SVNTeamUIPlugin.instance().getImageDescriptor("icons/common/actions/branch.gif")); //$NON-NLS-1$
			manager.add(new Separator());

			//Revert action
			manager.add(tAction = new Action(SVNUIMessages.CommitPanel_Revert_Action) {
				@Override
				public void run() {
					IResource[] changedResources = FileUtility.getResourcesRecursive(selectedResources,
							RevertAction.SF_REVERTABLE_OR_NEW);
					CompositeOperation revertOp = RevertAction.getRevertOperation(UIMonitorUtility.getShell(),
							changedResources, selectedResources);
					if (revertOp != null) {
						UIMonitorUtility.doTaskNowDefault(revertOp, true);
					}
				}
			});
			tAction.setImageDescriptor(
					SVNTeamUIPlugin.instance().getImageDescriptor("icons/common/actions/revert.gif")); //$NON-NLS-1$
			tAction.setEnabled(tSelection.size() > 0);

			//Ignore resources group
			if (tSelection.size() > 0
					&& selectedResources.length == FileUtility.getResourcesRecursive(selectedResources,
							AddToSVNIgnoreAction.SF_NEW_AND_PARENT_VERSIONED).length) {
				MenuManager subMenu = new MenuManager(SVNUIMessages.CommitPanel_Ignore_Group);
				if (tSelection.size() > 1) {
					subMenu.add(tAction = new Action(SVNUIMessages.CommitPanel_IgnoreByName_Multiple_Action) {
						@Override
						public void run() {
							CompositeOperation op = new CompositeOperation("Operation_AddToSVNIgnore", //$NON-NLS-1$
									SVNMessages.class);
							op.add(new AddToSVNIgnoreOperation(selectedResources, ISVNStorage.IGNORE_NAME, null));
							op.add(new RefreshResourcesOperation(
									new ResourcesParentsProvider(resources), IResource.DEPTH_INFINITE,
									RefreshResourcesOperation.REFRESH_ALL));
							UIMonitorUtility.doTaskNowDefault(op, true);
						}
					});
					tAction.setEnabled(true);
					subMenu.add(tAction = new Action(SVNUIMessages.CommitPanel_IgnoreByExtension_Multiple_Action) {
						@Override
						public void run() {
							CompositeOperation op = new CompositeOperation("Operation_AddToSVNIgnore", //$NON-NLS-1$
									SVNMessages.class);
							op.add(new AddToSVNIgnoreOperation(selectedResources, ISVNStorage.IGNORE_EXTENSION,
									null));
							op.add(new RefreshResourcesOperation(
									new ResourcesParentsProvider(resources), IResource.DEPTH_INFINITE,
									RefreshResourcesOperation.REFRESH_ALL));
							UIMonitorUtility.doTaskNowDefault(op, true);
						}
					});
					tAction.setEnabled(true);
				} else {
					subMenu.add(tAction = new Action(selectedResources[0].getName()) {
						@Override
						public void run() {
							CompositeOperation op = new CompositeOperation("Operation_AddToSVNIgnore", //$NON-NLS-1$
									SVNMessages.class);
							op.add(new AddToSVNIgnoreOperation(selectedResources, ISVNStorage.IGNORE_NAME, null));
							op.add(new RefreshResourcesOperation(
									new ResourcesParentsProvider(resources), IResource.DEPTH_INFINITE,
									RefreshResourcesOperation.REFRESH_ALL));
							UIMonitorUtility.doTaskNowDefault(op, true);
						}
					});
					tAction.setEnabled(true);
					String name = selectedResources[0].getName();
					String[] parts = name.split("\\."); //$NON-NLS-1$
					if (parts.length != 0) {
						subMenu.add(tAction = new Action("*." + parts[parts.length - 1]) { //$NON-NLS-1$
							@Override
							public void run() {
								CompositeOperation op = new CompositeOperation("Operation_AddToSVNIgnore", //$NON-NLS-1$
										SVNMessages.class);
								op.add(new AddToSVNIgnoreOperation(selectedResources, ISVNStorage.IGNORE_EXTENSION,
										null));
								op.add(new RefreshResourcesOperation(
										new ResourcesParentsProvider(resources), IResource.DEPTH_INFINITE,
										RefreshResourcesOperation.REFRESH_ALL));
								UIMonitorUtility.doTaskNowDefault(op, true);
							}
						});
						tAction.setEnabled(true);
					}
				}
				manager.add(subMenu);
			}

			//Edit conflicts action
			manager.add(tAction = new Action(SVNUIMessages.EditConflictsAction_label) {
				@Override
				public void run() {
					UIMonitorUtility.doTaskScheduledDefault(new ShowConflictEditorOperation(FileUtility
							.getResourcesRecursive(selectedResources, IStateFilter.SF_CONTENT_CONFLICTING), true));
				}
			});
			tAction.setEnabled(FileUtility.checkForResourcesPresenceRecursive(selectedResources,
					IStateFilter.SF_CONTENT_CONFLICTING));

			//Edit tree conflicts action
			manager.add(tAction = new Action(SVNUIMessages.EditTreeConflictsAction_label) {
				@Override
				public void run() {
					if (selectedResources.length > 0) {
						ILocalResource local = SVNRemoteStorage.instance().asLocalResource(selectedResources[0]);
						if (local.hasTreeConflict()) {
							EditTreeConflictsPanel editConflictsPanel = new EditTreeConflictsPanel(local);
							DefaultDialog dialog = new DefaultDialog(UIMonitorUtility.getShell(),
									editConflictsPanel);
							if (dialog.open() == 0 && editConflictsPanel.getOperation() != null) {
								UIMonitorUtility.doTaskScheduledDefault(editConflictsPanel.getOperation());
							}
						}
					}
				}
			});
			tAction.setEnabled(selectedResources.length == 1 && FileUtility.checkForResourcesPresence(
					selectedResources, IStateFilter.SF_TREE_CONFLICTING, IResource.DEPTH_ZERO));

			//Mark as merged action
			manager.add(tAction = new Action(SVNUIMessages.CommitPanel_MarkAsMerged_Action) {
				@Override
				public void run() {
					boolean ignoreExternals = SVNTeamPreferences.getBehaviourBoolean(
							SVNTeamUIPlugin.instance().getPreferenceStore(),
							SVNTeamPreferences.BEHAVIOUR_IGNORE_EXTERNALS_NAME);
					MarkAsMergedOperation mainOp = new MarkAsMergedOperation(selectedResources, false, null,
							ignoreExternals);
					CompositeOperation op = new CompositeOperation(mainOp.getId(), mainOp.getMessagesClass());
					op.add(mainOp);
					op.add(new ShowPostCommitErrorsOperation(mainOp));
					op.add(new RefreshResourcesOperation(FileUtility.getParents(selectedResources, false)));
					UIMonitorUtility.doTaskNowDefault(op, false);
				}
			});
			tAction.setEnabled(
					FileUtility.checkForResourcesPresenceRecursive(selectedResources, IStateFilter.SF_CONFLICTING)
							&& selectedResources.length == 1);
			manager.add(new Separator());

			//Lock action
			manager.add(tAction = new Action(SVNUIMessages.LockAction_label) {
				@Override
				public void run() {
					IResource[] filteredResources = FileUtility.getResourcesRecursive(selectedResources,
							IStateFilter.SF_READY_TO_LOCK, IResource.DEPTH_INFINITE);
					IActionOperation op = LockProposeUtility.performLockAction(filteredResources, false,
							UIMonitorUtility.getShell());
					if (op != null) {
						UIMonitorUtility.doTaskNowDefault(op, false);
					}
				}
			});
			tAction.setImageDescriptor(
					SVNTeamUIPlugin.instance().getImageDescriptor("icons/common/actions/lock.gif")); //$NON-NLS-1$
			tAction.setEnabled(FileUtility.checkForResourcesPresenceRecursive(selectedResources,
					IStateFilter.SF_READY_TO_LOCK));

			//Unlock action
			manager.add(tAction = new Action(SVNUIMessages.UnlockAction_label) {
				@Override
				public void run() {
					IResource[] filteredResources = FileUtility.getResourcesRecursive(selectedResources,
							IStateFilter.SF_LOCKED, IResource.DEPTH_INFINITE);
					IActionOperation op = LockProposeUtility.performUnlockAction(filteredResources,
							UIMonitorUtility.getShell());
					if (op != null) {
						UIMonitorUtility.doTaskNowDefault(op, false);
					}
				}
			});
			tAction.setImageDescriptor(
					SVNTeamUIPlugin.instance().getImageDescriptor("icons/common/actions/unlock.gif")); //$NON-NLS-1$
			tAction.setEnabled(
					FileUtility.checkForResourcesPresenceRecursive(selectedResources, IStateFilter.SF_LOCKED));
			manager.add(new Separator());

			//Compare With group
			MenuManager subMenu = new MenuManager(SVNUIMessages.CommitPanel_CompareWith_Group);
			subMenu.add(tAction = new Action(SVNUIMessages.CompareWithWorkingCopyAction_label) {
				@Override
				public void run() {
					IResource resource = selectedResources[0];
					ILocalResource local = SVNRemoteStorage.instance().asLocalResource(resource);
					if (!IStateFilter.SF_INTERNAL_INVALID.accept(local)) {
						IRepositoryResource remote = local.isCopied()
								? SVNUtility.getCopiedFrom(resource)
								: SVNRemoteStorage.instance().asRepositoryResource(resource);
						remote.setSelectedRevision(SVNRevision.BASE);
						UIMonitorUtility
								.doTaskScheduledDefault(new CompareResourcesOperation(local, remote, false, true));
					}
				}
			});
			tAction.setEnabled(tSelection.size() == 1 && FileUtility.checkForResourcesPresence(selectedResources,
					CompareWithWorkingCopyAction.COMPARE_FILTER, IResource.DEPTH_ZERO));
			subMenu.add(tAction = new Action(SVNUIMessages.CompareWithLatestRevisionAction_label) {
				@Override
				public void run() {
					IResource resource = selectedResources[0];
					ILocalResource local = SVNRemoteStorage.instance().asLocalResource(resource);
					if (!IStateFilter.SF_INTERNAL_INVALID.accept(local)) {
						IRepositoryResource remote = local.isCopied()
								? SVNUtility.getCopiedFrom(resource)
								: SVNRemoteStorage.instance().asRepositoryResource(resource);
						remote.setSelectedRevision(SVNRevision.HEAD);
						UIMonitorUtility
								.doTaskScheduledDefault(new CompareResourcesOperation(local, remote, false, true));
					}
				}
			});
			tAction.setEnabled(tSelection.size() == 1
					&& (CoreExtensionsManager.instance()
							.getSVNConnectorFactory()
							.getSVNAPIVersion() >= ISVNConnectorFactory.APICompatibility.SVNAPI_1_5_x
							|| selectedResources[0].getType() == IResource.FILE)
					&& FileUtility.checkForResourcesPresenceRecursive(selectedResources,
							CompareWithWorkingCopyAction.COMPARE_FILTER));
			subMenu.add(tAction = new Action(SVNUIMessages.CompareWithRevisionAction_label) {
				@Override
				public void run() {
					IResource resource = selectedResources[0];
					ILocalResource local = SVNRemoteStorage.instance().asLocalResource(resource);
					if (!IStateFilter.SF_INTERNAL_INVALID.accept(local)) {
						IRepositoryResource remote = local.isCopied()
								? SVNUtility.getCopiedFrom(resource)
								: SVNRemoteStorage.instance().asRepositoryResource(resource);
						ComparePanel panel = new ComparePanel(remote, local.getRevision());
						DefaultDialog dlg = new DefaultDialog(UIMonitorUtility.getShell(), panel);
						if (dlg.open() == 0) {
							remote = panel.getSelectedResource();
							UIMonitorUtility.doTaskScheduledDefault(
									new CompareResourcesOperation(local, remote, false, true));
						}
					}
				}
			});
			tAction.setEnabled(tSelection.size() == 1
					&& (CoreExtensionsManager.instance()
							.getSVNConnectorFactory()
							.getSVNAPIVersion() >= ISVNConnectorFactory.APICompatibility.SVNAPI_1_5_x
							|| selectedResources[0].getType() == IResource.FILE)
					&& FileUtility.checkForResourcesPresenceRecursive(selectedResources,
							CompareWithWorkingCopyAction.COMPARE_FILTER));
			manager.add(subMenu);

			//Replace with group
			subMenu = new MenuManager(SVNUIMessages.CommitPanel_ReplaceWith_Group);
			subMenu.add(tAction = new Action(SVNUIMessages.ReplaceWithLatestRevisionAction_label) {
				@Override
				public void run() {
					IResource[] resources = FileUtility.getResourcesRecursive(selectedResources,
							IStateFilter.SF_ONREPOSITORY, IResource.DEPTH_ZERO);
					IActionOperation op = ReplaceWithLatestRevisionAction.getReplaceOperation(resources,
							UIMonitorUtility.getShell());
					if (op != null) {
						UIMonitorUtility.doTaskNowDefault(op, true);
					}
				}
			});
			tAction.setEnabled(FileUtility.checkForResourcesPresenceRecursive(selectedResources,
					IStateFilter.SF_ONREPOSITORY));
			subMenu.add(tAction = new Action(SVNUIMessages.ReplaceWithRevisionAction_label) {
				@Override
				public void run() {
					IActionOperation op = ReplaceWithRevisionAction.getReplaceOperation(selectedResources,
							UIMonitorUtility.getShell());
					if (op != null) {
						UIMonitorUtility.doTaskNowDefault(op, true);
					}
				}
			});
			tAction.setEnabled(tSelection.size() == 1 && FileUtility.checkForResourcesPresence(selectedResources,
					IStateFilter.SF_ONREPOSITORY, IResource.DEPTH_ZERO));
			manager.add(subMenu);
			manager.add(new Separator());

			//Export action
			manager.add(tAction = new Action(SVNUIMessages.ExportCommand_label) {
				@Override
				public void run() {
					DirectoryDialog fileDialog = new DirectoryDialog(UIMonitorUtility.getShell());
					fileDialog.setText(SVNUIMessages.ExportAction_Select_Title);
					fileDialog.setMessage(SVNUIMessages.ExportAction_Select_Description);
					String path = fileDialog.open();
					if (path != null) {
						boolean ignoreExternals = SVNTeamPreferences.getBehaviourBoolean(
								SVNTeamUIPlugin.instance().getPreferenceStore(),
								SVNTeamPreferences.BEHAVIOUR_IGNORE_EXTERNALS_NAME);
						UIMonitorUtility.doTaskScheduledDefault(new ExportOperation(
								FileUtility.getResourcesRecursive(selectedResources,
										IStateFilter.SF_EXCLUDE_DELETED, IResource.DEPTH_ZERO),
								path, SVNRevision.WORKING, ignoreExternals));
					}
				}
			});
			tAction.setImageDescriptor(SVNTeamUIPlugin.instance().getImageDescriptor("icons/common/export.gif")); //$NON-NLS-1$
			tAction.setEnabled(tSelection.size() > 0 && FileUtility.checkForResourcesPresence(selectedResources,
					IStateFilter.SF_EXCLUDE_DELETED, IResource.DEPTH_ZERO));

			//Clean-up action
			manager.add(tAction = new Action(SVNUIMessages.CleanupCommand_label) {
				@Override
				public void run() {
					IResource[] resources = FileUtility.getResourcesRecursive(selectedResources,
							IStateFilter.SF_VERSIONED_FOLDERS, IResource.DEPTH_ZERO);
					CleanupOperation mainOp = new CleanupOperation(resources);
					CompositeOperation op = new CompositeOperation(mainOp.getId(), mainOp.getMessagesClass());
					op.add(mainOp);
					op.add(new RefreshResourcesOperation(resources));
					UIMonitorUtility.doTaskNowDefault(op, false);
				}
			});
			tAction.setEnabled(tSelection.size() > 0 && FileUtility.checkForResourcesPresence(selectedResources,
					IStateFilter.SF_VERSIONED_FOLDERS, IResource.DEPTH_ZERO));
			manager.add(new Separator());

			//Delete action
			manager.add(tAction = new Action(SVNUIMessages.CommitPanel_Delete_Action) {
				@Override
				public void run() {
					DiscardConfirmationDialog dialog = new DiscardConfirmationDialog(UIMonitorUtility.getShell(),
							selectedResources.length == 1, DiscardConfirmationDialog.MSG_RESOURCE);
					if (dialog.open() == 0) {
						DeleteResourceOperation deleteOperation = new DeleteResourceOperation(selectedResources);
						CompositeOperation op = new CompositeOperation(deleteOperation.getId(),
								deleteOperation.getMessagesClass());
						SaveProjectMetaOperation saveOp = new SaveProjectMetaOperation(selectedResources);
						RestoreProjectMetaOperation restoreOp = new RestoreProjectMetaOperation(saveOp);
						op.add(saveOp);
						op.add(deleteOperation);
						op.add(restoreOp);
						op.add(new RefreshResourcesOperation(selectedResources));
						UIMonitorUtility.doTaskNowDefault(op, true);
					}
				}
			});
			tAction.setImageDescriptor(SVNTeamUIPlugin.instance().getImageDescriptor("icons/common/delete.gif")); //$NON-NLS-1$
			tAction.setEnabled(tSelection.size() > 0 && !FileUtility.checkForResourcesPresence(selectedResources,
					IStateFilter.SF_DELETED, IResource.DEPTH_ZERO));
		});
		menuMgr.setRemoveAllWhenShown(true);
		tableViewer.getTable().setMenu(menu);
	}

	protected void updateResources(ResourceStatesChangedEvent event) {
		HashSet<IResource> allResources = new HashSet<>(Arrays.asList(resources));

		HashSet<IResource> toDeleteSet = new HashSet<>(Arrays.asList(
				FileUtility.getResourcesRecursive(event.resources, IStateFilter.SF_NOTMODIFIED, IResource.DEPTH_ZERO)));
		toDeleteSet.addAll(Arrays.asList(
				FileUtility.getResourcesRecursive(event.resources, IStateFilter.SF_NOTEXISTS, IResource.DEPTH_ZERO)));
		toDeleteSet.addAll(Arrays.asList(
				FileUtility.getResourcesRecursive(event.resources, IStateFilter.SF_IGNORED, IResource.DEPTH_ZERO)));

		allResources.removeAll(toDeleteSet);

		final IResource[] newResources = allResources.toArray(new IResource[allResources.size()]);

		if (!paneParticipantHelper.isParticipantPane()) {
			UIMonitorUtility.getDisplay().syncExec(() -> {
				//FIXME isDisposed() test is necessary as dispose() method is not called from FastTrack Commit Dialog
				if (!selectionComposite.isDisposed()) {
					selectionComposite.setResources(newResources);
					selectionComposite.fireSelectionChanged();
				}
			});
		}

		resources = newResources;
		resourcesChanged = true;
	}

	public boolean getResourcesChanged() {
		return resourcesChanged;
	}

	public IResource[] getSelectedResources() {
		if (paneParticipantHelper.isParticipantPane()) {
			return paneParticipantHelper.getSelectedResources();
		} else {
			return selectionComposite.getSelectedResources();
		}
	}

	public IResource[] getTreatAsEdits() {
		return paneParticipantHelper.isParticipantPane() ? new IResource[0] : selectionComposite.getTreatAsEdits();
	}

	public IResource[] getNotSelectedResources() {
		if (paneParticipantHelper.isParticipantPane()) {
			return paneParticipantHelper.getNotSelectedResources();
		} else {
			return selectionComposite.getNotSelectedResources();
		}
	}

	@Override
	public void addResourcesSelectionChangedListener(IResourceSelectionChangeListener listener) {
		changeListenerList.add(listener);
	}

	@Override
	public void removeResourcesSelectionChangedListener(IResourceSelectionChangeListener listener) {
		changeListenerList.remove(listener);
	}

	public void fireResourcesSelectionChanged(ResourceSelectionChangedEvent event) {
		validateContent();
		IResourceSelectionChangeListener[] listeners = changeListenerList
				.toArray(new IResourceSelectionChangeListener[changeListenerList.size()]);
		for (IResourceSelectionChangeListener listener : listeners) {
			listener.resourcesSelectionChanged(event);
		}
	}

	public boolean getKeepLocks() {
		return keepLocks;
	}

	@Override
	protected Point getPrefferedSizeImpl() {
		return new Point(600, SWT.DEFAULT);
	}

	protected void savePreferences() {
		int[] weights = sForm.getWeights();
		IPreferenceStore store = SVNTeamUIPlugin.instance().getPreferenceStore();
		SVNTeamPreferences.setDialogInt(store, SVNTeamPreferences.COMMIT_DIALOG_WEIGHT_NAME, weights[0] / 10);
		store.setValue(SVNTeamPreferences.COMMIT_DIALOG_KEEP_LOCKS, keepLocks);
	}

	@Override
	public void dispose() {
		super.dispose();

		SVNRemoteStorage.instance()
				.removeResourceStatesListener(ResourceStatesChangedEvent.class, resourceStatesListener);

		if (paneParticipantHelper.isParticipantPane()) {
			paneParticipantHelper.dispose();
		}
	}

	public static class CollectPropertiesOperation extends AbstractActionOperation {
		protected IResource[] resources;

		protected MinLogSizePropFindVisitor minLogVisitor;

		protected LogTemplatesPropFindVisitor logTemplateVisitor;

		protected BugtraqPropFindVisitor bugtraqVisitor;

		protected MaxLogWidthPropFindVisitor maxWidthVisitor;

		protected MinLockSizePropFindVisitor minLockVisitor;

		protected CompositePropFindVisitor compositeVisitor;

		public CollectPropertiesOperation(IResource[] resources) {
			super("Operation_CollectProperties", SVNUIMessages.class); //$NON-NLS-1$
			this.resources = resources;
			logTemplateVisitor = new LogTemplatesPropFindVisitor();
			bugtraqVisitor = new BugtraqPropFindVisitor();
			minLogVisitor = new MinLogSizePropFindVisitor();
			maxWidthVisitor = new MaxLogWidthPropFindVisitor();
			minLockVisitor = new MinLockSizePropFindVisitor();
			if (SVNTeamPreferences.getCommentTemplatesBoolean(SVNTeamUIPlugin.instance().getPreferenceStore(),
					SVNTeamPreferences.COMMENT_LOG_TEMPLATES_ENABLED_NAME)) {
				compositeVisitor = new CompositePropFindVisitor(new IPropFindVisitor[] { logTemplateVisitor,
						bugtraqVisitor, minLogVisitor, maxWidthVisitor, minLockVisitor });
			} else {
				compositeVisitor = new CompositePropFindVisitor(
						new IPropFindVisitor[] { bugtraqVisitor, minLogVisitor, maxWidthVisitor, minLockVisitor });
			}
		}

		@Override
		protected void runImpl(IProgressMonitor monitor) throws Exception {
			ArrayList<IResource> parentProperties = new ArrayList<>();

			int length = resources.length < CommitPanel.MAXIMUM_CHECKS_SIZE
					? resources.length
					: CommitPanel.MAXIMUM_CHECKS_SIZE;
			for (int i = 0; i < length && !monitor.isCanceled(); i++) {
				ProgressMonitorUtility.setTaskInfo(monitor, this, resources[i].getFullPath().toString());

				ILocalResource local = SVNRemoteStorage.instance().asLocalResourceAccessible(resources[i]);
				IResource resourceToProcess = resources[i];
				while (IStateFilter.SF_UNVERSIONED.accept(local) || IStateFilter.SF_TREE_CONFLICTING.accept(local)
						&& !IStateFilter.SF_TREE_CONFLICTING_REPOSITORY_EXIST.accept(local)) {
					resourceToProcess = resourceToProcess.getParent();
					local = SVNRemoteStorage.instance().asLocalResourceAccessible(resourceToProcess);
				}
				if (!processProperty(resourceToProcess, parentProperties, monitor)) {
					break;
				}

				ProgressMonitorUtility.progress(monitor, i, length);
			}
		}

		protected boolean processProperty(IResource resource, ArrayList<IResource> parentProperties,
				IProgressMonitor monitor) {
			if (parentProperties.contains(resource) || monitor.isCanceled()) {
				return true;
			}

			GetPropertiesOperation op = new GetPropertiesOperation(resource);
			ProgressMonitorUtility.doTaskExternalDefault(op, monitor);
			if (op.getExecutionState() == IStatus.OK) {
				SVNProperty[] properties = op.getProperties();
				if (properties != null) {
					for (SVNProperty property : properties) {
						if (!compositeVisitor.visit(property)) {
							return false;
						}
					}
				}
			}

			parentProperties.add(resource);

			IResource parent = resource.getParent();
			if (parent != null && !(parent instanceof IWorkspaceRoot) && !monitor.isCanceled()
					&& !IStateFilter.SF_UNVERSIONED.accept(SVNRemoteStorage.instance().asLocalResource(parent))) {
				return processProperty(parent, parentProperties, monitor);
			}
			return true;
		}

		public HashSet getLogTemplates() {
			return logTemplateVisitor.getLogTemplates();
		}

		public BugtraqModel getBugtraqModel() {
			return bugtraqVisitor.getBugtraqModel();
		}

		public int getMinLogSize() {
			return minLogVisitor.getMinLogSize();
		}

		public int getMinLockSize() {
			return minLockVisitor.getMinLockSize();
		}

		public int getMaxLogWidth() {
			return maxWidthVisitor.getMaxLogWidth();
		}
	}

	/**
	 * Check if resources have errors or warnings
	 * 
	 * @param resources
	 * @return
	 */
	public static String validateResources(IResource[] resources) {
		String message = null;
		int highestProblemSeverity = CommitPanel.getHighestProblemSeverity(resources);
		switch (highestProblemSeverity) {
			case IMarker.SEVERITY_WARNING:
				message = SVNUIMessages.CommitPanel_Resource_Validation_Warning_Message;
				break;

			case IMarker.SEVERITY_ERROR:
				message = SVNUIMessages.CommitPanel_Resource_Validation_Error_Message;
				break;
		}
		return message;
	}

	public static int getHighestProblemSeverity(IResource[] resources) {
		int res = -1;
		for (IResource resource : resources) {
			try {
				int problemSeverity = resource.findMaxProblemSeverity(IMarker.PROBLEM, true, IResource.DEPTH_ZERO);
				res = problemSeverity > res ? problemSeverity : res;
			} catch (CoreException e) {
				//ignore
			}
		}
		return res;
	}

}
