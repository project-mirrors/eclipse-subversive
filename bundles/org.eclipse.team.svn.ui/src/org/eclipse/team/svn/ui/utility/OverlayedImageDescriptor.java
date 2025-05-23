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

package org.eclipse.team.svn.ui.utility;

import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;

/**
 * Overlayed image descriptor
 * 
 * @author Alexander Gurov
 */
public class OverlayedImageDescriptor extends CompositeImageDescriptor {

	public static int LEFT = 0x00000000;

	public static int RIGHT = 0x00000001;

	public static int CENTER_H = 0x00000003;

	public static int TOP = 0x00000000;

	public static int BOTTOM = 0x00000010;

	public static int CENTER_V = 0x00000030;

	public static int CENTER = CENTER_H | CENTER_V;

	protected Point size;

	protected Image base;

	protected ImageDescriptor overlay;

	protected int where;

	public OverlayedImageDescriptor(Image base, ImageDescriptor overlay, Point size, int where) {
		this.base = base;
		this.size = size;
		this.overlay = overlay;
		this.where = where;
	}

	@Override
	protected void drawCompositeImage(int width, int height) {
		this.drawImage(base.getImageData(), 0, 0);

		ImageData overlayData = overlay.getImageData();
		int x = 0;
		int y = 0;
		if ((where & OverlayedImageDescriptor.CENTER_H) == OverlayedImageDescriptor.CENTER_H) {
			x = (size.x - overlayData.width) / 2;
		} else if ((where & OverlayedImageDescriptor.RIGHT) == OverlayedImageDescriptor.RIGHT) {
			x = size.x - overlayData.width;
		}
		if ((where & OverlayedImageDescriptor.CENTER_V) == OverlayedImageDescriptor.CENTER_V) {
			y = (size.y - overlayData.height) / 2;
		} else if ((where & OverlayedImageDescriptor.BOTTOM) == OverlayedImageDescriptor.BOTTOM) {
			y = size.y - overlayData.height;
		}
		this.drawImage(overlayData, x, y);
	}

	@Override
	protected Point getSize() {
		return size;
	}

	@Override
	public int hashCode() {
		return base.hashCode() ^ overlay.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj != null && obj instanceof OverlayedImageDescriptor) {
			OverlayedImageDescriptor other = (OverlayedImageDescriptor) obj;
			return base.equals(other.base) && overlay.equals(other.overlay) && where == other.where
					&& size.equals(other.size);
		}
		return super.equals(obj);
	}

}
