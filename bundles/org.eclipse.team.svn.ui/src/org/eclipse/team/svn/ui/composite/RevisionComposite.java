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

package org.eclipse.team.svn.ui.composite;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.ISVNConnector.Options;
import org.eclipse.team.svn.core.connector.SVNConnectorException;
import org.eclipse.team.svn.core.connector.SVNLogEntry;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.connector.SVNRevision.Kind;
import org.eclipse.team.svn.core.connector.SVNRevisionRange;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.operation.remote.GetLogMessagesOperation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.utility.SVNUtility;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.history.filter.RevisionLogEntryFilter;
import org.eclipse.team.svn.ui.panel.common.SVNHistoryPanel;
import org.eclipse.team.svn.ui.panel.common.SelectRevisionPanel;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;
import org.eclipse.team.svn.ui.verifier.AbstractVerifierProxy;
import org.eclipse.team.svn.ui.verifier.CompositeVerifier;
import org.eclipse.team.svn.ui.verifier.IValidationManager;
import org.eclipse.team.svn.ui.verifier.IntegerFieldVerifier;
import org.eclipse.team.svn.ui.verifier.NonEmptyFieldVerifier;
import org.eclipse.team.svn.ui.verifier.RevisionRangesVerifier;

/**
 * Allows to select a resource revision
 * 
 * @author Sergiy Logvin
 */
public class RevisionComposite extends Composite {
	protected IRepositoryResource selectedResource;

	protected IRepositoryResource baseResource;

	protected SVNRevision defaultRevision;

	protected long currentRevision;

	protected long lastSelectedRevision;

	protected boolean stopOnCopy;

	protected boolean toFilterCurrent;

	protected boolean startFromCopy;

	protected String[] captions;

	protected SVNRevision selectedRevision;

	protected Text revisionField;

	protected Button headRevisionRadioButton;

	protected Button startFromCopyRadioButton;

	protected Button changeRevisionRadioButton;

	protected Button changeRevisionButton;

	protected Button reverseRevisionsButton;

	protected Button dateTimeRadioButton;

	protected DateTime dateField;

	protected DateTime timeField;

	protected boolean reverseRevisions;

	protected IValidationManager validationManager;

	protected boolean checkStyled;

	//if checkStyled = false && hasDateTime = false then don't show 'Date' radio button
	//it's used to hide 'Date' radio button
	protected boolean hasDateTime;

	protected SVNRevisionRange[] revisions;

	public RevisionComposite(Composite parent, IValidationManager validationManager, boolean stopOnCopy,
			String[] captions, SVNRevision defaultRevision, boolean checkStyled) {
		this(parent, validationManager, stopOnCopy, captions, defaultRevision, checkStyled, true);
	}

	public RevisionComposite(Composite parent, IValidationManager validationManager, boolean stopOnCopy,
			String[] captions, SVNRevision defaultRevision, boolean checkStyled, boolean hasDateTime) {
		super(parent, SWT.NONE);
		this.stopOnCopy = stopOnCopy;
		toFilterCurrent = false;
		this.validationManager = validationManager;
		lastSelectedRevision = SVNRevision.INVALID_REVISION_NUMBER;
		this.captions = captions;
		this.defaultRevision = defaultRevision;
		this.checkStyled = checkStyled;
		this.hasDateTime = hasDateTime;
		createControls();
	}

	public void setBaseResource(IRepositoryResource baseResource) {
		this.baseResource = baseResource;
	}

	public void setFilterCurrent(boolean toFilter) {
		toFilterCurrent = toFilter;
	}

	public boolean isReverseRevisions() {
		return reverseRevisions;
	}

	public SVNRevision getSelectedRevision() {
		return selectedRevision;
	}

	public SVNRevisionRange[] getSelectedRevisions() {
		// check for unspecified
		if (revisions[0].from.getKind() == SVNRevision.Kind.START) {
			UIMonitorUtility.doTaskNowDefault(
					new AbstractActionOperation("Operation_DetectStartRevision", SVNUIMessages.class) { //$NON-NLS-1$
						@Override
						protected void runImpl(IProgressMonitor monitor) throws Exception {
							ISVNConnector proxy = selectedResource.getRepositoryLocation().acquireSVNProxy();
							try {
								SVNLogEntry[] msgs = SVNUtility.logEntries(proxy,
										SVNUtility.getEntryReference(selectedResource), SVNRevision.fromNumber(0),
										selectedResource.getSelectedRevision(),
										Options.DISCOVER_PATHS | (startFromCopy ? Options.STOP_ON_COPY : Options.NONE),
										ISVNConnector.EMPTY_LOG_ENTRY_PROPS, 1,
										new SVNProgressMonitor(this, monitor, null));
								if (msgs.length > 0) {
									revisions[0] = new SVNRevisionRange(
											SVNRevision.fromNumber(
													startFromCopy & msgs[0].revision > 1
															? msgs[0].revision - 1
															: msgs[0].revision),
											revisions[0].to);
								}
							} finally {
								selectedResource.getRepositoryLocation().releaseSVNProxy(proxy);
							}
						}
					}, true);
		}
		// align range bounds
		for (int i = 0; i < revisions.length; i++) {
			if (reverseRevisions ^ (revisions[i].from.getKind() == SVNRevision.Kind.HEAD
					|| revisions[i].from.getKind() == SVNRevision.Kind.NUMBER
							&& revisions[i].to.getKind() == SVNRevision.Kind.NUMBER
							&& ((SVNRevision.Number) revisions[i].from)
									.getNumber() > ((SVNRevision.Number) revisions[i].to).getNumber())) {
				revisions[i] = new SVNRevisionRange(revisions[i].to, revisions[i].from);
			}
		}
		//reorder revisions
		reorderRevisions(reverseRevisions);
		return revisions;
	}

