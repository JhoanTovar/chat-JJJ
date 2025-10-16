package chat.client;

import chat.audio.AudioCapture;
import chat.audio.AudioPlayback;
import chat.model.AudioPacket;

import javax.sound.sampled.LineUnavailableException;
import java.io.IOException;
import java.net.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class VoiceClient {
    private static final String SERVER_HOST = "localhost";
    private static final int UDP_PORT = 5001;
    private static final int BUFFER_SIZE = 4096;

    private DatagramSocket socket;
    private InetAddress serverAddress;
    private boolean inCall = false;
    private Thread sendThread;
    private Thread listenerThread;
    private boolean running = true;

    private AudioCapture audioCapture;
    private AudioPlayback audioPlayback;

    private int userId;
    private int otherUserId;
    private int sequenceNumber = 0;

    private ConcurrentLinkedQueue<byte[]> voiceNoteQueue = new ConcurrentLinkedQueue<>();
    private Thread voiceNotePlayerThread;

    public VoiceClient(int userId) throws SocketException, UnknownHostException {
        this.userId = userId;
        this.socket = new DatagramSocket();
        this.serverAddress = InetAddress.getByName(SERVER_HOST);
        this.audioCapture = new AudioCapture();
        this.audioPlayback = new AudioPlayback();

        registerWithServer();

        startBackgroundListener();
        startVoiceNotePlayer();
    }

    private void registerWithServer() {
        try {
            // Enviar paquete de registro (con datos vacíos)
            AudioPacket registerPacket = new AudioPacket(userId, 0, 0, new byte[0]);
            byte[] packetData = registerPacket.toBytes();

            DatagramPacket packet = new DatagramPacket(
                    packetData,
                    packetData.length,
                    serverAddress,
                    UDP_PORT
            );
            socket.send(packet);
            System.out.println("[VoiceClient] Registrado con servidor UDP");
        } catch (IOException e) {
            System.err.println("Error registrando con servidor UDP: " + e.getMessage());
        }
    }

    private void startBackgroundListener() {
        listenerThread = new Thread(() -> {
            byte[] buffer = new byte[BUFFER_SIZE];

            while (running) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.setSoTimeout(100);
                    socket.receive(packet);

                    byte[] receivedData = new byte[packet.getLength()];
                    System.arraycopy(packet.getData(), 0, receivedData, 0, packet.getLength());

                    AudioPacket audioPacket = AudioPacket.fromBytes(receivedData);

                    System.out.println("[VoiceClient] Paquete recibido de usuario " + audioPacket.getSenderId() +
                            ", seq: " + audioPacket.getSequenceNumber() +
                            ", datos: " + audioPacket.getDataLength() + " bytes");

                    // Si estamos en llamada, reproducir inmediatamente
                    if (inCall && audioPacket.getAudioData() != null && audioPacket.getAudioData().length > 0) {
                        if (!audioPlayback.isPlaying()) {
                            audioPlayback.startPlayback();
                        }
                        audioPlayback.playChunk(audioPacket.getAudioData());
                    }
                    // Si no estamos en llamada, es una nota de voz - agregar a cola
                    else if (!inCall && audioPacket.getAudioData() != null && audioPacket.getAudioData().length > 0) {
                        System.out.println("[VoiceClient] Nota de voz agregada a cola");
                        voiceNoteQueue.offer(audioPacket.getAudioData());
                    }

                } catch (SocketTimeoutException e) {
                    // Timeout normal
                } catch (IOException e) {
                    if (running) {
                        System.err.println("Error en listener de audio: " + e.getMessage());
                    }
                } catch (LineUnavailableException e) {
                    System.err.println("Error iniciando reproducción: " + e.getMessage());
                }
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private void startVoiceNotePlayer() {
        voiceNotePlayerThread = new Thread(() -> {
            while (running) {
                try {
                    if (!voiceNoteQueue.isEmpty() && !inCall) {
                        System.out.println("\n=== Reproduciendo nota de voz recibida ===");

                        // Iniciar reproducción
                        audioPlayback.startPlayback();

                        // Reproducir todos los chunks de la nota de voz
                        int chunksPlayed = 0;
                        while (!voiceNoteQueue.isEmpty()) {
                            byte[] chunk = voiceNoteQueue.poll();
                            if (chunk != null) {
                                audioPlayback.playChunk(chunk);
                                chunksPlayed++;
                            }
                        }

                        // Detener reproducción
                        audioPlayback.stopPlayback();
                        System.out.println("=== Nota de voz reproducida (" + chunksPlayed + " chunks) ===\n");
                    }

                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    break;
                } catch (LineUnavailableException e) {
                    System.err.println("Error reproduciendo nota de voz: " + e.getMessage());
                }
            }
        });
        voiceNotePlayerThread.setDaemon(true);
        voiceNotePlayerThread.start();
    }

    public void startCall(int otherUserId) {
        this.otherUserId = otherUserId;
        this.inCall = true;
        this.sequenceNumber = 0;

        // Limpiar cola de notas de voz
        voiceNoteQueue.clear();

        try {
            audioCapture.startCapture();
            audioPlayback.startPlayback();
        } catch (LineUnavailableException e) {
            System.err.println("Error iniciando dispositivos de audio: " + e.getMessage());
            inCall = false;
            return;
        }

        sendThread = new Thread(this::sendAudio);
        sendThread.start();

        System.out.println("=== Llamada iniciada - Hable ahora ===");
    }

    private void sendAudio() {
        while (inCall) {
            try {
                byte[] audioData = audioCapture.captureChunk();

                if (audioData.length > 0) {
                    AudioPacket audioPacket = new AudioPacket(userId, otherUserId, sequenceNumber++, audioData);
                    byte[] packetData = audioPacket.toBytes();

                    DatagramPacket packet = new DatagramPacket(
                            packetData,
                            packetData.length,
                            serverAddress,
                            UDP_PORT
                    );
                    socket.send(packet);

                    if (sequenceNumber % 50 == 0) {
                        System.out.println("[VoiceClient] Enviados " + sequenceNumber + " paquetes de audio");
                    }
                }

                Thread.sleep(20);

            } catch (IOException e) {
                if (inCall) {
                    System.err.println("Error enviando audio: " + e.getMessage());
                }
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    public void sendVoiceNote(int receiverId, int durationSeconds) {
        System.out.println("=== Grabando nota de voz WAV por " + durationSeconds + " segundos ===");
        System.out.println("Hable ahora...");

        try {
            byte[] recordedAudio = audioCapture.recordForDuration(durationSeconds * 1000);

            System.out.println("Enviando nota de voz (" + recordedAudio.length + " bytes)...");

            int chunkSize = 1024;
            int offset = 0;
            int seq = 0;

            while (offset < recordedAudio.length) {
                int length = Math.min(chunkSize, recordedAudio.length - offset);
                byte[] chunk = new byte[length];
                System.arraycopy(recordedAudio, offset, chunk, 0, length);

                AudioPacket audioPacket = new AudioPacket(userId, receiverId, seq++, chunk);
                byte[] packetData = audioPacket.toBytes();

                DatagramPacket packet = new DatagramPacket(
                        packetData,
                        packetData.length,
                        serverAddress,
                        UDP_PORT
                );
                socket.send(packet);

                offset += length;
                Thread.sleep(20);
            }

            System.out.println("=== Nota de voz enviada exitosamente (" + seq + " paquetes) ===");

        } catch (LineUnavailableException e) {
            System.err.println("Error accediendo al microfono: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error enviando nota de voz: " + e.getMessage());
        } catch (InterruptedException e) {
            System.err.println("Envio de nota de voz interrumpido");
        }
    }

    public void endCall() {
        inCall = false;

        audioCapture.stopCapture();
        audioPlayback.stopPlayback();

        if (sendThread != null) {
            sendThread.interrupt();
        }

        System.out.println("=== Llamada finalizada ===");
    }

    public void close() {
        running = false;
        endCall();

        if (listenerThread != null) {
            listenerThread.interrupt();
        }
        if (voiceNotePlayerThread != null) {
            voiceNotePlayerThread.interrupt();
        }

        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    public boolean isInCall() {
        return inCall;
    }
}
