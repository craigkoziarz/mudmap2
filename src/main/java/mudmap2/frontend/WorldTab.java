/*  MUD Map (v2) - A tool to create and organize maps for text-based games
 *  Copyright (C) 2014  Neop (email: mneop@web.de)
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, see <http://www.gnu.org/licenses/>.
 */

/*  File description
 *
 *  This class displays a world and the GUI elements that belong to it. It also
 *  processes keyboard commands. The class is supposed to be a tab in Mainwindow.
 *  It reads and writes the world meta (*_meta) files
 */

package mudmap2.frontend;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import mudmap2.Environment;
import mudmap2.backend.Layer;
import mudmap2.backend.Place;
import mudmap2.backend.World;
import mudmap2.backend.WorldCoordinate;
import mudmap2.backend.WorldFileReader.WorldFile;
import mudmap2.backend.WorldFileReader.WorldFileType;
import mudmap2.backend.WorldFileReader.current.WorldFileDefault;
import mudmap2.backend.WorldFileReader.current.WorldFileJSON;
import mudmap2.backend.WorldFileReader.current.WorldMetaJSON;
import mudmap2.backend.WorldManager;
import mudmap2.frontend.GUIElement.WorldPanel.PlaceSelectionListener;
import mudmap2.frontend.GUIElement.ScrollLabel;
import mudmap2.frontend.GUIElement.WorldPanel.MapPainterDefault;
import mudmap2.frontend.GUIElement.WorldPanel.StatusListener;
import mudmap2.frontend.GUIElement.WorldPanel.WorldPanel;
import mudmap2.frontend.sidePanel.LayerPanelListener;
import mudmap2.frontend.sidePanel.PlacePanelListener;
import mudmap2.frontend.sidePanel.SidePanel;
import org.json.JSONArray;
import org.json.JSONObject;
import mudmap2.frontend.GUIElement.WorldPanel.WorldPanelListener;

/**
 * A tab in the main window that displays a world
 *
 * @author neop
 */
public class WorldTab extends JPanel implements LayerPanelListener,PlacePanelListener,StatusListener,WorldPanelListener,WorldMetaJSON {
    private static final long serialVersionUID = 1L;

    JFrame parentFrame;

    String filename;

    // GUI elements
    WorldPanel worldPanel;
    SidePanel sidePanel;
    JSlider sliderZoom;
    JPanel panelSouth;
    ScrollLabel labelInfobar;

    // world_meta file version supported by this WorldTab
    static final int META_FILE_VER_MAJOR = 2;
    static final int META_FILE_VER_MINOR = 0;

    // ============================= Methods ===================================

    /**
     * Constructs the world tab, opens the world if necessary
     * @param world world
     * @param file
     * @param passive world won't be changed, if true
     */
    public WorldTab(JFrame parent, World world, String file, boolean passive){
        parentFrame = parent;
        this.filename = file;
        create(world, passive);
    }

    /**
     * Constructs the world tab, opens the world if necessary
     * @param world world
     * @param passive world won't be changed, if true
     */
    public WorldTab(JFrame parent, World world, boolean passive){
        parentFrame = parent;
        create(world, passive);
    }

    /**
     * Copies a WorldTab and creates a new passive one
     * @param wt
     */
    public WorldTab(WorldTab wt){
        parentFrame = wt.parentFrame;
        createGui(wt.getWorld(), true);

        worldPanel.setTileSize(wt.getWorldPanel().getTileSize());
        worldPanel.setCursorEnabled(wt.getWorldPanel().isCursorEnabled());
        ((MapPainterDefault) worldPanel.getMappainter()).setGridEnabled(((MapPainterDefault) getWorldPanel().getMappainter()).isGridEnabled());
    }

    /**
     * Creates the WorldTab from scratch
     */
    private void create(World world, boolean passive){
        createGui(world, passive);
        readMeta();
    }

