/*******************************************************************************
 * Copyright (c) 2005, 2025 Polarion Software and others.
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

package org.eclipse.team.svn.core.extension.factory;

import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.ISVNManager;
import org.eclipse.team.svn.core.operation.UnreportableException;

/**
 * Extension point factory for ISVNClientWrapper
 * 
 * @author Alexander Gurov
 */
public interface ISVNConnectorFactory {
	String DEFAULT_ID = "org.eclipse.team.svn.connector.svnkit1_10"; //$NON-NLS-1$

	String CURRENT_COMPATIBILITY_VERSION = "5.0.0"; //$NON-NLS-1$

	/**
	 * Enumeration of connector API compatibility levels
	 */
	public static class APICompatibility {
		/**
		 * Compatibility level for the connector library is not specified
		 */
		public static final int SVNAPI_NOT_SPECIFIED = -1;

		/**
		 * SVN 1.0 compatible API is supported by the connector
		 */
		public static final int SVNAPI_1_0_x = 0;

		/**
		 * SVN 1.1 compatible API is supported by the connector
		 */
		public static final int SVNAPI_1_1_x = 1;

		/**
		 * SVN 1.2 compatible API is supported by the connector
		 */
		public static final int SVNAPI_1_2_x = 2;

		/**
		 * SVN 1.3 compatible API is supported by the connector
		 */
		public static final int SVNAPI_1_3_x = 3;

		/**
		 * SVN 1.4 compatible API is supported by the connector
		 */
		public static final int SVNAPI_1_4_x = 4;

		/**
		 * SVN 1.5 compatible API is supported by the connector
		 */
		public static final int SVNAPI_1_5_x = 5;

		/**
		 * SVN 1.6 compatible API is supported by the connector
		 */
		public static final int SVNAPI_1_6_x = 6;

		/**
		 * SVN 1.7 compatible API is supported by the connector
		 */
		public static final int SVNAPI_1_7_x = 7;

		/**
		 * SVN 1.8 compatible API is supported by the connector
		 */
		public static final int SVNAPI_1_8_x = 8;

		/**
		 * SVN 1.9 compatible API is supported by the connector
		 */
		public static final int SVNAPI_1_9_x = 9;

		/**
		 * SVN 1.10 compatible API is supported by the connector
		 */
		public static final int SVNAPI_1_10_x = 10;
	}

	/**
	 * Enumeration of optional feature masks
	 */
	public static class OptionalFeatures {
		/**
		 * No optional features supported
		 */
		public static final int NO_OPTIONAL_FEATURES = 0;

		/**
		 * All optional features supported
		 */
		public static final int ALL_OPTIONAL_FEATURES = ~NO_OPTIONAL_FEATURES;

		/**
		 * Direct SSH settings specification is supported by a connector
		 */
		public static final int SSH_SETTINGS = 0x01;

		/**
		 * Direct PROXY settings specification is supported by a connector
		 */
		public static final int PROXY_SETTINGS = 0x02;

		/**
		 * Atomic cross-working copy commit is supported by a connector
		 */
		public static final int ATOMIC_X_COMMIT = 0x04;

		/**
		 * Creating of an FSFS repository is supported by a connector
		 */
		public static final int CREATE_REPOSITORY_FSFS = 0x08;

		/**
		 * Creating of an BDB repository is supported by a connector
		 */
		public static final int CREATE_REPOSITORY_BDB = 0x10;

		/**
		 * Creating of a repository is supported by a connector
		 */
		public static final int CREATE_REPOSITORY = CREATE_REPOSITORY_FSFS | CREATE_REPOSITORY_BDB;
	}

	ISVNConnectorFactory EMPTY = new ISVNConnectorFactory() {
		@Override
		public ISVNConnector createConnector() {
			throw new UnreportableException(getName());
		}

		@Override
		public ISVNManager createManager() {
			throw new UnreportableException(getName());
		}

		@Override
		public int getSupportedFeatures() {
			return OptionalFeatures.NO_OPTIONAL_FEATURES;
		}

		@Override
		public String getVersion() {
			return ""; //$NON-NLS-1$
		}

		@Override
		public String getName() {
			return SVNMessages.getErrorString(getId());
		}

		@Override
		public String getId() {
			return "Error_NoSVNClient"; //$NON-NLS-1$
		}

		@Override
		public String getCompatibilityVersion() {
			return ISVNConnectorFactory.CURRENT_COMPATIBILITY_VERSION;
		}

		@Override
		public String getClientVersion() {
			return ""; //$NON-NLS-1$
		}

		@Override
		public int getSVNAPIVersion() {
			return APICompatibility.SVNAPI_NOT_SPECIFIED;
		}
	};

	/**
	 * Makes new SVN Client Library instance
	 * 
	 * @return SVN Client Library instance
	 */
	ISVNConnector createConnector();

	/**
	 * Makes new SVN Client Library instance
	 * 
	 * @return SVN Client Library instance
	 */
	ISVNManager createManager();

	/**
	 * Returns unique SVN Client library plug-in id
	 * 
	 * @return SVN Client library plug-in id
	 */
	String getId();

	/**
	 * Returns user-friendly SVN Client library plug-in name
	 * 
	 * @return SVN Client library plug-in name
	 */
	String getName();

	/**
	 * Returns SVN Client library plug-in version
	 * 
	 * @return plug-in version
	 */
	String getVersion();

	/**
	 * Returns SVN Client library plug-in API compatibility version
	 * 
	 * @return plug-in version
	 */
	String getCompatibilityVersion();

	/**
	 * Returns SVN Client library version
	 * 
	 * @return connector version
	 */
	String getClientVersion();

	/**
	 * Returns supported optional features set
	 * 
	 * @return supported optional features set
	 */
	int getSupportedFeatures();

	/**
	 * Tell which SVN API version supported
	 * 
	 * @return API version Id
	 */
	int getSVNAPIVersion();

}
