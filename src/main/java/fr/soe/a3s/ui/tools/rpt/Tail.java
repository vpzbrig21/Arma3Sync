package fr.soe.a3s.ui.tools.rpt;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;

import javax.swing.JTextArea;
import javax.swing.SwingWorker;

/**
 * Implements console-based log file tailing, or more specifically, tail
 * following: it is somewhat equivalent to the unix command "tail -f"
 */
public class Tail implements LogFileTailerListener {
	/**
	 * The log file tailer
	 */
	private LogFileTailer tailer;

	private JTextArea textArea;

	public Tail(File file,JTextArea textArea) {
		this.textArea = textArea;
		preloadExistingContent(file);
		tailer = new LogFileTailer(file, 1000, false);
		tailer.addLogFileTailerListener(this);
		
	}

	/**
	 * A new line has been added to the tailed log file
	 * 
	 * @param line
	 *            The new line that has been added to the tailed log file
	 */
	public void newLogFileLine(String line) {
		//System.out.println(line);
		textArea.append(line + "\n");
		textArea.setCaretPosition(textArea.getDocument().getLength());
//		areaRows++;
//		textArea.setRows(areaRows);
	}
	
	private void preloadExistingContent(File file){
		if (file == null || !file.exists()) {
			return;
		}
		SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
			@Override
			protected String doInBackground() throws Exception {
				try (FileInputStream fin = new FileInputStream(file);
					 DataInputStream dataInputStream = new DataInputStream(fin)) {
					byte[] buffer = new byte[(int) file.length()];
					dataInputStream.readFully(buffer);
					return new String(buffer);
				}
			}

			@Override
			protected void done() {
				try {
					String content = get();
					if (content != null && !content.isEmpty()) {
						textArea.append(content);
						if (!content.endsWith("\n")) {
							textArea.append("\n");
						}
						textArea.setCaretPosition(textArea.getDocument().getLength());
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		worker.execute();
	}

	public void start() {
		tailer.start();
	}

	public void stop() {
		tailer.interrupt();
	}

	/**
	 * Command-line launcher
	 */
//	public static void main(String[] args) {
//	
//		if (args.length < 1) {
//			System.out.println("Usage: Tail <filename>");
//			System.exit(0);
//		}
//		Tail tail = new Tail(args[0]);
//	}
}
