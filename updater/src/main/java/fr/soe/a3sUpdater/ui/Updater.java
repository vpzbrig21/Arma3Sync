package fr.soe.a3sUpdater.ui;

import fr.soe.a3sUpdater.service.Service;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.IOException;
import java.util.concurrent.CancellationException;

public final class Updater extends JFrame implements UIConstants {
    private final Facade facade;
    private final Service service = new Service();
    private final JLabel versionLabel = new JLabel("Checking update metadata...");
    private final JLabel actionLabel = new JLabel(" ");
    private final JProgressBar progressBar = new JProgressBar();
    private final JButton cancelButton = new JButton("Cancel");
    private SwingWorker<Void, Integer> worker;

    public Updater(Facade facade) {
        super(APPLICATION_NAME);
        this.facade = facade;
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        if (ICON != null) setIconImage(ICON);
        setLayout(new BorderLayout(8, 8));
        var center = new javax.swing.JPanel(new GridLayout(2, 1, 4, 4));
        center.setBorder(BorderFactory.createEmptyBorder(12, 16, 4, 16));
        center.add(versionLabel);
        center.add(actionLabel);
        add(center, BorderLayout.CENTER);
        progressBar.setStringPainted(true);
        add(progressBar, BorderLayout.SOUTH);
        var buttons = new javax.swing.JPanel(new FlowLayout(FlowLayout.RIGHT));
        cancelButton.addActionListener(event -> confirmCancel());
        buttons.add(cancelButton);
        add(buttons, BorderLayout.NORTH);
        setPreferredSize(new Dimension(430, 150));
        pack();
        setLocationRelativeTo(null);
    }

    public void startUpdate() {
        setVisible(true);
        worker = new SwingWorker<Void, Integer>() {
            private String targetVersion;
            private long total;

            @Override
            protected Void doInBackground() throws Exception {
                service.setSourcePreference(facade.getSource());
                targetVersion = service.getManifest(facade.isDevMode()).version();
                if (!service.isUpdateAvailable(facade.isDevMode())) {
                    throw new IOException("No new update is available.");
                }
                total = service.getSize(facade.isDevMode());
                if (isCancelled()) return null;
                service.setDownload();
                service.addDownloadObserver(value -> publish(value));
                service.download(facade.isDevMode());
                if (isCancelled()) return null;
                service.install();
                return null;
            }

            @Override
            protected void process(java.util.List<Integer> values) {
                if (total > 0 && !values.isEmpty()) {
                    int current = values.get(values.size() - 1);
                    progressBar.setValue((int) Math.min(100L, current * 100L / total));
                }
            }

            @Override
            protected void done() {
                cancelButton.setEnabled(false);
                if (isCancelled()) {
                    service.clean();
                    dispose();
                    return;
                }
                try {
                    get();
                    JOptionPane.showMessageDialog(Updater.this,
                            "ArmA3Sync has been successfully updated to version " + targetVersion + ".",
                            "Update", JOptionPane.INFORMATION_MESSAGE);
                    service.clean();
                    dispose();
                    launchApplication();
                } catch (CancellationException exception) {
                    service.clean();
                    dispose();
                } catch (Exception exception) {
                    service.clean();
                    JOptionPane.showMessageDialog(Updater.this,
                            "An error occurred:\n" + exception.getMessage() + "\nUpdate process aborted.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                    dispose();
                }
            }
        };
        worker.execute();
    }

    private void confirmCancel() {
        int answer = JOptionPane.showConfirmDialog(this, "Cancel update process?", "Update", JOptionPane.YES_NO_OPTION);
        if (answer == JOptionPane.YES_OPTION) {
            cancelButton.setEnabled(false);
            actionLabel.setText("Cancelling update...");
            if (worker != null && !worker.isDone()) worker.cancel(true);
            service.cancel();
        }
    }

    private void launchApplication() {
        try {
            var directory = service.installationPath().toFile();
            var executable = new java.io.File(directory, "Arma3Sync.exe");
            if (!executable.isFile()) {
                // Compatibility with installations made by older releases.
                executable = new java.io.File(directory, "ArmA3Sync.exe");
            }
            if (executable.isFile()) {
                var command = new java.util.ArrayList<String>();
                command.add(executable.getName());
                if (facade.isDevMode()) command.add("-dev");
                new ProcessBuilder(command).directory(directory).start();
            } else {
                var script = new java.io.File(directory, "Arma3Sync.bat");
                if (!script.isFile()) script = new java.io.File(directory, "bin/Arma3Sync.bat");
                if (!script.isFile()) script = new java.io.File(directory, "ArmA3Sync.bat");
                if (script.isFile()) new ProcessBuilder("cmd", "/c", script.getAbsolutePath()).directory(directory).start();
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }
}
