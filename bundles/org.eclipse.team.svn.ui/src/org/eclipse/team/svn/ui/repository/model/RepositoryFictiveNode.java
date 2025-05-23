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

package org.eclipse.team.svn.ui.repository.model;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.model.IWorkbenchAdapter2;

/**
 * Fictive repository tree node (errors, pending etc.)
 * 
 * @author Alexander Gurov
 */
public abstract class RepositoryFictiveNode implements IWorkbenchAdapter, IWorkbenchAdapter2, IAdaptable {
	@Override
	public Object getParent(Object o) {
		return null;
	}

	@Override
	public Object getAdapter(Class adapter) {
		if (adapter.equals(IWorkbenchAdapter.class) || adapter.equals(IWorkbenchAdapter2.class)) {
			return this;
		}
		return null;
	}

	@Override
	public RGB getBackground(Object element) {
		// do not change default background color
		return null;
	}

	@Override
	public RGB getForeground(Object element) {
		return null;
	}

	@Override
	public FontData getFont(Object element) {
		// do not change default font
		return null;
	}

}