	protected void reorderRevisions(final boolean reverseRevisions) {
		Arrays.sort(revisions, (o1, o2) -> {
			long rev1 = ((SVNRevision.Number) o1.from).getNumber();
			long rev2 = ((SVNRevision.Number) o2.from).getNumber();
			int retVal = rev1 == rev2 ? 0 : rev1 < rev2 ? -1 : 1;
			return reverseRevisions ? retVal * -1 : retVal;
		});
	}

	public IRepositoryResource getSelectedResource() {
		return selectedResource;
	}

	public void addChangeRevisionListener(SelectionListener listener) {
		changeRevisionButton.addSelectionListener(listener);
	}

	public void setSelectedResource(IRepositoryResource resource) {
		selectedResource = resource;
		if (baseResource == null) {
			baseResource = resource;
		}
		if (selectedResource == null) {
			setEnabled(false);
		} else {
			SVNRevision rev = selectedResource.getSelectedRevision();
			setRevisionValue(rev);
		}
	}

	public void setRevisionValue(SVNRevision rev) {
		if (rev.getKind() == Kind.NUMBER) {
			selectedRevision = rev;
			lastSelectedRevision = ((SVNRevision.Number) selectedRevision).getNumber();

			revisionField.setText(selectedRevision.toString());
			headRevisionRadioButton.setSelection(false);
			if (checkStyled) {
				startFromCopyRadioButton.setSelection(false);
			} else if (hasDateTime) {
				dateTimeRadioButton.setSelection(false);
			}
			changeRevisionRadioButton.setSelection(true);
		} else if (rev.getKind() == Kind.DATE && !checkStyled && hasDateTime) {
			selectedRevision = rev;
			Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(((SVNRevision.Date) rev).getDate());
			dateField.setYear(calendar.get(Calendar.YEAR));
			dateField.setMonth(calendar.get(Calendar.MONTH));
			dateField.setDay(calendar.get(Calendar.DAY_OF_MONTH));
			timeField.setHours(calendar.get(Calendar.HOUR_OF_DAY));
			timeField.setMinutes(calendar.get(Calendar.MINUTE));
			timeField.setSeconds(calendar.get(Calendar.SECOND));
			headRevisionRadioButton.setSelection(false);
			dateTimeRadioButton.setSelection(true);
			changeRevisionRadioButton.setSelection(false);
		} else {
			selectedRevision = defaultRevision;
			lastSelectedRevision = -1;

			revisionField.setText(""); //$NON-NLS-1$
			headRevisionRadioButton.setSelection(!checkStyled);
			if (checkStyled) {
				startFromCopyRadioButton.setSelection(true);
			} else if (hasDateTime) {
				dateTimeRadioButton.setSelection(false);
			}
			changeRevisionRadioButton.setSelection(false);
		}
		setEnabled(true);
	}

	public long getCurrentRevision() {
		return currentRevision;
	}

	public void setCurrentRevision(long currentRevision) {
		this.currentRevision = currentRevision;
	}

