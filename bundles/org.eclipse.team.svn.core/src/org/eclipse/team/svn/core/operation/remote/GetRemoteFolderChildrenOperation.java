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
 *    Julien HENRY - fix for the issue #350143 (svn:externals should not be treated as IRepositoryRoot entries)
 *    Claudio Weiler - special thanks for ideas and help related to implementation of human-readable/numeric ordering for branches and tags (see bug #390467)
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.core.operation.remote;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.SVNTeamPlugin;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.SVNEntryRevisionReference;
import org.eclipse.team.svn.core.connector.SVNProperty;
import org.eclipse.team.svn.core.connector.SVNProperty.BuiltIn;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.operation.UnreportableException;
import org.eclipse.team.svn.core.resource.IRepositoryContainer;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IRepositoryResource.Information;
import org.eclipse.team.svn.core.resource.IRepositoryRoot;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * 
 * Load folder children. Used in asynchronous repository view refresh.
 * 
 * @author Alexander Gurov
 */
public class GetRemoteFolderChildrenOperation extends AbstractActionOperation {
	protected IRepositoryContainer parent;

	protected boolean handleExternals;

	protected IRepositoryResource[] children;

	protected String[] extNames;

	protected boolean caseInsensitive;

	public GetRemoteFolderChildrenOperation(IRepositoryContainer parent, boolean handleExternals) {
		this(parent, handleExternals, false);
	}

	public GetRemoteFolderChildrenOperation(IRepositoryContainer parent, boolean handleExternals,
			boolean caseInsensitive) {
		super("Operation_GetRemoteChildren", SVNMessages.class); //$NON-NLS-1$
		this.parent = parent;
		this.handleExternals = handleExternals;
		this.caseInsensitive = caseInsensitive;
	}

	public IRepositoryResource[] getChildren() {
		return children;
	}

	public String getExternalsName(int idx) {
		return extNames[idx];
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		IRepositoryResource[] tmp = parent.getChildren();
		List<Object[]> tmpSortableData = new ArrayList<>(tmp.length);
		for (IRepositoryResource element : tmp) {
			tmpSortableData.add(new Object[] { null, element });
		}

		// handle svn:externals, if present:
		Information info = parent.getInfo();
		if (info != null && info.hasProperties && handleExternals) {
			IRepositoryLocation location = parent.getRepositoryLocation();
			ISVNConnector proxy = location.acquireSVNProxy();
			try {
				SVNProperty data = proxy.getProperty(SVNUtility.getEntryRevisionReference(parent), BuiltIn.EXTERNALS,
						null, new SVNProgressMonitor(this, monitor, null));
				if (data != null) {
					//Map externals;
					try {
						Map<String, SVNEntryRevisionReference> externals = SVNUtility
								.parseSVNExternalsProperty(data.value, parent);

						for (Entry<String, SVNEntryRevisionReference> entry : externals.entrySet()) {
							try {
								String name = entry.getKey();
								SVNEntryRevisionReference ref = entry.getValue();
								IRepositoryResource repositoryResourtce = SVNRemoteStorage.instance()
										.asRepositoryResource(location, ref,
												new SVNProgressMonitor(this, monitor, null));
								if (repositoryResourtce != null) {
									repositoryResourtce.setSelectedRevision(ref.revision);
									repositoryResourtce.setPegRevision(ref.pegRevision);
									tmpSortableData.add(new Object[] { name, repositoryResourtce });
								}
							} catch (Exception e) {
								this.reportStatus(new Status(IStatus.WARNING, SVNTeamPlugin.NATURE_ID, IStatus.OK,
										getShortErrorMessage(e), e));
							}
						}
					} catch (UnreportableException ex) {
						this.reportStatus(new Status(IStatus.WARNING, SVNTeamPlugin.NATURE_ID, IStatus.OK,
								getShortErrorMessage(ex), ex));
					}
				}
			} finally {
				location.releaseSVNProxy(proxy);
			}
		}
		Object[][] sortableData = tmpSortableData.toArray(new Object[tmpSortableData.size()][]);

		Arrays.sort(sortableData, new Comparator<Object[]>() {
			private NaturalComparator comparator = new NaturalComparator();

			@Override
			public int compare(Object[] firstArray, Object[] secondArray) {
				IRepositoryResource first = (IRepositoryResource) firstArray[1],
						second = (IRepositoryResource) secondArray[1];
				String firstExtName = (String) firstArray[0], secondExtName = (String) secondArray[0];
				boolean firstContainer = first instanceof IRepositoryContainer;
				boolean secondContainer = second instanceof IRepositoryContainer;
				if (firstContainer && secondContainer) {
					boolean firstExternal = firstExtName != null;
					boolean secondExternal = secondExtName != null;
					//Externals should not be considered as IRepositoryRoot (see Bug 350143) and be sorted by name
					boolean firstRoot = !firstExternal && first instanceof IRepositoryRoot;
					boolean secondRoot = !secondExternal && second instanceof IRepositoryRoot;
					return firstRoot == secondRoot
							? firstRoot
									? compareRoots(((IRepositoryRoot) first).getKind(),
											((IRepositoryRoot) second).getKind())
									: compareNames(first, firstExtName, second, secondExtName)
							: firstRoot ? -1 : 1;
				}
				return firstContainer == secondContainer
						? compareNames(first, firstExtName, second, secondExtName)
						: firstContainer ? -1 : 1;
			}

			private int compareNames(IRepositoryResource first, String firstExtName, IRepositoryResource second,
					String secondExtName) {
				String firstName = firstExtName != null ? firstExtName : first.getName();
				String secondName = secondExtName != null ? secondExtName : second.getName();
				return comparator.compare(firstName, secondName);
			}

			private int compareRoots(int firstKind, int secondKind) {
				return firstKind < secondKind ? -1 : 1;
			}
		});

		children = new IRepositoryResource[sortableData.length];
		extNames = new String[sortableData.length];
		for (int i = 0; i < sortableData.length; i++) {
			children[i] = (IRepositoryResource) sortableData[i][1];
			extNames[i] = (String) sortableData[i][0];
		}
	}

	private int compareStrings(String firstName, String secondName) {
		return caseInsensitive ? firstName.compareToIgnoreCase(secondName) : firstName.compareTo(secondName);
	}

	private class NaturalComparator implements Comparator<String> {
		@Override
		public int compare(String o1, String o2) {
			// equal string, ignore all the rest
			if (compareStrings(o1, o2) == 0) {
				return 0;
			}
			// empty string is lower
			if (o1.length() == 0) {
				return -1;
			}
			if (o2.length() == 0) {
				return 1;
			}

			// from now we consider a 'part' a sequence of digits or non-digits

			// extract first part of o1
			String o1Part = "";
			int o1i = 0;
			boolean o1IsDigit = Character.isDigit(o1.charAt(o1i++));
			for (; o1i < o1.length(); o1i++) {
				if (Character.isDigit(o1.charAt(o1i)) != o1IsDigit) {
					break;
				}
			}
			o1Part = o1.substring(0, o1i);

			// extract first part of o2
			String o2Part = "";
			int o2i = 0;
			boolean o2IsDigit = Character.isDigit(o2.charAt(o2i++));
			for (; o2i < o2.length(); o2i++) {
				if (Character.isDigit(o2.charAt(o2i)) != o2IsDigit) {
					break;
				}
			}
			o2Part = o2.substring(0, o2i);

			// compare both parts
			int result = 0;
			if (o1IsDigit && o2IsDigit) {
				if (o1Part.charAt(0) == '0' && o2Part.charAt(0) != '0') {
					result = -1;
				} else if (o2Part.charAt(0) == '0' && o1Part.charAt(0) != '0') {
					result = 1;
				} else {
					// if both parts are number, then numeric test
					try {
						int n1 = Integer.parseInt(o1Part);
						int n2 = Integer.parseInt(o2Part);
						// if numbers are equal, make numeric test, else use string test
						result = n1 != n2 ? n1 < n2 ? -1 : 1 : 0;
						if (result == 0 && o1Part.length() != o2Part.length()) {
							// leading zeros are relevant: more zeros first
							result = o1Part.length() < o2Part.length() ? 1 : -1;
						}
					} catch (NumberFormatException nbe) {
						// should not enter here as we test for numeric parts
					}
				}
			} else {
				result = compareStrings(o1Part, o2Part);
			}

			// if parts aren't equal return, otherwise continue test for the rest of the string
			return result != 0 ? result : compare(o1.substring(o1i), o2.substring(o2i));
		}
	}
}
