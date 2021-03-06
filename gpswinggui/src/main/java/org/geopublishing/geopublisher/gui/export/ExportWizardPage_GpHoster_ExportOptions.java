package org.geopublishing.geopublisher.gui.export;

import java.awt.Component;
import java.io.IOException;

import javax.swing.JCheckBox;
import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;

import org.geopublishing.geopublisher.AtlasConfigEditable;
import org.geopublishing.geopublisher.export.gphoster.GpHosterClient;
import org.geopublishing.geopublisher.swing.GeopublisherGUI;
import org.netbeans.spi.wizard.WizardPage;

import de.schmitzm.swing.swingworker.AtlasSwingWorker;

public class ExportWizardPage_GpHoster_ExportOptions extends WizardPage {

	private JLabel explanationLabel;
	private JCheckBox makePublicCheckBox;
	private String validCredentials = null;
	private JLabel makePublicExplanationLabel;

	public ExportWizardPage_GpHoster_ExportOptions() {
		// GpHosterClient gphc = (GpHosterClient)
		// getWizardData(ExportWizard.GPHC);
		initGui();
	}

	@Override
	protected void renderingPage() {

		validCredentials = new AtlasSwingWorker<String>(
				this,
				GeopublisherGUI
						.R("ExportWizard.Ftp.ExportOptions.validatingCredentials")) {

			@Override
			protected String doInBackground() throws Exception {
				GpHosterClient gphc = (GpHosterClient) getWizardData(ExportWizard.GPHC);

				gphc.setUserName((String) getWizardData(ExportWizard.GPH_USERNAME));
				// if ((String) getWizardData(ExportWizard.GPH_EMAIL_FIELD) !=
				// null)
				// gphc.setUserName((String)
				// getWizardData(ExportWizard.GPH_EMAIL_FIELD));
				gphc.setPassword((String) getWizardData(ExportWizard.GPH_PASSWORD));

				String acebasename = ((AtlasConfigEditable) getWizardData(ExportWizard.ACE))
						.getBaseName();
				try {
					if (!gphc.validateCredentials())
						return GeopublisherGUI
								.R("ExportWizard.Ftp.ExportOptions.invalidCredentials");
					if (!gphc.canEditAtlas(acebasename))
						return GeopublisherGUI.R(
								"ExportWizard.Ftp.ExportOptions.NoPermission",
								gphc.getUserName(), acebasename);
					putWizardData(ExportWizardFTPBrancher.URL_FOR_ATLAS,
							gphc.getUrlForAtlas(acebasename));
					return null;
				} catch (IOException e) {
					return e.getLocalizedMessage();
				}
			}
		}.executeModalNoEx();

	}

	@Override
	protected String validateContents(Component component, Object event) {
		if (validCredentials != null)
			return validCredentials;
		return super.validateContents(component, event);
	}

	private void initGui() {
		setSize(ExportWizard.DEFAULT_WPANEL_SIZE);
		setPreferredSize(ExportWizard.DEFAULT_WPANEL_SIZE);
		setLayout(new MigLayout("wrap 1"));
		add(getExplanationLabel());
		add(getMakePublicCheckBox());
		add(getMakePublicExplanationLabel());
	}

	private JLabel getExplanationLabel() {
		if (explanationLabel == null) {
			explanationLabel = new JLabel(
					GeopublisherGUI.R("ExportWizard.Ftp.Export.Explanation"));
		}
		return explanationLabel;
	}

	private JCheckBox getMakePublicCheckBox() {
		if (makePublicCheckBox == null) {
			makePublicCheckBox = new JCheckBox(
					GeopublisherGUI.R("ExportWizard.Ftp.Export.Public"));
			makePublicCheckBox.setName(ExportWizard.GpHosterAuth);
		}
		return makePublicCheckBox;
	}

	private JLabel getMakePublicExplanationLabel() {
		if (makePublicExplanationLabel == null) {
			makePublicExplanationLabel = new JLabel(
					GeopublisherGUI
							.R("ExportWizard.Ftp.Export.Public.Explanation"));
		}
		return makePublicExplanationLabel;
	}

	public static String getDescription() {
		return GeopublisherGUI.R("ExportWizard.Ftp.Export");
	}

}
