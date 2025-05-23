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

package org.eclipse.team.svn.ui.operation;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;

/**
 * Copy file content into clipboard
 * 
 * @author Alexander Gurov
 */
public class FileToClipboardOperation extends AbstractActionOperation {
	protected String fileName;

	protected boolean deleteFile;

	protected String charset;

	public FileToClipboardOperation(String fileName) {
		this(fileName, "UTF-8");
	}

	public FileToClipboardOperation(String fileName, String charset) {
		this(fileName, charset, true);
	}

	public FileToClipboardOperation(String fileName, String charset, boolean deleteFile) {
		super("Operation_FileToClipboard", SVNUIMessages.class); //$NON-NLS-1$
		this.fileName = fileName;
		this.deleteFile = deleteFile;
		this.charset = charset;
	}

	@Override
	public int getOperationWeight() {
		return 0;
	}

	@Override
	protected void runImpl(final IProgressMonitor monitor) throws Exception {
		ByteArrayOutputStream data = new ByteArrayOutputStream();
		File tempFile = new File(fileName);
		FileInputStream stream = new FileInputStream(tempFile);
		try {
			byte[] buf = new byte[2048];
			int len = 0;
			while ((len = stream.read(buf)) > 0) {
				data.write(buf, 0, len);
			}
		} finally {
			try {
				stream.close();
			} catch (Exception ex) {
			}
		}
		if (deleteFile) {
			tempFile.delete();
		}
		final String text = data.toString(charset);
		if (data.size() > 0) {
			final Display display = UIMonitorUtility.getDisplay();
			display.syncExec(() -> {
				TextTransfer plainTextTransfer = TextTransfer.getInstance();
				Clipboard clipboard = new Clipboard(display);
				clipboard.setContents(new String[] { text }, new Transfer[] { plainTextTransfer });
				clipboard.dispose();
			});
		}
	}

}
