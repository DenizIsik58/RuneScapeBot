package scripts;

import lombok.SneakyThrows;
import org.tribot.script.sdk.Log;
import org.tribot.script.sdk.MyPlayer;
import org.tribot.script.sdk.Waiting;
import org.tribot.script.sdk.WorldHopper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/*public class MultiServerSocket implements Runnable{

    private ServerSocket serverSocket =  new ServerSocket(6667);
    private static List<String> names = new ArrayList<>();
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;

    public MultiServerSocket() throws IOException {
    }


    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    @SneakyThrows
    public void run() {
        while (true){
            Log.info("Accepting connections");
            clientSocket = serverSocket.accept();


            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));


            while ((in.readLine()) != null) {
                Log.info(in.readLine());

                out.println("hello");
                //names.add();
                MulerScript.setState(MulerState.MULING);
                Log.info("I read your message");
            }
            Waiting.wait(50);
        }
    }


    public static List<String> getNames() {
        return names;
    }
*/

public class MultiServerSocket implements Runnable {
    private ServerSocket serverSocket;
    private static List<String> names = new ArrayList<>();

    @SneakyThrows
    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(6668, 32, InetAddress.getByName("localhost"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        while (true) {
            //Log.info("Accepting connections");
            try {
                new EchoClientHandler(serverSocket.accept()).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Waiting.wait(50);
        }
    }

    public void stop() throws IOException {
        serverSocket.close();
    }


    public static class EchoClientHandler extends Thread {
        private Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;

        public EchoClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @SneakyThrows
        public void run() {
            try {
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(
                        new InputStreamReader(clientSocket.getInputStream()));

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    //Log.info(inputLine);
                    if (inputLine.contains("I want to mule!")) {
                        try {
                            var content = inputLine.split(" ");
                            String name = null;
                            if (content.length == 5) {
                                name = content[4];
                            } else if (content.length == 6) {
                                name = content[4] + " " + content[5];
                            } else if (content.length == 7) {
                                name = content[4] + " " + content[5] + " " + content[6];
                            } else if (content.length == 8) {
                                name = content[4] + " " + content[5] + " " + content[6] + " " + content[7];
                            }
                            names.add(name);
                            MulerScript.setState(MulerState.MULING);
                            out.println(MyPlayer.getTile().getX() + " " + MyPlayer.getTile().getY() + " " + MyPlayer.getTile().getPlane() + " " + MyPlayer.get().get().getName() + " " + WorldHopper.getCurrentWorld());
                            out.println();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

                in.close();
                out.close();
                clientSocket.close();
            } catch (Exception e) {
                Log.info("Exception occured");
            }
        }
    }

    public static List<String> getNames() {
        return names;
    }
}
