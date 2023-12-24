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

package org.eclipse.team.svn.ui.wizard.shareproject;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.SVNDepth;
import org.eclipse.team.svn.core.connector.SVNEntryInfo;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.SVNNullProgressMonitor;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.operation.local.management.FindRelatedProjectsOperation;
import org.eclipse.team.svn.core.operation.remote.management.AddRepositoryLocationOperation;
import org.eclipse.team.svn.core.operation.remote.management.SaveRepositoryLocationsOperation;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.SVNUtility;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.composite.ProjectListComposite;
import org.eclipse.team.svn.ui.composite.RepositoryPropertiesTabFolder;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.dialog.NonValidLocationErrorDialog;
import org.eclipse.team.svn.ui.operation.RefreshRepositoryLocationsOperation;
import org.eclipse.team.svn.ui.panel.AbstractDialogPanel;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;
import org.eclipse.team.svn.ui.verifier.AbstractFormattedVerifier;
import org.eclipse.team.svn.ui.wizard.AbstractVerifiedWizardPage;
import org.eclipse.ui.PlatformUI;

/**
 * Add repository location wizard page
 * 
 * @author Alexander Gurov
 */
public class AddRepositoryLocationPage extends AbstractVerifiedWizardPage {
	protected RepositoryPropertiesTabFolder propertiesTabFolder;

	protected IActionOperation operationToPerform;

	protected IRepositoryLocation editable;

	protected boolean alreadyConnected;

	protected boolean createNew;

	protected String initialUrl;

	protected String oldUrl;

	protected String oldLabel;

	public AddRepositoryLocationPage() {
		this(null);
	}

	public AddRepositoryLocationPage(IRepositoryLocation editable) {
		super(AddRepositoryLocationPage.class.getName(), SVNUIMessages.AddRepositoryLocationPage_Title,
				SVNTeamUIPlugin.instance().getImageDescriptor("icons/wizards/newconnect.gif")); //$NON-NLS-1$

		setDescription(SVNUIMessages.AddRepositoryLocationPage_Description);
		this.editable = editable;
		if (editable != null) {
			oldUrl = editable.getUrl();
			oldLabel = editable.getLabel();
		}
		alreadyConnected = false;
		createNew = true;
	}

	@Override
	protected Composite createControlImpl(Composite parent) {
		propertiesTabFolder = new RepositoryPropertiesTabFolder(parent, SWT.NONE, this, editable);
		propertiesTabFolder.initialize();
		propertiesTabFolder.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		propertiesTabFolder.resetChanges();

//		Setting context help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "org.eclipse.team.svn.help.newReposWizContext"); //$NON-NLS-1$

		return propertiesTabFolder;

	}

	public void setInitialUrl(String initialUrl) {
		this.initialUrl = initialUrl;
		if (alreadyConnected = initialUrl != null) {
			createNew = initialUrl.trim().length() == 0;
			getRepositoryLocation().setUrl(initialUrl);
			propertiesTabFolder.resetChanges();
		}
	}

	public void setForceDisableRoots(boolean force) {
		propertiesTabFolder.setForceDisableRoots(force,
				initialUrl == null || initialUrl.length() == 0
						? null
						: new AbstractFormattedVerifier(SVNUIMessages.AddRepositoryLocationPage_RootURL) {
							@Override
							protected String getErrorMessageImpl(Control input) {
								String url = getText(input);
								if (!SVNUtility.createPathForSVNUrl(url)
										.isPrefixOf(SVNUtility.createPathForSVNUrl(
												SVNUtility.decodeURL(initialUrl)))) {
									return BaseMessages.format(
											SVNUIMessages.AddRepositoryLocationPage_FixedURL_Verifier_Error,
											new String[] { AbstractFormattedVerifier.FIELD_NAME, initialUrl });
								}
								return null;
							}

							@Override
							protected String getWarningMessageImpl(Control input) {
								return null;
							}
						});
	}

	public IRepositoryLocation getRepositoryLocation() {
		return propertiesTabFolder.getRepositoryLocation();
	}

	@Override
	public boolean canFlipToNextPage() {
		return (!alreadyConnected || createNew) && isPageComplete();
	}

	@Override
	public IWizardPage getNextPage() {
		return performFinish() ? super.getNextPage() : this;
	}

	@Override
	public IWizardPage getPreviousPage() {
		performCancel();
		return super.getPreviousPage();
	}

	public void performCancel() {
		operationToPerform = null;
	}

