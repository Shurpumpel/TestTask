package com.fedotov.testtask;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;


public class FirstSocket extends Service {
    public static final int port = 5555;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("myLogs", "onStartCommand");
        ConnectionMaker connectionMaker = new ConnectionMaker();
        connectionMaker.start();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.d("myLogs", "onDestroy");
        super.onDestroy();
    }

    static class ConnectionMaker extends Thread{
        @Override
        public void run() {
            super.run();
            try {
                ServerSocket serverSocket = new ServerSocket(port);
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    new monoThreadClientServer(clientSocket).start();
                }
            } catch (SocketException ignored){
            } catch (IOException e) {
                Log.d("myLogs", "Connection maker error");
                e.printStackTrace();
            }
        }
    }
}



class monoThreadClientServer extends Thread{
    private final Socket client;

    public monoThreadClientServer(Socket client) {
        this.client = client;
    }

    @Override
    public void run() {
        try {
            DataOutputStream out = new DataOutputStream(client.getOutputStream());
            DataInputStream in = new DataInputStream(client.getInputStream());

            byte[] dataSize = new byte[4];
            for (int i = 0; i < 4; i++) {
                dataSize[i] = in.readByte();
            }

            ByteBuffer wrapped = ByteBuffer.wrap(dataSize); // big-endian by default
            int num = wrapped.getInt();

            byte[] data = new byte[num];
            for (int i = 0; i < num; i++) {
                data[i] = in.readByte();
            }

            String xmlFile = new String(data);
            xmlFile = addSignature(xmlFile);

            byte[] outDataSize = ByteBuffer.allocate(4).putInt(xmlFile.length()).array();
            out.write(ByteBuffer.allocate(4).putInt(xmlFile.length()).array());
            out.write(xmlFile.getBytes());
            out.flush();
            client.close();
        } catch (IOException | InvalidKeyException |
                NoSuchAlgorithmException | ParserConfigurationException |
                SAXException | TransformerException | NoSuchPaddingException |
                IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
        }

    }

    private String addSignature(String str) throws ParserConfigurationException, IOException, SAXException, NoSuchAlgorithmException, InvalidKeyException, TransformerException, NoSuchPaddingException, BadPaddingException, IllegalBlockSizeException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new InputSource(new StringReader(str)));
        NodeList blockElements = document.getElementsByTagName("block");

        for (int i = 0; i < blockElements.getLength(); i++) {
            Node block = blockElements.item(i);
            if(!(block.getAttributes().getNamedItem("signature")==null))
                continue;
            String sign = getSignature(block.getTextContent());
            ((Element) block).setAttribute("signature", sign);
        }

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        StringWriter sw = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(sw));

        return sw.toString();
    }


    public String getSignature(String text) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        byte[] hash = getHashWithSHA512(text.getBytes());
        Cipher cipher = Cipher.getInstance("RSA");
        KeyPair keyPair = MainActivity.getKeyPair();
        cipher.init(Cipher.ENCRYPT_MODE, keyPair.getPrivate());
        byte[] dataEncoded = cipher.doFinal(hash);
        return byteArrayToHexString(dataEncoded);
    }


    public byte[] getHashWithSHA512(byte[] input) throws NoSuchAlgorithmException {
        MessageDigest digester = MessageDigest.getInstance("SHA-512");
        byte[] digest = digester.digest(input);
        return digest;
    }

    public String byteArrayToHexString(byte[] input){
        StringBuilder result = new StringBuilder();
        for (byte aByte : input) {
            result.append(String.format("%02x", aByte));
        }
        return result.toString();
    }

}
