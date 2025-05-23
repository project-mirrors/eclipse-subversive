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
 *    Michael (msa) - Eclipse-SourceReferences support
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui;

import java.net.URI;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.team.core.ScmUrlImportDescription;
import org.eclipse.team.internal.ui.SWTUtils;
import org.eclipse.team.svn.core.SVNTeamProjectSetCapability;
import org.eclipse.team.ui.IScmUrlImportWizardPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

/**
 * Wizard page that allows the user to import repositories with SCM URLs.
 */
public class SVNScmUrlImportWizardPage extends WizardPage implements IScmUrlImportWizardPage {

	private ScmUrlImportDescription[] descriptions;

	private Label counterLabel;

	private TableViewer bundlesViewer;

	private Button useHead;

	private static final String SVN_PAGE_USE_HEAD = "org.eclipse.team.svn.ui.import.page.head"; //$NON-NLS-1$

	private class SvnLabelProvider extends StyledCellLabelProvider implements ILabelProvider {

		@Override
		public Image getImage(Object element) {
			return PlatformUI.getWorkbench().getSharedImages().getImage(IDE.SharedImages.IMG_OBJ_PROJECT);
		}

		@Override
		public String getText(Object element) {
			return getStyledText(element).getString();
		}

		@Override
		public void update(ViewerCell cell) {
			StyledString string = getStyledText(cell.getElement());
			cell.setText(string.getString());
			cell.setStyleRanges(string.getStyleRanges());
			cell.setImage(getImage(cell.getElement()));
			super.update(cell);
		}

		private StyledString getStyledText(Object element) {
			StyledString styledString = new StyledString();
			if (element instanceof ScmUrlImportDescription) {
				ScmUrlImportDescription description = (ScmUrlImportDescription) element;
				String project = description.getProject();
				URI scmUrl = description.getUri();
				String version = getTag(scmUrl);
				String host = getServer(scmUrl);
				styledString.append(project);
				if (version != null) {
					styledString.append(' ');
					styledString.append(version, StyledString.DECORATIONS_STYLER);
				}
				styledString.append(' ');
				styledString.append('[', StyledString.DECORATIONS_STYLER);
				styledString.append(host, StyledString.DECORATIONS_STYLER);
				styledString.append(']', StyledString.DECORATIONS_STYLER);
				return styledString;
			}
			styledString.append(element.toString());
			return styledString;
		}
	}

	public SVNScmUrlImportWizardPage() {
		super("svn", SVNUIMessages.SVNScmUrlImportWizardPage_Title, null); //$NON-NLS-1$
		setDescription(SVNUIMessages.SVNScmUrlImportWizardPage_Description);
	}

	@Override
	public void createControl(Composite parent) {
		Composite comp = SWTUtils.createHVFillComposite(parent, SWTUtils.MARGINS_NONE, 1);
		Composite group = SWTUtils.createHFillComposite(comp, SWTUtils.MARGINS_NONE, 1);

		Button versions = SWTUtils.createRadioButton(group, SVNUIMessages.SVNScmUrlImportWizardPage_ImportVersion);
		useHead = SWTUtils.createRadioButton(group, SVNUIMessages.SVNScmUrlImportWizardPage_ImportHEAD);
		SelectionListener listener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				bundlesViewer.refresh(true);
			}
		};
		versions.addSelectionListener(listener);
		useHead.addSelectionListener(listener);

		Table table = new Table(comp, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 200;
		gd.widthHint = 225;
		table.setLayoutData(gd);

		bundlesViewer = new TableViewer(table);
		bundlesViewer.setLabelProvider(new SvnLabelProvider());
		bundlesViewer.setContentProvider(new ArrayContentProvider());
		bundlesViewer.setComparator(new ViewerComparator());
		counterLabel = new Label(comp, SWT.NONE);
		counterLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		setControl(comp);
		setPageComplete(true);

		// Initialize versions versus HEAD
		// TODO: disabled because importing URLs with @<revision_nr> does not
		// seem to work
		// IDialogSettings settings = getWizard().getDialogSettings();
		// boolean useHEAD = settings != null &&
		// settings.getBoolean(SVN_PAGE_USE_HEAD);
		// useHead.setSelection(useHEAD);
		// versions.setSelection(!useHEAD);

		// force using HEAD => see disabled code above
		useHead.setSelection(true);
		versions.setEnabled(false);

		if (descriptions != null) {
			bundlesViewer.setInput(descriptions);
			updateCount();
		}

	}

	@Override
	public boolean finish() {

		boolean head = false;
		if (getControl() != null) {
			head = useHead.getSelection();
			// store settings
			IDialogSettings settings = getWizard().getDialogSettings();
			if (settings != null) {
				settings.put(SVNScmUrlImportWizardPage.SVN_PAGE_USE_HEAD, head);
			}
		} else {
			// use whatever was used last time
			IDialogSettings settings = getWizard().getDialogSettings();
			if (settings != null) {
				head = settings.getBoolean(SVNScmUrlImportWizardPage.SVN_PAGE_USE_HEAD);
			}
		}

		if (head && descriptions != null) {
			// modify tags on bundle import descriptions
			for (ScmUrlImportDescription description : descriptions) {
				URI scmUri = description.getUri();
				description.setUrl(removeTag(scmUri));
			}
		}

		return true;
	}

	@Override
	public ScmUrlImportDescription[] getSelection() {
		return descriptions;
	}

	@Override
	public void setSelection(ScmUrlImportDescription[] descriptions) {
		this.descriptions = descriptions;
		// fill viewer
		if (bundlesViewer != null) {
			bundlesViewer.setInput(descriptions);
			updateCount();
		}
	}

	/**
	 * Updates the count of bundles that will be imported
	 */
	private void updateCount() {
		counterLabel.setText(
				NLS.bind(SVNUIMessages.SVNScmUrlImportWizardPage_Counter, Integer.valueOf(descriptions.length)));
		counterLabel.getParent().layout();
	}

	private static String getTag(URI scmUri) {
		return SVNScmUrlImportWizardPage.splitTagPart(scmUri.toString())[1];
	}

	/**
	 * Remove tag attributes from the given URI reference. Results in the URI pointing to HEAD.
	 * 
	 * @param scmUri
	 *            a SCM URI reference to modify
	 * @return Returns the content of the stripped URI as a string.
	 */
	private static String removeTag(URI scmUri) {
		return SVNScmUrlImportWizardPage.splitTagPart(scmUri.toString())[0];
	}

	private static String getServer(URI scmUri) {
		// show the whole URL (without tag)
		return SVNScmUrlImportWizardPage.splitTagPart(SVNTeamProjectSetCapability.getSingleSchemeUrl(scmUri))[0];
	}

	private static String[] splitTagPart(String url) {
		int i = url.lastIndexOf('/');
		if (i < 0) {
			return new String[] { url, "" }; //$NON-NLS-1$
		}
		String lastUrlPart = url.substring(i);
		int j = lastUrlPart.lastIndexOf('@');
		return j < 0
				? new String[] { url, "" }
				: new String[] { url.substring(0, i) + lastUrlPart.substring(0, j), lastUrlPart.substring(j) };
	}

}