	public boolean performFinish() {
		String newUrl = propertiesTabFolder.getLocationUrl();
		String oldUuid = null;
		IProject[] projectsArray = {};
		if (editable != null && SVNRemoteStorage.instance().getRepositoryLocation(editable.getId()) != null
				&& !newUrl.equals(oldUrl)) {
			FindRelatedProjectsOperation op = new FindRelatedProjectsOperation(editable);
			UIMonitorUtility.doTaskBusyDefault(op);
			projectsArray = (IProject[]) op.getResources();

			if (projectsArray.length > 0) {
				SVNEntryInfo info = getLocationInfo(editable);
				oldUuid = info == null ? null : info.reposUUID;
			}
		}
		propertiesTabFolder.saveChanges();

		if (propertiesTabFolder.isStructureEnabled()) {
			String endsPart = SVNUtility.createPathForSVNUrl(newUrl).lastSegment();
			if (endsPart.equals(propertiesTabFolder.getRepositoryLocation().getTrunkLocation())
					|| endsPart.equals(propertiesTabFolder.getRepositoryLocation().getBranchesLocation())
					|| endsPart.equals(propertiesTabFolder.getRepositoryLocation().getTagsLocation())) {
				final int[] result = new int[1];
				final MessageDialog dialog = new MessageDialog(getShell(),
						SVNUIMessages.AddRepositoryLocationPage_Normalize_Title, null,
						SVNUIMessages.AddRepositoryLocationPage_Normalize_Message, MessageDialog.WARNING,
						new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL }, 0);
				UIMonitorUtility.getDisplay().syncExec(() -> result[0] = dialog.open());
				if (result[0] == IDialogConstants.OK_ID) {
					IRepositoryLocation location = editable == null ? getRepositoryLocation() : editable;
					boolean useCustomLabel = false;
					useCustomLabel = !location.getUrl().equals(location.getLabel());
					newUrl = SVNUtility.createPathForSVNUrl(newUrl).removeLastSegments(1).toString();
					location.setUrl(newUrl);
					if (!useCustomLabel) {
						location.setLabel(newUrl);
					}
					location.reconfigure();
				}
			}
		}

		ProjectListPanel panel = null;
		if (projectsArray.length > 0) {
			editable.reconfigure();
			SVNEntryInfo newInfo = getLocationInfo(editable);
			if (newInfo == null) {
				panel = new ProjectListPanel(projectsArray, false);
			} else if (oldUuid != null && !oldUuid.equals(newInfo.reposUUID)) {
				panel = new ProjectListPanel(projectsArray, true);
			}
			if (panel != null) {
				editable.setUrl(oldUrl);
				editable.setLabel(oldLabel);
				editable.reconfigure();
				new DefaultDialog(getShell(), panel).open();
			}
		}

		if (propertiesTabFolder.isValidateOnFinishRequested() && panel == null) {
			final Exception[] problem = new Exception[1];
			boolean cancelled = UIMonitorUtility.doTaskNowDefault(getShell(),
					new AbstractActionOperation("Operation_ValidateLocation", SVNUIMessages.class) { //$NON-NLS-1$
						@Override
						protected void runImpl(IProgressMonitor monitor) throws Exception {
							problem[0] = SVNUtility.validateRepositoryLocation(
									propertiesTabFolder.getRepositoryLocation(),
									new SVNProgressMonitor(this, monitor, null));
						}
					}, true).isCancelled();
			if (cancelled) {
				return false;
			}
			if (problem[0] != null) {
				NonValidLocationErrorDialog dialog = new NonValidLocationErrorDialog(getShell(),
						problem[0].getMessage());
				if (dialog.open() != 0) {
					return false;
				}
			}
		}

		boolean shouldntBeAdded = editable == null
				? false
				: SVNRemoteStorage.instance().getRepositoryLocation(editable.getId()) != null;

		AbstractActionOperation mainOp = shouldntBeAdded
				? new AbstractActionOperation("Operation_CommitLocationChanges", SVNUIMessages.class) { //$NON-NLS-1$
					@Override
					protected void runImpl(IProgressMonitor monitor) throws Exception {
						editable.reconfigure();
					}
				}
				: (AbstractActionOperation) new AddRepositoryLocationOperation(getRepositoryLocation());

		CompositeOperation op = new CompositeOperation(mainOp.getId(), mainOp.getMessagesClass());

		op.add(mainOp);
		op.add(new SaveRepositoryLocationsOperation());
		op.add(shouldntBeAdded
				? new RefreshRepositoryLocationsOperation(new IRepositoryLocation[] { editable }, true)
				: new RefreshRepositoryLocationsOperation(false));

		operationToPerform = op;

		return true;
	}

	public IActionOperation getOperationToPeform() {
		return operationToPerform;
	}

	protected static class ProjectListPanel extends AbstractDialogPanel {
		protected IProject[] resources;

		protected TableViewer tableViewer;

		public ProjectListPanel(IProject[] input, boolean differentUuid) {
			super(new String[] { IDialogConstants.OK_LABEL });

			dialogTitle = SVNUIMessages.AddRepositoryLocationPage_ProjectList_Title;
			dialogDescription = SVNUIMessages.AddRepositoryLocationPage_ProjectList_Description;
			defaultMessage = differentUuid
					? SVNUIMessages.AddRepositoryLocationPage_ProjectList_Message1
					: SVNUIMessages.AddRepositoryLocationPage_ProjectList_Message2;
			resources = input;
		}

		@Override
		public void createControlsImpl(Composite parent) {
			ProjectListComposite composite = new ProjectListComposite(parent, SWT.FILL, resources, false);
			composite.initialize();
		}

		@Override
		protected void saveChangesImpl() {
		}

		@Override
		protected void cancelChangesImpl() {
		}
	}

	protected SVNEntryInfo getLocationInfo(IRepositoryLocation location) {
		ISVNConnector proxy = location.acquireSVNProxy();
		SVNEntryInfo[] infos = null;
		try {
			infos = SVNUtility.info(proxy, SVNUtility.getEntryRevisionReference(location.getRoot()), SVNDepth.EMPTY,
					new SVNNullProgressMonitor());
		} catch (Exception ex) {
			return null;
		} finally {
			location.releaseSVNProxy(proxy);
		}
		return infos != null && infos.length > 0 ? infos[0] : null;
	}

}
