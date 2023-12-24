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
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.connector.SVNRevisionRange;
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
 * Repository resource selection composite
 * 
 * @author Alexander Gurov
 */
public class RepositoryResourceSelectionComposite extends RepositoryResourceBaseSelectionComposite {
	public static final int MODE_DEFAULT = 0;

	public static final int MODE_TWO = 1;

	public static final int MODE_CHECK = 2;

	public static final int TEXT_NONE = 0;

	public static final int TEXT_BASE = 1;

	public static final int TEXT_LAST = 2;

	protected RevisionComposite revisionComposite;

	protected RevisionComposite secondRevisionComposite;

	protected boolean stopOnCopy;

	protected boolean toFilterCurrent;

	protected int mode;

	protected int defaultTextType;

	public RepositoryResourceSelectionComposite(Composite parent, int style, IValidationManager validationManager,
			String historyKey, IRepositoryResource baseResource, boolean stopOnCopy, String selectionTitle,
			String selectionDescription, int mode, int defaultTextType) {
		this(parent, style, validationManager, historyKey, "RepositoryResourceSelectionComposite_URL", baseResource, //$NON-NLS-1$
				stopOnCopy, selectionTitle, selectionDescription, mode, defaultTextType);
	}

	public RepositoryResourceSelectionComposite(Composite parent, int style, IValidationManager validationManager,
			String historyKey, String comboId, IRepositoryResource baseResource, boolean stopOnCopy,
			String selectionTitle, String selectionDescription, int mode, int defaultTextType) {
		super(parent, style, validationManager, historyKey, comboId, baseResource, selectionTitle,
				selectionDescription);
		this.stopOnCopy = stopOnCopy;
		toFilterCurrent = false;
		this.mode = mode;
		this.defaultTextType = defaultTextType;

		createControls(defaultTextType);
	}

	@Override
	protected void setBaseResourceImpl() {
		if (revisionComposite != null) {
			revisionComposite.setBaseResource(baseResource);
		}
		if (secondRevisionComposite != null) {
			secondRevisionComposite.setBaseResource(baseResource);
		}
		if (defaultTextType == RepositoryResourceSelectionComposite.TEXT_BASE && baseResource != null) {
			super.setBaseResourceImpl();
		}
	}

	public void setFilterCurrent(boolean toFilter) {
		toFilterCurrent = toFilter;
		revisionComposite.setFilterCurrent(toFilterCurrent);
	}

	public boolean isReverseRevisions() {
		return revisionComposite.isReverseRevisions();
	}

	public boolean isReverseSecondResourceRevisions() {
		return secondRevisionComposite != null ? secondRevisionComposite.isReverseRevisions() : false;
	}

	@Override
	public IRepositoryResource getSelectedResource() {
		IRepositoryResource resource = super.getSelectedResource();
		resource.setSelectedRevision(revisionComposite.getSelectedRevision());
		return resource;
	}

	public IRepositoryResource getSecondSelectedResource() {
		if (secondRevisionComposite == null) {
			return null;
		}
		IRepositoryResource resource = super.getSelectedResource();
		resource.setSelectedRevision(secondRevisionComposite.getSelectedRevision());
		return resource;
	}

	public SVNRevisionRange[] getSelectedRevisions() {
		if (mode == MODE_CHECK) {
			return revisionComposite.getSelectedRevisions();
		}
		SVNRevision first = getSelectedResource().getSelectedRevision();
		SVNRevision second = getSecondSelectedRevision();
		return new SVNRevisionRange[] { new SVNRevisionRange(first, second == null ? first : second) };
	}

	public SVNRevision getStartRevision() {
		return revisionComposite.getSelectedRevision();
	}

	public SVNRevision getSecondSelectedRevision() {
		if (secondRevisionComposite == null) {
			return null;
		}
		return secondRevisionComposite.getSelectedRevision();
	}

	public void setCurrentRevision(long currentRevision) {
		revisionComposite.setCurrentRevision(currentRevision);
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		revisionComposite.setEnabled(enabled);
		if (secondRevisionComposite != null) {
			secondRevisionComposite.setEnabled(enabled);
		}
	}

