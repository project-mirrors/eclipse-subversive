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

package org.eclipse.team.svn.ui.composite;

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.svn.core.connector.SVNEntryReference;
import org.eclipse.team.svn.core.resource.IRepositoryFile;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.utility.SVNUtility;
import org.eclipse.team.svn.ui.utility.UserInputHistory;
import org.eclipse.team.svn.ui.verifier.AbstractVerifier;
import org.eclipse.team.svn.ui.verifier.CompositeVerifier;
import org.eclipse.team.svn.ui.verifier.IValidationManager;

/**
 * 
 * Repository resource selection composite
 * 
 * In contrast to RepositoryResourceSelectionComposite it doesn't contain any revision controls
 * 
 * @author Igor Burilo
 *
 */
public class RepositoryResourceBaseSelectionComposite extends Composite {
	protected Combo urlText;

	protected Button browse;

	protected UserInputHistory urlHistory;

	protected IValidationManager validationManager;

	protected IRepositoryResource baseResource;

	protected String url;

	protected CompositeVerifier verifier;

	protected String selectionTitle;

	protected String selectionDescription;

	protected String comboId;

	protected boolean foldersOnly;

	public RepositoryResourceBaseSelectionComposite(Composite parent, int style, IValidationManager validationManager,
			String historyKey, String comboId, IRepositoryResource baseResource, String selectionTitle,
			String selectionDescription) {
		super(parent, style);
		urlHistory = new UserInputHistory(historyKey);
		this.validationManager = validationManager;
		this.baseResource = baseResource;
		this.selectionTitle = selectionTitle;
		this.selectionDescription = selectionDescription;
		this.comboId = comboId;
		foldersOnly = !(baseResource instanceof IRepositoryFile);
	}

	public final void setBaseResource(IRepositoryResource baseResource) {
		this.baseResource = baseResource;
		setBaseResourceImpl();
	}

	protected void setBaseResourceImpl() {
		urlText.setText(baseResource.getUrl());
	}

	public void setFoldersOnly(boolean foldersOnly) {
		this.foldersOnly = foldersOnly;
	}

	public boolean isSelectionAvailable() {
		return getDestination(SVNUtility.asEntryReference(url), true) != null;
	}

	public IRepositoryResource getSelectedResource() {
		return getDestination(SVNUtility.asEntryReference(url), false);
	}

	public void addVerifier(AbstractVerifier verifier) {
		this.verifier.add(verifier);
	}

	public void removeVerifier(AbstractVerifier verifier) {
		this.verifier.remove(verifier);
	}

	public void setUrl(String url) {
		urlText.setText(url);
	}

	public String getUrl() {
		return urlText.getText();
	}

	public void saveHistory() {
		urlHistory.addLine(urlText.getText());
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		urlText.setEnabled(enabled);
		browse.setEnabled(enabled);
	}

	protected IRepositoryResource getDestination(SVNEntryReference ref, boolean allowsNull) {
		if (ref == null) {
			if (baseResource == null) {
				if (allowsNull) {
					return null;
				}
				throw new IllegalArgumentException("SVN entry reference cannot be null.");
			}
			return SVNUtility.copyOf(baseResource);
		}
		String url = SVNUtility.normalizeURL(ref.path);
		try {
			IRepositoryResource base = baseResource;
			IRepositoryResource resource = null;
			if (base != null) {
				resource = foldersOnly
						? (IRepositoryResource) baseResource.asRepositoryContainer(url, false)
						: baseResource.asRepositoryFile(url, false);
			} else {
				SVNUtility.getSVNUrl(url); // validate an URL
				resource = SVNUtility.asRepositoryResource(url, foldersOnly);
			}
			if (ref.pegRevision != null) {
				resource.setPegRevision(ref.pegRevision);
			}
			return resource;
		} catch (Exception ex) {
			if (allowsNull) {
				return null;
			}
			if (baseResource == null) {
				throw new IllegalArgumentException("SVN entry reference must contain a valid value.");
			}
			return SVNUtility.copyOf(baseResource);
		}
	}
}
