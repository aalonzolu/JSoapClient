package gt.io.java.SoapClient;


import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.json.JSONObject;
import org.json.XML;




public class SoapClient {
    private String uri = "";
    private String actionResponse = "";
    private String soap_prefix = "";


    private String soap_header = "";
    private String soap_body = "";

    private String action_prefix = "";
    private String action_prefix_url = "";
    private String soap_action = "";


    private String soap_namespace_prefix = "xmlns";

    private static HashMap<String,String> mHeaders = new HashMap<>();

    public SoapClient(String Enpoint, String SoapPrefix){
            mHeaders.put("Content-Type", "text/xml; charset=utf-8");
            mHeaders.put("SOAPAction", Enpoint);
        if(Enpoint != null){
            this.uri = Enpoint;
        }
        if(SoapPrefix != null){
            this.soap_prefix = SoapPrefix;
        }
    }

    public void setAction (String Action, String actionPrefix, String actionURI, String ResponseAction){
        this.action_prefix = actionPrefix;
        this.action_prefix_url = actionURI;
        this.soap_action = Action;
        this.actionResponse = ResponseAction;
    }

    public JSONObject Send(){

        String actionNamespace = "";
        String body_action = "";
        if(!this.action_prefix.equals("") && !this.action_prefix_url.equals("")){
            actionNamespace = soap_namespace_prefix+":"+this.action_prefix+"=\""+action_prefix_url+"\" ";
            body_action = "<"+this.action_prefix+":"+this.soap_action+">\n"+
                    this.soap_body+
                    "</"+this.action_prefix+":"+this.soap_action+">\n";
        }else {
            body_action = "<"+this.soap_action+">\n"+
                    this.soap_body+
                    "</"+this.soap_action+">\n";
        }
        String xmlstring= "<"+this.soap_prefix+":Envelope "+soap_namespace_prefix+":"+this.soap_prefix+"=\"http://schemas.xmlsoap.org/soap/envelope/\" "+actionNamespace+">\n" +
                "<"+this.soap_prefix+":Header>\n" + this.soap_header+ "\n</"+this.soap_prefix+":Header>\n" +
                "<"+this.soap_prefix+":Body>\n"+
                body_action +"\n"+
                "\n</"+this.soap_prefix+":Body>\n" +
                "</"+this.soap_prefix+":Envelope>";

        System.out.println(xmlstring);
        StringBuffer chaine = new StringBuffer("");

        HttpURLConnection connection = null;
        InputStream inputStream = null;
        int status=0;
        String responsebody = "";
        JSONObject jsonResponse = new JSONObject();
        try {
            URL url = new URL(this.uri);
            connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Length", xmlstring.getBytes().length + "");

            for(Map.Entry<String, String> entry : mHeaders.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                connection.setRequestProperty(key,value);

            }
            connection.setRequestMethod("POST");
            connection.setDoInput(true);

            OutputStream outputStream = connection.getOutputStream();
            outputStream.write(xmlstring.getBytes("UTF-8"));
            outputStream.close();

            connection.connect();

            inputStream = connection.getInputStream();

            // create a new DocumentBuilderFactory
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

            // use the factory to create a documentbuilder
            DocumentBuilder builder = factory.newDocumentBuilder();

            Document doc = builder.parse(inputStream);
            responsebody = DoctoString(doc);
            String soapString = responsebody;

            MessageFactory messageFactory = MessageFactory.newInstance();
            ByteArrayInputStream soapStringStream = new ByteArrayInputStream(soapString.getBytes(Charset.forName("UTF-8")));
            SOAPMessage soapMessage = messageFactory.createMessage(new MimeHeaders(), soapStringStream);
            Document bodyDoc = soapMessage.getSOAPBody().extractContentAsDocument();

            Element root = bodyDoc.getDocumentElement();
            bodyDoc.renameNode(root, root.getNamespaceURI(), this.actionResponse);

            responsebody = DoctoString(bodyDoc);

            jsonResponse = XML.toJSONObject(responsebody);

            jsonResponse.put("response_code",connection.getResponseCode());
        }catch(Exception e){
            jsonResponse.put("response_code",0);
            jsonResponse.put("response_message",e.getMessage());
        }
        return jsonResponse;
    }

    public  void SetSecurityHeader(Integer mustUnderstand, String passwordType, String username,String password,String Nonce,String Created) {
        String xml = "<wsse:Security soapenv:mustUnderstand=\"" + mustUnderstand + "\" "+soap_namespace_prefix+":wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\" "+soap_namespace_prefix+":wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\">\n" +
                "         <wsse:UsernameToken wsu:Id=\"UsernameToken-526CE4F4947942C59C13984432439524\">\n" +
                "            <wsse:Username>" + username + "</wsse:Username>\n" +
                "            <wsse:Password Type=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#" + passwordType + "\">" + password + "</wsse:Password>\n" +
                "            <wsse:Nonce EncodingType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary\">" + Nonce + "</wsse:Nonce>\n" +
                "            <wsu:Created>"+Created+"</wsu:Created>\n" +
                "         </wsse:UsernameToken>\n" +
                "      </wsse:Security>";
        this.soap_header += xml;
    }


    public static String DoctoString(Document doc) {
        try {
            StringWriter sw = new StringWriter();
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

            transformer.transform(new DOMSource(doc), new StreamResult(sw));
            String XMLS = sw.toString();

            System.out.println(XMLS);

            return XMLS;
        } catch (Exception ex) {
            throw new RuntimeException("Error converting to String", ex);
        }
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getActionResponse() {
        return actionResponse;
    }

    public void setActionResponse(String actionResponse) {
        this.actionResponse = actionResponse;
    }

    public String getSoapprefix() {
        return soap_prefix;
    }

    public void setSoapprefix(String soap_prefix) {
        this.soap_prefix = soap_prefix;
    }

    public String getSoapeader() {
        return soap_header;
    }

    public void setRawSoapheader(String soap_header) {
        this.soap_header = soap_header;
    }

    public String getSoapbody() {
        return soap_body;
    }

    public void setRawSoapbody(String soap_body) {
        this.soap_body = soap_body;
    }


}

