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
 *  This class staticly provides file path information
 */

package mudmap2;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import static java.lang.Math.max;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Helper class to get common paths and filenames
 * @author neop
 */
public class Environment {

    static String user_data_dir;
    public final static String website_url = "http://mudmap.sf.net";
    public final static String manual_url = website_url;

    public static final String github_url = "https://github.com/Neop/mudmap2";
    public static final String sourceforge_url = "http://sf.net/p/mudmap";

    /**
     * Gets the user data path
     * @return user data path
     */
    public static String getUserDataDir(){
        if(user_data_dir == null || user_data_dir.isEmpty()){
            // read the user data path from environment variables
            // operating system Windows
            if(System.getProperty("os.name").toLowerCase().contains("win"))
                user_data_dir = System.getenv().get("APPDATA") + File.separator + "mudmap" + File.separator;
            // other operating systems
            else user_data_dir = System.getProperty("user.home") + File.separator + ".mudmap" + File.separator;
        }
        return user_data_dir;
    }

    /**
     * Gets the directory that contains the world files
     * @return worlds directory
     */
    public static String getWorldsDir(){
        return getUserDataDir() + "worlds" + File.separator;
    }

    /**
     * Gets the available worlds file (path + filename)
     * @return available worlds file
     */
    public static String getAvailableWorldsFile(){
        return getWorldsDir() + "worlds";
    }

    /**
     * Gets the config file path
     * @return
     */
    public static String getConfigFile(){
        return getUserDataDir() + "config";
    }

    /**
     * Tries to open the url in a web-browser
     * @param url
     */
    public static void openWebsite(String url){
        try {
            Desktop.getDesktop().browse(URI.create(url));
        } catch (IOException ex) {
            Logger.getLogger(Environment.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * checks if path is a directory
     * @param path
     * @return true, if path is a directory
     */
    public static boolean isDirectory(String path){
        File f = new File(path);
        return f.exists() && f.isDirectory();
    }

    /**
     * Creates a directory
     * @param path
     */
    public static void createDirectory(String path){
        Integer sep = path.lastIndexOf(File.separator);
        sep = max(sep, path.lastIndexOf('/'));
        if(sep > 0) createDirectory(path.substring(0, sep));

        File f = new File(path);
        if(!f.exists()) f.mkdir();
    }
}
