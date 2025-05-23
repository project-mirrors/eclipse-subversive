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

package org.eclipse.team.svn.core.resource;

import java.util.Map;

/**
 * This interface is desinged for deffered-in-time data acquisition
 * 
 * @author Alexei Goncharov
 */
public interface IRepositoryResourceWithStatusProvider extends IRepositoryResourceProvider {

	public class DefaultRepositoryResourceWithStatusProvider extends DefaultRepositoryResourceProvider
			implements IRepositoryResourceWithStatusProvider {

		protected Map<String, String> url2status;

		public DefaultRepositoryResourceWithStatusProvider(IRepositoryResource[] resources,
				Map<String, String> url2status) {
			super(resources);
			this.url2status = url2status;
		}

		@Override
		public Map<String, String> getStatusesMap() {
			return url2status;
		}
	}

	Map<String, String> getStatusesMap();

}
