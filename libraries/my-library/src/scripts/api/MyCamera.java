package scripts.api;

public class MyCamera {

    public static void setZoom(){
        setZoomMethod();
        if (org.tribot.script.sdk.Camera.getZoomPercent() != 0) {
            org.tribot.script.sdk.Camera.setZoomPercent(0);
        }

    }

    public static void setCameraAngle(){
        if (org.tribot.script.sdk.Camera.getAngle() != 100) {
            org.tribot.script.sdk.Camera.setAngle(100);
        }
    }

    public static void setZoomMethod(){
        org.tribot.script.sdk.Camera.setZoomMethod(org.tribot.script.sdk.Camera.ZoomMethod.MOUSE_SCROLL);
    }
    public static void setCameraMoveMethod(){
        if (org.tribot.script.sdk.Camera.getRotationMethod() != org.tribot.script.sdk.Camera.RotationMethod.MOUSE){
            org.tribot.script.sdk.Camera.setRotationMethod(org.tribot.script.sdk.Camera.RotationMethod.MOUSE);
        }
    }
    public static void init(){
        setZoom();
        setCameraAngle();
        setCameraMoveMethod();
    }

}
