/*******************************************************************************
 * Copyright (c) 2000, 2024 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.team.svn.internal.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Layout;

/**
 * This layout controls the position and size of the children of a tab folder.
 * In addition this layout can be used for any component that wants
 * TabFolder-Like behaviour (e.g. only showing all component at the full size
 * of the composite stacked onto each other)
 *
 * FIXME: To be removed after upgrade to 2024-12 target
 */
public class TabFolderLayout extends Layout {

	@Override
	protected Point computeSize(Composite composite, int wHint, int hHint, boolean flushCache) {
		if (wHint != SWT.DEFAULT && hHint != SWT.DEFAULT) {
			return new Point(wHint, hHint);
		}

		Control[] children = composite.getChildren();
		int count = children.length;
		int maxWidth = 0, maxHeight = 0;
		for (int i = 0; i < count; i++) {
			Control child = children[i];
			Point pt = child.computeSize(SWT.DEFAULT, SWT.DEFAULT, flushCache);
			maxWidth = Math.max(maxWidth, pt.x);
			maxHeight = Math.max(maxHeight, pt.y);
		}

		if (wHint != SWT.DEFAULT) {
			maxWidth = wHint;
		}
		if (hHint != SWT.DEFAULT) {
			maxHeight = hHint;
		}

		return new Point(maxWidth, maxHeight);

	}

	@Override
	protected void layout(Composite composite, boolean flushCache) {
		Rectangle rect = composite.getClientArea();

		Control[] children = composite.getChildren();
		for (Control c : children) {
			c.setBounds(rect);
		}
	}
}
