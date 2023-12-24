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
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.composite.CommentComposite;
import org.eclipse.team.svn.ui.panel.AbstractDialogPanel;
import org.eclipse.team.svn.ui.verifier.AbsolutePathVerifier;
import org.eclipse.team.svn.ui.verifier.AbstractFormattedVerifier;
import org.eclipse.team.svn.ui.verifier.AbstractVerifier;
import org.eclipse.team.svn.ui.verifier.CompositeVerifier;
import org.eclipse.team.svn.ui.verifier.NonEmptyFieldVerifier;
import org.eclipse.team.svn.ui.verifier.ResourceNameVerifier;

/**
 * Abstract panel implementation that allows us to get resource name for remote resource
 * 
 * @author Alexander Gurov
 */
public abstract class AbstractGetResourceNamePanel extends AbstractDialogPanel {
	protected String resourceName;

	protected String disallowedName;

	protected Text text;

	protected CommentComposite comment;

	protected boolean allowMultipart;

	public AbstractGetResourceNamePanel(String title, boolean allowMultipart) {
		dialogTitle = title;
		this.allowMultipart = allowMultipart;
		defaultMessage = allowMultipart
				? SVNUIMessages.AbstractGetResourceNamePanel_Message_MultiPart
				: SVNUIMessages.AbstractGetResourceNamePanel_Message_Simple;
		resourceName = ""; //$NON-NLS-1$
	}

	public String getResourceName() {
		return resourceName.trim();
	}

	public String getMessage() {
		return comment.getMessage();
	}

	@Override
	public void createControlsImpl(Composite parent) {
		GridData data = null;
		GridLayout layout = null;

		Composite nameComposite = new Composite(parent, SWT.NONE);
		layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		nameComposite.setLayout(layout);
		data = new GridData(GridData.FILL_HORIZONTAL);
		nameComposite.setLayoutData(data);

		Label nameLabel = new Label(nameComposite, SWT.NONE);
		nameLabel.setText(SVNUIMessages.AbstractGetResourceNamePanel_Name);
		text = new Text(nameComposite, SWT.SINGLE | SWT.BORDER);
		data = new GridData(GridData.FILL_HORIZONTAL);
		text.setLayoutData(data);
		text.setEditable(true);
		CompositeVerifier verifier = new CompositeVerifier();
		verifier.add(createNonEmptyNameFieldVerifier());
		String name = SVNUIMessages.AbstractGetResourceNamePanel_Name_Verifier;
		verifier.add(new ResourceNameVerifier(name, allowMultipart));
		verifier.add(new AbsolutePathVerifier(name));
		verifier.add(new AbstractFormattedVerifier(name) {
			private String msg = SVNUIMessages.AbstractGetResourceNamePanel_Name_Verifier_Error;

			@Override
			protected String getErrorMessageImpl(Control input) {
				String text = getText(input);
				if (disallowedName != null && disallowedName.equals(text)) {
					return BaseMessages.format(msg, new Object[] { AbstractFormattedVerifier.FIELD_NAME, text });
				}
				return null;
			}

			@Override
			protected String getWarningMessageImpl(Control input) {
				return null;
			}
		});
		attachTo(text, verifier);

		Group group = new Group(parent, SWT.NULL);
		group.setLayout(new GridLayout());
		data = new GridData(GridData.FILL_BOTH);
		group.setLayoutData(data);
		group.setText(SVNUIMessages.AbstractGetResourceNamePanel_Comment);

		comment = new CommentComposite(group, this);
		data = new GridData(GridData.FILL_BOTH);
		comment.setLayoutData(data);
	}

	@Override
	public Point getPrefferedSizeImpl() {
		return new Point(525, SWT.DEFAULT);
	}

	@Override
	public void postInit() {
		super.postInit();
		comment.postInit(manager);
	}

	@Override
	protected void saveChangesImpl() {
		resourceName = text.getText();
		comment.saveChanges();
	}

	@Override
	protected void cancelChangesImpl() {
		comment.cancelChanges();
	}

	protected AbstractVerifier createNonEmptyNameFieldVerifier() {
		return new NonEmptyFieldVerifier(SVNUIMessages.AbstractGetResourceNamePanel_Name_Verifier);
	}

}
