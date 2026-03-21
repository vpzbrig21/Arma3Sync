package fr.soe.a3s.ui.repository.dialogs.connection;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.ComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import fr.soe.a3s.constant.ProtocolType;
import fr.soe.a3s.dto.ProtocolDTO;

public class ProtocolPanel extends JPanel {

	private JLabel labelProtocol;
	private JComboBox comboBoxProtocol;
	private JCheckBox checkBoxValidateSSLCertificate;
	private final ConnectionPanel connectionPanel;

	public ProtocolPanel(ConnectionPanel connectionPanel) {

		this.connectionPanel = connectionPanel;

		this.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Protocol"));

		this.setLayout(new FlowLayout(FlowLayout.LEFT));
		JPanel panel = new JPanel();
		panel.setLayout(new GridBagLayout());
		this.add(panel);
		{
			labelProtocol = new JLabel();
			labelProtocol.setText("File transfer protocol:");
			comboBoxProtocol = new JComboBox();
			comboBoxProtocol.setFocusable(false);
			checkBoxValidateSSLCertificate = new JCheckBox();
			checkBoxValidateSSLCertificate.setText("Validate SSL certificate");
			checkBoxValidateSSLCertificate.setFocusable(false);
		}
		{
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.HORIZONTAL;
			c.weightx = 0;
			c.weighty = 0;
			c.gridx = 0;
			c.gridy = 0;
			c.insets = new Insets(0, 10, 10, 0);
			panel.add(labelProtocol, c);
		}
		{
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;
			c.weightx = 20;
			c.weighty = 20;
			c.gridx = 1;
			c.gridy = 0;
			c.insets = new Insets(0, 10, 10, 10);
			panel.add(comboBoxProtocol, c);
		}
		{
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.HORIZONTAL;
			c.weightx = 0;
			c.weighty = 0;
			c.gridx = 2;
			c.gridy = 0;
			c.insets = new Insets(0, 10, 10, 0);
			panel.add(checkBoxValidateSSLCertificate, c);
		}

		comboBoxProtocol.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				comboBoxProtocolPerformed();
			}
		});
	}

	public void init(ComboBoxModel comboBoxProtocolModel) {
		comboBoxProtocol.setModel(comboBoxProtocolModel);
		checkBoxValidateSSLCertificate.setVisible(false);
		checkBoxValidateSSLCertificate.setSelected(true);
	}

	public void init(ProtocolDTO protocoleDTO) {

		ProtocolType protocolType = protocoleDTO.getProtocolType();
		if (protocolType.equals(ProtocolType.HTTPS) || protocolType.equals(ProtocolType.HTTPS_WEBDAV)) {
			checkBoxValidateSSLCertificate.setVisible(true);
		} else {
			checkBoxValidateSSLCertificate.setVisible(false);
		}
		checkBoxValidateSSLCertificate.setSelected(protocoleDTO.isValidateSSLCertificate());
		this.comboBoxProtocol.setSelectedItem(protocolType.getDescription());
	}

	private void comboBoxProtocolPerformed() {

		String description = (String) this.comboBoxProtocol.getSelectedItem();
		ProtocolType protocolType = ProtocolType.getEnum(description);
		if (protocolType != null) {
			if (protocolType.equals(ProtocolType.HTTPS) || protocolType.equals(ProtocolType.HTTPS_WEBDAV)) {
				checkBoxValidateSSLCertificate.setVisible(true);
			} else {
				checkBoxValidateSSLCertificate.setVisible(false);
			}
			this.connectionPanel.init(protocolType);
		}
	}

	public void activate(boolean value) {
		comboBoxProtocol.setEnabled(value);
	}

	public JLabel getLabelProtocol() {
		return labelProtocol;
	}

	public JCheckBox getCheckBoxValidateSSLCertificate() {
		return checkBoxValidateSSLCertificate;
	}
}
