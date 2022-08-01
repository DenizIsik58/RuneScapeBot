package scripts.rev;

import org.tribot.script.sdk.Camera;

public class CameraManager {

    public static void setZoom(){
        setZoomMethod();
        if (Camera.getZoomPercent() != 0) {
            Camera.setZoomPercent(0);
        }

    }

    public static void setCameraAngle(){
        if (Camera.getAngle() != 100) {
            Camera.setAngle(100);
        }
    }

    public static void setZoomMethod(){
        Camera.setZoomMethod(Camera.ZoomMethod.MOUSE_SCROLL);
    }
    public static void setCameraMoveMethod(){
        if (Camera.getRotationMethod() != Camera.RotationMethod.MOUSE){
            Camera.setRotationMethod(Camera.RotationMethod.MOUSE);
        }
    }
    public static void init(){
        setZoom();
        setCameraAngle();
        setCameraMoveMethod();
    }
}
