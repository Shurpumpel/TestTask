package com.fedotov.testtask;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Parcelable;

import androidx.annotation.Nullable;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class SecondSocket extends Service {
    public static int port = 5556;

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
        CheckSignatureThread checkSignatureThread = new CheckSignatureThread();
        checkSignatureThread.start();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    class CheckSignatureThread extends Thread{
        @Override
        public void run() {
            try {
                ServerSocket serverSocket = new ServerSocket(port);
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    try {
                        DataInputStream in = new DataInputStream(clientSocket.getInputStream());

                        byte[] dataSize = new byte[4];
                        for (int i = 0; i < 4; i++) {
                            dataSize[i] = in.readByte();
                        }

                        ByteBuffer wrapped = ByteBuffer.wrap(dataSize);
                        int num = wrapped.getInt();

                        byte[] data = new byte[num];
                        for (int i = 0; i < num; i++) {
                            data[i] = in.readByte();
                        }

                        String xmlFile = new String(data);
                        showListOfTags(check(xmlFile));
                    }catch (IOException | ParserConfigurationException | SAXException | NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException e){
                        e.printStackTrace();
                    }

                    clientSocket.close();
                }
            }catch (SocketException ignored){

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private List<TagWithExpression> check(String xml) throws ParserConfigurationException, IOException, SAXException,
            NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException, BadPaddingException, IllegalBlockSizeException {
        List<TagWithExpression> list = new ArrayList<>();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new InputSource(new StringReader(xml)));
        NodeList blockElements = document.getElementsByTagName("block");
        for (int i = 0; i < blockElements.getLength(); i++) {
            Node block = blockElements.item(i);
            if(block.getAttributes().getNamedItem("signature") == null) {
                list.add(new TagWithExpression(block.getAttributes().getNamedItem("name").getTextContent(), " подпись отутствует"));
                continue;
            }
            boolean isSignatureCorrect;
            try {
                String sign = block.getAttributes().getNamedItem("signature").getTextContent();
                byte[] digest = getHashWithSHA512(block.getTextContent().getBytes());
                isSignatureCorrect = checkSignature(sign, digest);


            }catch (IllegalArgumentException e){
                isSignatureCorrect = false;
            }
            if(isSignatureCorrect)
                list.add(
                        new TagWithExpression(block.getAttributes().getNamedItem("name").getTextContent(),
                                " подпись правильная"));
            else
                list.add(
                        new TagWithExpression(block.getAttributes().getNamedItem("name").getTextContent(),
                                " подпись неправильная"));
        }
        return list;
    }

    public static boolean checkSignature(String signature, byte[] digest) throws NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException, IllegalBlockSizeException, InvalidKeyException {
        KeyPair keyPair = MainActivity.getKeyPair();
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, keyPair.getPublic());
        byte[] newData = stringHexTobyteArray(signature);
        byte[] result = cipher.doFinal(newData);
        return Arrays.toString(digest).equals(Arrays.toString(result));
    }

    public static byte[] getHashWithSHA512(byte[] input) throws NoSuchAlgorithmException {
        MessageDigest digester = MessageDigest.getInstance("SHA-512");
        return digester.digest(input);
    }

    public static byte[] stringHexTobyteArray(String str){
        int len = str.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(str.charAt(i), 16) << 4)
                    + Character.digit(str.charAt(i+1), 16));
        }
        return data;
    }

    private void showListOfTags(List<TagWithExpression> list){
        Intent intent = new Intent(MainActivity.currentContext, listBlocks.class);
        intent.putExtra(Intent.EXTRA_TEXT, (Serializable) list);

        MainActivity.currentContext.startActivity(intent);
    }

}
