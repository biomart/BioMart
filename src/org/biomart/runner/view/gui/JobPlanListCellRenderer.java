package org.biomart.runner.view.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import org.biomart.configurator.view.gui.dialogs.MartRunnerMonitorDialog;
import org.biomart.runner.model.JobPlan;
import org.biomart.runner.model.JobStatus;

// Renders cells nicely.
public class JobPlanListCellRenderer implements ListCellRenderer {
	private static final long serialVersionUID = 1L;

	public Component getListCellRendererComponent(final JList list,
			final Object value, final int index, final boolean isSelected,
			final boolean cellHasFocus) {
		final JLabel label = new JLabel(value.toString());
		label.setOpaque(true);
		Color fgColor = Color.BLACK;
		Color bgColor = Color.WHITE;
		Font font = MartRunnerMonitorDialog.PLAIN_FONT;
		// A Job Plan entry node?
		if (value instanceof JobPlan) {
			final JobStatus status = ((JobPlan) value).getRoot()
					.getStatus();
			// White/Cyan stripes.
			bgColor = index % 2 == 0 ? Color.WHITE
					: MartRunnerMonitorDialog.PALE_BLUE;
			// Color-code text.
			fgColor = (Color) MartRunnerMonitorDialog.STATUS_COLOR_MAP
					.get(status);
			// Set font.
			font = (Font) MartRunnerMonitorDialog.STATUS_FONT_MAP
					.get(status);
		}
		// Always white-on-color or color-on-white.
		label.setFont(font);
		label.setForeground(isSelected ? bgColor : fgColor);
		label.setBackground(isSelected ? fgColor : bgColor);
		// Others get no extra material.
		return label;
	}
}
