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
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.history.model;

import org.eclipse.team.svn.core.utility.PatternProvider;

/**
 * Abstract implementation of ILogNode
 * 
 * @author Alexander Gurov
 */
public abstract class AbstractLogNode implements ILogNode {
	protected ILogNode parent;

	public AbstractLogNode(ILogNode parent) {
		this.parent = parent;
	}

	public ILogNode getParent() {
		return this.parent;
	}

	protected static String flattenMultiLineText(String input, String lineSeparatorReplacement) {
		String retVal = PatternProvider.replaceAll(input, "\r\n", lineSeparatorReplacement); //$NON-NLS-1$
		retVal = PatternProvider.replaceAll(retVal, "\n", lineSeparatorReplacement); //$NON-NLS-1$
		retVal = PatternProvider.replaceAll(retVal, "\r", lineSeparatorReplacement); //$NON-NLS-1$
		return retVal;
	}
	
}
