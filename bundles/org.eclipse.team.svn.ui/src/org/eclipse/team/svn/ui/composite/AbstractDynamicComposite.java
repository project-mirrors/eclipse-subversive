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
 *    Alexey Mikoyan - Initial implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/
package org.eclipse.team.svn.ui.composite;

import org.eclipse.swt.widgets.Composite;

/**
 * Helps to create components that can be made visible or invisible on demand
 *
 * @author Alexey Mikoyan
 *
 */
public abstract class AbstractDynamicComposite extends Composite {

	public AbstractDynamicComposite(Composite parent, int style) {
		super(parent, style);
	}

	public abstract void saveAppearance();

	public abstract void restoreAppearance();

	public abstract void revalidateContent();

}