    /**
     * Creates the GUI elements
     */
    private void createGui(World world, boolean passive){
        setLayout(new BorderLayout());

        worldPanel = new WorldPanel(parentFrame, world, passive);
        add(worldPanel, BorderLayout.CENTER);
        worldPanel.addTileSiteListener(this);
        worldPanel.addStatusListener(this);
        worldPanel.addPlaceSelectionListener(new PlaceSelectionListener() {
            @Override
            public void placeSelected(Place place) {
                updateInfobar();
            }

            @Override
            public void placeDeselected(Place place) {
                updateInfobar();
            }
        });

        sidePanel = new SidePanel(world);
        add(sidePanel, BorderLayout.EAST);
        sidePanel.addLayerPanelListener(this);
        sidePanel.addPlacePanelListener(this);

        add(panelSouth = new JPanel(), BorderLayout.SOUTH);
        panelSouth.setLayout(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(1, 2, 0, 2);

        // add bottom panel elements
        // previous / next buttons for the history
        JButton button_prev = new JButton("Prev");
        constraints.gridx++;
        panelSouth.add(button_prev, constraints);
        button_prev.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                worldPanel.popPosition();
            }
        });

        JButton button_next = new JButton("Next");
        constraints.gridx++;
        panelSouth.add(button_next, constraints);
        button_next.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                worldPanel.restorePosition();
            }
        });

        constraints.gridx++;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.BOTH;
        panelSouth.add(labelInfobar = new ScrollLabel(), constraints);
        labelInfobar.startThread();

        // set default selected place to hte center place
        worldPanel.setCursor((int) Math.round(worldPanel.getPosition().getX()), (int) Math.round(worldPanel.getPosition().getY()));

        constraints.gridx++;
        constraints.weightx = 0.0;
        constraints.fill = GridBagConstraints.NONE;
        panelSouth.add(new JLabel("Map zoom:"), constraints);
        
        constraints.gridx++;
        sliderZoom = new JSlider(0, 100, (int) (100.0 / WorldPanel.TILE_SIZE_MAX * worldPanel.getTileSize()));
        panelSouth.add(sliderZoom, constraints);
        sliderZoom.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent arg0) {
                worldPanel.setTileSize((int) ((double) worldPanel.TILE_SIZE_MAX * ((JSlider) arg0.getSource()).getValue() / 100.0));
            }
        });
    }

    public WorldPanel getWorldPanel() {
        return worldPanel;
    }

    /**
     * Get the world
     * @return world
     */
    public World getWorld(){
        return getWorldPanel().getWorld();
    }

    /**
     * Get the worlds file name
     * @return
     */
    public String getFilename() {
        return filename;
    }

    /**
     * Set the worlds file name
     * @param filename
     */
    public void setFilename(String filename) {
        this.filename = filename;
    }

    /**
     * Set visibility of the side panel
     * @param b
     */
    public void setSidePanelVisible(Boolean b){
        sidePanel.setVisible(b);
    }

    /**
     * Saves the changes in the world
     */
    public void save(){
        if(!worldPanel.isPassive()){
            WorldFile worldFile = getWorld().getWorldFile();

            if(worldFile == null){
                if(filename == null || filename.isEmpty() || (new File(filename)).exists()){
                    // get new filename
                    int ret;
                    do {
                        JFileChooser fileChooser = new JFileChooser(Environment.getWorldsDir());
                        ret = fileChooser.showSaveDialog(this);
                        if(ret == JFileChooser.APPROVE_OPTION){
                            filename = fileChooser.getSelectedFile().getAbsolutePath();
                        } else {
                            ret = JOptionPane.showConfirmDialog(this,
                                    "No file chosen: " + getWorld().getName()
                                    + " will not be saved!", "Saving world",
                                    JOptionPane.OK_CANCEL_OPTION);
                            if(ret == JOptionPane.OK_OPTION) return;
                        }
                    } while(ret != JFileChooser.APPROVE_OPTION || ret != JOptionPane.OK_OPTION);
                }

                worldFile = new WorldFileDefault(filename);
                getWorld().setWorldFile(worldFile);
            }

            // set meta data writer
            if(worldFile.getWorldFileType() == WorldFileType.JSON){
                WorldFileJSON wfj = null;
                if(worldFile instanceof WorldFileJSON){
                    wfj = (WorldFileJSON) worldFile;
                } else if(worldFile instanceof WorldFileDefault){
                    wfj = (WorldFileJSON) ((WorldFileDefault) worldFile).getWorldFile();
                }
                if(wfj != null) wfj.setMetaGetter(this);
            }

            // write world file
            try {
                worldFile.writeFile(getWorld());
                WorldManager.putWorld(worldFile.getFilename(), getWorld());
            } catch (IOException ex) {
                Logger.getLogger(WorldTab.class.getName()).log(Level.SEVERE, null, ex);
                JOptionPane.showMessageDialog(getParent(),
                        "Could not save world file " + worldFile.getFilename(),
                        "Saving world file",
                        JOptionPane.ERROR_MESSAGE);
            }
        }

        showMessage("World saved");
    }

    /**
     * Show message in infobar
     * @param message
     */
    public void showMessage(String message){
        labelInfobar.showMessage(message);
    }

    // ========================== Place selection ==============================

    /**
     * Updates the infobar
     */
    private void updateInfobar(){
        if(labelInfobar != null){
            if(worldPanel.isCursorEnabled()){
                Layer layer = getWorld().getLayer(worldPanel.getPosition().getLayer());
                if(layer != null && layer.exist(worldPanel.getCursorX(), worldPanel.getCursorY())){
                    Place pl;
                        pl = layer.get(worldPanel.getCursorX(), worldPanel.getCursorY());

                        boolean has_place_group = pl.getPlaceGroup() != null;
                        boolean has_comments = !pl.getComments().isEmpty();

                        String infotext = pl.getName();
                        if(has_place_group || has_comments) infotext += " (";
                        if(has_place_group) infotext += pl.getPlaceGroup().getName();
                        if(has_comments) infotext += (has_place_group ? ", " : "") + pl.getCommentsString(false);
                        if(has_place_group || has_comments) infotext += ")";

                        labelInfobar.setText(infotext);
                } else {
                    labelInfobar.setText("");
                }
            } else labelInfobar.setText("");
        }
    }

    // ========================= selection listener ============================
    @Override
    public void layerSelected(Layer layer) {
        worldPanel.pushPosition(new WorldCoordinate(layer.getId(), layer.getCenterX(), layer.getCenterY()));
        repaint();
    }

    @Override
    public void createLayer() {
        String name = JOptionPane.showInputDialog(this, "Map name", "New map");
        if(name != null) worldPanel.pushPosition(new WorldCoordinate(getWorld().getNewLayer(name).getId(), 0, 0));
        repaint();
    }

    @Override
    public void placeSelected(Place place) {
        worldPanel.pushPosition(place.getCoordinate());
        repaint();
    }

    @Override
    public void messageReceived(String message) {
        showMessage(message);
    }

    @Override
    public void statusUpdate() {
        updateInfobar();
    }

    @Override
    public void TileSizeChanged() {
        sliderZoom.setValue((int) (100.0 / WorldPanel.TILE_SIZE_MAX * worldPanel.getTileSize()));
    }

    @Override
    public void LayerChanged(Layer l) {
        ((SidePanel) sidePanel).getLayerPanel().setActiveLayer(l);
    }
    
    @Override
    public JSONObject getMeta(HashMap<Integer, Integer> layerTranslation){
        JSONObject root = new JSONObject();

        MapPainterDefault mapPainter = (MapPainterDefault) getWorldPanel().getMappainter();
        root.put("showPaths", mapPainter.getShowPaths());
        root.put("pathsCurved", mapPainter.getPathsCurved());
        root.put("showCursor", getWorldPanel().isCursorEnabled());
        root.put("showGrid", mapPainter.isGridEnabled());
        root.put("tileSize", getWorldPanel().getTileSize());

        JSONArray history = new JSONArray();
        root.put("history", history);

        Integer cnt = 0;
        for(WorldCoordinate coord: getWorldPanel().getHistory()){
            if(layerTranslation.containsKey(coord.getLayer())){
                JSONObject el = new JSONObject();
                el.put("l", layerTranslation.get(coord.getLayer()));
                el.put("x", coord.getX());
                el.put("y", coord.getY());
                history.put(el);
            }
            if(++cnt >= 25) break;
        }

        return root;
    }

    public void readMeta(){
        WorldFile worldFile = getWorld().getWorldFile();

        // set meta data writer
        if(worldFile != null && worldFile.getWorldFileType() == WorldFileType.JSON){
            WorldFileJSON wfj = null;
            if(worldFile instanceof WorldFileJSON){
                wfj = (WorldFileJSON) worldFile;
            } else if(worldFile instanceof WorldFileDefault){
                wfj = (WorldFileJSON) ((WorldFileDefault) worldFile).getWorldFile();
            }
            if(wfj != null) setMeta(wfj.getMetaData());
        }
    }

    public void setMeta(JSONObject meta){
        if(meta != null){
            MapPainterDefault mapPainter = (MapPainterDefault) getWorldPanel().getMappainter();
            if(meta.has("showPaths")) mapPainter.setShowPaths(meta.getBoolean("showPaths"));
            if(meta.has("pathsCurved")) mapPainter.setPathsCurved(meta.getBoolean("pathsCurved"));
            if(meta.has("showCursor")) getWorldPanel().setCursorEnabled(meta.getBoolean("showCursor"));
            if(meta.has("showGrid")) mapPainter.setGridEnabled(meta.getBoolean("showGrid"));
            if(meta.has("tileSize")) getWorldPanel().setTileSize(meta.getInt("tileSize"));

            if(meta.has("history")){
                worldPanel.getHistory().clear();

                JSONArray history = meta.getJSONArray("history");
                Integer size = history.length();

                for(Integer i = 0; i < size; ++i){
                    JSONObject histEl = history.getJSONObject(i);
                    if(histEl.has("l") && histEl.has("x") && histEl.has("y")){
                        Integer layer = histEl.getInt("l");
                        Integer x = histEl.getInt("x");
                        Integer y = histEl.getInt("y");
                        worldPanel.getHistory().add(new WorldCoordinate(layer, x, y));
                    }
                }
            }
        }
    }
}