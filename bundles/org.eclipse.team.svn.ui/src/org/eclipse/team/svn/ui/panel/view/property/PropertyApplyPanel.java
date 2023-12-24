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
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.panel.view.property;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.composite.ApplyPropertyMethodComposite;
import org.eclipse.team.svn.ui.panel.AbstractDialogPanel;
import org.eclipse.team.svn.ui.properties.ResourcePropertyEditPanel;

/**
 * Apply property recursively panel
 *
 * @author Sergiy Logvin
 */
public class PropertyApplyPanel extends AbstractDialogPanel {

	protected ApplyPropertyMethodComposite applyComposite;

	public PropertyApplyPanel(boolean oneProperty) {
		dialogTitle = oneProperty
				? SVNUIMessages.PropertyApplyPanel_Title_Single
				: SVNUIMessages.PropertyApplyPanel_Title_Multi;
		dialogDescription = oneProperty
				? SVNUIMessages.PropertyApplyPanel_Description_Single
				: SVNUIMessages.PropertyApplyPanel_Description_Multi;
		defaultMessage = oneProperty
				? SVNUIMessages.PropertyApplyPanel_Message_Single
				: SVNUIMessages.PropertyApplyPanel_Message_Multi;
	}

	public int getApplyMethod() {
		return applyComposite.getApplyMethod();
	}

	public boolean useMask() {
		return applyComposite.useMask();
	}

	public String getFilterMask() {
		return applyComposite.getFilterMask();
	}

	@Override
	public void createControlsImpl(Composite parent) {
		applyComposite = new ApplyPropertyMethodComposite(parent, SWT.NONE, this,
				ResourcePropertyEditPanel.MIXED_RESOURCES);
	}

	@Override
	protected void cancelChangesImpl() {
	}

	@Override
	protected void saveChangesImpl() {
		applyComposite.saveChanges();
	}

}
