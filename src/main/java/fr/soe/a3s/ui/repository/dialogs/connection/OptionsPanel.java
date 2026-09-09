package fr.soe.a3s.ui.repository.dialogs.connection;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class OptionsPanel extends JPanel {

	private JCheckBox checkBoxOptions;
	private JLabel labelParallelUploads;
	private JComboBox<Integer> comboBoxParallelUploads;

	public OptionsPanel() {

		this.setBorder(BorderFactory.createTitledBorder(
				BorderFactory.createEtchedBorder(), "Options"));

		this.setLayout(new FlowLayout(FlowLayout.LEFT));
		JPanel panel = new JPanel();
		panel.setLayout(new GridBagLayout());
		this.add(panel);
		{
			checkBoxOptions = new JCheckBox();
			checkBoxOptions.setText("Upload only compressed pbo files");
		}
		{
			labelParallelUploads = new JLabel("Parallel FTP/SFTP uploads");
			comboBoxParallelUploads = new JComboBox<Integer>(new Integer[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 });
			comboBoxParallelUploads.setToolTipText(
					"Number of simultaneous FTP/SFTP upload connections (1-10). The default is 4.");
		}
		{
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.HORIZONTAL;
			c.weightx = 0.5;
			c.weighty = 0;
			c.gridx = 0;
			c.gridy = 0;
			c.insets = new Insets(5, 10, 5, 10);
			panel.add(checkBoxOptions, c);
		}
		{
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.HORIZONTAL;
			c.weightx = 0.5;
			c.weighty = 0;
			c.gridx = 0;
			c.gridy = 1;
			c.insets = new Insets(5, 10, 5, 10);
			panel.add(labelParallelUploads, c);
			c.gridx = 1;
			panel.add(comboBoxParallelUploads, c);
		}
	}

	public void init(boolean enabled, boolean selected, int parallelUploads, boolean parallelUploadsEnabled) {
		checkBoxOptions.setEnabled(enabled);
		checkBoxOptions.setSelected(selected);
		comboBoxParallelUploads.setSelectedItem(Math.max(1, Math.min(10, parallelUploads)));
		labelParallelUploads.setEnabled(parallelUploadsEnabled);
		comboBoxParallelUploads.setEnabled(parallelUploadsEnabled);
	}

	public boolean isSelected() {
		return checkBoxOptions.isSelected();
	}

	public int getParallelUploads() {
		return (Integer) comboBoxParallelUploads.getSelectedItem();
	}

	public void setParallelUploadsEnabled(boolean enabled) {
		labelParallelUploads.setEnabled(enabled);
		comboBoxParallelUploads.setEnabled(enabled);
	}
}
