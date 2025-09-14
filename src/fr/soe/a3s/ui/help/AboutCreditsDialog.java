
 package fr.soe.a3s.ui.help;
 
 import java.awt.BorderLayout;
 import java.awt.Font;
 import java.awt.Image;
 
 import javax.swing.BorderFactory;
 import javax.swing.Box;
 import javax.swing.JEditorPane;
 import javax.swing.JPanel;
 import javax.swing.JScrollPane;
 import javax.swing.UIManager;
 import javax.swing.border.BevelBorder;
 import javax.swing.text.html.HTMLEditorKit;
 
 import fr.soe.a3s.ui.AbstractDialog;
 import fr.soe.a3s.ui.Facade;
 import fr.soe.a3s.ui.ImagePanel;
 import fr.soe.a3s.ui.ImageResizer;
 
 public class AboutCreditsDialog extends AbstractDialog {
 
 	public AboutCreditsDialog(Facade facade) {
                 super(facade, "Credits", true);
                 setResizable(false);
                 setPreferredSize(new java.awt.Dimension(550, 300));
 
 		{
 			{
 				buttonOK.setEnabled(false);
 				buttonOK.setVisible(false);
 				buttonCancel.setText("Close");
 				getRootPane().setDefaultButton(buttonCancel);
 			}
 			{
 				JPanel mainPanel = new JPanel();
  public class AboutCreditsDialog extends AbstractDialog {
 					mainPanel.add(hBox, BorderLayout.CENTER);
 					{
 						JTextArea textArea = new JTextArea();
 						hBox.add(textArea);
 						textArea.setText("Software development\n"
 								+ "[S.o.E] Major_Shepard\n"
 								+ "\nGraphical design\n" + "[S.o.E] Matt2507\n"
 								+ "\nTesting\n"
 								+ "[S.o.E],[F27],[BWF]\nTeam Members\n"
 								+ "\nMaintained since 2025 by\n"
 								+ "[PzBrig21] Soro\n"
 								+ "\nInspired from \n"
 								+ "- ArmA II Game Launcher"
 								+ "\n  by SpiritedMachine.\n"
 								+ "- AddonSync 2009 by Yoma.");
 						Font fontTextField = UIManager
 								.getFont("TextField.font");
 						textArea.setFont(fontTextField);
 						textArea.setEditable(false);
 					}*/
 					
                                         Box hBox = Box.createHorizontalBox();
                                         mainPanel.add(hBox, BorderLayout.CENTER);
                                         {
                                                 JEditorPane textPane = new JEditorPane();
-                                                hBox.add(textPane);
                                                 textPane.setEditorKit(new HTMLEditorKit());
-                                                textPane.setText("<html><body style='white-space:nowrap;'>"
-                                                                + "Original Development<br>"
-                                                                + "Software Development<br>"
-                                                                + "- [S.o.E] Major_Shepard<br>"
-                                                                + "Graphical design<br>"
-                                                                + "- [S.o.E] Matt2507<br>"
-                                                                + "Testing<br>"
-                                                                + "- [S.o.E],[F27],[BWF]<br>"
-                                                                + "- Team Members<br><br>"
-                                                                + "Inspired by<br>"
-                                                                + "- ArmA II Game Launcher by SpiritedMachine<br>"
-                                                                + "- AddonSync 2009 by Yoma<br><br>"
-                                                                + "Maintainer since 2025<br>"
-                                                                + "- [PzBrig21] Soro" + "</body></html>");
                                                 Font fontTextField = UIManager.getFont("TextField.font");
+                                                StringBuilder credits = new StringBuilder();
+                                                credits.append("<html><head><style>body{margin:0;}" +
+                                                                "ul{margin-top:0;margin-bottom:0;padding-left:20px;}" +
+                                                                "h3{margin-bottom:2px;}</style></head><body style='font-family:")
+                                                                .append(fontTextField.getFamily())
+                                                                .append(";font-size:")
+                                                                .append(fontTextField.getSize())
+                                                                .append("pt;'>");
+                                                credits.append("<h3 style='font-size:")
+                                                                .append(fontTextField.getSize() + 2)
+                                                                .append("pt;'>Original Development</h3>");
+                                                credits.append("<ul>")
+                                                                .append("<li>Software Development - [S.o.E] Major_Shepard</li>")
+                                                                .append("<li>Graphical Design - [S.o.E] Matt2507</li>")
+                                                                .append("</ul>");
+                                                credits.append("<h3 style='font-size:")
+                                                                .append(fontTextField.getSize() + 2)
+                                                                .append("pt;'>Testing</h3>");
+                                                credits.append("<ul>")
+                                                                .append("<li>[S.o.E]</li>")
+                                                                .append("<li>[F27]</li>")
+                                                                .append("<li>[BWF]</li>")
+                                                                .append("<li>Team Members</li>")
+                                                                .append("</ul>");
+                                                credits.append("<h3 style='font-size:")
+                                                                .append(fontTextField.getSize() + 2)
+                                                                .append("pt;'>Inspired by</h3>");
+                                                credits.append("<ul>")
+                                                                .append("<li>ArmA II Game Launcher by SpiritedMachine</li>")
+                                                                .append("<li>AddonSync 2009 by Yoma</li>")
+                                                                .append("</ul>");
+                                                credits.append("<h3 style='font-size:")
+                                                                .append(fontTextField.getSize() + 2)
+                                                                .append("pt;'>Maintainer since 2025</h3>");
+                                                credits.append("<ul>")
+                                                                .append("<li>[PzBrig21] Soro</li>")
+                                                                .append("</ul>");
+                                                credits.append("</body></html>");
+                                                textPane.setText(credits.toString());
                                                 textPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES,
                                                                 Boolean.TRUE);
                                                 textPane.setFont(fontTextField);
                                                 textPane.setEditable(false);
-                                                textPane.setMinimumSize(new java.awt.Dimension(340, 260));
-                                                textPane.setPreferredSize(new java.awt.Dimension(340, 260));
+                                                textPane.setOpaque(false);
+                                                JScrollPane scrollPane = new JScrollPane(textPane);
+                                                scrollPane.setBorder(null);
+                                                scrollPane.setOpaque(false);
+                                                scrollPane.getViewport().setOpaque(false);
+                                                scrollPane.setPreferredSize(new java.awt.Dimension(340, 260));
+                                                scrollPane.setMinimumSize(new java.awt.Dimension(340, 260));
+                                                hBox.add(scrollPane);
                                         }
                                         hBox.add(Box.createHorizontalStrut(10));
 
-					{
-						Box vBox = Box.createVerticalBox();
-						hBox.add(vBox);
-						{
-							vBox.add(Box.createVerticalGlue());
-							ImagePanel imagePanel = new ImagePanel();
-							Image image = ImageResizer.resizeToNewWidth(SOE,
-									150);
-							imagePanel.setImage(image);
-							imagePanel.repaint();
-							imagePanel.setBackground(new java.awt.Color(255,
-									255, 255));
-							vBox.add(imagePanel);
-							vBox.add(Box.createVerticalGlue());
-						}
-					}
-				}
-			}
-		}
+                                        {
+                                                Box vBox = Box.createVerticalBox();
+                                                hBox.add(vBox);
+                                                {
+                                                        vBox.add(Box.createVerticalGlue());
+                                                        ImagePanel imagePanel = new ImagePanel();
+                                                        Image image = ImageResizer.resizeToNewWidth(SOE,
+                                                                        150);
+                                                        imagePanel.setImage(image);
+                                                        imagePanel.setMinimumSize(imagePanel.getPreferredSize());
+                                                        imagePanel.repaint();
+                                                        imagePanel.setBackground(new java.awt.Color(255,
+                                                                        255, 255));
+                                                        vBox.add(imagePanel);
+                                                        vBox.add(Box.createVerticalGlue());
+                                                }
+                                        }
+                                }
+                        }
+                }
 
 		this.pack();
 		this.setLocationRelativeTo(facade.getMainPanel());
 	}
 
 	@Override
 	protected void buttonOKPerformed() {
 		this.dispose();
 	}
 
 	@Override
 	protected void buttonCancelPerformed() {
 		this.dispose();
 	}
 
 	@Override
 	protected void menuExitPerformed() {
 		this.dispose();
 	}
 }
