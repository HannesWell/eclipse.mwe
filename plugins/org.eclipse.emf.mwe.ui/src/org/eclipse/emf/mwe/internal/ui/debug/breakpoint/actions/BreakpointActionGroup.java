/*******************************************************************************
 * Copyright (c) 2007 committers of openArchitectureWare and others.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     committers of openArchitectureWare - initial API and implementation
 *******************************************************************************/
package org.eclipse.emf.mwe.internal.ui.debug.breakpoint.actions;

import java.lang.reflect.Method;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.editors.text.TextEditor;

/**
 * Action group with 2 actions: "Toggle Breakpoints" and
 * "Enable/Disable Breakpoints".<br>
 * Despite of usual breakpoint actions these actions can be used not only for
 * vertical ruler context menu (incl. double click), but also for editor context
 * menu. That way "in line" breakpoints can be handled.
 * 
 */
public class BreakpointActionGroup extends ActionGroup {

	private EnableDisableBreakpointAction enableAction;

	private ToggleBreakpointAction toggleAction;

	private int lastSelectedLine;

	private int lastSelectedOffset;

	private IVerticalRuler verticalRuler;

	private boolean rulerSelected;

	private StyledText textWidget;

	// -------------------------------------------------------------------------

	public BreakpointActionGroup(final TextEditor editor) {
		Assert.isNotNull(editor);

		// Note: We don't want to define a new "IOurOwnTextEditor" interface, so
		// we do it via Reflection
		Object obj = getterMethod("getSourceViewer", editor);
		if (obj == null)
			return;
		textWidget = ((ISourceViewer) obj).getTextWidget();

		obj = getterMethod("getVerticalRuler", editor);
		if (obj == null)
			return;
		verticalRuler = (IVerticalRuler) obj;

		enableAction = new EnableDisableBreakpointAction(editor, this);
		toggleAction = new ToggleBreakpointAction(editor, this);

		// set lastSelectedLine if RightMouseClick on text
		textWidget.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(final MouseEvent e) {
				if (e.button == 3) {
					updateLastSelectedOffset(e.x, e.y);
				}
			}

		});

		// set lastSelectedLine if RightMouseClick or DoubleClick on vertical
		// ruler
		verticalRuler.getControl().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(final MouseEvent e) {
				if (e.button == 3) {
					updateLastSelectedLine(e.y);
				}

			}

			@Override
			public void mouseDoubleClick(final MouseEvent e) {
				updateLastSelectedLine(e.y);
				// see note below for why we call it here
				toggleAction.run();
			}

		});
	}

	private void updateLastSelectedLine(final int y) {
		// Note: we use our own "lastSelectedLine mechanism" (not the ruler's
		// one) because of sequencing problems
		// our action mouseHandler would be called after the ruler mouseHandler,
		// so for doubleClick the
		// lastSelectedLine value may not be correct
		// (see AbstractTextEditor.createPartCOntrol(): createVerticalRuler() is
		// before createActions()
		rulerSelected = true;
		final int oldLine = lastSelectedLine;
		final int oldOffset = lastSelectedOffset;
		try {
			lastSelectedLine = verticalRuler.toDocumentLineNumber(y);
			lastSelectedOffset = textWidget
					.getOffsetAtPoint(new Point(textWidget.getLeftMargin(), y));
		} catch (IllegalArgumentException e) {
			// Restore original values
			lastSelectedLine = oldLine;
			lastSelectedOffset = oldOffset;
		}
	}

	private void updateLastSelectedOffset(final int x, final int y) {
		rulerSelected = false;
		final int oldLine = lastSelectedLine;
		try {
			lastSelectedLine = verticalRuler.toDocumentLineNumber(y);
			try {
				lastSelectedOffset = textWidget.getOffsetAtPoint(new Point(x, y));
			}
			catch (Exception e) {
				try {
					// If we got the offset, move to the end of the line.
					lastSelectedOffset = textWidget.getOffsetAtPoint(new Point(textWidget.getLeftMargin(), y));
					int lineIndex = textWidget.getLineAtOffset(lastSelectedOffset);
					lastSelectedOffset += textWidget.getLine(lineIndex).length();
				}
				catch (Exception e2) {
					// Otherwise, create an offset to the end of the entire text.
					lastSelectedOffset = textWidget.getText().length();
				}
			}
		} catch(Exception e) {
			lastSelectedLine = oldLine;
		}
	}

	// -------------------------------------------------------------------------

	public boolean isRulerSelected() {
		return rulerSelected;
	}

	public int getLastSelectedLine() {
		return lastSelectedLine;
	}

	public int getLastSelectedOffset() {
		return lastSelectedOffset;
	}

	public int getOffsetAtLine(final int line) {
		return textWidget.getOffsetAtLine(line);
	}

	// -------------------------------------------------------------------------

	@Override
	public void fillContextMenu(final IMenuManager manager) {
		toggleAction.updateText();
		manager.appendToGroup("mwe", toggleAction);
		enableAction.updateText();
		manager.appendToGroup("mwe", enableAction);
	}

	@Override
	public void dispose() {
		enableAction = null;
		toggleAction = null;
		super.dispose();
	}

	private Object getterMethod(final String name, final Object element) {
		try {
			Method m = findMethod(name, element.getClass());
			if (m != null) {
				m.setAccessible(true);
				return m.invoke(element, new Object[] {});
			}
		}
		catch (Exception e) {
			System.out.println("error");
		}
		return null;
	}

	private Method findMethod(final String name, final Class<?> clazz) {
		if (!Object.class.equals(clazz)) {
			Method[] methods = clazz.getDeclaredMethods();
			for (Method method : methods) {
				if (method.getName().equals(name))
					return method;
			}
			return findMethod(name, clazz.getSuperclass());
		}
		return null;
	}

}
