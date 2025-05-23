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

package org.eclipse.team.svn.ui.info;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.SVNTeamPlugin;
import org.eclipse.team.svn.core.SVNTeamProvider;
import org.eclipse.team.svn.core.connector.SVNConflictDescriptor;
import org.eclipse.team.svn.core.connector.SVNEntryInfo;
import org.eclipse.team.svn.core.connector.SVNLock;
import org.eclipse.team.svn.core.operation.IResourcePropertyProvider;
import org.eclipse.team.svn.core.operation.LoggedOperation;
import org.eclipse.team.svn.core.operation.local.InfoOperation;
import org.eclipse.team.svn.core.operation.local.property.GetPropertiesOperation;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.utility.SVNUtility;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.composite.PropertiesComposite;
import org.eclipse.team.svn.ui.utility.DateFormatter;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PropertyPage;

/**
 * This page allows to view working copy information for local resource
 * 
 * @author Alexander Gurov
 */
public class LocalInfoPage extends PropertyPage {
	protected PropertiesComposite properties;

	protected IResource resource;

	protected ILocalResource local;

	protected Button verifyTagButton;

	protected boolean isVerifyTagOnCommit;

	public LocalInfoPage() {
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		layout.numColumns = 2;
		composite.setLayout(layout);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		composite.setLayoutData(data);

		resource = getElement().getAdapter(IResource.class);
		InfoOperation op = new InfoOperation(resource);
		UIMonitorUtility.doTaskBusyDefault(op);

		local = op.getLocal();

		if (!(resource instanceof IProject)) {
			noDefaultAndApplyButton();
		}

		Label description = new Label(composite, SWT.WRAP);
		description.setLayoutData(new GridData());
		description.setText(SVNUIMessages.LocalInfoPage_LocalPath);

		Text content = new Text(composite, SWT.WRAP);
		data = new GridData();
		data.widthHint = 300;
		content.setLayoutData(data);
		content.setEditable(false);
		content.setText(resource.getFullPath().toString());

		//text status
		description = new Label(composite, SWT.WRAP);
		description.setLayoutData(new GridData());
		description.setText(SVNUIMessages.LocalInfoPage_TextStatus);

		content = new Text(composite, SWT.SINGLE);
		content.setLayoutData(new GridData());
		content.setEditable(false);
		content.setText(SVNUtility.getStatusText(local.getTextStatus()));

		//property status
		description = new Label(composite, SWT.WRAP);
		description.setLayoutData(new GridData());
		description.setText(SVNUIMessages.LocalInfoPage_PropertyStatus);

		content = new Text(composite, SWT.SINGLE);
		content.setLayoutData(new GridData());
		content.setEditable(false);
		content.setText(SVNUtility.getStatusText(local.getPropStatus()));

		//is copied
		description = new Label(composite, SWT.WRAP);
		description.setLayoutData(new GridData());
		description.setText(SVNUIMessages.LocalInfoPage_Copied);

		content = new Text(composite, SWT.SINGLE);
		content.setLayoutData(new GridData());
		content.setEditable(false);
		content.setText(String.valueOf(local.isCopied()));

		SVNEntryInfo info = op.getInfo();
		if (IStateFilter.SF_ONREPOSITORY.accept(local) && info != null) {
			// add space
			new Label(composite, SWT.WRAP);
			new Label(composite, SWT.WRAP);

			description = new Label(composite, SWT.WRAP);
			description.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
			description.setText(SVNUIMessages.LocalInfoPage_ResourceURL);

			content = new Text(composite, SWT.WRAP);
			data = new GridData();
			data.widthHint = 300;
			content.setLayoutData(data);
			content.setEditable(false);
			content.setText(SVNUtility.decodeURL(info.url));

			description = new Label(composite, SWT.WRAP);
			description.setLayoutData(new GridData());
			description.setText(SVNUIMessages.LocalInfoPage_Revision);

			content = new Text(composite, SWT.SINGLE);
			content.setLayoutData(new GridData());
			content.setEditable(false);
			content.setText(String.valueOf(info.revision));

			description = new Label(composite, SWT.WRAP);
			description.setLayoutData(new GridData());
			description.setText(SVNUIMessages.LocalInfoPage_LastChangedAuthor);

			content = new Text(composite, SWT.SINGLE);
			content.setLayoutData(new GridData());
			content.setEditable(false);
			content.setText(info.lastChangedAuthor == null ? SVNMessages.SVNInfo_NoAuthor : info.lastChangedAuthor);

			description = new Label(composite, SWT.WRAP);
			description.setLayoutData(new GridData());
			description.setText(SVNUIMessages.LocalInfoPage_LastChangedDate);

			content = new Text(composite, SWT.SINGLE);
			content.setLayoutData(new GridData());
			content.setEditable(false);
			content.setText(info.lastChangedDate == 0
					? SVNMessages.SVNInfo_NoDate
					: DateFormatter.formatDate(info.lastChangedDate));

			description = new Label(composite, SWT.WRAP);
			description.setLayoutData(new GridData());
			description.setText(SVNUIMessages.LocalInfoPage_LastChangedRevision);

			content = new Text(composite, SWT.SINGLE);
			content.setLayoutData(new GridData());
			content.setEditable(false);
			content.setText(String.valueOf(info.lastChangedRevision));

			SVNLock lock = info.lock;
			if (lock != null) {
				// add space
				new Label(composite, SWT.WRAP);
				new Label(composite, SWT.WRAP);

				description = new Label(composite, SWT.WRAP);
				description.setLayoutData(new GridData());
				description.setText(SVNUIMessages.LocalInfoPage_LockOwner);

				content = new Text(composite, SWT.SINGLE);
				content.setLayoutData(new GridData());
				content.setEditable(false);
				content.setText(lock.owner == null ? SVNMessages.SVNInfo_NoAuthor : lock.owner);

				description = new Label(composite, SWT.WRAP);
				description.setLayoutData(new GridData());
				description.setText(SVNUIMessages.LocalInfoPage_LockComment);

				content = new Text(composite, SWT.SINGLE);
				content.setLayoutData(new GridData());
				content.setEditable(false);
				content.setText(lock.comment == null ? SVNMessages.SVNInfo_NoComment : lock.comment);

				description = new Label(composite, SWT.WRAP);
				description.setLayoutData(new GridData());
				description.setText(SVNUIMessages.LocalInfoPage_LockCreationDate);

				content = new Text(composite, SWT.SINGLE);
				content.setLayoutData(new GridData());
				content.setEditable(false);
				content.setText(lock.creationDate == 0
						? SVNMessages.SVNInfo_NoAuthor
						: DateFormatter.formatDate(lock.creationDate));
				if (lock.expirationDate != 0) {
					description = new Label(composite, SWT.WRAP);
					description.setLayoutData(new GridData());
					description.setText(SVNUIMessages.LocalInfoPage_LockExpirationDate);

					content = new Text(composite, SWT.SINGLE);
					content.setLayoutData(new GridData());
					content.setEditable(false);
					content.setText(lock.expirationDate == 0
							? SVNMessages.SVNInfo_NoDate
							: DateFormatter.formatDate(lock.expirationDate));
				}
			}
		}

		//tree conflict
		if (local.hasTreeConflict()) {
			//add space
			new Label(composite, SWT.WRAP);
			new Label(composite, SWT.WRAP);
			description = new Label(composite, SWT.WRAP);
			description.setLayoutData(new GridData());
			description.setText(SVNUIMessages.LocalInfoPage_TreeConflict);

			content = new Text(composite, SWT.WRAP);
			data = new GridData();
			data.widthHint = 300;
			content.setLayoutData(data);
			content.setEditable(false);
			content.setText(getTreeConflictDescription(local.getTreeConflictDescriptor()));
		}

		createOptions(composite);

		if (IStateFilter.SF_VERSIONED.accept(local)) {
			//add space
			new Label(composite, SWT.WRAP);
			new Label(composite, SWT.WRAP);

			Composite group = new Composite(composite, SWT.BORDER);
			data = new GridData(GridData.FILL_BOTH);
			data.horizontalSpan = 2;
			group.setLayoutData(data);
			layout = new GridLayout();
			layout.marginHeight = layout.marginWidth = 0;
			group.setLayout(layout);
			properties = new PropertiesComposite(group);
			IResourcePropertyProvider propertyProvider = new GetPropertiesOperation(resource);
			UIMonitorUtility.doTaskBusyDefault(propertyProvider);
			properties.setResource(resource, propertyProvider);
			UIMonitorUtility.doTaskBusyDefault(properties.getRefreshViewOperation());
			properties.setLayoutData(new GridData(GridData.FILL_BOTH));
		}

//		Setting context help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "org.eclipse.team.svn.help.SVNInfoContext"); //$NON-NLS-1$

		return composite;
	}

