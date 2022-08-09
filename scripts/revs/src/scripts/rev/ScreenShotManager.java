package scripts.rev;

import org.tribot.script.sdk.Screenshot;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.Date;

public class ScreenShotManager {

    public static File takeScreenShotAndSave(){
        var outputFile = new File("C:\\Users\\Administrator\\AppData\\Roaming\\.tribot\\Deniz\\onstart\\" + new Date().getTime() + ".png");

        try {
            ImageIO.write(Screenshot.captureWithPaint(), "png", outputFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return outputFile;
    }
}
