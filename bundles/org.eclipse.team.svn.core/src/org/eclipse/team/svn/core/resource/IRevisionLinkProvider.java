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

package org.eclipse.team.svn.core.resource;

/**
 * This interface is designed for differed-in-time data acquisition
 * 
 * @author Igor Burilo
 */
public interface IRevisionLinkProvider {
	public class DefaultRevisionLinkProvider implements IRevisionLinkProvider {
		protected IRevisionLink[] links;

		public DefaultRevisionLinkProvider(IRevisionLink[] links) {
			this.links = links;
		}

		public IRevisionLink[] getRevisionLinks() {
			return this.links;
		}
	}

	public IRevisionLink[] getRevisionLinks();
}
