package gui;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.GridLayout;
import java.awt.Panel;
import java.awt.TextField;

import javax.swing.Box;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;

import com.jgoodies.forms.factories.DefaultComponentFactory;

import javax.swing.JTextArea;

import java.awt.FlowLayout;

import javax.swing.BoxLayout;

import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.CardLayout;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultMutableTreeNode;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.File;

public class ControllerGUI {

	private JFrame frame;
	private JTextField txt_requestedPages;
	private JTextField txt_recievedPages;
	boolean treeViewUnloaded = true;
	public final static String STORE_PATH = "store/";

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					ControllerGUI window = new ControllerGUI();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public ControllerGUI() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 605, 300);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		frame.getContentPane().add(tabbedPane, BorderLayout.CENTER);
		
		JPanel panel = new JPanel();
		tabbedPane.addTab("Crawler", null, panel, null);
		panel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		
		JPanel panel_1 = new JPanel();
		panel.add(panel_1);
		FlowLayout flowLayout_1 = (FlowLayout) panel_1.getLayout();
		flowLayout_1.setAlignOnBaseline(true);
		flowLayout_1.setAlignment(FlowLayout.RIGHT);
		panel_1.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Accuracy", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		
		Box verticalBox = Box.createVerticalBox();
		panel_1.add(verticalBox);
		
		JPanel requestedPages_Panel = new JPanel();
		verticalBox.add(requestedPages_Panel);
		
		JLabel lblNewLabel = new JLabel("Requested Pages");
		
		txt_requestedPages = new JTextField();
		txt_requestedPages.setText("0");
		txt_requestedPages.setColumns(10);
		requestedPages_Panel.setLayout(new GridLayout(0, 2, 0, 0));
		requestedPages_Panel.add(lblNewLabel);
		requestedPages_Panel.add(txt_requestedPages);
		
		JPanel recievedPages_Panel = new JPanel();
		verticalBox.add(recievedPages_Panel);
		recievedPages_Panel.setLayout(new GridLayout(0, 2, 0, 0));
		
		JLabel lblNewJgoodiesLabel = DefaultComponentFactory.getInstance().createLabel("Recieved Pages");
		recievedPages_Panel.add(lblNewJgoodiesLabel);
		
		txt_recievedPages = new JTextField();
		txt_recievedPages.setText("0");
		recievedPages_Panel.add(txt_recievedPages);
		txt_recievedPages.setColumns(10);
		
		JPanel panel_5 = new JPanel();
		panel.add(panel_5);
		
		JPanel panel_2 = new JPanel();
		panel_5.add(panel_2);
		FlowLayout flowLayout_2 = (FlowLayout) panel_2.getLayout();
		flowLayout_2.setAlignOnBaseline(true);
		panel_2.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Scope", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		
		Box verticalBox_1 = Box.createVerticalBox();
		panel_2.add(verticalBox_1);
		
		Panel panel_3 = new Panel();
		verticalBox_1.add(panel_3);
		
		JLabel lblNewLabel_1 = new JLabel("Domain Count");
		panel_3.add(lblNewLabel_1);
		
		TextField txt_domainCount = new TextField();
		panel_3.add(txt_domainCount);
		
		Panel panel_4 = new Panel();
		verticalBox_1.add(panel_4);
		
		JLabel label = new JLabel("Domain Count");
		panel_4.add(label);
		
		TextField textField = new TextField();
		panel_4.add(textField);
		
		JPanel panel_6 = new JPanel();
		tabbedPane.addTab("Console", null, panel_6, null);
		
		JTextArea console = new JTextArea();
		console.setColumns(71);
		console.setRows(12);
		console.setEditable(false);
		panel_6.add(console);
		
		final JPanel panel_7 = new JPanel();
		tabbedPane.addTab("PageBrowser", null, panel_7, null);
		panel_7.setLayout(new BorderLayout(0, 0));
		
		JTree tree = new JTree();
		DefaultMutableTreeNode byDomain = new DefaultMutableTreeNode("ByDomain");
		tree.setModel(new DefaultTreeModel(byDomain));
		
		File path = new File(STORE_PATH);

	    File [] files = path.listFiles();
	    for (int i = 0; i < files.length; i++){
	        if (files[i].isFile()){ //this line weeds out other directories/folders
	        	DefaultMutableTreeNode node = new DefaultMutableTreeNode(files[i].getName());
	        	byDomain.add(node);
	        }
	    }
	    
		panel_7.add(tree);
	}

}
