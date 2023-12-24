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

package org.eclipse.team.svn.ui.compare;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.connector.SVNEntryRevisionReference;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.ui.SVNUIMessages;

/**
 * Compare input for comparison remote resources' SVN properties.
 * 
 * @author Alexei Goncharov
 */
public class TwoWayPropertyCompareInput extends PropertyCompareInput {

	public TwoWayPropertyCompareInput(CompareConfiguration configuration, SVNEntryRevisionReference left,
			SVNEntryRevisionReference right, IRepositoryLocation location) {
		super(configuration, left, right, null, location);
	}

	@Override
	protected void fillMenu(IMenuManager manager, TreeSelection selection) {
		// is menu needed???
	}

	@Override
	public String getTitle() {
		String nameLeft = left.path.substring(left.path.lastIndexOf("/") + 1); //$NON-NLS-1$
		String nameRight = right.path.substring(right.path.lastIndexOf("/") + 1); //$NON-NLS-1$
		if (nameLeft.equals(nameRight)) {
			return BaseMessages.format(SVNUIMessages.PropertyCompareInput_Title2,
					new String[] { nameLeft + " [" + getRevisionPart(left), //$NON-NLS-1$
							getRevisionPart(right) + "] " //$NON-NLS-1$
					});
		}
		return BaseMessages.format(SVNUIMessages.PropertyCompareInput_Title2,
				new String[] { nameLeft + " [" + getRevisionPart(left) + "]", //$NON-NLS-1$ //$NON-NLS-2$
						nameRight + " [" + getRevisionPart(right) + "] " //$NON-NLS-1$ //$NON-NLS-2$
				});

	}

	@Override
	protected String getRevisionPart(SVNEntryRevisionReference reference) {
		return BaseMessages.format(SVNUIMessages.ResourceCompareInput_RevisionSign,
				new String[] { String.valueOf(reference.revision) });
	}

}
