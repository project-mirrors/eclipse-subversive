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

package org.eclipse.team.svn.ui.mapping;

import java.util.Comparator;

import org.eclipse.team.internal.core.subscribers.ChangeSet;

public class SVNChangeSetComparator implements Comparator<ChangeSet> {
	private SVNChangeSetSorter fSorter = new SVNChangeSetSorter();

	public int compare(ChangeSet o1, ChangeSet o2) {
		return this.fSorter.compare(null, o1, o2);
	}
}
