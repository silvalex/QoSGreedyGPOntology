package gp;

import org.apache.log4j.Level;

/**
 * Custom level class for specifying the
 * logging tag text.
 * 
 * @author Alex
 */
class CustomLevel extends Level {
    public CustomLevel(String name) {
        super(0,name,0);
    }
}
