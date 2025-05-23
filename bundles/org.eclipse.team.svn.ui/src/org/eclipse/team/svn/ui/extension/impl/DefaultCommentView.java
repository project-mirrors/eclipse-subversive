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
 *    Andrej Zachar - Initial API and implementation
 *    Jens Scheidtmann - butraq:logregex property display disgresses from specification (bug 243678)
 *    Alexei Goncharov (Polarion Software) - URL decoration with bugtraq properties does not work properly (bug 252563)
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.extension.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.ui.extension.factory.ICommentView;
import org.eclipse.team.svn.ui.panel.local.CommitPanel;
import org.eclipse.team.svn.ui.properties.bugtraq.BugtraqModel;
import org.eclipse.team.svn.ui.properties.bugtraq.IssueList;
import org.eclipse.team.svn.ui.properties.bugtraq.LinkList;
import org.eclipse.team.svn.ui.properties.bugtraq.LinkList.LinkPlacement;

/**
 * Default implementation of history comment
 * 
 * @author Andrej Zachar
 */
public class DefaultCommentView implements ICommentView {
	protected StyledText multilineComment;

	protected Cursor handCursor;

	protected Cursor busyCursor;

	protected Color black;

	protected Color blue;

	protected boolean mouseDown;

	protected boolean dragEvent;

	protected BugtraqModel model;

	protected IssueList linkList = new IssueList();

	protected IssueList hyperList = new IssueList();

	protected final static String linkRegExp = "(?:http|https|file|svn|svn\\+[\\w]+)\\:/(?:/)?(?:/[^\\s\\|\\{\\}\"><#\\^\\~\\[\\]`]+)+"; //$NON-NLS-1$

	@Override
	public void createCommentView(Composite parent) {
		this.createCommentView(parent, SWT.V_SCROLL | SWT.MULTI | SWT.BORDER | SWT.WRAP);
	}

	@Override
	public void createCommentView(Composite parent, int style) {
		multilineComment = new StyledText(parent, style);
		multilineComment.setEditable(false);
		// set system color
		multilineComment.setBackground(multilineComment.getBackground());

		handCursor = new Cursor(parent.getDisplay(), SWT.CURSOR_HAND);
		busyCursor = new Cursor(parent.getDisplay(), SWT.CURSOR_WAIT);

		blue = new Color(parent.getDisplay(), 0, 0, 192);
		black = new Color(parent.getDisplay(), 2, 200, 30);

		multilineComment.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				if (e.button == 1) {
					mouseDown = true;
				}
			}

			@Override
			public void mouseUp(MouseEvent e) {
				mouseDown = false;
				StyledText text = (StyledText) e.widget;
				int offset = text.getCaretOffset();
				LinkPlacement issue = linkList.getLinkAt(offset);
				LinkPlacement hIssue = hyperList.getLinkAt(offset);
				if (dragEvent) {
					dragEvent = false;
					if (issue != null) {
						text.setCursor(handCursor);
						text.getStyleRangeAtOffset(offset).background = blue;
					} else if (hIssue != null) {
						text.setCursor(handCursor);
						text.getStyleRangeAtOffset(offset).background = blue;
					}
				} else if (issue != null) {
					text.setCursor(busyCursor);
					String url = DefaultCommentView.this.getModel().getResultingURL(issue);
					if (url != null) {
						Program.launch(url);
					}
					text.setCursor(null);
					text.getStyleRangeAtOffset(offset).background = black;
				} else if (hIssue != null) {
					text.setCursor(busyCursor);
					String url = hIssue.getURL();
					if (url != null) {
						Program.launch(url);
					}
					text.setCursor(null);
					text.getStyleRangeAtOffset(offset).background = black;
				}
			}
		});
		multilineComment.addMouseMoveListener(e -> {
			// Do not change cursor on drag events
			if (mouseDown) {
				if (!dragEvent) {
					StyledText text = (StyledText) e.widget;
					text.setCursor(null);
				}
				dragEvent = true;
				return;
			}
			StyledText text = (StyledText) e.widget;
			int offset = -1;
			try {
				offset = text.getOffsetAtLocation(new Point(e.x, e.y));
			} catch (IllegalArgumentException ex) {
				// ok
			}
			if (offset != -1 && linkList.hasLinkAt(offset)) {
				text.setCursor(handCursor);
				text.getStyleRangeAtOffset(offset).background = blue;
				multilineComment.redraw();
			} else if (offset != -1 && hyperList.hasLinkAt(offset)) {
				text.setCursor(handCursor);
				text.getStyleRangeAtOffset(offset).background = blue;
				multilineComment.redraw();
			} else {
				text.setCursor(null);
			}
		});

		multilineComment.addModifyListener(e -> {
			linkList.getLinks().clear();
			hyperList.getLinks().clear();
			StyledText textView = (StyledText) e.getSource();
			String text = textView.getText();
			Pattern linkPattern = Pattern.compile(DefaultCommentView.linkRegExp);
			Matcher linkMatcher = linkPattern.matcher(text);
			int start = 0;
			while (linkMatcher.find(start)) {
				start = linkMatcher.end();
				hyperList.getLinks().add(new LinkPlacement(linkMatcher.start(), start, text));
			}
			if (DefaultCommentView.this.getModel().getMessage() != null
					|| DefaultCommentView.this.getModel().getLogregex() != null) {
				linkList.parseMessage(text, DefaultCommentView.this.getModel());
			}
			List<StyleRange> styledRanges = new ArrayList<>();
			for (LinkPlacement issue : linkList.getLinks()) {
				StyleRange range = new StyleRange();
				range.start = issue.getStart();
				range.length = issue.getEnd() - issue.getStart();
				range.foreground = blue;
				range.underline = true;
				styledRanges.add(range);
			}
			for (LinkList.LinkPlacement issue : hyperList.getLinks()) {
				StyleRange range = new StyleRange();
				range.start = issue.getStart();
				range.length = issue.getEnd() - issue.getStart();
				range.foreground = blue;
				range.underline = true;
				styledRanges.add(range);
			}
			StyleRange[] sorted = styledRanges.toArray(new StyleRange[styledRanges.size()]);
			for (int i = 0; i < sorted.length - 1; i++) {
				for (int j = sorted.length - 1; j > i; j--) {
					if (sorted[j].start < sorted[j - 1].start) {
						StyleRange tmp = sorted[j];
						sorted[j] = sorted[j - 1];
						sorted[j - 1] = tmp;
					}
				}
			}
			textView.setStyleRanges(sorted);
		});

		multilineComment.addDisposeListener(e -> {
			busyCursor.dispose();
			handCursor.dispose();
			blue.dispose();
			black.dispose();
		});
	}

	@Override
	public void usedFor(IResource resource) {
		CommitPanel.CollectPropertiesOperation bugtraqOp = new CommitPanel.CollectPropertiesOperation(
				new IResource[] { resource });
		bugtraqOp.run(new NullProgressMonitor());
		model = bugtraqOp.getBugtraqModel();
	}

	@Override
	public void usedFor(IRepositoryResource resource) {
		//FIXME implement support of Bugtraq properties
	}

	@Override
	public void setComment(String comment) {
		multilineComment.setText(comment);
	}

	protected BugtraqModel getModel() {
		return model != null ? model : new BugtraqModel();
	}

}