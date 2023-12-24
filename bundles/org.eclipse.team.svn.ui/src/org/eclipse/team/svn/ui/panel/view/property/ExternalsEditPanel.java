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

package org.eclipse.team.svn.ui.panel.view.property;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.extension.CoreExtensionsManager;
import org.eclipse.team.svn.core.extension.factory.ISVNConnectorFactory;
import org.eclipse.team.svn.core.resource.IRepositoryFolder;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.utility.SVNUtility;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.composite.RevisionComposite;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.panel.AbstractDialogPanel;
import org.eclipse.team.svn.ui.utility.UserInputHistory;
import org.eclipse.team.svn.ui.verifier.AbstractFormattedVerifier;
import org.eclipse.team.svn.ui.verifier.CompositeVerifier;
import org.eclipse.team.svn.ui.verifier.NonEmptyFieldVerifier;
import org.eclipse.team.svn.ui.verifier.URLVerifier;
import org.eclipse.team.svn.ui.wizard.SelectRepositoryResourceWizard;

/**
 * Set externals property panel
 * 
 * @author Igor Burilo
 */
public class ExternalsEditPanel extends AbstractDialogPanel {

	protected IResource resource;

	protected IRepositoryResource repositoryResource;

	protected Combo urlText;

	protected Button browse;

	protected UserInputHistory urlHistory;

	protected String comboId;

	protected Button folderButton;

	protected Text localPathText;

	protected RevisionComposite revisionComposite;

	protected Button priorToSVN15FormatButton;

	protected Combo relativeText;

	protected Button formatButton;

	protected String localPath;

	protected boolean priorToSVN15Format;

	protected String url;

	protected SVNRevision revision;

	protected boolean isFolder;

	protected String processedUrl;

	protected IRepositoryResource repositoryResourceForUrl;

	protected final static int REPOSITORY_ROOT_INDEX = 0;

	protected final static int URL_SCHEME_INDEX = 1;

	protected final static int HOST_NAME_INDEX = 2;

	protected final static int EXTERNAL_DIRECTORY_INDEX = 3;

	public ExternalsEditPanel(String historyKey, String comboId, IResource resource,
			IRepositoryResource repositoryResource) {
		this.resource = resource;
		this.repositoryResource = repositoryResource;

		dialogTitle = SVNUIMessages.ExternalsEditPanel_DialogTitle;
		dialogDescription = SVNUIMessages.ExternalsEditPanel_DialogDescription;
		defaultMessage = SVNUIMessages.ExternalsEditPanel_DialogDefaultMessage;

		urlHistory = new UserInputHistory(historyKey);
		this.comboId = comboId;
	}

	@Override
	protected void createControlsImpl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		layout.numColumns = 3;
		composite.setLayout(layout);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		composite.setLayoutData(data);

		createLocalPathSelectionControls(composite);

		createRepositoryResourceSelectionControls(composite);

		revisionComposite = new RevisionComposite(composite, this, true,
				new String[] { SVNUIMessages.RevisionComposite_Revision,
						SVNUIMessages.RepositoryResourceSelectionComposite_HeadRevision },
				SVNRevision.INVALID_REVISION, false, false);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 3;
		revisionComposite.setLayoutData(data);

