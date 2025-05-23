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
 *    Alexander Gurov (Polarion Software) - initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.dialog;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;

/**
 * Prompt any option with toggle
 * 
 * @author Alexander Gurov
 */
public class PromptOptionDialog extends MessageDialogWithToggle {
	protected IOptionManager optionManager;

	public interface IOptionManager {
		String[] getButtons();

		void buttonPressed(IPreferenceStore store, int idx, boolean toggle);
	}

	public static abstract class AbstractOptionManager implements IOptionManager {
		@Override
		public String[] getButtons() {
			return new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL };
		}
	}

	public PromptOptionDialog(Shell parentShell, String title, String message, String toggleMessage,
			IOptionManager optionManager) {
		super(parentShell, title, null, message, MessageDialog.QUESTION, optionManager.getButtons(), 0, toggleMessage,
				false);
		this.optionManager = optionManager;
		setPrefStore(SVNTeamUIPlugin.instance().getPreferenceStore());
	}

	@Override
	protected void buttonPressed(int buttonId) {
		int idx = -1;
		for (int i = 0; i < getButtonLabels().length; i++) {
			int id = ((Integer) getButton(i).getData());
			if (id == buttonId) {
				idx = i;
				break;
			}
		}

		super.buttonPressed(buttonId);

		optionManager.buttonPressed(getPrefStore(), idx, getToggleState());
	}

}
