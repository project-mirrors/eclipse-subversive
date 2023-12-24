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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.resource.ISVNStorage;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.panel.AbstractDialogPanel;
import org.eclipse.team.svn.ui.verifier.AbstractVerifierProxy;
import org.eclipse.team.svn.ui.verifier.PatternVerifier;

/**
 * Ignore method selection panel
 * 
 * @author Alexander Gurov
 */
public class IgnoreMethodPanel extends AbstractDialogPanel {
	protected int ignoreType;

	protected String ignorePattern;

	protected IResource[] resources;

	protected Button patternButton;

	protected Text ignorePatternField;

	public IgnoreMethodPanel(IResource[] resources) {
		if (resources.length == 1) {
			dialogTitle = SVNUIMessages.IgnoreMethodPanel_Title_Single;
			dialogDescription = SVNUIMessages.IgnoreMethodPanel_Description_Single;
		} else {
			dialogTitle = SVNUIMessages.IgnoreMethodPanel_Title_Multi;
			dialogDescription = SVNUIMessages.IgnoreMethodPanel_Description_Multi;
		}
		defaultMessage = SVNUIMessages.IgnoreMethodPanel_Message;

		ignoreType = ISVNStorage.IGNORE_NAME;
		ignorePattern = null;
		this.resources = resources;
	}

	public int getIgnoreType() {
		return ignoreType;
	}

	public String getIgnorePattern() {
		return ignorePattern;
	}

	@Override
	public void createControlsImpl(Composite parent) {
		GridData data = null;

		Button nameButton = new Button(parent, SWT.RADIO);
		data = new GridData(GridData.FILL_HORIZONTAL);
		nameButton.setLayoutData(data);
		String text = BaseMessages.format(resources.length == 1
				? SVNUIMessages.IgnoreMethodPanel_Name_Single
				: SVNUIMessages.IgnoreMethodPanel_Name_Multi, new String[] { resources[0].getName() });
		nameButton.setText(text);
		nameButton.setSelection(true);
		nameButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ignoreType = ISVNStorage.IGNORE_NAME;
				ignorePatternField.setEnabled(false);
			}
		});

		Button extensionButton = new Button(parent, SWT.RADIO);
		data = new GridData(GridData.FILL_HORIZONTAL);
		extensionButton.setLayoutData(data);
		String extension = null;
		for (IResource element : resources) {
			if (extension == null) {
				extension = element.getFileExtension();
			} else {
				break;
			}
		}
		text = BaseMessages.format(
				resources.length == 1
						? SVNUIMessages.IgnoreMethodPanel_Extension_Single
						: SVNUIMessages.IgnoreMethodPanel_Extension_Multi,
				new String[] { extension == null ? "" : extension }); //$NON-NLS-1$
		extensionButton.setText(text);
		extensionButton.setSelection(false);
		extensionButton.setEnabled(extension != null);
		extensionButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ignoreType = ISVNStorage.IGNORE_EXTENSION;
				ignorePatternField.setEnabled(false);
			}
		});

		patternButton = new Button(parent, SWT.RADIO);
		data = new GridData(GridData.FILL_HORIZONTAL);
		patternButton.setLayoutData(data);
		text = BaseMessages.format(SVNUIMessages.IgnoreMethodPanel_Pattern,
				new String[] { resources[0].getName().substring(1) });
		patternButton.setText(text);
		patternButton.setSelection(false);
		patternButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ignoreType = ISVNStorage.IGNORE_PATTERN;
				ignorePatternField.setEnabled(true);
			}
		});

		ignorePatternField = new Text(parent, SWT.SINGLE | SWT.BORDER);
		data = new GridData(GridData.FILL_HORIZONTAL);
		ignorePatternField.setLayoutData(data);
		ignorePatternField.setEnabled(false);
		ignorePatternField.setText(resources[0].getName());
		attachTo(ignorePatternField, new AbstractVerifierProxy(
				new PatternVerifier(SVNUIMessages.IgnoreMethodPanel_Pattern_Verifier, resources)) {
			@Override
			protected boolean isVerificationEnabled(Control input) {
				return patternButton.getSelection();
			}
		});
	}

	@Override
	public String getHelpId() {
		return "org.eclipse.team.svn.help.addToIgnoreDialogContext"; //$NON-NLS-1$
	}

	@Override
	public Point getPrefferedSizeImpl() {
		return new Point(470, 130);
	}

	@Override
	protected void saveChangesImpl() {
		ignorePattern = patternButton.getSelection() ? ignorePatternField.getText() : null;
	}

	@Override
	protected void cancelChangesImpl() {
	}

}