	protected String getTreeConflictDescription(SVNConflictDescriptor conflictDescriptor) {
		String reason = ""; //$NON-NLS-1$
		String action = ""; //$NON-NLS-1$
		String operation = ""; //$NON-NLS-1$
		switch (conflictDescriptor.reason) {
			case MODIFIED:
				reason = "edit"; //$NON-NLS-1$
				break;
			case OBSTRUCTED:
				reason = "obstruction"; //$NON-NLS-1$
				break;
			case DELETED:
				reason = "delete"; //$NON-NLS-1$
				break;
			case MISSING:
				reason = "missing"; //$NON-NLS-1$
				break;
			case UNVERSIONED:
				reason = "unversioned"; //$NON-NLS-1$
				break;
			case ADDED:
				reason = "add"; //$NON-NLS-1$
				break;
			case MOVED_AWAY:
				reason = "moved away"; //$NON-NLS-1$
				break;
			case MOVED_HERE:
				reason = "moved here"; //$NON-NLS-1$
				break;
			case REPLACED:
				reason = "replaced"; //$NON-NLS-1$
				break;
		}
		switch (conflictDescriptor.action) {
			case MODIFY:
				action = "edit"; //$NON-NLS-1$
				break;
			case ADD:
				action = "add"; //$NON-NLS-1$
				break;
			case DELETE:
				action = "delete"; //$NON-NLS-1$
				break;
			case REPLACE:
				action = "replace"; //$NON-NLS-1$
				break;
		}
		switch (conflictDescriptor.operation) {
			case NONE:
				operation = "none"; //$NON-NLS-1$
				break;
			case UPDATE:
				operation = "update"; //$NON-NLS-1$
				break;
			case SWITCHED:
				operation = "switch"; //$NON-NLS-1$
				break;
			case MERGE:
				operation = "merge"; //$NON-NLS-1$
				break;
		}
		return BaseMessages.format(SVNUIMessages.LocalInfoPage_TreeConflictDescription,
				new String[] { reason, action, operation });
	}

