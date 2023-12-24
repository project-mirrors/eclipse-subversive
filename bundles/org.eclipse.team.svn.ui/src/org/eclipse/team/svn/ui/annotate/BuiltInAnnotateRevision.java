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
 *    Alexander Gurov (Polarion Software) - initial API and implementation
 *    Thomas Champagne - Bug 217561 : additional date formats for label decorations
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.annotate;

import java.util.ArrayList;
import java.util.Date;

import org.eclipse.jface.text.revisions.Revision;
import org.eclipse.jface.text.source.LineRange;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.SVNLogEntry;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.utility.DateFormatter;

/**
 * Built-in annotate revision model
 * 
 * @author Alexander Gurov
 */
public class BuiltInAnnotateRevision extends Revision {
	public static final int END_LINE = -1;

	protected class MergeInfo {
		public final String line;

		public final String reference;

		public final long date;

		public final String author;

		public MergeInfo(String line, String reference, long date, String author) {
			this.line = line;
			this.reference = reference;
			this.date = date;
			this.author = author;
		}
	}

	protected String id;

	protected String author;

	private int startLine;

	private int stopLine;

	protected RGB color;

	protected SVNLogEntry msg;

	protected ArrayList<MergeInfo> mergeInfoList;

	public BuiltInAnnotateRevision(String id, String author, RGB color) {
		this.id = id;
		this.color = color;
		this.author = author;
		startLine = stopLine = BuiltInAnnotateRevision.END_LINE;
	}

	public void setLogMessage(SVNLogEntry msg) {
		this.msg = msg;
	}

	public long getRevision() {
		return Long.parseLong(id);
	}

	@Override
	public String getId() {
		return mergeInfoList != null ? id + "+" : id; //$NON-NLS-1$
	}

	public void addLine(int line) {
		if (startLine == BuiltInAnnotateRevision.END_LINE) {
			startLine = stopLine = line;
		} else if (line == BuiltInAnnotateRevision.END_LINE) {
			addRange(new LineRange(startLine - 1, stopLine - startLine + 1));
		} else if (line - stopLine == 1) {
			stopLine = line;
		} else {
			addRange(new LineRange(startLine - 1, stopLine - startLine + 1));
			startLine = stopLine = line;
		}
	}

	public void addMergeInfo(int line, long mergedRevision, long mergedDate, String mergedAuthor, String mergedPath) {
		// Merged lines:
		// XX with line from path@rev made by ZZZ at YYY
		if (mergeInfoList == null) {
			mergeInfoList = new ArrayList<>();
		}
		String reference = mergedPath != null ? mergedPath + "@" + mergedRevision : String.valueOf(mergedRevision); //$NON-NLS-1$
		mergeInfoList.add(new MergeInfo(String.valueOf(line), reference, mergedDate, mergedAuthor));
	}

	@Override
	public RGB getColor() {
		return color;
	}

	@Override
	public Object getHoverInfo() {
		String info = "<b>" + SVNUIMessages.BuiltInAnnotateRevision_Revision + " </b>" + id + "<br>"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		if (author != null) {
			info += "<b>" + SVNUIMessages.BuiltInAnnotateRevision_Author + " </b>" + author; //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (getDateImpl() != null) {
			info += "<br><b>" + SVNUIMessages.BuiltInAnnotateRevision_Date + " </b>" //$NON-NLS-1$//$NON-NLS-2$
					+ DateFormatter.formatDate(getDate());
		}
		String message = msg == null ? null : msg.message;
		if (message != null && message.length() > 0) {
			info += "<br><b>" + SVNUIMessages.BuiltInAnnotateRevision_Message + "</b><br>" + msg.message; //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (mergeInfoList != null) {
			info += "<br>"; //$NON-NLS-1$
			for (MergeInfo mergeInfo : mergeInfoList) {
				info += "<br>" + BaseMessages.format(SVNUIMessages.BuiltInAnnotateRevision_MergedWith, //$NON-NLS-1$
						new String[] { mergeInfo.line, mergeInfo.reference, mergeInfo.author,
								DateFormatter.formatDate(new Date(mergeInfo.date)) });
			}
		}
		return info;
	}

	@Override
	public Date getDate() {
		Date date = getDateImpl();
		return date == null ? new Date(0) : date;
	}

	protected Date getDateImpl() {
		return msg == null || msg.date == 0 ? null : new Date(msg.date);
	}

	@Override
	public String getAuthor() {
		String author = this.author == null ? SVNMessages.SVNInfo_NoAuthor : this.author;
		return author + " "; // Eclipse IDE does not separate line numbers and author names  //$NON-NLS-1$
	}

}
