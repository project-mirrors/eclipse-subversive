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
 *    Alexei Goncharov (Polarion Software) - URL decoration with bugtraq properties does not work properly (bug 252563)
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.properties.bugtraq;

import java.util.ArrayList;
import java.util.List;

/**
 * Link placements
 * 
 * @author Alexander Gurov
 */
public class LinkList {
	protected ArrayList<LinkPlacement> links = new ArrayList<>();

	public LinkList() {
	}

	public List<LinkPlacement> getLinks() {
		return links;
	}

	public boolean hasLinkAt(int offset) {
		for (LinkPlacement link : links) {
			if (link.existAtOffset(offset)) {
				return true;
			}
		}
		return false;
	}

	public LinkPlacement getLinkAt(int offset) {
		for (LinkPlacement link : links) {
			if (link.existAtOffset(offset)) {
				return link;
			}
		}
		return null;
	}

	public static class LinkPlacement {
		protected int start;

		protected int end;

		protected String linkText;

		public LinkPlacement(int start, int end, String message) {
			linkText = message.substring(start, end);
			this.start = start;
			this.end = end;
		}

		protected boolean existAtOffset(int offset) {
			return start <= offset && offset < end;
		}

		public int getStart() {
			return start;
		}

		public void setStart(int start) {
			this.start = start;
		}

		public int getEnd() {
			return end;
		}

		public void setEnd(int end) {
			this.end = end;
		}

		public String getURL() {
			return linkText;
		}
	}

}
