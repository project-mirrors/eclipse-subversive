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

package org.eclipse.team.svn.core.connector;

/**
 * SVN post-commit hook's message holder
 * 
 * @author Alexander Gurov
 */
public class SVNCommitStatus {
	public final String message;

	public final String repository;

	public final long revision;

	public final String author;

	public final long date;

	public SVNCommitStatus(String message, String repository, long revision, long date, String author) {
		this.message = message;
		this.repository = repository;
		this.revision = revision;
		this.date = date;
		this.author = author;
	}
}
