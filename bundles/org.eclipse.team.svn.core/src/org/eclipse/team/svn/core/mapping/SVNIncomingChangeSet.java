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
 *    Alexei Goncharov (Polarion Software) - initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.core.mapping;

import java.util.Date;

import org.eclipse.team.internal.core.subscribers.DiffChangeSet;

public class SVNIncomingChangeSet extends DiffChangeSet {

	protected String comment;

	protected String author;

	protected Long revision;

	protected Date date;

	public SVNIncomingChangeSet() {
		comment = ""; //$NON-NLS-1$
		revision = Long.MIN_VALUE;
		author = ""; //$NON-NLS-1$
		date = null;
	}

	@Override
	public void setName(String name) {
		super.setName(name);
	}

	public void setRevision(Long revision) {
		this.revision = revision;
	}

	public Long getRevision() {
		return revision;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public String getAuthor() {
		return author;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public Date getDate() {
		return date;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	@Override
	public String getComment() {
		return comment;
	}
}
