package pagerank;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Utils {
    public static String reljoin(String path) {
        final String classFilePath = Utils.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        final Path joined = Paths.get(classFilePath, path);
        
        return joined.toString();
    }
}
