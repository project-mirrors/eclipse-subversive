/*******************************************************************************
 * Copyright (c) 2005, 2024 Polarion Software and others.
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
 *    Nikifor Fedorov (ArSysOp) - issue subversive/#245
 *******************************************************************************/

package org.eclipse.team.svn.ui.panel.local;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.team.svn.core.connector.SVNDepth;
import org.eclipse.team.svn.core.connector.SVNNotification;
import org.eclipse.team.svn.core.connector.SVNNotification.PerformedAction;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.connector.SVNRevisionRange;
import org.eclipse.team.svn.core.extension.CoreExtensionsManager;
import org.eclipse.team.svn.core.extension.factory.ISVNConnectorFactory;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.SVNNullProgressMonitor;
import org.eclipse.team.svn.core.operation.local.JavaHLMergeOperation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IRepositoryResourceProvider;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.SVNUtility;
import org.eclipse.team.svn.internal.ui.TabFolderLayout;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.composite.DepthSelectionComposite;
import org.eclipse.team.svn.ui.composite.RepositoryResourceSelectionComposite;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.panel.AbstractAdvancedDialogPanel;
import org.eclipse.team.svn.ui.panel.IDialogManagerEx;
import org.eclipse.team.svn.ui.panel.reporting.PreviewPanel;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;
import org.eclipse.team.svn.ui.verifier.AbstractVerifier;
import org.eclipse.team.svn.ui.verifier.AbstractVerifierProxy;
import org.eclipse.team.svn.ui.verifier.IValidationManager;

/**
 * Merge panel implementation
 * 
 * @author Alexander Gurov
 */
public class MergePanel extends AbstractAdvancedDialogPanel {
	public static final int MODE_1URL = 0;

	public static final int MODE_2URL = 1;

	public static final int MODE_REINTEGRATE = 2;

	protected static final String FIRST_URL_HISTORY = "Merge_FirstUrl"; //$NON-NLS-1$

	protected static final String SECOND_URL_HISTORY = "Merge_SecondUrl"; //$NON-NLS-1$

	protected IResource[] to;

	protected IRepositoryResource baseResource;

	protected long currentRevision;

	protected DepthSelectionComposite depthSelector;

	protected DepthSelectionComposite depthSelectorSimple;

	protected IRepositoryResource firstSelectedResource;

	protected IRepositoryResource secondSelectedResource;

	protected SVNRevisionRange[] selectedRevisions;

	protected boolean isReverseRevisions;

	protected int mode;

	protected boolean ignoreAncestry;

	protected boolean recordOnly;

	protected RepositoryResourceSelectionComposite simpleSelectionComposite;

	protected RepositoryResourceSelectionComposite firstSelectionComposite;

	protected RepositoryResourceSelectionComposite secondSelectionComposite;

	protected RepositoryResourceSelectionComposite reintegrateSelectionComposite;

	protected Button ignoreAncestryButton;

	protected Button ignoreAncestrySimpleButton;

	protected Button recordOnlyButton;

	protected Button recordOnlySimpleButton;

	public MergePanel(IResource[] to, IRepositoryResource baseResource, long currentRevision) {
		super(new String[] { IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL },
				new String[] { SVNUIMessages.MergePanel_Preview });

		dialogTitle = SVNUIMessages.MergePanel_Title;
		dialogDescription = SVNUIMessages.MergePanel_Description;
		defaultMessage = SVNUIMessages.MergePanel_Message;

		this.to = to;
		this.baseResource = firstSelectedResource = secondSelectedResource = baseResource;
		this.currentRevision = currentRevision;
	}

	public int getMode() {
		return mode;
	}

	@Override
	public Point getPrefferedSizeImpl() {
		return new Point(727, 245);
	}

	public SVNRevision getStartRevision() {
		return simpleSelectionComposite.getStartRevision();
	}

	public IRepositoryResource getSelectedResource() {
		IRepositoryResource retVal = SVNUtility.copyOf(simpleSelectionComposite.getSelectedResource());
		retVal.setSelectedRevision(simpleSelectionComposite.getSecondSelectedRevision());
		return retVal;
	}

	public IRepositoryResource[] getSelection() {
		return this.getSelection(getSelectedResource());
	}

