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

package org.eclipse.team.svn.ui.repository.model;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.IDecorationContext;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;

/**
 * Tooltip message container Provides the way to create tooltip like decoration
 *
 * @author Alexey Mikoyan
 *
 */
public class ToolTipMessage implements IDecoration {

	public String prefix = ""; //$NON-NLS-1$

	public String suffix = ""; //$NON-NLS-1$

	@Override
	public void addOverlay(ImageDescriptor overlay) {
	}

	@Override
	public void addOverlay(ImageDescriptor overlay, int quadrant) {
	}

	@Override
	public void addPrefix(String prefix) {
		this.prefix = prefix;
	}

	@Override
	public void addSuffix(String suffix) {
		this.suffix = suffix;
	}

	public String getMessage() {
		return prefix + suffix;
	}

	@Override
	public IDecorationContext getDecorationContext() {
		return null;
	}

	@Override
	public void setBackgroundColor(Color color) {
	}

	@Override
	public void setFont(Font font) {
	}

	@Override
	public void setForegroundColor(Color color) {
	}

}
