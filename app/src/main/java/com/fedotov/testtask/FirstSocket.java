package com.fedotov.testtask;
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
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Base64;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;


public class FirstSocket extends Thread {
    public ServerSocket serverSocket;
    public final int port = 5555;
    private ExecutorService executor = Executors.newFixedThreadPool(5);
    public FirstSocket() throws IOException {
        this.serverSocket = new ServerSocket(port);
    }

    @Override
    public void run() {
        try {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new monoThreadClientServer(clientSocket).start();
            }
        } catch (SocketException ignored){
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class monoThreadClientServer extends Thread{
    private Socket client;

    public monoThreadClientServer(Socket client) {
        this.client = client;
    }

    @Override
    public void run() {
        try {
            DataOutputStream out = new DataOutputStream(client.getOutputStream());
            DataInputStream in = new DataInputStream(client.getInputStream());

            byte[] buffToIn = new byte[in.available()];
            in.readFully(buffToIn);

            byte[] xml = new byte[buffToIn.length-4];
            System.arraycopy(buffToIn, 4, xml, 0, buffToIn.length - 4);
            String xmlFile = new String(xml);
            xmlFile = addSignature(xmlFile);
            out.write(xmlFile.getBytes());
            out.flush();
            client.close();
        } catch (IOException | InvalidKeyException | NoSuchAlgorithmException |
                ParserConfigurationException | SignatureException | SAXException |
                TransformerException e) {
            e.printStackTrace();
        }

    }

    private String addSignature(String str) throws ParserConfigurationException, IOException, SAXException, NoSuchAlgorithmException, InvalidKeyException, SignatureException, TransformerException {
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

    private String getSignature(String str) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature signature = Signature.getInstance("SHA512WithRSA");
        signature.initSign(MainActivity.getKeyPair().getPrivate());
        byte[] data = str.getBytes();
        signature.update(data);
        return Base64.getEncoder().encodeToString(signature.sign());
    }
}