	public IRepositoryResource[] getFirstSelection() {
		return this.getSelection(firstSelectedResource);
	}

	public IRepositoryResource[] getSecondSelection() {
		return this.getSelection(secondSelectedResource);
	}

	public boolean getIgnoreAncestry() {
		return ignoreAncestry;
	}

	public boolean getRecordOnly() {
		return recordOnly;
	}

	public SVNRevisionRange[] getSelectedRevisions() {
		SVNRevisionRange[] revisions = null;
		if (selectedRevisions != null) {
			revisions = new SVNRevisionRange[selectedRevisions.length];
			for (int i = 0; i < selectedRevisions.length; i++) {
				SVNRevisionRange range = selectedRevisions[i];
				/*
				 * If 'from' revision equals to 'to' revision
				 * (it can be in case if only one revision is selected in revision selection),
				 * we process it as --change option (see svn merge command) and we handle it as
				 * from = Rev -1, to = Rev, taking into account 'Reversed merge' option.
				 */
				if (range.from.equals(range.to) && range.from.getKind() == SVNRevision.Kind.NUMBER) {
					SVNRevision from = SVNRevision.fromNumber(((SVNRevision.Number) range.from).getNumber() - 1);
					SVNRevision to = range.from;
					if (isReverseRevisions) {
						SVNRevision tmp = from;
						from = to;
						to = tmp;
					}
					range = new SVNRevisionRange(from, to);
				}
				revisions[i] = range;
			}
		}
		return revisions;
	}

