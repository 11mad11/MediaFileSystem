package fr.mad.ImageUtil;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.*;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.swing.AutoCompleteSupport;

public class UI {
	public static JFrame frame = new JFrame("Image Utilitaire");
	private static JImage imageComponent;
	private static JComboBox<String> comboBoxTag;
	
	private static EventList<String> listOfTag;
	private static DefaultListModel<String> tagListModel;
	
	public UI() {
		frame.getContentPane().setLayout(new BorderLayout(0, 0));
		
		JPanel panel_image = new JPanel();
		frame.getContentPane().add(panel_image, BorderLayout.CENTER);
		panel_image.setLayout(new BorderLayout(0, 0));
		
		imageComponent = new JImage(null);
		panel_image.add(imageComponent, BorderLayout.CENTER);
		
		JPanel panel_control = new JPanel();
		frame.getContentPane().add(panel_control, BorderLayout.EAST);
		panel_control.setLayout(new BorderLayout(0, 0));
		
		JPanel panel = new JPanel();
		panel_control.add(panel, BorderLayout.NORTH);
		
		JLabel lblTag = new JLabel("Tag:");
		panel.add(lblTag);
		
		comboBoxTag = new JComboBox<String>();
		comboBoxTag.setEditable(true);
		panel.add(comboBoxTag);
		
		JButton btnAddTag = new JButton("add");
		btnAddTag.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String tag = comboBoxTag.getSelectedItem().toString();
				listOfTag.remove(tag);
				listOfTag.add(tag);
				
				tagListModel.removeElement(tag);
				tagListModel.addElement(tag);
			}
		});
		panel.add(btnAddTag);
		
		JScrollPane scrollPane_1 = new JScrollPane();
		scrollPane_1.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		panel_control.add(scrollPane_1, BorderLayout.EAST);
		
		final JList<String> listTags = new JList<String>();
		listTags.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		listTags.setModel(tagListModel = new DefaultListModel<String>());
		scrollPane_1.setViewportView(listTags);
		
		JButton btnRemoveTag = new JButton("Remove");
		btnRemoveTag.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				tagListModel.remove(listTags.getSelectedIndex());
			}
		});
		scrollPane_1.setColumnHeaderView(btnRemoveTag);
		
		JList<?> list_1 = new JList<Object>();
		panel_control.add(list_1, BorderLayout.SOUTH);
		
		JPanel panel_info = new JPanel();
		frame.getContentPane().add(panel_info, BorderLayout.NORTH);
		
		JLabel lblInfoImage = new JLabel("New label");
		panel_info.add(lblInfoImage);
		
		JMenuBar menuBar = new JMenuBar();
		frame.setJMenuBar(menuBar);
		
		JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);
		
		JMenuItem mntmRun = new JMenuItem("Run");
		mnFile.add(mntmRun);
		
		JMenuItem mntmSelectScript = new JMenuItem("Select Script...");
		mnFile.add(mntmSelectScript);
		
		JSeparator separator = new JSeparator();
		mnFile.add(separator);
		
		JMenuItem mntmExit = new JMenuItem("Exit");
		mnFile.add(mntmExit);
		
		SwingUtilities.invokeLater(new Runnable() {
			
			public void run() {
				setupTagList();
			}
		});
		
		frame.show();
		//frame.pack();
	}
	
	private static void setupTagList() {
		listOfTag = GlazedLists.eventListOf();
		AutoCompleteSupport.install(comboBoxTag, listOfTag);
	}
}
