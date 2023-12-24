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

package org.eclipse.team.svn.core.synchronize;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.operation.local.AbstractMergeSet;
import org.eclipse.team.svn.core.operation.local.MergeSet1URL;
import org.eclipse.team.svn.core.operation.local.MergeSet2URL;
import org.eclipse.team.svn.core.operation.local.MergeSetReintegrate;

/**
 * Merge resources scope. Non-persistent.
 * 
 * @author Alexander Gurov
 */
public class MergeScopeHelper {
	protected AbstractMergeSet info;

	public MergeScopeHelper() {
	}

	public MergeScopeHelper(AbstractMergeSet info) {
		this.info = info;
	}

	public String getName() {
		if (info.to == null) {
			return ""; //$NON-NLS-1$
		}
		String url = null;
		if (info instanceof MergeSet1URL) {
			MergeSet1URL info = (MergeSet1URL) this.info;
			url = (info.from.length > 1 ? info.from[0].getRoot() : info.from[0]).getUrl();
		} else if (info instanceof MergeSet2URL) {
			MergeSet2URL info = (MergeSet2URL) this.info;
			url = (info.fromEnd.length > 1 ? info.fromEnd[0].getRoot() : info.fromEnd[0]).getUrl();
		} else {
			MergeSetReintegrate info = (MergeSetReintegrate) this.info;
			url = (info.from.length > 1 ? info.from[0].getRoot() : info.from[0]).getUrl();
		}
		String names = null;
		for (IResource element : info.to) {
			String path = element.getFullPath().toString().substring(1);
			names = names == null ? path : names + ", " + path; //$NON-NLS-1$
		}
		return BaseMessages.format(SVNMessages.MergeScope_Name, new String[] { url, names });
	}

	public IResource[] getRoots() {
		return info.to;
	}

	public void setMergeSet(AbstractMergeSet info) {
		this.info = info;
	}

	public AbstractMergeSet getMergeSet() {
		return info;
	}

}
