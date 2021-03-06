package ke.co.am.ISOIntegrator;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import ke.co.ars.entity.TrxRequest;
import ke.co.ars.entity.TrxResponse;
import ke.co.am.ISOIntegrator.AmCardToAM;
import ke.co.am.ISOIntegrator.AmIsoParser;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
//import org.jpos.util.*;
import org.jpos.iso.ISOException;
import org.jpos.iso.channel.ASCIIChannel;
import org.jpos.iso.packager.GenericPackager;
import org.jpos.iso.ISOMsg;


public class AmCardToAM {
	
    /* Get actual class name to be printed on */
    static Logger log = Logger.getLogger(AmCardToAM.class.getName());
	
	public TrxResponse CardToAMRequest (TrxRequest request) throws ISOException, IOException {
	    
	  //PropertiesConfigurator is used to configure logger from properties file
        PropertyConfigurator.configure("/opt/log4j/amlog4j.properties");
        
        log.info("Recieved Card To AM request.....");
        
	    String msisdn = request.getMsisdn();
	    
	    String AMOUNT = request.getAmount(); 
	    
//	    String transactionID = request.getTransactionID();
	    
        String InstitutionCode = request.getInstitutionCode();
        
//	    String merchantCode = request.getMerchantCode();
	    
//	    String currencyCode = request.getCurrencyCode();
	    
        int timeout = request.getTimeout();
        
        String serverIP = request.getISOServerIP();
        
        int serverPort = request.getISOServerPort();
	    
	    String PROCESSING_CODE = "21";
	    
	    String cardAcceptorTerminalID = "MMMOBILE";
	    
	    String echoData = "Card2Mobile";
	    
	    TrxResponse responseMsg = new TrxResponse();
	    
//        Logger logger = new Logger();
//        logger.addListener (new SimpleLogListener(System.out));
 
        ASCIIChannel channel = new ASCIIChannel(
                serverIP, serverPort, new GenericPackager("opt/ISO/iso87ascii.xml")
        );
 
//        ((LogSource)channel).setLogger (logger, "test-channel");

        
		SimpleDateFormat transactionTime = new SimpleDateFormat("hhmmss");
		SimpleDateFormat transmissionDate = new SimpleDateFormat("MMddhhmmss");
		SimpleDateFormat transactionMonthDay = new SimpleDateFormat("MMdd");
		Date date = new Date();

		String pc = PROCESSING_CODE + "000000000000";
		pc = pc.substring(0,6);
		
		int stan = (int)Math.floor( Math.random() * 999998 + 1 );
        NumberFormat formatter = new DecimalFormat("000000");
        String stanNumber = formatter.format(stan);
        
        String refrenceNumber = "MMM000000000" + Integer.toString(stan);       
        refrenceNumber = refrenceNumber.substring(refrenceNumber.length()-12);
        
        String cardAcceptorIDCode = "000000000000000" + InstitutionCode;
        cardAcceptorIDCode = cardAcceptorIDCode.substring(cardAcceptorIDCode.length()-15);
        
		if (AMOUNT.indexOf(".") != -1) {
            String substra = AMOUNT.substring(AMOUNT.indexOf("."), AMOUNT
                    .length());
//            System.out.println("The substr is " + substra);
//            System.out.println("The length of substr is " + substra.length());
            if (substra.length() < 3) {
                AMOUNT = AMOUNT + "0";
//                System.out.println("amt_str = " + AMOUNT);
            }
                AMOUNT = AMOUNT.replaceAll("\\.", "");
        } else
        {
            AMOUNT = AMOUNT + "00";
        }

            AMOUNT = AMOUNT.replaceAll("\\.", "");
            AMOUNT = "000000000000" + AMOUNT;
            AMOUNT = AMOUNT.substring(AMOUNT.length() - 12);
            
            
        try {
            channel.setTimeout(timeout);
			channel.connect ();
		} catch (IOException e) {

		    responseMsg.setStatusCode(97);
            
            responseMsg.setStatusDescription("ERROR: connection timeout to " + serverIP + " on port " + serverPort);
            
//			e.printStackTrace();
            log.error("Exception: ",e.fillInStackTrace());
		}
        
        ISOMsg m = new ISOMsg ();
        m.setMTI("0200");
        m.set(2,msisdn);
        m.set(3,pc);
        m.set(4,AMOUNT);
        m.set(7,transmissionDate.format(date));
        m.set(11,stanNumber);
        m.set(12,transactionTime.format(date));
        m.set(13,transactionMonthDay.format(date));
        m.set(32,InstitutionCode);
        m.set(37,refrenceNumber);
        m.set(41,cardAcceptorTerminalID);
        m.set(42,cardAcceptorIDCode);
        m.set(43,"00000000000000000000BANK TO AIRTEL MONEY");
        m.set(59,echoData + " for " + msisdn + " of amount " + AMOUNT + ", TransactionID: "+ stanNumber + " on " + date);
        m.set(103,msisdn);
        
        try {
            
            log.info("card2Mobile ISO request : " + m.toString());
            
			channel.send (m);

//			System.out.println(channel.toString());
			ISOMsg isoResponse = channel.receive();
			
			AmIsoParser isoMessageParser = new AmIsoParser();
			
			responseMsg = isoMessageParser.ParseISOMessage(isoResponse);
            
            channel.disconnect ();
			
		} catch (IOException e) {
		    
		    responseMsg.setStatusCode(96);
            
            responseMsg.setStatusDescription("ERROR: Unable to parse ISO response message");
            
//			e.printStackTrace();
            log.error("Exception: ",e.fillInStackTrace());
		}
        
        return responseMsg;
	}

}
