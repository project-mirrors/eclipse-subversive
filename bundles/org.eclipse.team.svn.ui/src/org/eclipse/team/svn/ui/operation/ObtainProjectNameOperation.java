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
 *    Sergiy Logvin - Initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.operation;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.connector.SVNConnectorException;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.remote.GetFileContentOperation;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IRepositoryResourceProvider;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.ProgressMonitorUtility;
import org.eclipse.team.svn.core.utility.SVNUtility;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.preferences.SVNTeamPreferences;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * Obtain project names from .project files operation
 *
 * @author Sergiy Logvin
 */
public class ObtainProjectNameOperation extends AbstractActionOperation {

	protected IRepositoryResourceProvider resourceProvider;

	protected IRepositoryResource[] resources;

	protected HashMap<String, IRepositoryResource> names2Resources;

	public ObtainProjectNameOperation(IRepositoryResource[] resources) {
		super("Operation_ObtainProjectName", SVNUIMessages.class); //$NON-NLS-1$
		this.resources = resources;
		names2Resources = new HashMap<>();
	}

	public ObtainProjectNameOperation(IRepositoryResourceProvider resourceProvider) {
		this((IRepositoryResource[]) null);
		this.resourceProvider = resourceProvider;
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		IPreferenceStore store = SVNTeamUIPlugin.instance().getPreferenceStore();
		final boolean doObtainFromDotProject = SVNTeamPreferences.getCheckoutBoolean(store,
				SVNTeamPreferences.CHECKOUT_USE_DOT_PROJECT_NAME);
		final Set<String> lowerCaseNames = new HashSet<>();
		final boolean caseInsensitiveOS = FileUtility.isCaseInsensitiveOS();
		if (resourceProvider != null) {
			resources = resourceProvider.getRepositoryResources();
		}
		for (int i = 0; i < resources.length && !monitor.isCanceled(); i++) {
			ProgressMonitorUtility.setTaskInfo(monitor, this, BaseMessages.format(getOperationResource("Scanning"), //$NON-NLS-1$
					new Object[] { resources[i].getName() }));

			final int j = i;
			this.protectStep(monitor1 -> {
				String name = null;
				String newName = null;
				try {
					name = doObtainFromDotProject
							? ObtainProjectNameOperation.this.obtainProjectName(resources[j], monitor1)
							: null;
				} catch (SVNConnectorException ex) {
					// do nothing
				}
				if (name == null) {
					IRepositoryResource resource = resources[j];
					IRepositoryLocation location = resource.getRepositoryLocation();
					if (location.isStructureEnabled() && location.getTrunkLocation().equals(resource.getName())) {
						if (resource.getParent() != null) {
							name = resource.getParent().getName();
						} else {
							name = SVNUtility.createPathForSVNUrl(location.getUrl()).lastSegment();
						}
						name = FileUtility.formatResourceName(name);
					} else {
						name = resource.getName();
					}
				}
				if (!names2Resources.containsKey(name)
						&& (caseInsensitiveOS ? !lowerCaseNames.contains(name.toLowerCase()) : true)) {
					names2Resources.put(name, resources[j]);
					if (caseInsensitiveOS) {
						lowerCaseNames.add(name.toLowerCase());
					}
				} else {
					for (int k = 1;; k++) {
						newName = name + " (" + k + ")"; //$NON-NLS-1$ //$NON-NLS-2$
						if (!names2Resources.containsKey(newName)
								&& (caseInsensitiveOS ? !lowerCaseNames.contains(newName.toLowerCase()) : true)) {
							names2Resources.put(newName, resources[j]);
							if (caseInsensitiveOS) {
								lowerCaseNames.add(newName.toLowerCase());
							}
							break;
						}
					}
				}

			}, monitor, resources.length);

		}
	}

	protected String obtainProjectName(IRepositoryResource resource, IProgressMonitor monitor) throws Exception {
		String projFileUrl = resource.getUrl() + "/.project"; //$NON-NLS-1$
		IRepositoryResource projFile = resource.getRepositoryLocation().asRepositoryFile(projFileUrl, false);
		if (projFile.exists()) {
			GetFileContentOperation op = new GetFileContentOperation(projFile);
			ProgressMonitorUtility.doTaskExternal(op, monitor);
			InputStream is = op.getContent();
			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new InputStreamReader(is, "UTF-8")); //$NON-NLS-1$

				NameReceiver receiver = new NameReceiver();
				XMLReader parser = XMLReaderFactory.createXMLReader();
				parser.setContentHandler(receiver);
				parser.parse(new InputSource(reader));
				return receiver.getName();
			} catch (Exception ex) {
				// do nothing, try reading plainly
			} finally {
				if (reader != null) {
					try {
						reader.close();
					} catch (Exception ex) {
					}
				}
				try {
					is.close();
				} catch (Exception ex) {
				}
			}
			is = op.getContent();
			try {
				reader = new BufferedReader(new InputStreamReader(is, "UTF-8")); //$NON-NLS-1$

				String currentString;
				int first;
				int last;
				while ((currentString = reader.readLine()) != null) {
					if ((first = currentString.indexOf("<name>")) >= 0 && //$NON-NLS-1$
							(last = currentString.indexOf("</name>")) >= 0) { //$NON-NLS-1$
						String name = currentString.substring(first + "<name>".length(), last); //$NON-NLS-1$
						return name.length() > 0 ? name : null;
					}
				}
			} finally {
				if (reader != null) {
					try {
						reader.close();
					} catch (Exception ex) {
					}
				}
				try {
					is.close();
				} catch (Exception ex) {
				}
			}
		}

		return null;
	}

	public HashMap<String, IRepositoryResource> getNames2Resources() {
		return names2Resources;
	}

	private static class NameReceiver implements ContentHandler {
		private int state = 0;

		private String name = "";//$NON-NLS-1$

		private int level = 0;

		public String getName() {
			return name;
		}

		@Override
		public void setDocumentLocator(Locator locator) {
		}

		@Override
		public void startDocument() throws SAXException {
		}

		@Override
		public void endDocument() throws SAXException {
		}

		@Override
		public void startPrefixMapping(String prefix, String uri) throws SAXException {
		}

		@Override
		public void endPrefixMapping(String prefix) throws SAXException {
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
			level++;
			if (localName.equals("name") && level == 2) {//$NON-NLS-1$
				state = 1;
			}
		}

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			if (localName.equals("name") && level == 2) {//$NON-NLS-1$
				state = 0;
			}
			level--;
		}

		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			if (state == 1) {
				name += new String(ch, start, length);
			}
		}

		@Override
		public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
		}

		@Override
		public void processingInstruction(String target, String data) throws SAXException {
		}

		@Override
		public void skippedEntity(String name) throws SAXException {
		}
	}
}
