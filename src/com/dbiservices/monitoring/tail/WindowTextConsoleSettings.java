package com.dbiservices.monitoring.tail;

import com.dbiservices.monitoring.tail.textconsole.SwingConsole;
import com.dbiservices.tools.Logger;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class WindowTextConsoleSettings {
    
    private static final Logger logger = Logger.getLogger(WindowTextConsoleSettings.class);
    
    private CheckBox autoSearch;
    private CheckBox caseSensitive;
    private CheckBox wholeWord;

    private TextField searchText = null;
    private MenuButton searchMenuButton = new MenuButton();

    public WindowTextConsoleSettings() {
        searchText = new TextField();
        searchText.setPrefWidth(100);
        searchText.setPrefHeight(30);
        
        searchMenuButton.setGraphic(new ImageView(new Image("magnifier-empty.png")));
        autoSearch = new CheckBox("Auto-Search");
        CustomMenuItem smb_item_autoSearch = new CustomMenuItem(autoSearch);
        smb_item_autoSearch.setHideOnClick(false);

        caseSensitive = new CheckBox("Match Case");
        CustomMenuItem smb_item_caseSensitive = new CustomMenuItem(caseSensitive);
        smb_item_caseSensitive.setHideOnClick(false);

        wholeWord = new CheckBox("Whole Word");
        wholeWord.setSelected(true);
        CustomMenuItem smb_item_wholeWord = new CustomMenuItem(wholeWord);
        smb_item_wholeWord.setHideOnClick(false);

        searchMenuButton.getItems().setAll(smb_item_autoSearch, smb_item_caseSensitive, smb_item_wholeWord);
    }

    public MenuButton getMenuButton() {
        
        System.out.println("GET MENU");
        logger.warning("GET MENU");
        
        return searchMenuButton;
    }

    public boolean isAutoSearch() {return autoSearch.isSelected();}
    public boolean isCaseSensitive() {return caseSensitive.isSelected();}
    public boolean isWholeWord() {return wholeWord.isSelected();}

    public TextField getSearchTextField() {
        
        logger.warning("GET SEARCH TEXT FIELD");
        System.out.println("GET SEARCH TEXT FIELD");
        return searchText;
    }
}
