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

package org.eclipse.team.svn.ui.panel.reporting;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.svn.core.utility.PatternProvider;
import org.eclipse.team.svn.ui.panel.AbstractDialogPanel;

/**
 * Styled preview panel
 * 
 * @author Alexander Gurov
 */
public class PreviewPanel extends AbstractDialogPanel {
	protected String report;

	protected Font font;

	public PreviewPanel(String title, String description, String message, String report) {
		this(title, description, message, report, null);
	}

	public PreviewPanel(String title, String description, String message, String report, Font font) {
		super(new String[] { IDialogConstants.OK_LABEL });
		dialogTitle = title;
		dialogDescription = description;
		defaultMessage = message;
		this.report = report;
		this.font = font;
	}

	@Override
	public void createControlsImpl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		composite.setLayout(layout);
		GridData data = new GridData(GridData.FILL_BOTH);
		Point prefferedSize = getPrefferedSize();
		data.widthHint = prefferedSize.x;
		data.heightHint = prefferedSize.y;
		composite.setLayoutData(data);

		report = PatternProvider.replaceAll(report, "<br>", "\n"); //$NON-NLS-1$ //$NON-NLS-2$
		report = PatternProvider.replaceAll(report, "&lt;", "<"); //$NON-NLS-1$ //$NON-NLS-2$
		report = PatternProvider.replaceAll(report, "&gt;", ">"); //$NON-NLS-1$ //$NON-NLS-2$
		StyledText styledText = new StyledText(composite,
				SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.BORDER | SWT.WRAP);

		if (font != null) {
			styledText.setFont(font);
		}

		List<StyleRange> styledRanges = new ArrayList<>();
		styledRanges = getStyleRanges();
		styledText.setText(report);
		styledText.setStyleRanges(styledRanges.toArray(new StyleRange[styledRanges.size()]));
		styledText.setEditable(false);
		styledText.setLayoutData(data);
	}

	@Override
	public void dispose() {
		super.dispose();
		if (font != null) {
			font.dispose();
		}
	}

	@Override
	protected void saveChangesImpl() {
	}

	@Override
	protected void cancelChangesImpl() {
	}

	protected List<StyleRange> getStyleRanges() {
		List<StyleRange> styledRanges = new ArrayList<>();

		Stack<StyleRange> boldEntries = new Stack<>();
		Stack<StyleRange> italicEntries = new Stack<>();

		for (int i = 0; i < report.length(); i++) {
			if (report.charAt(i) == '<' && i < report.length() - 2) {
				if (report.charAt(i + 2) == '>') {
					StyleRange range = new StyleRange();
					range.start = i;
					if (report.charAt(i + 1) == 'b') {
						range.fontStyle = SWT.BOLD;
						boldEntries.push(range);
						report = report.substring(0, i) + report.substring(i + 3);
					} else if (report.charAt(i + 1) == 'i') {
						range.fontStyle = SWT.ITALIC;
						italicEntries.push(range);
						report = report.substring(0, i) + report.substring(i + 3);
					}
				} else if (report.charAt(i + 1) == '/') {
					if (i < report.length() - 3 && report.charAt(i + 3) == '>') {
						if (report.charAt(i + 2) == 'b') {
							if (boldEntries.size() > 0) {
								StyleRange range = boldEntries.pop();
								range.length = i - range.start;
								styledRanges.add(range);
								report = report.substring(0, i) + report.substring(i + 4);
							}
						} else if (report.charAt(i + 2) == 'i') {
							if (italicEntries.size() > 0) {
								StyleRange range = italicEntries.pop();
								range.length = i - range.start;
								styledRanges.add(range);
								report = report.substring(0, i) + report.substring(i + 4);
							}
						}
					}
				}
			}
		}

		return styledRanges;
	}

	@Override
	public Point getPrefferedSizeImpl() {
		return new Point(640, 300);
	}

}
