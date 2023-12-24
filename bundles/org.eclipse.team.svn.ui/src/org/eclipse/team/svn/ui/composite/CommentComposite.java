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
 *    Alessandro Nistico - [patch] Change Set's implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.composite;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.SpellcheckedTextProvider;
import org.eclipse.team.svn.ui.extension.ExtensionsManager;
import org.eclipse.team.svn.ui.extension.factory.ICommentManager;
import org.eclipse.team.svn.ui.panel.IDialogManager;
import org.eclipse.team.svn.ui.preferences.SVNTeamPreferences;
import org.eclipse.team.svn.ui.properties.bugtraq.BugtraqModel;
import org.eclipse.team.svn.ui.utility.UserInputHistory;
import org.eclipse.team.svn.ui.verifier.AbstractVerifier;
import org.eclipse.team.svn.ui.verifier.CommentVerifier;
import org.eclipse.team.svn.ui.verifier.CompositeVerifier;
import org.eclipse.team.svn.ui.verifier.IValidationManager;

/**
 * Operation comment composite
 * 
 * @author Alexander Gurov
 */
public class CommentComposite extends Composite implements ICommentManager {
	public static String TEMPORARY_COMMENT = null;

	protected static final String COMMENT_HISTORY_NAME = "comment"; //$NON-NLS-1$

	protected StyledText text;

	protected Text bugIdText;

	protected String message;

	protected String bugID;

	protected UserInputHistory history;

	protected Set logTemplates;

	protected Set ignoredStrings;

	protected BugtraqModel bugtraqModel;

	protected int minLogSize;

	protected int maxLogWidth;

	protected IValidationManager validationManager;

	protected IDialogManager dialogManager;

	protected Map<String, String> sections = new LinkedHashMap<>();

	protected Map<String, List<String>> sectionComments = new LinkedHashMap<>();

	public CommentComposite(Composite parent, IValidationManager validationManager) {
		this(parent, validationManager, null);
	}

	public CommentComposite(Composite parent, IValidationManager validationManager, Set logTemplates) {
		this(parent, null, validationManager, logTemplates, null);
	}

	public CommentComposite(Composite parent, String message, IValidationManager validationManager, Set logTemplates,
			BugtraqModel bugtraqModel) {
		this(parent, message, validationManager, logTemplates, null, 0);
	}

	public CommentComposite(Composite parent, String message, IValidationManager validationManager, Set logTemplates,
			BugtraqModel bugtraqModel, int minLogSize) {
		this(parent, message, validationManager, logTemplates, null, minLogSize, 0);
	}

	public CommentComposite(Composite parent, String message, IValidationManager validationManager, Set logTemplates,
			BugtraqModel bugtraqModel, int minLogSize, int maxLogWidth) {
		super(parent, SWT.NONE);

		this.message = message;
		this.validationManager = validationManager;
		this.logTemplates = logTemplates;
		ignoredStrings = new HashSet();
		this.bugtraqModel = bugtraqModel;
		this.minLogSize = minLogSize;
		this.maxLogWidth = maxLogWidth;
		createControls();
	}

	@Override
	public String getTemporarySavedComment() {
		return CommentComposite.TEMPORARY_COMMENT;
	}

	@Override
	public String getMessage() {
		return message;
	}

	@Override
	public void setMessage(String message) {
		this.message = message;
		if (text != null) {
			text.setText(message);
		}
	}

	public String getBugID() {
		return bugID;
	}

	public void insertText(String text) {
		this.text.insert(text);
	}

	public void saveChanges() {
		message = text.getText();
		history.addLine(message);
		CommentComposite.TEMPORARY_COMMENT = null;

		if (bugIdText != null) {
			bugID = bugIdText.getText();
		}
		ExtensionsManager.getInstance().getCurrentCommitFactory().confirmMessage(this);
	}

	public void cancelChanges() {
		CommentComposite.TEMPORARY_COMMENT = text.getText().trim().length() == 0 ? null : text.getText();
		ExtensionsManager.getInstance().getCurrentCommitFactory().cancelMessage(this);
	}

