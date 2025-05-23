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
 *    Gabor Liptak - Speedup Pattern's usage
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.panel.callback;

import java.util.ArrayList;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.connector.ssl.SSLServerCertificateFailures;
import org.eclipse.team.svn.core.connector.ssl.SSLServerCertificateInfo;
import org.eclipse.team.svn.core.utility.PatternProvider;
import org.eclipse.team.svn.core.utility.SVNUtility;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.panel.AbstractDialogPanel;
import org.eclipse.team.svn.ui.utility.ArrayStructuredContentProvider;

/**
 * This panel allows us to ask user about trust to SSL server
 * 
 * @author Alexander Gurov
 */
public class AskTrustSSLServerPanel extends AbstractDialogPanel {
	protected SSLServerCertificateFailures failures;

	protected SSLServerCertificateInfo info;

	public AskTrustSSLServerPanel(String location, SSLServerCertificateFailures failures, SSLServerCertificateInfo info,
			boolean allowPermanently) {
		super(allowPermanently
				? new String[] { SVNUIMessages.AskTrustSSLServerPanel_Trust,
						SVNUIMessages.AskTrustSSLServerPanel_TrustAlways, IDialogConstants.NO_LABEL }
				: new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL });
		dialogTitle = SVNUIMessages.AskTrustSSLServerPanel_Title;
		dialogDescription = SVNUIMessages.AskTrustSSLServerPanel_Description;
		defaultMessage = BaseMessages.format(SVNUIMessages.AskTrustSSLServerPanel_Message, new String[] { location });
		this.failures = failures;
		this.info = info;
	}

	@Override
	public Point getPrefferedSizeImpl() {
		return new Point(530, 250);
	}

	@Override
	public void createControlsImpl(Composite parent) {
		ArrayList tData = new ArrayList();
		String[] line = new String[2];
		line[0] = SVNUIMessages.AskTrustSSLServerPanel_Server;
		for (String s : info.hostnames) {
			line[1] = line[1] != null ? line[1] + ", " + s : s; //$NON-NLS-1$
		}
		tData.add(line);
		line = new String[2];
		line[0] = SVNUIMessages.AskTrustSSLServerPanel_Problems;
		line[1] = "";
		if (failures.anyOf(SSLServerCertificateFailures.UNKNOWN_CA | SSLServerCertificateFailures.OTHER)) {
			line[1] += SVNUIMessages.AskTrustSSLServerPanel_MsgNotTrusted;
		}
		if (failures.anyOf(SSLServerCertificateFailures.CN_MISMATCH | SSLServerCertificateFailures.OTHER)) {
			line[1] += line[1].length() > 0
					? "\n" + SVNUIMessages.AskTrustSSLServerPanel_MsgHostNameDoNotMatch //$NON-NLS-1$
					: SVNUIMessages.AskTrustSSLServerPanel_MsgHostNameDoNotMatch;
		}
		if (failures.anyOf(SSLServerCertificateFailures.NOT_YET_VALID | SSLServerCertificateFailures.OTHER)) {
			line[1] += line[1].length() > 0
					? "\n" + SVNUIMessages.AskTrustSSLServerPanel_MsgNotYetValid //$NON-NLS-1$
					: SVNUIMessages.AskTrustSSLServerPanel_MsgNotYetValid;
		}
		if (failures.anyOf(SSLServerCertificateFailures.EXPIRED | SSLServerCertificateFailures.OTHER)) {
			line[1] += line[1].length() > 0
					? "\n" + SVNUIMessages.AskTrustSSLServerPanel_MsgExpired //$NON-NLS-1$
					: SVNUIMessages.AskTrustSSLServerPanel_MsgExpired;
		}
		tData.add(line);
		line = new String[2];
		line[0] = "Subject"; //$NON-NLS-1$
		line[1] = info.subject;
		tData.add(line);
		line = new String[2];
		line[0] = "Valid"; //$NON-NLS-1$
		line[1] = SVNUtility.formatSSLValid(info.validFrom, info.validTo);
		tData.add(line);
		line = new String[2];
		line[0] = "Issuer"; //$NON-NLS-1$
		line[1] = info.issuer;
		tData.add(line);
		line = new String[2];
		line[0] = "Fingerprint"; //$NON-NLS-1$
		line[1] = SVNUtility.formatSSLFingerprint(info.fingerprint);
		tData.add(line);
		final String[][] tableData = (String[][]) tData.toArray(new String[tData.size()][]);

		GridData data = null;

		SashForm innerSashForm = new SashForm(parent, SWT.VERTICAL);
		data = new GridData(GridData.FILL_BOTH);
		innerSashForm.setLayoutData(data);

		final Table table = new Table(innerSashForm,
				SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		data = new GridData(GridData.FILL_BOTH);
		table.setLayoutData(data);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		TableLayout tLayout = new TableLayout();
		tLayout.addColumnData(new ColumnWeightData(20, true));
		tLayout.addColumnData(new ColumnWeightData(80, true));
		table.setLayout(tLayout);

		final Text text = new Text(innerSashForm, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		innerSashForm.setWeights(new int[] { 25, 75 });

		TableColumn col = new TableColumn(table, SWT.LEFT);
		col.setResizable(true);
		col.setText(SVNUIMessages.AskTrustSSLServerPanel_Field);
		col = new TableColumn(table, SWT.LEFT);
		col.setResizable(true);
		col.setText(SVNUIMessages.AskTrustSSLServerPanel_Value);

		TableViewer view = new TableViewer(table);
		view.setContentProvider(new ArrayStructuredContentProvider());
		view.setLabelProvider(new ITableLabelProvider() {
			@Override
			public Image getColumnImage(Object element, int columnIndex) {
				return null;
			}

			@Override
			public String getColumnText(Object element, int columnIndex) {
				String[] row = (String[]) element;
				return row[columnIndex];
			}

			@Override
			public void addListener(ILabelProviderListener listener) {
			}

			@Override
			public void dispose() {
			}

			@Override
			public boolean isLabelProperty(Object element, String property) {
				return true;
			}

			@Override
			public void removeListener(ILabelProviderListener listener) {
			}
		});
		view.setInput(tableData);
		SelectionListener listener = new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				int idx = table.getSelectionIndex();
				if (idx > -1 && idx < tableData.length) {
					text.setText(PatternProvider.replaceAll(tableData[idx][1].trim(), ", ", "\n")); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		};
		table.addSelectionListener(listener);

		data = new GridData(GridData.FILL_BOTH);
		text.setLayoutData(data);
		text.setEditable(false);

		table.setSelection(0);
		listener.widgetSelected(null);
	}

	@Override
	protected void saveChangesImpl() {
	}

	@Override
	protected void cancelChangesImpl() {
	}

}
