package chat.server;

import chat.config.ServerConfig;
import chat.model.AudioPacket;

import java.io.IOException;
import java.net.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class VoiceServer implements Runnable {
    private final ServerConfig config;
    private DatagramSocket socket;
    private boolean running = true;

    private static final int BUFFER_SIZE = 4096;
    private static final Map<Integer, InetSocketAddress> activeVoiceClients = new ConcurrentHashMap<>();

    public VoiceServer() throws SocketException {
        this.config = ServerConfig.getInstance();
        this.socket = new DatagramSocket(config.getUdpPort());
        this.socket.setSoTimeout(20);
    }

    public static void main(String[] args) {
        try {
            VoiceServer server = new VoiceServer();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\nCerrando servidor de voz...");
                server.stop();
            }));

            server.run();

        } catch (SocketException e) {
            System.err.println("Error al iniciar servidor de voz: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        System.out.println("=== Servidor de voz UDP iniciado en puerto " + config.getUdpPort() + " ===");

        byte[] buffer = new byte[BUFFER_SIZE];

        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                byte[] packetData = new byte[packet.getLength()];
                System.arraycopy(packet.getData(), 0, packetData, 0, packet.getLength());

                InetAddress senderAddress = packet.getAddress();
                int senderPort = packet.getPort();

                new Thread(() -> handleAudioPacket(packetData, senderAddress, senderPort)).start();

            } catch (SocketTimeoutException e) {
                // Timeout normal
            } catch (IOException e) {
                if (running) {
                    System.err.println("Error en servidor de voz: " + e.getMessage());
                }
            }
        }

        socket.close();
        System.out.println("Servidor de voz UDP detenido");
    }

    private void handleAudioPacket(byte[] packetData, InetAddress senderAddress, int senderPort) {
        try {
            AudioPacket audioPacket = AudioPacket.fromBytes(packetData);

            int senderId = audioPacket.getSenderId();
            int receiverId = audioPacket.getReceiverId();

            // Registrar o actualizar la dirección del remitente
            InetSocketAddress senderSocketAddress = new InetSocketAddress(senderAddress, senderPort);
            activeVoiceClients.put(senderId, senderSocketAddress);

            System.out.println("[VoiceServer] Paquete de " + senderId + " -> " + receiverId +
                    ", seq: " + audioPacket.getSequenceNumber() +
                    ", datos: " + audioPacket.getDataLength() + " bytes");

            // Si receiverId es 0, es solo un registro, no reenviar
            if (receiverId == 0) {
                System.out.println("[VoiceServer] Cliente " + senderId + " registrado desde " +
                        senderAddress.getHostAddress() + ":" + senderPort);
                return;
            }

            // Reenviar al destinatario
            InetSocketAddress receiverAddress = activeVoiceClients.get(receiverId);
            if (receiverAddress != null) {
                DatagramPacket forwardPacket = new DatagramPacket(
                        packetData,
                        packetData.length,
                        receiverAddress.getAddress(),
                        receiverAddress.getPort()
                );
                socket.send(forwardPacket);
                System.out.println("[VoiceServer] Reenviado a " + receiverId + " en " +
                        receiverAddress.getAddress().getHostAddress() + ":" + receiverAddress.getPort());
            } else {
                System.out.println("[VoiceServer] Destinatario " + receiverId + " no encontrado");
            }

        } catch (IOException e) {
            System.err.println("Error procesando paquete de audio: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void stop() {
        running = false;
    }
}
