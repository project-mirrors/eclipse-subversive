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

package org.eclipse.team.svn.ui.debugmail;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.Properties;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.team.svn.core.extension.CoreExtensionsManager;
import org.eclipse.team.svn.core.extension.factory.ISVNConnectorFactory;
import org.eclipse.team.svn.ui.extension.factory.IReportingDescriptor;

/**
 * Mail reporter. Should implement only mail sending (non-UI part)
 * 
 * @author Alexander Gurov
 */
public class ReportPartsFactory {
	public interface IStatusVisitor {
		boolean visit(IStatus status);
	}

	public static boolean checkStatus(IStatus status, IStatusVisitor visitor) {
		if (!status.isMultiStatus()) {
			return visitor.visit(status);
		}
		IStatus[] children = status.getChildren();
		for (IStatus child : children) {
			if (ReportPartsFactory.checkStatus(child, visitor)) {
				return true;
			}
		}
		return false;
	}

	public static String getStackTrace(IStatus operationStatus) {
		final String[] stackTrace = { "" }; //$NON-NLS-1$
		ReportPartsFactory.checkStatus(operationStatus, status -> {
			String trace = ReportPartsFactory.getOutput(status);
			stackTrace[0] += trace + "\n"; //$NON-NLS-1$
			return false;
		});
		return stackTrace[0];
	}

	public static String getOutput(IStatus status) {
		Throwable t = status.getException();
		String message = ""; //$NON-NLS-1$
		if (t != null) {
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			PrintWriter writer = new PrintWriter(output);
			try (writer) {
				t.printStackTrace(writer);
			}
			message = output.toString();
		}
		return message;
	}

	public static String removeHTMLTags(String report) {
		report = report.replace("<b>", ""); //$NON-NLS-1$ //$NON-NLS-2$
		report = report.replace("</b> ", "\t"); //$NON-NLS-1$ //$NON-NLS-2$
		report = report.replace("</b>", ""); //$NON-NLS-1$ //$NON-NLS-2$
		report = report.replace("<i>", ""); //$NON-NLS-1$ //$NON-NLS-2$
		report = report.replace("</i>", ""); //$NON-NLS-1$ //$NON-NLS-2$
		report = report.replace("<br>", "\n"); //$NON-NLS-1$ //$NON-NLS-2$
		return report;
	}

	public static String getStatusPart(IStatus status) {
		String retVal = ""; //$NON-NLS-1$
		String[] stackTraces = ReportPartsFactory.getStackTrace(status).split("\n\n"); //$NON-NLS-1$
		for (String element : stackTraces) {
			int idx = element.indexOf('\n');
			if (idx == -1) {
				retVal += element + "<br><br>"; //$NON-NLS-1$
			} else {
				retVal += "<b>" + element.substring(0, idx) + "</b><br>" + element + "<br><br>"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
		}
		retVal += "<br>"; //$NON-NLS-1$
		return retVal;
	}

	public static String getReportIdPart(String id) {
		return "<b>" + id + "</b><br><br>"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	public static String getProductPart(IReportingDescriptor provider) {
		return "<b>Product:</b> " + provider.getProductName() + "<br><br>"; //$NON-NLS-1$//$NON-NLS-2$
	}

	public static String getVersionPart(IReportingDescriptor provider) {
		return "<b>Version:</b> " + provider.getProductVersion() + "<br><br>"; //$NON-NLS-1$//$NON-NLS-2$
	}

	public static String getAuthorPart(String email, String name) {
		String author = (name != null ? name : "") //$NON-NLS-1$
				+ (email != null && email.trim().length() > 0 ? " &lt;" + email + "&gt;" : ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		author = author.trim().length() > 0 ? author : "<i>[not specified]</i>"; //$NON-NLS-1$
		return "<b>From:</b> " + author + "<br><br>"; //$NON-NLS-1$//$NON-NLS-2$
	}

	public static String getUserCommentPart(String userComment) {
		userComment = userComment != null && userComment.trim().length() > 0 ? userComment : "<i>[empty]</i>"; //$NON-NLS-1$
		return "<b>User comment:</b><br>" + userComment + "<br><br>"; //$NON-NLS-1$//$NON-NLS-2$
	}

	public static String getSVNClientPart() {
		ISVNConnectorFactory factory = CoreExtensionsManager.instance().getSVNConnectorFactory();
		return "<b>SVN Client:</b> " + factory.getId() + " " + factory.getVersion() + " " + factory.getClientVersion() //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
				+ "<br><br>"; //$NON-NLS-1$
	}

	public static String getJVMPropertiesPart() {
		Properties systemProps = System.getProperties();
		Properties props = new Properties();
		String[] keys = { "os.name", //$NON-NLS-1$
				"os.version", //$NON-NLS-1$
				"os.arch", //$NON-NLS-1$
				"user.timezone", //$NON-NLS-1$
				"file.separator", //$NON-NLS-1$
				"line.separator", //$NON-NLS-1$
				"path.separator", //$NON-NLS-1$
				"file.encoding", //$NON-NLS-1$
				"user.language", //$NON-NLS-1$
				"user.country", //$NON-NLS-1$
				"java.version", //$NON-NLS-1$
				"java.runtime.version", //$NON-NLS-1$
				"java.class.version", //$NON-NLS-1$
				"java.vm.name", //$NON-NLS-1$
				"java.vm.info", //$NON-NLS-1$
				"java.vendor", //$NON-NLS-1$
				"java.runtime.name", //$NON-NLS-1$
				"osgi.framework.version", //$NON-NLS-1$
				"eclipse.commands" //$NON-NLS-1$
		};
		for (String key : keys) {
			if (systemProps.containsKey(key)) {
				props.put(key, systemProps.getProperty(key));
			}
		}
		return "<b>JVM Properties:</b><br>" + props.toString().replace('\n', ' ') + "<br><br>"; //$NON-NLS-1$//$NON-NLS-2$
	}

}
