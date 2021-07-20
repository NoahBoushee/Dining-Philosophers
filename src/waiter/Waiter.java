package waiter;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.ActiveMQConnection;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.MessageProducer;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;

import java.io.File;
import java.io.FileNotFoundException;

import java.util.Scanner;
import java.util.Arrays;
import java.util.ArrayList;

/*
Noah Boushee
CSCI 364
Waiter Class uses activemq to communicate with the Philosopher Threads and decides whether or not they can pick up the forks
or if the forks are already picked up

*/

//Use MessageListner
//INCLUDE java -cp lib/activemq-all-5.15.8.jar; build/waiter
public class Waiter extends Thread implements MessageListener{
	
	
	
    public static void main (String[] args) throws Exception{
		Waiter waiter = new Waiter();
		waiter.start();
		
	}
	private boolean[] Forks = {false, false, false, false, false};
	private Session session;
	private Destination destination2;
	private Destination destination;
	private Connection connection;
	private MessageProducer producer;
	private MessageConsumer consumer;
	
	public void run(){
		try{
			ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://localhost:61616");
			connection = connectionFactory.createConnection();
			connection.start();
			//CONSUMES DATA AND PRODUCES RETURN
			session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			// Create the destination (Topic or Queue)
            destination2 = session.createQueue("DATA_QUEUE");
			destination = session.createQueue("RETURN_QUEUE");
            // Create a MessageProducer from the Session to the Topic or Queue
            producer = session.createProducer(null);
			consumer = session.createConsumer(destination2);
            producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
			
            // Create a messages READ THE STUFF FROM THE FILES
            String text = "";
			consumer.setMessageListener(this);
			System.out.println("WAITING ON PHILOSOPHER FOR FORK REQUESTS");
            // Clean up
            //consumeMessagesAndClose(connection, session, consumer, 5000);
		}
		catch(Exception e){
			System.out.println("Caught: " + e);
			e.printStackTrace();
		}
	}
	
	
	public void onMessage(Message message){
		try{
			TextMessage txtMsg = (TextMessage) message;
			String msg = txtMsg.getText();
			/*if(msg.contains("RELEASE")){
				//TextMessage response = this.session.createTextMessage();
				//response.setText("END");
				//response.setJMSCorrelationID(message.getJMSCorrelationID());
				//producer.send(message.getJMSReplyTo(), response);
				consumer.close();
				session.close();
				connection.close();
				System.out.println("Final message received closing down!");
				System.exit(0);
			}*/
			System.out.println("Received message: " + msg);
			String results = doWork(msg);
			System.out.println("Sending message: " + results);
			TextMessage response = this.session.createTextMessage(results);
			//response.setText(results);
			response.setJMSCorrelationID(message.getJMSCorrelationID());
			producer.send(message.getJMSReplyTo(), response);
			//producer.send(response);
			
		}
		catch(JMSException e){
			System.out.println("Caught an exception: " + e);
            e.printStackTrace();
		}
	}
	
	public String doWork(String tempMsg){
		//DETERMINE IF FORKS ARE AVAILABLE
		//RECEIVED THING LOOKS LIKE THIS <ID> WANT or <ID> RELEASE 
		int id2;
		int id3;
		int tempId;
		if (tempMsg.contains("RELEASE")){
			tempId = Integer.parseInt(String.valueOf(tempMsg.charAt(0)));
			
			if (tempId == 1){
				id2 = 0;
				id3 = 4;
			}
			else if (tempId == 5){
				id2 = 4;
				id3 = 0;
			}
			else{
				id2 = tempId;
				id3 = tempId-1;
			}
			Forks[id2] = false;
			Forks[id3] = false;
		    tempMsg = "Forks have been released Philosopher " + tempId;
			return tempMsg;
		}
		else{
			tempId = Integer.parseInt(tempMsg.valueOf(tempMsg.charAt(0)));
			
			if (tempId == 1){
				id2 = 0;
				id3 = 4;
			}
			else if (tempId == 5){
				id2 = 4;
				id3 = 0;
			}
			else{
				id2 = tempId;
				id3 = tempId-1;
			}
			if (Forks[id2] == false && Forks[id3] == false){
				Forks[id2] = true;
				Forks[id3] = true;
				tempMsg = "Gave forks " + id2 + " " + id3 + " are claimed by " + tempId;
				return tempMsg;
			}
			else{
				tempMsg = "Forks Not available " + tempId;
				return tempMsg;
			}
		}
		
	}

	
}