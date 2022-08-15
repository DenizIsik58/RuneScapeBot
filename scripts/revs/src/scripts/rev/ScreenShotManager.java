package scripts.rev;

import org.tribot.script.sdk.Log;
import org.tribot.script.sdk.MyPlayer;
import org.tribot.script.sdk.Screenshot;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.Date;

public class ScreenShotManager {

    public static File takeScreenShotAndSave(String path){
        var outputFile = new File("C:\\Users\\Administrator\\AppData\\Roaming\\.tribot\\Deniz\\" + path + "\\" + new Date().getTime() + MyPlayer.getUsername() +  ".png");

        try {
            ImageIO.write(Screenshot.captureWithPaint(), "png", outputFile);
        } catch (IOException e) {
            Log.error(e);
            e.printStackTrace();
        }
        return outputFile;
    }
}