	@Override
	public void createControlsImpl(Composite parent) {
		((GridLayout) parent.getLayout()).verticalSpacing = 2;

		final TabFolder tabFolder = new TabFolder(parent, SWT.NONE);
		tabFolder.setLayout(new TabFolderLayout());
		tabFolder.setLayoutData(new GridData(GridData.FILL_BOTH));

		TabItem tabItem = new TabItem(tabFolder, SWT.NONE);
		tabItem.setText(SVNUIMessages.MergePanel_1URL);
		tabItem.setControl(create1URLModeView(tabFolder));

		tabItem = new TabItem(tabFolder, SWT.NONE);
		tabItem.setText(SVNUIMessages.MergePanel_2URL);
		tabItem.setControl(create2URLModeView(tabFolder));

		if (CoreExtensionsManager.instance()
				.getSVNConnectorFactory()
				.getSVNAPIVersion() >= ISVNConnectorFactory.APICompatibility.SVNAPI_1_5_x) {
			tabItem = new TabItem(tabFolder, SWT.NONE);
			tabItem.setText(SVNUIMessages.MergePanel_Reintegrate);
			tabItem.setControl(createReintegrateModeView(tabFolder));
		}

		tabFolder.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				mode = tabFolder.getSelectionIndex();
				MergePanel.this.validateContent();
			}
		});

		mode = MergePanel.MODE_1URL;
	}

	protected Composite create1URLModeView(Composite parent) {
		GridData data = null;
		GridLayout layout = null;

		parent = new Composite(parent, SWT.NONE);
		layout = new GridLayout();
		parent.setLayout(layout);
		data = new GridData(GridData.FILL_HORIZONTAL);
		parent.setLayoutData(data);

		int mode = CoreExtensionsManager.instance()
				.getSVNConnectorFactory()
				.getSVNAPIVersion() >= ISVNConnectorFactory.APICompatibility.SVNAPI_1_5_x
						? RepositoryResourceSelectionComposite.MODE_CHECK
						: RepositoryResourceSelectionComposite.MODE_TWO;
		simpleSelectionComposite = new RepositoryResourceSelectionComposite(
				parent, SWT.NONE, new ValidationManagerProxy() {
					@Override
					protected AbstractVerifier wrapVerifier(AbstractVerifier verifier) {
						return new AbstractVerifierProxy(verifier) {
							@Override
							protected boolean isVerificationEnabled(Control input) {
								return MergePanel.this.mode == MergePanel.MODE_1URL;
							}
						};
					}
				}, MergePanel.FIRST_URL_HISTORY, firstSelectedResource, true, SVNUIMessages.MergePanel_Selection_Title,
				SVNUIMessages.MergePanel_Selection_Description, mode, RepositoryResourceSelectionComposite.TEXT_NONE);
		data = new GridData(GridData.FILL_HORIZONTAL);
		simpleSelectionComposite.setLayoutData(data);
		simpleSelectionComposite.setCurrentRevision(currentRevision);

		Label separator = new Label(parent, SWT.HORIZONTAL | SWT.SEPARATOR);
		separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		separator.setVisible(false);

		data = new GridData();
		ignoreAncestrySimpleButton = new Button(parent, SWT.CHECK);
		ignoreAncestrySimpleButton.setLayoutData(data);
		ignoreAncestrySimpleButton.setText(SVNUIMessages.MergePanel_Button_IgnoreAncestry);
		ignoreAncestrySimpleButton.setSelection(ignoreAncestry);

		data = new GridData();
		recordOnlySimpleButton = new Button(parent, SWT.CHECK);
		recordOnlySimpleButton.setLayoutData(data);
		recordOnlySimpleButton.setText(SVNUIMessages.MergePanel_Button_RecordOnly);
		recordOnlySimpleButton.setSelection(recordOnly);

		depthSelectorSimple = new DepthSelectionComposite(parent, SWT.NONE, true);
		depthSelectorSimple.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		return parent;
	}

	protected Composite create2URLModeView(Composite parent) {
		GridData data = null;
		GridLayout layout = null;

		parent = new Composite(parent, SWT.NONE);
		layout = new GridLayout();
		parent.setLayout(layout);
		data = new GridData(GridData.FILL_HORIZONTAL);
		parent.setLayoutData(data);

		final ValidationManagerProxy proxy2 = new ValidationManagerProxy() {
			@Override
			protected AbstractVerifier wrapVerifier(AbstractVerifier verifier) {
				return new AbstractVerifierProxy(verifier) {
					@Override
					protected boolean isVerificationEnabled(Control input) {
						return mode == MergePanel.MODE_2URL;
					}
				};
			}
		};

		ValidationManagerProxy proxy = new ValidationManagerProxy() {
			@Override
			protected AbstractVerifier wrapVerifier(AbstractVerifier verifier) {
				return new AbstractVerifierProxy(verifier) {
					@Override
					protected boolean isVerificationEnabled(Control input) {
						return mode == MergePanel.MODE_2URL;
					}

					@Override
					public boolean verify(Control input) {
						for (Control cmp : proxy2.getControls()) {
							proxy2.validateControl(cmp);
						}
						return super.verify(input);
					}
				};
			}
		};

		firstSelectionComposite = new RepositoryResourceSelectionComposite(
				parent, SWT.NONE, proxy, MergePanel.FIRST_URL_HISTORY, "MergePanel_SourceURL1", //$NON-NLS-1$
				firstSelectedResource, true, SVNUIMessages.MergePanel_Selection_Title,
				SVNUIMessages.MergePanel_Selection_Description, RepositoryResourceSelectionComposite.MODE_DEFAULT,
				RepositoryResourceSelectionComposite.TEXT_NONE);
		data = new GridData(GridData.FILL_HORIZONTAL);
		firstSelectionComposite.setLayoutData(data);
		firstSelectionComposite.setCurrentRevision(currentRevision);

		Label separator = new Label(parent, SWT.HORIZONTAL | SWT.SEPARATOR);
		separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		separator.setVisible(false);

		secondSelectionComposite = new RepositoryResourceSelectionComposite(
				parent, SWT.NONE, proxy2, MergePanel.SECOND_URL_HISTORY, "MergePanel_SourceURL2", //$NON-NLS-1$
				secondSelectedResource, true, SVNUIMessages.MergePanel_Selection_Title,
				SVNUIMessages.MergePanel_Selection_Description, RepositoryResourceSelectionComposite.MODE_DEFAULT,
				RepositoryResourceSelectionComposite.TEXT_NONE);
		data = new GridData(GridData.FILL_HORIZONTAL);
		secondSelectionComposite.setLayoutData(data);
		secondSelectionComposite.setCurrentRevision(currentRevision);

		separator = new Label(parent, SWT.HORIZONTAL | SWT.SEPARATOR);
		separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		separator.setVisible(false);

		data = new GridData();
		ignoreAncestryButton = new Button(parent, SWT.CHECK);
		ignoreAncestryButton.setLayoutData(data);
		ignoreAncestryButton.setText(SVNUIMessages.MergePanel_Button_IgnoreAncestry);
		ignoreAncestryButton.setSelection(ignoreAncestry);

		data = new GridData();
		recordOnlyButton = new Button(parent, SWT.CHECK);
		recordOnlyButton.setLayoutData(data);
		recordOnlyButton.setText(SVNUIMessages.MergePanel_Button_RecordOnly);
		recordOnlyButton.setSelection(recordOnly);

		depthSelector = new DepthSelectionComposite(parent, SWT.NONE, true);
		depthSelector.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		return parent;
	}

	protected Composite createReintegrateModeView(Composite parent) {
		GridData data = null;
		GridLayout layout = null;

		parent = new Composite(parent, SWT.NONE);
		layout = new GridLayout();
		parent.setLayout(layout);
		data = new GridData(GridData.FILL_HORIZONTAL);
		parent.setLayoutData(data);

		reintegrateSelectionComposite = new RepositoryResourceSelectionComposite(
				parent, SWT.NONE, new ValidationManagerProxy() {
					@Override
					protected AbstractVerifier wrapVerifier(AbstractVerifier verifier) {
						return new AbstractVerifierProxy(verifier) {
							@Override
							protected boolean isVerificationEnabled(Control input) {
								return mode == MergePanel.MODE_REINTEGRATE;
							}
						};
					}
				}, MergePanel.FIRST_URL_HISTORY, firstSelectedResource, true, SVNUIMessages.MergePanel_Selection_Title,
				SVNUIMessages.MergePanel_Selection_Description, RepositoryResourceSelectionComposite.MODE_DEFAULT,
				RepositoryResourceSelectionComposite.TEXT_NONE);
		data = new GridData(GridData.FILL_HORIZONTAL);
		reintegrateSelectionComposite.setLayoutData(data);
		reintegrateSelectionComposite.setCurrentRevision(currentRevision);

		return parent;
	}

	public SVNDepth getDepth() {
		return mode == MergePanel.MODE_1URL
				? depthSelectorSimple.getDepth()
				: mode == MergePanel.MODE_2URL ? depthSelector.getDepth() : SVNDepth.UNKNOWN;
	}

	@Override
	protected void showDetails() {
		saveChangesImpl();

		IRepositoryResourceProvider firstSet = new IRepositoryResourceProvider.DefaultRepositoryResourceProvider(
				getFirstSelection());
		IRepositoryResourceProvider secondSet = null;
		if (mode == MergePanel.MODE_2URL) {
			secondSet = new IRepositoryResourceProvider.DefaultRepositoryResourceProvider(getSecondSelection());
		}
		JavaHLMergeOperation mergeOp = null;
		if (mode == MergePanel.MODE_2URL) {
			mergeOp = new JavaHLMergeOperation(to, firstSet, secondSet, true, getIgnoreAncestry(), getDepth());
			mergeOp.setRecordOnly(getRecordOnly());
		} else if (mode == MergePanel.MODE_1URL) {
			mergeOp = new JavaHLMergeOperation(to, firstSet, getSelectedRevisions(), true, getIgnoreAncestry(),
					getDepth());
			mergeOp.setRecordOnly(getRecordOnly());
		} else {
			mergeOp = new JavaHLMergeOperation(to, firstSet, true);
		}
		final StringBuilder buf = new StringBuilder();
		buf.append(SVNUIMessages.MergePanel_Preview_Header_Text);
		buf.append(SVNUIMessages.MergePanel_Preview_Header_Line);
		mergeOp.setExternalMonitor(new SVNNullProgressMonitor() {
			@Override
			public void progress(int current, int total, ItemState state) {
				buf.append("<b>"); //$NON-NLS-1$
				switch (PerformedAction.fromId(state.action)) {
					case UPDATE_ADD: {
						buf.append(SVNUIMessages.MergePanel_Preview_Added);
						break;
					}
					case UPDATE_DELETE: {
						buf.append(SVNUIMessages.MergePanel_Preview_Deleted);
						break;
					}
					case UPDATE_UPDATE: {
						buf.append(SVNUIMessages.MergePanel_Preview_Modified);
						break;
					}
					default: {
						if (SVNNotification.PerformedAction.isActionKnown(state.action)) {
							buf.append(PerformedAction.actionNames[state.action]);
						} else {
							buf.append("\t"); //$NON-NLS-1$
						}
						buf.append(SVNUIMessages.MergePanel_Preview_Default);
					}
				}
				buf.append(state.path);
				buf.append("\n"); //$NON-NLS-1$
			}
		});

		UIMonitorUtility.doTaskNowDefault(mergeOp, true);

		if (mergeOp.getExecutionState() == IActionOperation.OK) {
			Font font = new Font(UIMonitorUtility.getDisplay(), "Courier New", 8, SWT.NORMAL); //$NON-NLS-1$
			new DefaultDialog(manager.getShell(),
					new PreviewPanel(SVNUIMessages.MergePanel_Preview_Title,
							SVNUIMessages.MergePanel_Preview_Description, SVNUIMessages.MergePanel_Preview_Message,
							buf.toString(), font)).open();
		}
	}

	@Override
	protected void saveChangesImpl() {
		if (mode == MergePanel.MODE_1URL) {
			firstSelectedResource = simpleSelectionComposite.getSelectedResource();
			secondSelectedResource = simpleSelectionComposite.getSecondSelectedResource();
			selectedRevisions = simpleSelectionComposite.getSelectedRevisions();
			isReverseRevisions = simpleSelectionComposite.isReverseRevisions();
			simpleSelectionComposite.saveHistory();

			ignoreAncestry = ignoreAncestrySimpleButton.getSelection();
			recordOnly = recordOnlySimpleButton.getSelection();
		} else if (mode == MergePanel.MODE_2URL) {
			firstSelectedResource = firstSelectionComposite.getSelectedResource();
			firstSelectionComposite.saveHistory();

			secondSelectedResource = secondSelectionComposite.getSelectedResource();
			secondSelectionComposite.saveHistory();

			ignoreAncestry = ignoreAncestryButton.getSelection();
			recordOnly = recordOnlyButton.getSelection();
		} else {
			firstSelectedResource = secondSelectedResource = reintegrateSelectionComposite.getSelectedResource();
			reintegrateSelectionComposite.saveHistory();
		}
	}

	@Override
	protected void cancelChangesImpl() {
	}

	@Override
	protected void setButtonsEnabled(boolean enabled) {
		((IDialogManagerEx) manager).setExtendedButtonEnabled(0, enabled);
	}

	protected IRepositoryResource[] getSelection(IRepositoryResource base) {
		if (to.length == 1) {
			return new IRepositoryResource[] { base };
		}
		IRepositoryResource[] retVal = new IRepositoryResource[to.length];
		String baseUrl = base.getUrl();
		for (int i = 0; i < retVal.length; i++) {
			String url = baseUrl + "/" + SVNRemoteStorage.instance().asRepositoryResource(to[i]).getName(); //$NON-NLS-1$
			retVal[i] = to[i].getType() == IResource.FILE
					? (IRepositoryResource) base.asRepositoryFile(url, false)
					: base.asRepositoryContainer(url, false);
		}
		return retVal;
	}

	protected abstract class ValidationManagerProxy implements IValidationManager {
		protected Set<Control> controls = new HashSet<>();

		@Override
		public void attachTo(Control cmp, AbstractVerifier verifier) {
			controls.add(cmp);
			MergePanel.this.attachTo(cmp, wrapVerifier(verifier));
		}

		@Override
		public void detachFrom(Control cmp) {
			controls.remove(cmp);
			MergePanel.this.detachFrom(cmp);
		}

		@Override
		public void detachAll() {
			MergePanel.this.detachAll();
		}

		@Override
		public boolean isFilledRight() {
			return MergePanel.this.isFilledRight();
		}

		@Override
		public void validateContent() {
			MergePanel.this.validateContent();
		}

		@Override
		public boolean validateControl(Control cmp) {
			return MergePanel.this.validateControl(cmp);
		}

		public Set<Control> getControls() {
			return controls;
		}

		protected abstract AbstractVerifier wrapVerifier(AbstractVerifier verifier);

	}

	@Override
	public String getHelpId() {
		return "org.eclipse.team.svn.help.mergeDialogContext"; //$NON-NLS-1$
	}

}
