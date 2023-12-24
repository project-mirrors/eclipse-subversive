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

package org.eclipse.team.svn.ui.panel.local;

import org.eclipse.core.resources.IResource;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.composite.ResourceSelectionComposite;
import org.eclipse.team.svn.ui.panel.AbstractDialogPanel;
import org.eclipse.team.svn.ui.panel.participant.BasePaneParticipant;
import org.eclipse.team.svn.ui.panel.participant.PaneParticipantHelper;
import org.eclipse.team.svn.ui.panel.participant.PaneParticipantHelper.PaneVerifier;
import org.eclipse.team.svn.ui.verifier.AbstractVerifier;

/**
 * Abstract resource selection panel implementation
 * 
 * @author Alexander Gurov
 */
public abstract class AbstractResourceSelectionPanel extends AbstractDialogPanel {
	protected IResource[] resources;

	protected ResourceSelectionComposite selectionComposite;

//	protected int subPathStart;	// common root length, unfortunately doesn't work with more than one repository location
	protected IResource[] userSelectedResources;

	protected PaneParticipantHelper paneParticipantHelper;

	public AbstractResourceSelectionPanel(IResource[] resources, IResource[] userSelectedResources,
			String[] buttonNames) {
		super(buttonNames);
		this.resources = resources;
		this.userSelectedResources = userSelectedResources;

		paneParticipantHelper = new PaneParticipantHelper();
	}

	public IResource[] getSelectedResources() {
		if (paneParticipantHelper.isParticipantPane()) {
			return paneParticipantHelper.getSelectedResources();
		}
		return selectionComposite.getSelectedResources();
	}

	public IResource[] getNotSelectedResources() {
		if (paneParticipantHelper.isParticipantPane()) {
			return paneParticipantHelper.getNotSelectedResources();
		}
		return selectionComposite.getNotSelectedResources();
	}

	public IResource[] getTreatAsEdits() {
		return paneParticipantHelper.isParticipantPane() ? new IResource[0] : selectionComposite.getTreatAsEdits();
	}

	@Override
	public Point getPrefferedSizeImpl() {
		return new Point(600, SWT.DEFAULT);
	}

	@Override
	public void createControlsImpl(Composite parent) {
		if (paneParticipantHelper.isParticipantPane()) {
			paneParticipantHelper.init(createPaneParticipant());
			createPaneControls(parent);
		} else {
			selectionComposite = new ResourceSelectionComposite(parent, SWT.NONE, resources, false,
					userSelectedResources, false);
			GridData data = new GridData(GridData.FILL_BOTH);
			data.heightHint = 210;
			selectionComposite.setLayoutData(data);
			selectionComposite.addResourcesSelectionChangedListener(event -> AbstractResourceSelectionPanel.this.validateContent());
			attachTo(selectionComposite, new AbstractVerifier() {
				@Override
				protected String getErrorMessage(Control input) {
					IResource[] selection = AbstractResourceSelectionPanel.this.getSelectedResources();
					if (selection == null || selection.length == 0) {
						return SVNUIMessages.ResourceSelectionComposite_Verifier_Error;
					}
					return null;
				}

				@Override
				protected String getWarningMessage(Control input) {
					return null;
				}
			});
			addContextMenu();
		}
	}

	protected void createPaneControls(Composite parent) {
		Control paneControl = paneParticipantHelper.createChangesPage(parent);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.heightHint = 210;
		paneControl.setLayoutData(data);

		paneParticipantHelper.initListeners();

		//add validator to pane
		attachTo(paneControl, new PaneVerifier(paneParticipantHelper));
	}

	@Override
	public void postInit() {
		super.postInit();
		if (paneParticipantHelper.isParticipantPane()) {
			paneParticipantHelper.expandPaneTree();
		}
	}

	@Override
	public void dispose() {
		super.dispose();
		if (paneParticipantHelper.isParticipantPane()) {
			paneParticipantHelper.dispose();
		}
	}

	@Override
	protected void saveChangesImpl() {
	}

	@Override
	protected void cancelChangesImpl() {
	}

	protected void addContextMenu() {
	}

	protected abstract BasePaneParticipant createPaneParticipant();
}