		priorToSVN15FormatButton = new Button(composite, SWT.CHECK);
		data = new GridData();
		data.horizontalSpan = 3;
		priorToSVN15FormatButton.setLayoutData(data);
		priorToSVN15FormatButton.setText(SVNUIMessages.ExternalsEditPanel_PriortoSVN15);
		priorToSVN15FormatButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				priorToSVN15Format = priorToSVN15FormatButton.getSelection();
				ExternalsEditPanel.this.enableFormatUrl();
				ExternalsEditPanel.this.enableIsFolder();
				ExternalsEditPanel.this.onChangeUrlText();
				ExternalsEditPanel.this.validateContent();
			}
		});

		initValues();
	}

	protected void initValues() {
		if (CoreExtensionsManager.instance()
				.getSVNConnectorFactory()
				.getSVNAPIVersion() < ISVNConnectorFactory.APICompatibility.SVNAPI_1_5_x) {
			priorToSVN15Format = false;
			priorToSVN15FormatButton.setEnabled(priorToSVN15Format);
		}

		folderButton.setSelection(isFolder = true);
		enableIsFolder();

		enableFormatUrl();
	}

	protected void enableIsFolder() {
		if (isLessSVN16() || isPriorToSVN15Format()) {
			folderButton.setSelection(isFolder = true);
			folderButton.setEnabled(false);
		} else {
			folderButton.setEnabled(true);
		}
	}

	protected void enableFormatUrl() {
		boolean isEnable = false;
		if (!isPriorToSVN15Format() && CoreExtensionsManager.instance()
				.getSVNConnectorFactory()
				.getSVNAPIVersion() > ISVNConnectorFactory.APICompatibility.SVNAPI_1_4_x) {
			//url is full and from the same repository
			isEnable = url != null && repositoryResourceForUrl != null && SVNUtility.isValidSVNURL(url)
					&& repositoryResourceForUrl.getRepositoryLocation()
							.getRepositoryRoot()
							.equals(repositoryResource.getRepositoryLocation().getRepositoryRoot());
		}
		relativeText.setEnabled(isEnable);
		formatButton.setEnabled(isEnable);
	}

	protected void createLocalPathSelectionControls(Composite parent) {
		Label localPathLabel = new Label(parent, SWT.NONE);
		GridData data = new GridData();
		localPathLabel.setLayoutData(data);
		localPathLabel.setText(SVNUIMessages.ExternalsEditPanel_LocalPathLabel);

		localPathText = new Text(parent, SWT.BORDER);
		data = new GridData(GridData.FILL_HORIZONTAL);
		localPathText.setLayoutData(data);
		localPathText.addModifyListener(e -> localPath = localPathText.getText());

		folderButton = new Button(parent, SWT.CHECK);
		folderButton.setLayoutData(new GridData());
		folderButton.setText(SVNUIMessages.ExternalsEditPanel_IsFolder);
		folderButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				isFolder = folderButton.getSelection();
			}
		});
	}

	protected void createRepositoryResourceSelectionControls(Composite parent) {
		Label urlLabel = new Label(parent, SWT.NONE);
		urlLabel.setLayoutData(new GridData());
		urlLabel.setText(SVNUIMessages.getString(comboId));

		urlText = new Combo(parent, SWT.NULL);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		urlText.setLayoutData(data);
		urlText.setVisibleItemCount(urlHistory.getDepth());
		urlText.setItems(urlHistory.getHistory());

		Listener urlTextListener = event -> ExternalsEditPanel.this.onChangeUrlText();
		urlText.addListener(SWT.Selection, urlTextListener);
		urlText.addListener(SWT.Modify, urlTextListener);

		browse = new Button(parent, SWT.PUSH);
		browse.setText(SVNUIMessages.Button_Browse);
		data = new GridData();
		data.widthHint = DefaultDialog.computeButtonWidth(browse);
		browse.setLayoutData(data);
		browse.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ExternalsEditPanel.this.onRepositoryResourceSelection();
			}
		});

		//format url
		Label label = new Label(parent, SWT.NONE);
		label.setLayoutData(new GridData());
		label.setText(SVNUIMessages.ExternalsEditPanel_FormatUrl);

		relativeText = new Combo(parent, SWT.READ_ONLY);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		relativeText.setLayoutData(data);

		relativeText.add(SVNUIMessages.ExternalsEditPanel_RelativeToRepositoryRoot,
				ExternalsEditPanel.REPOSITORY_ROOT_INDEX);
		relativeText.add(SVNUIMessages.ExternalsEditPanel_RelativeToUrlScheme, ExternalsEditPanel.URL_SCHEME_INDEX);
		relativeText.add(SVNUIMessages.ExternalsEditPanel_RelativeToHostName, ExternalsEditPanel.HOST_NAME_INDEX);
		relativeText.add(SVNUIMessages.ExternalsEditPanel_RelativeToDirectory,
				ExternalsEditPanel.EXTERNAL_DIRECTORY_INDEX);
		relativeText.setVisibleItemCount(4);
		relativeText.select(0);

		formatButton = new Button(parent, SWT.PUSH);
		formatButton.setText(SVNUIMessages.ExternalsEditPanel_FormatButton);
		data = new GridData();
		data.widthHint = DefaultDialog.computeButtonWidth(formatButton);
		formatButton.setLayoutData(data);

		formatButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ExternalsEditPanel.this.formatUrl();
			}
		});

		CompositeVerifier urlVerifier = new CompositeVerifier() {
			@Override
			protected void fireError(String errorReason) {
				super.fireError(errorReason);
				ExternalsEditPanel.this.onUrlValidity(false);
			}

			@Override
			protected void fireOk() {
				super.fireOk();
				ExternalsEditPanel.this.onUrlValidity(true);
			}
		};
		urlVerifier.add(new NonEmptyFieldVerifier(SVNUIMessages.getString(comboId + "_Verifier"))); //$NON-NLS-1$
		urlVerifier.add(new URLVerifier(SVNUIMessages.getString(comboId + "_Verifier")) { //$NON-NLS-1$
			@Override
			protected String getText(Control input) {
				return processedUrl != null ? processedUrl : url;
			}
		});
		attachTo(urlText, urlVerifier);

		CompositeVerifier localPathVerifier = new CompositeVerifier();
		localPathVerifier.add(new NonEmptyFieldVerifier(SVNUIMessages.ExternalsEditPanel_LocalPathLabel_Verifier));
		//don't allow spaces  if SVN < 1.6
		localPathVerifier.add(new AbstractFormattedVerifier(SVNUIMessages.ExternalsEditPanel_LocalPathLabel_Verifier) {
			@Override
			protected String getErrorMessageImpl(Control input) {
				String text = getText(input);
				text = text.trim();
				if (text.contains(" ") && ExternalsEditPanel.this.isLessSVN16()) { //$NON-NLS-1$
					return BaseMessages.format(SVNUIMessages.Verifier_NoSpaces,
							new String[] { AbstractFormattedVerifier.FIELD_NAME });
				}
				return null;
			}

			@Override
			protected String getWarningMessageImpl(Control input) {
				return null;
			}
		});
		//TODO check that resource doesn't exist on file system ?
		attachTo(localPathText, localPathVerifier);
	}

	protected void onUrlValidity(boolean isValidUrl) {
		revisionComposite.setEnabled(isValidUrl);
		enableFormatUrl();
	}

	protected void formatUrl() {
		String fullUrl = url.trim();
		IPath fullUrlPath = SVNUtility.createPathForSVNUrl(fullUrl);
		int relativeIndex = relativeText.getSelectionIndex();
		if (relativeIndex == ExternalsEditPanel.REPOSITORY_ROOT_INDEX) {
			IPath repositoryRoot = SVNUtility
					.createPathForSVNUrl(repositoryResource.getRepositoryLocation().getRepositoryRootUrl());
			if (repositoryRoot.isPrefixOf(fullUrlPath)) {
				String relative = fullUrlPath.makeRelativeTo(repositoryRoot).toString();
				urlText.setText("^/" + relative); //$NON-NLS-1$
			}
		} else if (relativeIndex == ExternalsEditPanel.URL_SCHEME_INDEX) {
			int index = 0;
			if (fullUrl.startsWith("file:///")) { //$NON-NLS-1$
				index = "file:///".length(); //$NON-NLS-1$
			} else if (fullUrl.startsWith("file://")) { //$NON-NLS-1$
				index = "file://".length(); //$NON-NLS-1$
			} else if (fullUrl.startsWith("http://")) { //$NON-NLS-1$
				index = "http://".length(); //$NON-NLS-1$
			} else if (fullUrl.startsWith("https://")) { //$NON-NLS-1$
				index = "https://".length(); //$NON-NLS-1$
			} else if (fullUrl.startsWith("svn://")) { //$NON-NLS-1$
				index = "svn://".length(); //$NON-NLS-1$
			} else if (fullUrl.startsWith("svn+ssh://")) { //$NON-NLS-1$
				index = "svn+ssh://".length(); //$NON-NLS-1$
			}
			if (index != 0) {
				String relativaPath = "//" + fullUrl.substring(index); //$NON-NLS-1$
				urlText.setText(relativaPath);
			}
		} else if (relativeIndex == ExternalsEditPanel.HOST_NAME_INDEX) {
			try {
				URL url = SVNUtility.getSVNUrl(fullUrl);
				String relativePath = url.getFile();
				urlText.setText(relativePath);
			} catch (MalformedURLException me) {
				//ignore
			}
		} else if (relativeIndex == ExternalsEditPanel.EXTERNAL_DIRECTORY_INDEX) {
			//find common path
			IPath resourcePath = SVNUtility.createPathForSVNUrl(repositoryResource.getUrl());
			IPath commonPath = resourcePath;
			int relativeSegmentsCount = 0;
			do {
				commonPath = commonPath.removeLastSegments(1);
				relativeSegmentsCount++;
			} while (!commonPath.isPrefixOf(fullUrlPath) && !commonPath.isEmpty());
			if (!commonPath.isEmpty()) {
				StringBuilder relativePath = new StringBuilder();
				for (int i = 0; i < relativeSegmentsCount; i++) {
					relativePath.append("../"); //$NON-NLS-1$
				}
				relativePath.append(fullUrlPath.makeRelativeTo(commonPath).toString());
				urlText.setText(relativePath.toString());
			}
		}
	}

	protected void onChangeUrlText() {
		url = urlText.getText();
		if (!isPriorToSVN15Format() && CoreExtensionsManager.instance()
				.getSVNConnectorFactory()
				.getSVNAPIVersion() > ISVNConnectorFactory.APICompatibility.SVNAPI_1_4_x) {
			try {
				processedUrl = SVNUtility.replaceRelativeExternalParts(url, repositoryResource);
			} catch (Exception e) {
				processedUrl = null;
			}
		} else {
			processedUrl = url;
		}
		repositoryResourceForUrl = processedUrl != null
				? SVNUtility.asRepositoryResource(processedUrl, isFolder)
				: null;
		revisionComposite.setSelectedResource(repositoryResourceForUrl);
	}

	protected void onRepositoryResourceSelection() {
		SelectRepositoryResourceWizard wizard;
		if (isFolder) {
			wizard = new SelectRepositoryResourceWizard(isFolder);
		} else {
			wizard = new SelectRepositoryResourceWizard(isFolder, repositoryResource.getRepositoryLocation());
		}
		WizardDialog dialog = new WizardDialog(manager.getShell(), wizard);
		if (dialog.open() == 0) {
			IRepositoryResource resource = wizard.getSelectedResource();
			folderButton.setSelection(resource instanceof IRepositoryFolder);
			urlText.setText(resource.getUrl());
		}
	}

	@Override
	public String getHelpId() {
		return "org.eclipse.team.svn.help.setExternalsDialogContext"; //$NON-NLS-1$
	}

	@Override
	protected void saveChangesImpl() {
		revision = revisionComposite.getSelectedRevision();

		urlHistory.addLine(urlText.getText());
	}

	@Override
	protected void cancelChangesImpl() {
	}

	protected boolean isLessSVN16() {
		return CoreExtensionsManager.instance()
				.getSVNConnectorFactory()
				.getSVNAPIVersion() < ISVNConnectorFactory.APICompatibility.SVNAPI_1_6_x;
	}

	public String getLocalPath() {
		return localPath;
	}

	public boolean isPriorToSVN15Format() {
		return priorToSVN15Format;
	}

	public String getUrl() {
		return url;
	}

	public SVNRevision getRevision() {
		return revision;
	}

	public boolean isFolder() {
		return isFolder;
	}

	@Override
	protected Point getPrefferedSizeImpl() {
		return new Point(520, SWT.DEFAULT);
	}
}
