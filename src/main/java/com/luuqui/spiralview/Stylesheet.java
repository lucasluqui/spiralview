package com.luuqui.spiralview;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.io.InputStream;

public class Stylesheet {

  public static void load() {
    // Start: KL Stylesheet
    //UIManager.put("TabbedPane.underlineColor", CustomColors.KL);
    //UIManager.put("TabbedPane.inactiveUnderlineColor", CustomColors.KL);

    //UIManager.put("Slider.thumbColor", CustomColors.KL);

    UIManager.put("ComboBox.selectionBackground", CustomColors.INTERFACE_DEFAULT_FOCUS);

    UIManager.put("TextField.selectionBackground", CustomColors.INTERFACE_DEFAULT_FOCUS);

    UIManager.put("EditorPane.selectionBackground", CustomColors.INTERFACE_DEFAULT_FOCUS);

    UIManager.put("ScrollPane.smoothScrolling", true);

    UIManager.put("Button.default.focusedBorderColor", CustomColors.INTERFACE_COMPONENT_FOCUS);
    UIManager.put("Button.default.focusColor", CustomColors.INTERFACE_COMPONENT_FOCUS);
    UIManager.put("Button.default.hoverBorderColor", CustomColors.INTERFACE_COMPONENT_FOCUS);
    UIManager.put("Button.focusedBorderColor", CustomColors.INTERFACE_COMPONENT_FOCUS);
    UIManager.put("Button.focusColor", CustomColors.INTERFACE_COMPONENT_FOCUS);
    UIManager.put("Button.hoverBorderColor", CustomColors.INTERFACE_COMPONENT_FOCUS);

    UIManager.put("CheckBox.icon.focusedBorderColor", CustomColors.INTERFACE_COMPONENT_FOCUS);
    UIManager.put("CheckBox.icon.focusColor", CustomColors.INTERFACE_COMPONENT_FOCUS);
    UIManager.put("CheckBox.icon.hoverBorderColor", CustomColors.INTERFACE_COMPONENT_FOCUS);

    UIManager.put("Component.focusedBorderColor", CustomColors.INTERFACE_COMPONENT_FOCUS);
    UIManager.put("Component.focusColor", CustomColors.INTERFACE_COMPONENT_FOCUS);
    UIManager.put("Component.hoverBorderColor", CustomColors.INTERFACE_COMPONENT_FOCUS);
    // End: KL Stylesheet


    // Start: spiralview Stylesheet
    try {
      InputStream fontDefaultIs = ModelViewerHook.class.getResourceAsStream("/font/Figtree-Regular.ttf");
      InputStream fontCodeIs = ModelViewerHook.class.getResourceAsStream("/font/SourceCodePro-Regular.ttf");
      InputStream fontCodeSmIs = ModelViewerHook.class.getResourceAsStream("/font/SourceCodePro-Regular.ttf");

      Font fontDefault = Font.createFont(Font.TRUETYPE_FONT, fontDefaultIs);
      Font fontCode = Font.createFont(Font.TRUETYPE_FONT, fontCodeIs);
      Font fontCodeSm = Font.createFont(Font.TRUETYPE_FONT, fontCodeSmIs);

      fontDefault = fontDefault.deriveFont(12.0f);
      fontCode = fontCode.deriveFont(12.0f);
      fontCodeSm = fontCodeSm.deriveFont(9.0f);

      final FontUIResource fontDefaultRes = new FontUIResource(fontDefault);
      final FontUIResource fontCodeRes = new FontUIResource(fontCode);
      final FontUIResource fontCodeSmRes = new FontUIResource(fontCodeSm);

      UIManager.getLookAndFeelDefaults().put("Component.font", fontDefaultRes);
      UIManager.getLookAndFeelDefaults().put("Panel.font", fontDefaultRes);
      UIManager.getLookAndFeelDefaults().put("EditorPanel.font", fontDefaultRes);
      UIManager.getLookAndFeelDefaults().put("Button.font", fontDefaultRes);
      UIManager.getLookAndFeelDefaults().put("RadioButton.font", fontDefaultRes);
      UIManager.getLookAndFeelDefaults().put("ToggleButton.font", fontDefaultRes);
      UIManager.getLookAndFeelDefaults().put("CheckBox.font", fontDefaultRes);
      UIManager.getLookAndFeelDefaults().put("Label.font", fontDefaultRes);
      UIManager.getLookAndFeelDefaults().put("TitlePane.font", fontDefaultRes);
      UIManager.getLookAndFeelDefaults().put("MenuBar.font", fontDefaultRes);
      UIManager.getLookAndFeelDefaults().put("MenuItem.font", fontDefaultRes);
      UIManager.getLookAndFeelDefaults().put("Menu.font", fontDefaultRes);
      UIManager.getLookAndFeelDefaults().put("CheckBoxMenuItem.font", fontDefaultRes);
      UIManager.getLookAndFeelDefaults().put("RadioButtonMenuItem.font", fontDefaultRes);
      UIManager.getLookAndFeelDefaults().put("PopupMenu.font", fontDefaultRes);
      UIManager.getLookAndFeelDefaults().put("TabbedPane.font", fontDefaultRes);
      UIManager.getLookAndFeelDefaults().put("ToolBar.font", fontDefaultRes);
      UIManager.getLookAndFeelDefaults().put("ToolTip.font", fontDefaultRes);
      UIManager.getLookAndFeelDefaults().put("TableHeader.font", fontDefaultRes);
      UIManager.getLookAndFeelDefaults().put("TitledBorder.font", fontDefaultRes);

      UIManager.getLookAndFeelDefaults().put("TextField.font", fontCodeRes);
      UIManager.getLookAndFeelDefaults().put("FormattedTextField.font", fontCodeRes);
      UIManager.getLookAndFeelDefaults().put("TextArea.font", fontCodeRes);
      UIManager.getLookAndFeelDefaults().put("TextPane.font", fontCodeRes);
      UIManager.getLookAndFeelDefaults().put("ComboBox.font", fontCodeRes);
      UIManager.getLookAndFeelDefaults().put("List.font", fontCodeRes);
      UIManager.getLookAndFeelDefaults().put("Spinner.font", fontCodeSmRes);
      UIManager.getLookAndFeelDefaults().put("DraggableSpinner.font", fontCodeSmRes);
      UIManager.getLookAndFeelDefaults().put("Slider.font", fontCodeRes);
      UIManager.getLookAndFeelDefaults().put("Tree.font", fontCodeRes);
      UIManager.getLookAndFeelDefaults().put("Table.font", fontCodeRes);
      UIManager.getLookAndFeelDefaults().put("MenuItem.acceleratorFont", fontCodeRes);
    } catch (Exception e) {
      Log.log.error(e);
    }
    // End: spiralview Stylesheet
  }

}
