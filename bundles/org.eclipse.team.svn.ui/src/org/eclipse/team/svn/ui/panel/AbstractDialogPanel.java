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

package org.eclipse.team.svn.ui.panel;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.preferences.SVNTeamPreferences;
import org.eclipse.team.svn.ui.verifier.AbstractVerificationKeyListener;
import org.eclipse.team.svn.ui.verifier.AbstractVerifier;
import org.eclipse.team.svn.ui.verifier.IValidationManager;

/**
 * Abstract dialog panel
 * 
 * @author Alexander Gurov
 */
public abstract class AbstractDialogPanel implements IDialogPanel, IValidationManager {
	private AbstractVerificationKeyListener changeListener;

	protected IDialogManager manager;

	protected String dialogTitle;

	protected String dialogDescription;

	protected String defaultMessage;

	protected String imagePath;

	protected String[] buttonNames;

	protected Composite parent;

	public AbstractDialogPanel() {
		this(new String[] { IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL });
	}

	public AbstractDialogPanel(String[] buttonNames) {
		this.buttonNames = buttonNames;
		changeListener = new VerificationKeyListener();
		parent = null;
	}

	@Override
	public void initPanel(IDialogManager manager) {
		this.manager = manager;
	}

	@Override
	public void postInit() {
		validateContent();
		setMessage(IDialogManager.LEVEL_OK, null);
	}

	@Override
	public void addListeners() {
		changeListener.addListeners();
	}

	@Override
	public void dispose() {
		detachAll();
	}

	@Override
	public String getDialogTitle() {
		return dialogTitle;
	}

	@Override
	public String getDialogDescription() {
		return dialogDescription;
	}

	@Override
	public String getDefaultMessage() {
		return defaultMessage;
	}

	@Override
	public String getImagePath() {
		return imagePath;
	}

	protected String getDialogID() {
		return this.getClass().getName();
	}

	@Override
	public final Point getPrefferedSize() {
		IPreferenceStore store = SVNTeamUIPlugin.instance().getPreferenceStore();
		int width = SVNTeamPreferences.getDialogInt(store, getDialogID() + ".width"); //$NON-NLS-1$
		int height = SVNTeamPreferences.getDialogInt(store, getDialogID() + ".height"); //$NON-NLS-1$
		Point prefSize = getPrefferedSizeImpl();
		width = Math.max(width, prefSize.x);
		height = Math.max(height, prefSize.y);
		return new Point(width, height);
	}

	@Override
	public String[] getButtonNames() {
		return buttonNames;
	}

	@Override
	public String getHelpId() {
		return null;
	}

	@Override
	public final void createControls(Composite parent) {
		this.parent = parent;
		createControlsImpl(parent);
	}

	@Override
	public void buttonPressed(int idx) {
		if (idx == 0) {
			saveChanges();
		} else {
			cancelChanges();
		}
	}

	@Override
	public boolean isFilledRight() {
		return changeListener.isFilledRight();
	}

	@Override
	public void attachTo(Control cmp, AbstractVerifier verifier) {
		changeListener.attachTo(cmp, verifier);
	}

	@Override
	public void detachFrom(Control cmp) {
		changeListener.detachFrom(cmp);
	}

	@Override
	public void detachAll() {
		changeListener.detachAll();
	}

	@Override
	public void validateContent() {
		changeListener.validateContent();
	}

	@Override
	public boolean validateControl(Control cmp) {
		return changeListener.validateControl(cmp);
	}

	protected void setMessage(int level, String message) {
		manager.setMessage(level, message);
	}

	protected void setButtonsEnabled(boolean enabled) {

	}

	protected void retainSize() {
		IPreferenceStore store = SVNTeamUIPlugin.instance().getPreferenceStore();
		Point size = parent.getSize();
		SVNTeamPreferences.setDialogInt(store, getDialogID() + ".width", size.x); //$NON-NLS-1$
		SVNTeamPreferences.setDialogInt(store, getDialogID() + ".height", size.y); //$NON-NLS-1$
	}

	protected final void saveChanges() {
		retainSize();
		saveChangesImpl();
	}

	protected final void cancelChanges() {
		retainSize();
		cancelChangesImpl();
	}

	protected Point getPrefferedSizeImpl() {
		return new Point(470, SWT.DEFAULT);
	}

	protected abstract void saveChangesImpl();

	protected abstract void cancelChangesImpl();

	protected abstract void createControlsImpl(Composite parent);

	/*
	 * return false if dialog should not be closed
	 * override if needed
	 */
	@Override
	public boolean canClose() {
		return true;
	}

	public static String makeToBeOperatedMessage(IRepositoryResource[] resources) {
		String message;
		if (resources.length == 1) {
			message = SVNUIMessages.RepositoryTreePanel_Message_Single;
		} else if (resources.length < 5) {
			message = SVNUIMessages.RepositoryTreePanel_Message_UpTo4;
		} else {
			message = SVNUIMessages.RepositoryTreePanel_Message_Multi;
		}
		return BaseMessages.format(message, new String[] { FileUtility.getNamesListAsString(resources) });
	}

	protected class VerificationKeyListener extends AbstractVerificationKeyListener {
		public VerificationKeyListener() {
		}

		@Override
		public void hasError(String errorReason) {
			setMessage(IDialogManager.LEVEL_ERROR, errorReason);
			handleButtons();
		}

		@Override
		public void hasWarning(String warningReason) {
			setMessage(IDialogManager.LEVEL_WARNING, warningReason);
			handleButtons();
		}

		@Override
		public void hasNoError() {
			setMessage(IDialogManager.LEVEL_OK, null);
			handleButtons();
		}

		protected void handleButtons() {
			manager.setButtonEnabled(0, isFilledRight());
			setButtonsEnabled(isFilledRight());
		}

	}

}
