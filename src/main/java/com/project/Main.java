package com.project;

import com.project.gui.Table;
import javax.swing.SwingUtilities;

public class Main {

  public static void main(String[] args) {
    SwingUtilities.invokeLater(Table::new);
  }
}