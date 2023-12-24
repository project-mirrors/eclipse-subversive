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
 *    Igor Burilo - Initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.composite;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.utility.SVNUtility;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.panel.common.RepositoryTreePanel;
import org.eclipse.team.svn.ui.verifier.AbsolutePathVerifier;
import org.eclipse.team.svn.ui.verifier.CompositeVerifier;
import org.eclipse.team.svn.ui.verifier.IValidationManager;
import org.eclipse.team.svn.ui.verifier.NonEmptyFieldVerifier;
import org.eclipse.team.svn.ui.verifier.URLVerifier;

/**
 * 
 * Repository resource selection composite
 * 
 * In contrast to RepositoryResourceSelectionComposite it doesn't contain any revision controls
 * 
 * @author Igor Burilo
 *
 */
public class RepositoryResourceOnlySelectionComposite extends RepositoryResourceBaseSelectionComposite {
	/*
	 * Flag which determines whether to verify that selected
	 * resource starts with base resource.
	 * This can be used if we want that selected resource only starts
	 * with base resource but not with repository root corresponding to base resource
	 */
	protected boolean isMatchToBaseResource;

	public RepositoryResourceOnlySelectionComposite(Composite parent, int style, IValidationManager validationManager,
			String historyKey, IRepositoryResource baseResource, String selectionTitle, String selectionDescription) {
		this(parent, style, validationManager, historyKey, "RepositoryResourceOnlySelectionComposite_URL", baseResource, //$NON-NLS-1$
				selectionTitle, selectionDescription);
	}

	public RepositoryResourceOnlySelectionComposite(Composite parent, int style, IValidationManager validationManager,
			String historyKey, String comboId, IRepositoryResource baseResource, String selectionTitle,
			String selectionDescription) {
		super(parent, style, validationManager, historyKey, comboId, baseResource, selectionTitle,
				selectionDescription);

		createControls();
	}

	public void setMatchToBaseResource(boolean isMatchToBaseResource) {
		this.isMatchToBaseResource = isMatchToBaseResource;
	}

	public boolean isMatchToBaseResource() {
		return isMatchToBaseResource;
	}

	private void createControls() {
		GridLayout layout = null;
		GridData data = null;

		layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = layout.marginWidth = 0;
		setLayout(layout);

		Label urlLabel = new Label(this, SWT.NONE);
		urlLabel.setLayoutData(new GridData());
		urlLabel.setText(SVNUIMessages.getString(comboId));

		Composite select = new Composite(this, SWT.NONE);
		layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		select.setLayout(layout);
		data = new GridData(GridData.FILL_HORIZONTAL);
		select.setLayoutData(data);

		urlText = new Combo(select, SWT.NULL);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		urlText.setLayoutData(data);
		urlText.setVisibleItemCount(urlHistory.getDepth());
		urlText.setItems(urlHistory.getHistory());

		if (baseResource != null) {
			urlText.setText(baseResource.getUrl());
		}

		url = urlText.getText();

		Listener urlTextListener = e -> RepositoryResourceOnlySelectionComposite.this.url = ((Combo) e.widget).getText();
		urlText.addListener(SWT.Selection, urlTextListener);
		urlText.addListener(SWT.Modify, urlTextListener);

		verifier = new CompositeVerifier();
		verifier.add(new NonEmptyFieldVerifier(SVNUIMessages.getString(comboId + "_Verifier"))); //$NON-NLS-1$
		verifier.add(new URLVerifier(SVNUIMessages.getString(comboId + "_Verifier")) { //$NON-NLS-1$
			@Override
			protected String getErrorMessage(Control input) {
				String error = super.getErrorMessage(input);
				if (RepositoryResourceOnlySelectionComposite.this.baseResource != null && error == null) {
					String url = getText(input);
					if (RepositoryResourceOnlySelectionComposite.this.getDestination(SVNUtility.asEntryReference(url),
							true) == null) {
						error = BaseMessages.format(
								SVNUIMessages.RepositoryResourceOnlySelectionComposite_URL_Verifier_Error,
								new String[] { url,
										RepositoryResourceOnlySelectionComposite.this.baseResource
												.getRepositoryLocation()
												.getUrl() });
					}

					//check that resource starts with location
					if (error == null && isMatchToBaseResource) {
						String baseResourceUrl = RepositoryResourceOnlySelectionComposite.this.baseResource.getUrl();
						if (!url.startsWith(baseResourceUrl)) {
							error = BaseMessages.format(
									SVNUIMessages.RepositoryResourceOnlySelectionComposite_URL_Verifier_Error,
									new String[] { url, baseResourceUrl });
						}
					}
				}
				return error;
			}
		});
		verifier.add(new AbsolutePathVerifier(comboId));
		validationManager.attachTo(urlText, verifier);

		browse = new Button(select, SWT.PUSH);
		browse.setText(SVNUIMessages.Button_Browse);
		data = new GridData();
		data.widthHint = DefaultDialog.computeButtonWidth(browse);
		browse.setLayoutData(data);
		browse.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				RepositoryTreePanel panel = new RepositoryTreePanel(
						SVNUIMessages.RepositoryResourceOnlySelectionComposite_Select_Title,
						RepositoryResourceOnlySelectionComposite.this.selectionTitle,
						RepositoryResourceOnlySelectionComposite.this.selectionDescription, null, true,
						RepositoryResourceOnlySelectionComposite.this.baseResource, false);
				panel.setAllowFiles(!RepositoryResourceOnlySelectionComposite.this.foldersOnly);
				DefaultDialog browser = new DefaultDialog(RepositoryResourceOnlySelectionComposite.this.getShell(),
						panel);
				if (browser.open() == 0) {
					IRepositoryResource selectedResource = panel.getSelectedResource();
					boolean samePeg = RepositoryResourceOnlySelectionComposite.this.baseResource != null
							&& selectedResource.getPegRevision()
									.equals(RepositoryResourceOnlySelectionComposite.this.baseResource
											.getPegRevision());
					RepositoryResourceOnlySelectionComposite.this.urlText.setText(samePeg
							? selectedResource.getUrl()
							: SVNUtility.getEntryReference(selectedResource).toString());
					RepositoryResourceOnlySelectionComposite.this.validationManager.validateContent();
				}
			}
		});
	}

}
