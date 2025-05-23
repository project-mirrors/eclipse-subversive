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

package org.eclipse.team.svn.ui.panel.remote;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.composite.DiffFormatComposite;
import org.eclipse.team.svn.ui.composite.RepositoryResourceSelectionComposite;
import org.eclipse.team.svn.ui.panel.common.AbstractRepositoryResourceSelectionPanel;
import org.eclipse.team.svn.ui.utility.InputHistory;

/**
 * Compare operation repository resource selection panel
 * 
 * @author Alexander Gurov
 */
public class ComparePanel extends AbstractRepositoryResourceSelectionPanel {

	protected DiffFormatComposite diffFormatComposite;

	protected long options;

	protected Button ignoreAncestryButton;

	protected InputHistory ignoreHistory;

	public ComparePanel(IRepositoryResource baseResource) {
		super(baseResource, SVNRevision.INVALID_REVISION_NUMBER, SVNUIMessages.ComparePanel_Title,
				SVNUIMessages.ComparePanel_Description, "compareUrl", SVNUIMessages.ComparePanel_Selection_Title, //$NON-NLS-1$
				SVNUIMessages.ComparePanel_Selection_Description, RepositoryResourceSelectionComposite.TEXT_BASE);
		defaultMessage = SVNUIMessages.ComparePanel_Message;
	}

	public ComparePanel(IRepositoryResource baseResource, long revision) {
		super(baseResource, revision, SVNUIMessages.ComparePanel_Title, SVNUIMessages.ComparePanel_Description,
				"compareUrl", SVNUIMessages.ComparePanel_Selection_Title, //$NON-NLS-1$
				SVNUIMessages.ComparePanel_Selection_Description, RepositoryResourceSelectionComposite.TEXT_BASE);
		defaultMessage = SVNUIMessages.ComparePanel_Message;
	}

	@Override
	public String getHelpId() {
		return "org.eclipse.team.svn.help.remote_compareDialogContext"; //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.svn.ui.panel.common.AbstractRepositoryResourceSelectionPanel#createControlsImpl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControlsImpl(Composite parent) {
		super.createControlsImpl(parent);

		GridData data = new GridData();
		ignoreAncestryButton = new Button(parent, SWT.CHECK);
		ignoreAncestryButton.setLayoutData(data);
		ignoreAncestryButton.setText(SVNUIMessages.MergePanel_Button_IgnoreAncestry);
		ignoreHistory = new InputHistory("ignoreAncestry", InputHistory.TYPE_BOOLEAN, //$NON-NLS-1$
				(options & ISVNConnector.Options.IGNORE_ANCESTRY) != 0);
		ignoreAncestryButton.setSelection((Boolean) ignoreHistory.getValue());

		diffFormatComposite = new DiffFormatComposite(parent, this);
	}

	public String getDiffFile() {
		return diffFormatComposite.getDiffFile();
	}

	public long getDiffOptions() {
		return options;
	}

	@Override
	protected void saveChangesImpl() {
		super.saveChangesImpl();
		options |= ignoreAncestryButton.getSelection()
				? ISVNConnector.Options.IGNORE_ANCESTRY
				: ISVNConnector.Options.NONE;
		ignoreHistory.setValue(ignoreAncestryButton.getSelection());
	}

}