	private void createControls() {
		GridData data = null;

		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		setLayout(layout);

		final Text[] tBugIdTextA = new Text[1];
		if (bugtraqModel != null && (bugtraqModel.getMessage() != null || bugtraqModel.getLogregex() != null)) {
			Composite bugtraqComposite = new Composite(this, SWT.NONE);
			layout = new GridLayout();
			layout.numColumns = 2;
			layout.marginHeight = layout.marginWidth = 0;
			bugtraqComposite.setLayout(layout);

			bugtraqComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			Label label = new Label(bugtraqComposite, SWT.NONE);
			label.setLayoutData(new GridData(GridData.BEGINNING));
			label.setText(bugtraqModel.getLabel());

			if (bugtraqModel.getLogregex() == null) {
				bugIdText = new Text(bugtraqComposite, SWT.FILL | SWT.BORDER);
				bugIdText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				bugIdText.setFocus();

				validationManager.attachTo(bugIdText, new AbstractVerifier() {
					@Override
					protected String getErrorMessage(Control input) {
						String bugId = getText(input);
						if (bugId.length() > 0 && bugtraqModel.isNumber()
								&& !bugId.matches("[0-9]+(\\s*,\\s*?[0-9]+)*")) {
							return BaseMessages.format(SVNUIMessages.CommentComposite_BugID_Verifier_Error_Number,
									new String[] { bugtraqModel.getLabel() });
						}
						return null;
					}

					@Override
					protected String getWarningMessage(Control input) {
						if (bugtraqModel.isWarnIfNoIssue() && getText(input).length() == 0) {
							return SVNUIMessages.CommentComposite_BugID_Verifier_Warning;
						}
						return null;
					}
				});
			} else {
				final Text tBugIdText = new Text(bugtraqComposite, SWT.FILL | SWT.BORDER | SWT.READ_ONLY);
				tBugIdText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				tBugIdTextA[0] = tBugIdText;
			}
		}
		data = new GridData(GridData.FILL_BOTH);
		data.heightHint = 80;
		text = SpellcheckedTextProvider.getTextWidget(this, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.WRAP, data,
				maxLogWidth);
		text.addTraverseListener(e -> {
			if (!SVNTeamPreferences.getCommentTemplatesBoolean(SVNTeamUIPlugin.instance().getPreferenceStore(),
					SVNTeamPreferences.COMMENT_USE_SHIFT_ENTER_NAME) && (e.stateMask & SWT.SHIFT) != 0
					&& e.detail == SWT.TRAVERSE_RETURN) {
				e.doit = false;
			} else if (e.character == SWT.TAB) {
				// no TABs are accepted as a text part
				e.doit = true;
			}
		});
		text.addModifyListener(e -> message = text.getText());
		CompositeVerifier verifier = new CompositeVerifier();
		verifier.add(new CommentVerifier(SVNUIMessages.CommentComposite_Comment_Verifier, minLogSize));
		if (bugtraqModel != null && bugtraqModel.getLogregex() != null) {
			text.setFocus();
			String[] logregex = bugtraqModel.getLogregex();
			final Pattern mainRegex = Pattern.compile(logregex[0]);
			final Pattern numberRegex = logregex.length > 1
					? Pattern.compile(logregex[1])
					: bugtraqModel.isNumber() ? Pattern.compile("[0-9]+(\\s*,\\s*?[0-9]+)*") : null;
			verifier.add(new AbstractVerifier() {
				@Override
				protected String getErrorMessage(Control input) {
					return null;
				}

				@Override
				protected String getWarningMessage(Control input) {
					if (bugtraqModel.isWarnIfNoIssue()) {
						String text = getText(input);
						Matcher matcher = mainRegex.matcher(text);
						if (matcher.find()) {
							String bugIdEntry = matcher.group();
							if (numberRegex != null) {
								matcher = numberRegex.matcher(bugIdEntry);
								String entryList = null;
								while (matcher.find()) {
									entryList = entryList == null
											? matcher.group()
											: entryList + ", " + matcher.group();
								}
								if (entryList != null) {
									tBugIdTextA[0].setText(entryList);
									return null;
								}
								tBugIdTextA[0].setText("");
								return BaseMessages.format(SVNUIMessages.CommentComposite_BugID_Verifier_Error_Text,
										new String[] { bugtraqModel.getLabel(), numberRegex.pattern() });
							}
							tBugIdTextA[0].setText(bugIdEntry);
							return null;
						}
						tBugIdTextA[0].setText("");
						return SVNUIMessages.CommentComposite_BugID_Verifier_Warning;
					}
					return null;
				}
			});
		}
		validationManager.attachTo(text, verifier);

		Label label = new Label(this, SWT.NULL);
		label.setLayoutData(new GridData());
		label.setText(SVNUIMessages.CommentComposite_ChooseComment);

		history = new UserInputHistory(CommentComposite.COMMENT_HISTORY_NAME, SVNTeamPreferences.getCommentTemplatesInt(
				SVNTeamUIPlugin.instance().getPreferenceStore(), SVNTeamPreferences.COMMENT_SAVED_COMMENTS_COUNT_NAME));

		final Combo previousCommentsCombo = new Combo(this, SWT.READ_ONLY);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		previousCommentsCombo.setLayoutData(data);

		String logTemplateMessage = initCommentsMap();
		final List commentsList = getCommentsList();
		if (message != null && message.length() > 0) {
			text.setText(message);
		} else if (CommentComposite.TEMPORARY_COMMENT != null) {
			text.setText(CommentComposite.TEMPORARY_COMMENT);
		} else if (logTemplateMessage != null) {
			text.setText(logTemplateMessage);
		}
		text.selectAll();
		List flattenCommentsList = new ArrayList();
		for (Iterator iter = commentsList.iterator(); iter.hasNext();) {
			flattenCommentsList.add(FileUtility.flattenText((String) iter.next()));
		}
		previousCommentsCombo.setVisibleItemCount(flattenCommentsList.size());
		previousCommentsCombo.setItems((String[]) flattenCommentsList.toArray(new String[flattenCommentsList.size()]));
		previousCommentsCombo.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int idx = previousCommentsCombo.getSelectionIndex();
				if (idx != -1) {
					String comboText = (String) commentsList.get(idx);
					text.setText(ignoredStrings.contains(comboText) ? text.getText() : comboText);
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
	}

	public void postInit(IDialogManager dialogManager) {
		this.dialogManager = dialogManager;
		text.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent event) {
				if (event.keyCode == SWT.TAB) {
					text.traverse(SWT.TRAVERSE_TAB_NEXT);
					event.doit = false;
				}
			}
		});
		if (minLogSize > 0) {
			validationManager.validateContent();
		}
	}

	protected String initCommentsMap() {
		String retVal = null;
		addCommentsSection(ICommentManager.PREVIOUS_COMMENTS_HEADER,
				"    " + SVNUIMessages.CommentComposite_Previous_Hint); //$NON-NLS-1$
		addCommentsToSection(ICommentManager.PREVIOUS_COMMENTS_HEADER, Arrays.asList(history.getHistory()));

		IPreferenceStore store = SVNTeamUIPlugin.instance().getPreferenceStore();
		boolean logTemplatesEnabled = SVNTeamPreferences.getCommentTemplatesBoolean(store,
				SVNTeamPreferences.COMMENT_LOG_TEMPLATES_ENABLED_NAME);
		boolean userTemplatesEnabled = SVNTeamPreferences.getCommentTemplatesBoolean(store,
				SVNTeamPreferences.COMMENT_TEMPLATES_LIST_ENABLED_NAME);

		if (userTemplatesEnabled) {
			addCommentsSection(ICommentManager.TEMPLATE_HEADER, "    " + SVNUIMessages.CommentComposite_Template_Hint); //$NON-NLS-1$
			String[] templates = FileUtility.decodeStringToArray(SVNTeamPreferences.getCommentTemplatesString(store,
					SVNTeamPreferences.COMMENT_TEMPLATES_LIST_NAME));
			addCommentsToSection(ICommentManager.TEMPLATE_HEADER, Arrays.asList(templates));
		}

		if (logTemplates != null && logTemplatesEnabled) {
			addCommentsSection(ICommentManager.TSVN_LOGTEMPLATE_HEADER,
					"    " + SVNUIMessages.CommentComposite_LogTemplate_Hint); //$NON-NLS-1$
			addCommentsToSection(ICommentManager.TSVN_LOGTEMPLATE_HEADER, logTemplates);
			if (logTemplates.size() > 0) {
				retVal = (String) logTemplates.iterator().next();
			}
		}
		ExtensionsManager.getInstance().getCurrentCommitFactory().initCommentManager(this);
		return retVal;
	}

	@Override
	public void addCommentsSection(String sectionHeader, String sectionHint) {
		sectionHeader = SVNUIMessages.getString(sectionHeader);
		sections.put(sectionHeader, sectionHint);
		sectionComments.put(sectionHeader, new LinkedList<>());
	}

	@Override
	public void addCommentsToSection(String sectionHeader, Collection<String> templates) {
		sectionHeader = SVNUIMessages.getString(sectionHeader);
		if (sectionComments.containsKey(sectionHeader)) {
			sectionComments.get(sectionHeader).addAll(templates);
		}
	}

	protected List getCommentsList() {
		List commentsList = new ArrayList();

		for (Map.Entry<String, String> entry : sections.entrySet()) {
			List<String> comments = sectionComments.get(entry.getKey());
			if (sections.size() > 1) {
				commentsList.add(entry.getKey());
				ignoredStrings.add(entry.getKey());
				if (comments.size() == 0) {
					commentsList.add(entry.getValue());
					ignoredStrings.add(entry.getValue());
				}
				commentsList.addAll(comments);
			}
		}

		return commentsList;
	}

}
