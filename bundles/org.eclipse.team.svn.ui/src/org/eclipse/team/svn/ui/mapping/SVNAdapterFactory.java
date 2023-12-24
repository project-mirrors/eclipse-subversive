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
 *    Igor Burilo - Initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.mapping;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.team.ui.mapping.ISynchronizationCompareAdapter;

/**
 * @author Igor Burilo
 *
 */
public class SVNAdapterFactory implements IAdapterFactory {

	protected ChangeSetCompareAdapter compareAdapter;

	@Override
	@SuppressWarnings("unchecked")
	public Object getAdapter(Object adaptableObject, Class adapterType) {
		if (ISynchronizationCompareAdapter.class == adapterType) {
			if (compareAdapter == null) {
				compareAdapter = new ChangeSetCompareAdapter();
			}
			return compareAdapter;
		}

		return null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Class[] getAdapterList() {
		return new Class[] { ISynchronizationCompareAdapter.class };
	}

}
