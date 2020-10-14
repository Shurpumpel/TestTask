package com.fedotov.testtask;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Window;
import android.widget.TextView;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class SecondSocket extends Thread{
    private Context context;
    public ServerSocket serverSocket;
    public SecondSocket(Context context) throws IOException {
        this.context = context;
        this.serverSocket = new ServerSocket(5556);
    }

    @Override
    public void run() {
        try {
            while (true) {
                Socket clientSocket = serverSocket.accept();

                try {
                    DataInputStream in = new DataInputStream(clientSocket.getInputStream());

                    byte[] buffToIn = new byte[in.available()];
                    in.readFully(buffToIn);
                    byte[] xml = new byte[buffToIn.length - 4];
                    System.arraycopy(buffToIn, 4, xml, 0, buffToIn.length - 4);
                    String xmlFile = new String(xml);

                    showDialog(check(xmlFile));


                }catch (IOException | ParserConfigurationException |
                        SAXException | NoSuchAlgorithmException |
                        InvalidKeyException | SignatureException e){
                    e.printStackTrace();
                }

                clientSocket.close();
            }
        }catch (SocketException ignored){

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<String> check(String xml) throws ParserConfigurationException, IOException, SAXException,
            NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        List<String> list = new ArrayList<>();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new InputSource(new StringReader(xml)));
        NodeList blockElements = document.getElementsByTagName("block");
        for (int i = 0; i < blockElements.getLength(); i++) {
            Node block = blockElements.item(i);
            if(block.getAttributes().getNamedItem("signature") == null) {
                list.add(block.getAttributes().getNamedItem("name").getTextContent() + " == подпись отутствует");
                continue;
            }
            boolean flag;
            try {
                String sign = block.getAttributes().getNamedItem("signature").getTextContent();
                flag = verifySignature(block.getTextContent().getBytes(), Base64.getDecoder().decode(sign));
            }catch (IllegalArgumentException e){
                flag = false;
            }
            if(flag)
                list.add(block.getAttributes().getNamedItem("name").getTextContent() +  " == подпись правильная");
            else
                list.add(block.getAttributes().getNamedItem("name").getTextContent() + " == подпись неправильная");
        }
        return list;
    }

    private boolean verifySignature(byte[] data, byte[] signat) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature signature = Signature.getInstance("SHA512WithRSA");
        signature.initVerify(MainActivity.getKeyPair().getPublic());
        signature.update(data);
        return signature.verify(signat);
    }

    private void showDialog(List<String> list){
        StringBuilder str = new StringBuilder();
        for(String s : list){
            str.append(s);
            str.append("\n\n");
        }
        Intent intent = new Intent(this.context, listBlocks.class);
        intent.putExtra(Intent.EXTRA_TEXT, str.toString());

        context.startActivity(intent);
    }

}