	private void createControls() {
		GridLayout layout = null;
		GridData data = null;

		layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		setLayout(layout);

		Group group = new Group(this, SWT.NONE);
		group.setText(captions == null
				? checkStyled ? SVNUIMessages.RevisionComposite_Revisions : SVNUIMessages.RevisionComposite_Revision
				: captions[0]);
		layout = new GridLayout();
		layout.numColumns = 3;
		group.setLayout(layout);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 3;
		group.setLayoutData(data);

		headRevisionRadioButton = new Button(group, SWT.RADIO);
		headRevisionRadioButton.setText(captions == null
				? checkStyled ? SVNUIMessages.RevisionComposite_All : SVNUIMessages.RevisionComposite_HeadRevision
				: captions[1]);
		data = new GridData();
		data.horizontalSpan = 3;
		headRevisionRadioButton.setLayoutData(data);
		headRevisionRadioButton.setSelection(!checkStyled);

		headRevisionRadioButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				validationManager.validateContent();
				if (((Button) e.widget).getSelection()) {
					changeRevisionButton.setEnabled(false);
					revisionField.setEnabled(false);
					if (!checkStyled && hasDateTime) {
						dateField.setEnabled(false);
						timeField.setEnabled(false);
					}
					startFromCopy = false;
					RevisionComposite.this.defaultToRevisions();
				}
				RevisionComposite.this.additionalValidation();
			}
		});

		if (checkStyled) {
			startFromCopyRadioButton = new Button(group, SWT.RADIO);
			startFromCopyRadioButton.setText(SVNUIMessages.RevisionComposite_StartFromCopy);
			data = new GridData();
			data.horizontalSpan = 3;
			startFromCopyRadioButton.setLayoutData(data);
			startFromCopyRadioButton.setSelection(startFromCopy = true);
			startFromCopyRadioButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					validationManager.validateContent();
					if (((Button) e.widget).getSelection()) {
						changeRevisionButton.setEnabled(false);
						revisionField.setEnabled(false);
						if (!checkStyled && hasDateTime) {
							dateField.setEnabled(false);
							timeField.setEnabled(false);
						}
						startFromCopy = true;
						RevisionComposite.this.defaultToRevisions();
					}
					RevisionComposite.this.additionalValidation();
				}
			});
		} else if (hasDateTime) {
			dateTimeRadioButton = new Button(group, SWT.RADIO);
			dateTimeRadioButton.setText(SVNUIMessages.RevisionComposite_DateTime);
			data = new GridData();
			dateTimeRadioButton.setLayoutData(data);
			dateTimeRadioButton.setSelection(false);

			dateTimeRadioButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					validationManager.validateContent();
					if (((Button) e.widget).getSelection()) {
						changeRevisionButton.setEnabled(false);
						revisionField.setEnabled(false);
						dateField.setEnabled(true);
						timeField.setEnabled(true);
						startFromCopy = false;
						RevisionComposite.this.dateTimeToRevision();
					}
					RevisionComposite.this.additionalValidation();
				}
			});

			Composite cmp = new Composite(group, SWT.NONE);
			layout = new GridLayout();
			layout.numColumns = 2;
			layout.marginHeight = layout.marginWidth = 0;
			cmp.setLayout(layout);
			data = new GridData();
			cmp.setLayoutData(data);

			dateField = new DateTime(cmp, SWT.DATE | SWT.MEDIUM);
			data = new GridData();
			dateField.setLayoutData(data);
			dateField.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					RevisionComposite.this.dateTimeToRevision();
				}
			});

			timeField = new DateTime(cmp, SWT.TIME | SWT.MEDIUM);
			data = new GridData();
			timeField.setLayoutData(data);
			timeField.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					RevisionComposite.this.dateTimeToRevision();
				}
			});

			Label label = new Label(group, SWT.NONE);
			data = new GridData();
			label.setLayoutData(data);

			dateField.setEnabled(false);
			timeField.setEnabled(false);
		}

		changeRevisionRadioButton = new Button(group, SWT.RADIO);
		changeRevisionRadioButton.setText(checkStyled
				? SVNUIMessages.RevisionComposite_RevisionsCtrl
				: SVNUIMessages.RevisionComposite_RevisionCtrl);

		changeRevisionRadioButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				validationManager.validateContent();
				if (((Button) e.widget).getSelection()) {
					changeRevisionButton.setEnabled(true);
					revisionField.setEnabled(true);
					if (!checkStyled && hasDateTime) {
						dateField.setEnabled(false);
						timeField.setEnabled(false);
					}
					startFromCopy = false;
					RevisionComposite.this.textToRevisions();
				}
				RevisionComposite.this.additionalValidation();
			}
		});

		if (checkStyled) {
			data = new GridData(GridData.FILL_HORIZONTAL);
		} else {
			data = new GridData();
			data.horizontalAlignment = SWT.FILL;
		}
		revisionField = new Text(group, SWT.SINGLE | SWT.BORDER);
		revisionField.setLayoutData(data);
		revisionField.setEnabled(false);
		CompositeVerifier verifier = new CompositeVerifier();
		String name = changeRevisionRadioButton.getText();
		verifier.add(new NonEmptyFieldVerifier(name));
		verifier.add(checkStyled ? new RevisionRangesVerifier(name) : new IntegerFieldVerifier(name, true));
		validationManager.attachTo(revisionField, new AbstractVerifierProxy(verifier) {
			@Override
			protected boolean isVerificationEnabled(Control input) {
				return changeRevisionRadioButton.getSelection();
			}
		});
		revisionField.addModifyListener(e -> RevisionComposite.this.textToRevisions());

		changeRevisionButton = new Button(group, SWT.PUSH);
		changeRevisionButton.setText(SVNUIMessages.Button_Browse);
		data = new GridData();
		data.widthHint = DefaultDialog.computeButtonWidth(changeRevisionButton);
		changeRevisionButton.setLayoutData(data);
		changeRevisionButton.setEnabled(false);

		changeRevisionButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				GetLogMessagesOperation msgsOp = SVNHistoryPanel.getMsgsOp(selectedResource, stopOnCopy);
				if (!UIMonitorUtility.doTaskNowDefault(RevisionComposite.this.getShell(), msgsOp, true).isCancelled()
						&& msgsOp.getExecutionState() == IActionOperation.OK) {
					SelectRevisionPanel panel = new SelectRevisionPanel(msgsOp, checkStyled, checkStyled,
							currentRevision);
					if (toFilterCurrent) {
						RevisionLogEntryFilter revFilter = new RevisionLogEntryFilter();
						long revNum = SVNRevision.INVALID_REVISION_NUMBER;
						if (baseResource != null) {
							try {
								revNum = baseResource.getRevision();
							} catch (SVNConnectorException ex) {
							}
						}
						revFilter.setRevisionstoHide(revNum, revNum);
						panel.addFilter(revFilter);
					}
					DefaultDialog dialog = new DefaultDialog(RevisionComposite.this.getShell(), panel);
					if (dialog.open() == 0) {
						if (checkStyled) {
							revisions = panel.getSelectedRevisions();
							RevisionComposite.this.reorderRevisions(false);
							String text = ""; //$NON-NLS-1$
							for (SVNRevisionRange range : revisions) {
								text += text.length() == 0 ? range.toString() : ", " + range.toString(); //$NON-NLS-1$
							}
							revisionField.setText(text);
						} else {
							long selectedRevisionNum = panel.getSelectedRevision();
							lastSelectedRevision = selectedRevisionNum;
							selectedRevision = SVNRevision.fromNumber(selectedRevisionNum);
							revisionField.setText(String.valueOf(selectedRevisionNum));
						}
					}
				}
				RevisionComposite.this.additionalValidation();
			}
		});
		if (checkStyled) {
			reverseRevisionsButton = new Button(group, SWT.CHECK);
			reverseRevisionsButton.setText(SVNUIMessages.RevisionComposite_Reverse);
			data = new GridData(GridData.FILL_HORIZONTAL);
			data.horizontalSpan = 2;
			reverseRevisionsButton.setLayoutData(data);
			reverseRevisionsButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					reverseRevisions = ((Button) e.widget).getSelection();
				}
			});
		}

		defaultToRevisions();
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		headRevisionRadioButton.setEnabled(enabled);
		if (checkStyled) {
			startFromCopyRadioButton.setEnabled(enabled);
			reverseRevisionsButton.setEnabled(enabled);
		} else if (hasDateTime) {
			dateTimeRadioButton.setEnabled(enabled);
			dateField.setEnabled(enabled && dateTimeRadioButton.getSelection());
			timeField.setEnabled(enabled && dateTimeRadioButton.getSelection());
		}
		changeRevisionRadioButton.setEnabled(enabled);
		changeRevisionButton.setEnabled(enabled && changeRevisionRadioButton.getSelection());
		revisionField.setEnabled(enabled && changeRevisionRadioButton.getSelection());
	}

	public void additionalValidation() {
		//override this if there is a need to perform additional validation
	}

	protected void dateTimeToRevision() {
		Calendar calendar = Calendar.getInstance();
		calendar.set(dateField.getYear(), dateField.getMonth(), dateField.getDay(), timeField.getHours(),
				timeField.getMinutes(), timeField.getSeconds());
		selectedRevision = SVNRevision.fromDate(calendar.getTimeInMillis());
	}

	protected void defaultToRevisions() {
		if (checkStyled) {
			revisions = new SVNRevisionRange[] { new SVNRevisionRange(SVNRevision.START, defaultRevision) };
		} else {
			selectedRevision = defaultRevision;
		}
	}

	protected void textToRevisions() {
		String input = revisionField.getText();
		try {
			if (checkStyled) {
				String[] parts = input.split(","); //$NON-NLS-1$
				ArrayList<SVNRevisionRange> revisions = new ArrayList<>();
				for (String part : parts) {
					revisions.add(new SVNRevisionRange(part.trim()));
				}
				this.revisions = revisions.toArray(new SVNRevisionRange[revisions.size()]);
			} else {
				long selectedRevisionNum = Long.parseLong(input);
				if (selectedRevisionNum >= 0) {
					lastSelectedRevision = selectedRevisionNum;
					selectedRevision = SVNRevision.fromNumber(selectedRevisionNum);
				}
			}
		} catch (NumberFormatException ex) {
			//don't handle this exception - already handled by the verifier
		}
	}

}