	protected void createOptions(Composite parent) {
		if (resource instanceof IProject) {
			SVNTeamProvider provider = (SVNTeamProvider) RepositoryProvider.getProvider((IProject) resource,
					SVNTeamPlugin.NATURE_ID);

			//add space
			new Label(parent, SWT.WRAP);
			new Label(parent, SWT.WRAP);

			verifyTagButton = new Button(parent, SWT.CHECK);
			GridData data = new GridData();
			data.horizontalSpan = 2;
			verifyTagButton.setLayoutData(data);
			verifyTagButton.setText(SVNUIMessages.LocalInfoPage_VerifyTagModification);
			verifyTagButton.addListener(SWT.Selection, event -> isVerifyTagOnCommit = verifyTagButton.getSelection());

			verifyTagButton.setSelection(isVerifyTagOnCommit = provider.isVerifyTagOnCommit());
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performOk()
	 */
	@Override
	public boolean performOk() {
		if (resource instanceof IProject) {
			SVNTeamProvider provider = (SVNTeamProvider) RepositoryProvider.getProvider((IProject) resource,
					SVNTeamPlugin.NATURE_ID);
			if (isVerifyTagOnCommit != provider.isVerifyTagOnCommit()) {
				try {
					provider.setVerifyTagOnCommit(isVerifyTagOnCommit);
				} catch (CoreException e) {
					LoggedOperation.reportError(this.getClass().getName(), e);
				}
			}
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
	 */
	@Override
	protected void performDefaults() {
		super.performDefaults();

		verifyTagButton.setSelection(isVerifyTagOnCommit = SVNTeamProvider.DEFAULT_VERIFY_TAG_ON_COMMIT);
	}

}
