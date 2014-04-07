package org.biomart.runner.view.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.tree.TreeCellRenderer;

import org.biomart.configurator.view.gui.dialogs.MartRunnerMonitorDialog;
import org.biomart.runner.model.JobStatus;

// Renders cells nicely.
public class JobPlanTreeCellRenderer implements TreeCellRenderer {
	private static final long serialVersionUID = 1L;

	public Component getTreeCellRendererComponent(final JTree tree,
			final Object value, final boolean sel, final boolean expanded,
			final boolean leaf, final int row, final boolean hasFocus) {
		final JLabel label = new JLabel(value.toString());
		label.setOpaque(true);
		Color fgColor = Color.BLACK;
		Color bgColor = Color.WHITE;
		Font font = MartRunnerMonitorDialog.PLAIN_FONT;
		// Sections are given text labels.
		if (value instanceof SectionNode) {
			final JobStatus status = ((SectionNode) value).getSection()
					.getStatus();
			// White/Cyan stripes.
			bgColor = row % 2 == 0 ? Color.WHITE
					: MartRunnerMonitorDialog.PALE_BLUE;
			// Color-code text.
			fgColor = (Color) MartRunnerMonitorDialog.STATUS_COLOR_MAP
					.get(status);
			// Set font.
			font = (Font) MartRunnerMonitorDialog.STATUS_FONT_MAP
					.get(status);
		}
		// Actions are given text labels.
		else if (value instanceof ActionNode) {
			final JobStatus status = ((ActionNode) value).getAction()
					.getStatus();
			// White/Cyan stripes.
			bgColor = row % 2 == 0 ? Color.WHITE
					: MartRunnerMonitorDialog.PALE_GREEN;
			// Color-code text.
			fgColor = (Color) MartRunnerMonitorDialog.STATUS_COLOR_MAP
					.get(status);
			// Set font.
			font = (Font) MartRunnerMonitorDialog.STATUS_FONT_MAP
					.get(status);
		}
		// Always white-on-color or color-on-white.
		label.setFont(font);
		label.setForeground(sel ? bgColor : fgColor);
		label.setBackground(sel ? fgColor : bgColor);
		// Everything else is default.
		return label;
	}
}
