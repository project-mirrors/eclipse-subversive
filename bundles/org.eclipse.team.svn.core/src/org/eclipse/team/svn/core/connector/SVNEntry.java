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

package org.eclipse.team.svn.core.connector;

/**
 * Directory entry information container
 * 
 * The JavaHL API's is the only way to interact between SVN and Java-based tools. At the same time JavaHL connector library is not EPL
 * compatible and we won't to pin plug-in with concrete connector implementation. So, the only way to do this is providing our own connector
 * interface which will be covered by concrete connector implementation.
 * 
 * @author Alexander Gurov
 */
public class SVNEntry {
	/**
	 * Entry node kind enumeration
	 */
	public enum Kind {
		/**
		 * The entry is absent.
		 */
		NONE(0),
		/**
		 * The entry is a file
		 */
		FILE(1),
		/**
		 * The entry is a directory
		 */
		DIR(2),
		/**
		 * The entry kind is unknown
		 */
		UNKNOWN(3),
		/**
		 * The entry kind is a symbolic link
		 * 
		 * @since 1.8
		 */
		SYMLINK(4);

		public final int id;

		public static Kind fromId(int id) {
			for (Kind kind : values()) {
				if (kind.id == id) {
					return kind;
				}
			}
			throw new IllegalArgumentException("Invalid entry kind: " + id); //$NON-NLS-1$
		}

		Kind(int id) {
			this.id = id;
		}
	}

	/**
	 * The (@link DirEntry} fields acquisition masks. Combined mask could be passed into the {@link ISVNConnector#list} method.
	 */
	public static class Fields {

		/**
		 * Specify that no fields will be requested.
		 */
		public static final int NONE = 0;

		/**
		 * Specify that all fields will be requested.
		 */
		public static final int ALL = ~NONE;

		/**
		 * Specify that the author field will be requested.
		 */
		public static final int AUTHOR = 0x00020;

		/**
		 * Specify that the date field will be requested.
		 */
		public static final int DATE = 0x00010;

		/**
		 * Specify that the revision field will be requested.
		 */
		public static final int REVISION = 0x00008;

		/**
		 * Specify that the hasProperties field will be requested.
		 */
		public static final int HAS_PROPERTIES = 0x00004;

		/**
		 * Specify that the size field will be requested.
		 */
		public static final int SIZE = 0x00002;

		/**
		 * Specify that the nodeKind field will be requested.
		 */
		public static final int NODE_KIND = 0x00001;

	}

	/**
	 * The path to the directory entry. The entry path is reported relatively to directory path.
	 */
	public final String path;

	/**
	 * The last change revision. Could be {@link SVNRevision#INVALID_REVISION_NUMBER} if access mask does not specify the revision field.
	 */
	public final long revision;

	/**
	 * The last change date. Could be 0 if access mask does not specify the date field.
	 */
	public final long date;

	/**
	 * The last change author. Could be <code>null</code> if the last commit is performed with anonymous access or if the access mask does
	 * not specify the author field.
	 */
	public final String author;

	/**
	 * True if the entry has properties. Undetermined if access mask does not specify the hasProperties field.
	 */
	public final boolean hasProperties;

	/**
	 * {@link Kind#DIR} for directories or {@link Kind#FILE} for file. Undetermined if access mask does not specify the nodeKind field.
	 */
	public final Kind nodeKind;

	/**
	 * Size in bytes. Valid only for files, for directories always zero. Undetermined if access mask does not specify the size field.
	 */
	public final long size;

	/**
	 * Information about the entry lock. Could be <code>null</code> if entry has no lock.
	 */
	public final SVNLock lock;

	/**
	 * The {@link SVNEntry} instance could be initialized only once because all fields are final
	 * 
	 * @param path
	 *            the entry path
	 * @param revision
	 *            the last change revision
	 * @param date
	 *            the last change date
	 * @param author
	 *            the last change author
	 * @param hasProperties
	 *            specify if entry has properties
	 * @param nodeKind
	 *            the entry node kind
	 * @param size
	 *            file size in bytes
	 * @param lock
	 *            entry lock information
	 */
	public SVNEntry(String path, long revision, long date, String author, boolean hasProperties, Kind nodeKind,
			long size, SVNLock lock) {
		this.date = date;
		this.revision = revision;
		this.hasProperties = hasProperties;
		this.author = author;
		this.nodeKind = nodeKind;
		this.size = size;
		this.path = path;
		this.lock = lock;
	}

	@Override
	public String toString() {
		return path;
	}

}
