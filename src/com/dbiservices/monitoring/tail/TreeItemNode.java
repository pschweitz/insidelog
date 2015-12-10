/*
 * Copyright 2015 Philippe Schweitzer.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dbiservices.monitoring.tail;

/**
 *
 * @author Philippe Schweitzer
 * @version 1.1
 * @since 16.11.2015
 */
import com.dbiservices.monitoring.common.schedulerservice.IScheduledService;
import com.dbiservices.monitoring.common.schedulerservice.ScheduledDefinition;
import com.dbiservices.monitoring.common.schedulerservice.ServiceScheduler;
import com.dbiservices.tools.ApplicationContext;
import com.dbiservices.tools.Logger;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.StringTokenizer;
import javafx.beans.property.BooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class TreeItemNode extends TreeItem<String> implements Serializable {

    private static final Logger logger = Logger.getLogger(TreeItemNode.class);

    private boolean isNode = false;
    private InformationObject informationObject;
    private IScheduledService tailThread;

    public TreeItemNode() {
        super();
    }

    private TreeItemNode(String text) {
        super(text);
    }

    private TreeItemNode(String text, boolean isNode) {
        this(text);
        this.isNode = isNode;
    }

    public TreeItemNode(InformationObject informationObject, boolean isNode) {
        this(informationObject.getDisplayName(), isNode);
        this.informationObject = informationObject;
        this.isNode = isNode;

        if (informationObject.getFilePath().toLowerCase().startsWith("ssh://")) {
            tailThread = new TailSSH(informationObject);
        } else {
            tailThread = new TailFile(informationObject);
        }
    }

    public static TreeItemNode createSubGroups(TreeView<String> treeView, String fullName) {
        TreeItemNode rootNode = (TreeItemNode) treeView.getRoot();
        StringTokenizer stringTokenizer;
        stringTokenizer = new StringTokenizer(fullName, "/");

        int counter = stringTokenizer.countTokens() - 1;

        while (counter != 0) {

            String token = stringTokenizer.nextToken();

            if (rootNode.isUniqChild(token)) {
                InformationObject informationObject = new InformationObject(token, token, null, 0, 0, false, DbiTail.colorFileName);

                TreeItemNode treeItem = new TreeItemNode(informationObject, true);
                treeItem.setGraphic(new ImageView(new Image("folder-horizontal.png")));
                rootNode.addNode(treeItem, treeView);
                rootNode = treeItem;
            } else {
                for (TreeItem<String> child : rootNode.getChildren()) {
                    if (child.getValue().equals(token)) {
                        rootNode = (TreeItemNode) child;
                        break;
                    }
                }
            }

            counter--;
        }

        return rootNode;
    }

    public void startTailSchedule() {
        if (!ServiceScheduler.getScheduledDefinitionPool().containsKey(informationObject.getFullName())) {
            ServiceScheduler.addScheduledDefinition(informationObject.getFullName(), new ScheduledDefinition(informationObject, tailThread));
        }
    }

    public void stopTailSchedule() {
        ServiceScheduler.removeScheduledDefinition(informationObject.getFullName());
    }

    public void showTextconsole() {
        if (informationObject.getWindowTextConsole() != null) {
            informationObject.getWindowTextConsole().show();
        }
    }

    public void hideTextconsole() {
        if (informationObject.getWindowTextConsole() != null) {
            informationObject.getWindowTextConsole().hide();
        }
    }

    public InformationObject getInformationObject() {
        return this.informationObject;
    }

    public boolean isNode() {
        return isNode;
    }

    public boolean isFile() {
        return !isNode;
    }

    public static String getFullPath(TreeItemNode newNode) {
        String result = newNode.getValue();
        TreeItemNode node = newNode;

        if (node.getParent() != null) {
            while (node.getParent().getParent() != null) {
                result = node.getParent().getValue() + "/" + result;
                node = (TreeItemNode) node.getParent();
            }
        }

        return result;
    }

    public static String getFileName(TreeItemNode newNode) {
        return newNode.getInformationObject().getFilePath().toString();
    }

    public boolean isChild(TreeItemNode parentNode) {
        boolean result = false;
        ArrayList<TreeItemNode> nodes = new ArrayList();

        getRecursiveNode((TreeItemNode) parentNode, nodes);

        for (TreeItemNode node : nodes) {
            if (this.informationObject.getFullName().equals(node.informationObject.getFullName())) {
                return true;
            }
        }

        return result;
    }

    public static void getRecursiveNode(TreeItemNode treeItemNode, ArrayList<TreeItemNode> nodes) {

        if (((TreeItemNode) treeItemNode).isNode) {
            nodes.add(treeItemNode);
        }
        ObservableList<TreeItem<String>> children = treeItemNode.getChildren();

        for (TreeItem<String> child : children) {
            if (((TreeItemNode) child).isNode) {
                getRecursiveNode((TreeItemNode) child, nodes);

                nodes.add((TreeItemNode) child);
            }
        }
    }

    public static void refreshColorConfiguration(TreeItemNode treeItemNode, ColorConfiguration colorConfiguration) {

        
                
        if (((TreeItemNode) treeItemNode).isFile()) {

                logger.debug("treeItemNode: " + treeItemNode.informationObject.getDisplayName());
            if (treeItemNode.informationObject.getColorConfiguration().templateName.equals(colorConfiguration.templateName)
                    && ! treeItemNode.informationObject.getColorConfiguration().templateName.equals(DbiTail.colorFileName)) {
                
                logger.debug("refresh color configuration: " + treeItemNode.informationObject.getDisplayName());
                
                treeItemNode.informationObject.setColorConfiguration(colorConfiguration);
       //         ServiceScheduler.getScheduledDefinition(treeItemNode.informationObject.getFullName()).refreshWindowConfiguration(colorConfiguration);
            }

        } else {
            ObservableList<TreeItem<String>> children = treeItemNode.getChildren();

            for (TreeItem<String> child : children) {
                //if (((TreeItemNode) child).isNode) {
                    refreshColorConfiguration((TreeItemNode) child, colorConfiguration);
                //}
            }
        }
    }

    public void removeNode(TreeView<String> treeView) {

        if (this.getParent() != null) {
            TreeItemNode parent = (TreeItemNode) this.getParent();

            parent.getChildren().remove(this);
            treeView.getSelectionModel().select(parent);

            ArrayList<TreeItemNode> nodes = new ArrayList();

            getRecursiveLeaf((TreeItemNode) this, nodes);

            for (TreeItemNode node : nodes) {
                //node.stopTailSchedule();

                if (ApplicationContext.getInstance().containsKey(node.getInformationObject().getFullName())) {
                    WindowTextConsole textComsole = (WindowTextConsole) ApplicationContext.getInstance().get(node.getInformationObject().getFullName());
                    //textComsole.clear();
                }
                //node.hideTextconsole();
            }
        }
    }

    public static void getRecursiveLeaf(TreeItemNode treeItemNode, ArrayList<TreeItemNode> nodes) {

        if (!treeItemNode.isNode) {
            nodes.add(treeItemNode);
        } else {
            ObservableList<TreeItem<String>> children = treeItemNode.getChildren();

            for (TreeItem<String> child : children) {
                if (((TreeItemNode) child).isNode) {
                    getRecursiveLeaf((TreeItemNode) child, nodes);
                } else {
                    nodes.add((TreeItemNode) child);
                }
            }
        }
    }

    public boolean refreshNode(TreeView<String> treeView) {

        if (this.getParent() != null) {
            TreeItemNode parent = (TreeItemNode) this.getParent();

            this.removeNode(treeView);
            parent.addNode(this, treeView);

            ArrayList<TreeItemNode> nodes = new ArrayList();

            getRecursiveLeaf((TreeItemNode) this, nodes);

            for (TreeItemNode node : nodes) {
                String fullname = getFullPath(node);
                node.getInformationObject().setFullName(fullname);
                if (node.getInformationObject().getWindowTextConsole() != null) {                    
                    node.getInformationObject().getWindowTextConsole().setName(fullname);
                }
            }
        }
        return true;
    }

    public boolean isUniqChild(String name) {

        boolean result = true;

        ArrayList<TreeItemNode> groups = new ArrayList();
        ArrayList<TreeItemNode> files = new ArrayList();

        for (TreeItem<String> treeItem : this.getChildren()) {
            if (name.compareTo(treeItem.getValue()) == 0) {
                return false;
            }
        }
        return result;
    }

    public boolean isUniqNode(String name) {

        boolean result = true;

        ArrayList<TreeItemNode> groups = new ArrayList();
        ArrayList<TreeItemNode> files = new ArrayList();

        for (TreeItem treeItem : this.getParent().getChildren()) {

            if (((TreeItemNode) treeItem).isNode) {
                groups.add((TreeItemNode) treeItem);
            } else {
                files.add((TreeItemNode) treeItem);
            }
        }

        if (this.isNode) {
            for (TreeItemNode treeItemNode : groups) {
                if (name.compareTo(treeItemNode.getValue()) == 0) {
                    return false;
                }
            }
        } else {
            for (TreeItemNode treeItemNode : files) {
                if (name.compareTo(treeItemNode.getValue()) == 0) {
                    return false;
                }
            }
        }

        return result;
    }

    public boolean addNode(TreeItemNode newNode, TreeView<String> treeView) {

        int index = 0;

        ArrayList<TreeItemNode> groups = new ArrayList();
        ArrayList<TreeItemNode> files = new ArrayList();

        for (TreeItem treeItem : this.getChildren()) {

            if (((TreeItemNode) treeItem).isNode) {
                groups.add((TreeItemNode) treeItem);
            } else {
                files.add((TreeItemNode) treeItem);
            }
        }

        if (newNode.isNode) {
            for (TreeItemNode treeItemNode : groups) {
                if (newNode.getValue().compareTo(treeItemNode.getValue()) < 0) {
                    break;
                } else if (newNode.getValue().compareTo(treeItemNode.getValue()) == 0) {
                    return false;
                }
                index++;
            }
        } else {
            index = groups.size();
            for (TreeItemNode treeItemNode : files) {
                if (newNode.getValue().compareTo(treeItemNode.getValue()) < 0) {
                    break;
                } else if (newNode.getValue().compareTo(treeItemNode.getValue()) == 0) {
                    return false;
                }
                index++;
            }
        }

        if (this.getParent() != null) {
            this.setGraphic(new ImageView(new Image("folder-horizontal-open.png")));
        }

        this.setExpanded(true);
        this.getChildren().add(index, newNode);
        if (treeView != null) {
            treeView.getSelectionModel().select(newNode);
        }

        newNode.getInformationObject().setFullName(getFullPath(newNode));

        newNode.expandedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {

                BooleanProperty bb = (BooleanProperty) observable;

                TreeItem t = (TreeItem) bb.getBean();

                if (t.getParent() != null && ((TreeItemNode) t).isNode()) {
                    if (t.isExpanded()) {
                        t.setGraphic(new ImageView(new Image("folder-horizontal-open.png")));
                    } else {
                        t.setGraphic(new ImageView(new Image("folder-horizontal.png")));
                    }
                }
            }
        });
        return true;
    }
}