	private void createControls(int defaultTextType) {
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
		if (defaultTextType == RepositoryResourceSelectionComposite.TEXT_BASE && baseResource != null) {
			urlText.setText(baseResource.getUrl());
		} else if (defaultTextType == RepositoryResourceSelectionComposite.TEXT_LAST && urlText.getItemCount() > 0) {
			urlText.select(0);
		}
		url = urlText.getText();

		Listener urlTextListener = e -> {
			RepositoryResourceSelectionComposite.this.url = ((Combo) e.widget).getText();
			if (RepositoryResourceSelectionComposite.this.isSelectionAvailable()) {
				revisionComposite
						.setSelectedResource(RepositoryResourceSelectionComposite.this.getSelectedResource());
				boolean toFilter = toFilterCurrent && RepositoryResourceSelectionComposite.this.baseResource != null
						&& (RepositoryResourceSelectionComposite.this.getSelectedResource()
								.getUrl()
								.equals(RepositoryResourceSelectionComposite.this.baseResource.getUrl())
								|| RepositoryResourceSelectionComposite.this.getSelectedResource()
										.getUrl()
										.equals(RepositoryResourceSelectionComposite.this.baseResource.getUrl()
												+ "/")); //$NON-NLS-1$
				revisionComposite.setFilterCurrent(toFilter);
				if (secondRevisionComposite != null) {
					secondRevisionComposite.setSelectedResource(
							RepositoryResourceSelectionComposite.this.getSecondSelectedResource());
					secondRevisionComposite.setFilterCurrent(toFilter);
				}
			}
		};
		urlText.addListener(SWT.Modify, urlTextListener);
		urlText.addListener(SWT.Selection, urlTextListener);

		verifier = new CompositeVerifier() {
			@Override
			protected void fireError(String errorReason) {
				revisionComposite.setEnabled(false);
				if (secondRevisionComposite != null) {
					secondRevisionComposite.setEnabled(false);
				}
				super.fireError(errorReason);
			}

			@Override
			protected void fireOk() {
				revisionComposite.setEnabled(true);
				if (secondRevisionComposite != null) {
					secondRevisionComposite.setEnabled(true);
				}
				super.fireOk();
			}
		};
		verifier.add(new NonEmptyFieldVerifier(SVNUIMessages.getString(comboId + "_Verifier"))); //$NON-NLS-1$
		verifier.add(new URLVerifier(SVNUIMessages.getString(comboId + "_Verifier")) { //$NON-NLS-1$
			@Override
			protected String getErrorMessage(Control input) {
				String error = super.getErrorMessage(input);
				if (RepositoryResourceSelectionComposite.this.baseResource != null && error == null) {
					String url = getText(input);
					if (RepositoryResourceSelectionComposite.this.getDestination(SVNUtility.asEntryReference(url),
							true) == null) {
						error = BaseMessages
								.format(SVNUIMessages.RepositoryResourceSelectionComposite_URL_Verifier_Error,
										new String[] { url,
												RepositoryResourceSelectionComposite.this.baseResource
														.getRepositoryLocation()
														.getUrl() });
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
						SVNUIMessages.RepositoryResourceSelectionComposite_Select_Title,
						RepositoryResourceSelectionComposite.this.selectionTitle,
						RepositoryResourceSelectionComposite.this.selectionDescription,
						RepositoryResourceSelectionComposite.this.baseResource == null
								? new IRepositoryResource[0]
								: new IRepositoryResource[] {
										RepositoryResourceSelectionComposite.this.getSelectedResource() },
						true, true);
				panel.setAllowFiles(!RepositoryResourceSelectionComposite.this.foldersOnly);
				DefaultDialog browser = new DefaultDialog(RepositoryResourceSelectionComposite.this.getShell(), panel);
				if (browser.open() == 0) {
					IRepositoryResource selectedResource = panel.getSelectedResource();
					boolean samePeg = RepositoryResourceSelectionComposite.this.baseResource != null
							&& selectedResource.getPegRevision()
									.equals(RepositoryResourceSelectionComposite.this.baseResource.getPegRevision());
					RepositoryResourceSelectionComposite.this.urlText.setText(samePeg
							? selectedResource.getUrl()
							: SVNUtility.getEntryReference(selectedResource).toString());
					revisionComposite.setSelectedResource(selectedResource);
					if (secondRevisionComposite != null) {
						secondRevisionComposite.setSelectedResource(selectedResource);
					}
					RepositoryResourceSelectionComposite.this.validationManager.validateContent();
				}
			}
		});

		Composite revisions = new Composite(this, SWT.NONE);
		layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		layout.numColumns = 2;
		revisions.setLayout(layout);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		revisions.setLayoutData(data);
		String revTitle = SVNUIMessages.RevisionComposite_Revision;
		if (mode == MODE_TWO) {
			revTitle = SVNUIMessages.RepositoryResourceSelectionComposite_StartRevision;
		} else if (mode == MODE_CHECK) {
			revTitle = SVNUIMessages.RevisionComposite_Revisions;
		}
		String revHeadName = mode == MODE_CHECK
				? SVNUIMessages.RevisionComposite_All
				: SVNUIMessages.RevisionComposite_HeadRevision;
		revisionComposite = new RevisionComposite(revisions, validationManager, stopOnCopy,
				new String[] { revTitle, revHeadName }, SVNRevision.HEAD, mode == MODE_CHECK) {
			@Override
			public void additionalValidation() {
				RepositoryResourceSelectionComposite.this.validateRevisions();
			}
		};
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = mode == MODE_TWO ? 1 : 2;
		revisionComposite.setLayoutData(data);
		revisionComposite.setBaseResource(baseResource);
		if (baseResource != null) {
			revisionComposite.setSelectedResource(getSelectedResource());
		}
		if (mode == MODE_TWO) {
			secondRevisionComposite = new RevisionComposite(revisions, validationManager, stopOnCopy,
					new String[] { SVNUIMessages.RepositoryResourceSelectionComposite_StopRevision,
							SVNUIMessages.RepositoryResourceSelectionComposite_HeadRevision },
					SVNRevision.HEAD, false) {
				@Override
				public void additionalValidation() {
					RepositoryResourceSelectionComposite.this.validateRevisions();
				}
			};
			data = new GridData(GridData.FILL_HORIZONTAL);
			secondRevisionComposite.setLayoutData(data);
			secondRevisionComposite.setBaseResource(baseResource);
			secondRevisionComposite.setSelectedResource(getSelectedResource());
		}
	}

	protected void validateRevisions() {
		if ((mode & MODE_TWO) != 0) {
			validationManager.validateContent();
		}
	}

}
