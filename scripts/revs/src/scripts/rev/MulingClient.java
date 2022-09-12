package scripts.rev;

import org.tribot.script.sdk.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class MulingClient {

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    public void startConnection(String ip, int port) {
        try {
            socket = new Socket(ip, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            Log.info("Connected to mule!");
        } catch (Exception exception) {
            Log.error("Muling client failed to start", exception);
            // Exception occured
        }
    }


    public String sendMessage(String msg) {
        try {
            out.println(msg);
            return in.readLine();
        } catch (IOException ignored) {
            Log.warn("Was an exception with Muling Client sending message: " + msg);
        }
        return "";
    }

    public void stopConnection() {
        try {

            in.close();
            out.close();
            socket.close();
        } catch (IOException exception) {
            Log.error("Failed stopping Muling Client connection", exception);
        }
    }

    public Socket getSocket() {
        return socket;
    }
}
