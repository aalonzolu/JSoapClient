package com.io.java.SoapTest;




import org.json.JSONObject;
import gt.io.java.SoapClient.SoapClient;

public class ExampleClient {

    public JSONObject testSoap() {

        // new soap client
        SoapClient SOAP = new SoapClient("http://www.webservicex.com/currencyconvertor.asmx","x");
        // set action
        SOAP.setAction("ConversionRate","www","http://www.webserviceX.NET/","ConversionRateResponse");

        // raw soap body
        String RawBody = "<FromCurrency>GTQ</FromCurrency>\n" +
                "            <ToCurrency>USD</ToCurrency>";

        // set raw body
        SOAP.setRawSoapbody(RawBody);

        // SOAP Security Header (optional)
//        SOAP.SetSecurityHeader(1,"PasswordText","MyUsername","MyPassword","base64password","2017-11-30T14:21:00.000Z");

        JSONObject response = SOAP.Send();

        return response;
    }
}

