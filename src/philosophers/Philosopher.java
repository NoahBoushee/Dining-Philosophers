package philosophers;

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

import java.util.Scanner;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Random;

/*
Noah Boushee
CSCI 364
Philosopher Class runs via threads and uses activemq to communicate with the waiter for putting down and picking up the forks

*/


//Use MessageListner
//INCLUDE java -cp lib/activemq-all-5.15.8.jar; 
class Philosopher1 implements Runnable, MessageListener{
	private int ID;
	private int loopTimes;
	private int eatTimes;
	private float totalCritTime; // IN MILISECONDS
	private float totalRunTime; // IN MILISECONDS
	private boolean conFlag = false;
	private boolean hasForks = false;
	private String forkMessage = "";
	
	private Session sessionPhil;
	private Destination destinationPhil;
	private Destination destinationPhil2;
	private Connection connectionPhil;
	private MessageProducer producerPhil;
	private MessageConsumer consumerPhil;
	private Random random = new Random();	 
	Philosopher1(int id, Connection tempConnection){
		this.ID = id;
		this.connectionPhil = tempConnection;
		
		
		//this.consumerPhil = tempConsumer;
		try{
			this.sessionPhil = connectionPhil.createSession(false, Session.AUTO_ACKNOWLEDGE);
			this.destinationPhil = sessionPhil.createQueue("DATA_QUEUE");
			this.producerPhil = sessionPhil.createProducer(destinationPhil);
			this.producerPhil.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
			this.connectionPhil.start();
			this.destinationPhil2 = sessionPhil.createTemporaryQueue();
			MessageConsumer consumerPhil = sessionPhil.createConsumer(destinationPhil2);
			consumerPhil.setMessageListener(this);
		}
		catch(JMSException e){
			System.out.println(e);
			System.exit(1);
		}
	}	 
	
	public void run(){
		
		while(!conFlag){
			try{
				//forkMessage = "";
				// THINK
				long startWait = System.currentTimeMillis();
			    System.out.println("Philosopher " + this.ID + " is starting to thinking");
				Thread.sleep(random.nextInt(100)); // THINK A RANDOM TIME
			
				//ASK FOR FORKS
				if (hasForks == false){
					
					String text = this.ID + " WANT";
					TextMessage message = sessionPhil.createTextMessage(text);
					message.setJMSReplyTo(destinationPhil2);
					String correlationId = this.createRandomString();
					message.setJMSCorrelationID(correlationId);
					// Tell the producer to send the message
					System.out.println("Sent message: <"+ text + "> : From Philosopher " + this.ID);
					producerPhil.send(message);
				}
                Thread.sleep(random.nextInt(25));
				
				//Thread.wait(100);
				if (hasForks == true){
					hasForks = false;
					String text2 = this.ID + " RELEASE";
					TextMessage message1 = sessionPhil.createTextMessage(text2);
					message1.setJMSReplyTo(destinationPhil2);
					String correlationId1 = this.createRandomString();
					message1.setJMSCorrelationID(correlationId1);
					// Tell the producer to send the message
					//System.out.println("Sent message: <"+ text + "> : From Philosopher " + this.ID);
					producerPhil.send(message1);
					eatTimes += 1;
					System.out.println("Philosopher " + this.ID + " released forks!");
				}
				long endWait = System.currentTimeMillis();
			    totalRunTime += (endWait - startWait);
				
				
				//WAIT SOME TIME FOR THE FORKS
				
				//GIVEUP ACCESS TO THE FORKS
			}
			catch(Exception e){
				System.out.println("Caught: " + e);
				e.printStackTrace();
			}
			
			loopTimes +=1;
			
		}
		try{
			connectionPhil.close();
		}
		catch(JMSException JMS){
			System.out.println("ERROR CLOSING");
		}
	}
	
	 private String createRandomString() {
        Random random = new Random(System.currentTimeMillis());
        long randomLong = random.nextLong();
        return Long.toHexString(randomLong);
    }
	
	public void onMessage(Message message){
		
		try{
			TextMessage txtMsg = (TextMessage) message;
			forkMessage = txtMsg.getText();
			if (forkMessage.contains("Gave")){
				long startWait = System.currentTimeMillis();
				hasForks = true;
				System.out.println("Philosopher " + this.ID + " has forks and is now eating!");
				try{
					Thread.sleep(random.nextInt(50)); // EAT A RANDOM TIME
				}
				catch(Exception t){
					System.out.println(t);
				}
				long endWait = System.currentTimeMillis();
				long temp = (endWait - startWait); 
				totalCritTime += temp;
			}
			//PRINTS OUT THE DATA NEED TO ORGANIZE IT
			System.out.println("Received message from Waiter " + forkMessage);
			//IF MESSAGE IS GIVE CALL EAT IF MESSAGE IS TAKEN THINK MORE
			//notify();
		}
		catch(JMSException e){
			System.out.println("Caught: " + e);
            e.printStackTrace();
		}
	}
	
	
    public void setStopFlag(){
		this.conFlag = true;
	}
	public int getLoopTimes(){
		return loopTimes;
	}	
	public int getEatTimes(){
		return eatTimes;
	}
	public float getTotalCritTime(){
		return totalCritTime;
	}
	public float getTotalRunTime(){
		return totalRunTime;
	}	
	public int getID(){
		return ID;
	}

	
}

public class Philosopher extends Thread{
	    public static void main (String[] args) throws Exception{
		 long waitTime = 0;
		 if (args.length < 1){
			 System.out.println("NOT ENOUGH ARGS SEE USAGE");
			 System.out.println("Usage <Philosopher <main thread wait time in ms> >");
			 System.exit(1);
		 }
		 try{
			waitTime = Long.parseLong(args[0]); 
		 }
		 catch(Exception e){
			 System.out.println("INVALID TIME INPUT");
			 System.exit(1);
		 }
		 
		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://localhost:61616");
		//Connection connection = connectionFactory.createConnection();
		//connection.start();
		//Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		// Create the destination (Topic or Queue)
       // Destination destination = session.createQueue("DATA_QUEUE");
        // Create a MessageProducer from the Session to the Topic or Queue
        //MessageProducer producer = session.createProducer(destination);
		//producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
		//Destination destination2 = session.createQueue("RETURN_QUEUE");
		//MessageConsumer consumer = session.createConsumer(destination2);
		//consumer.setMessageListener(this);
		 //CREATE 5 CONNECTIONS AND PASS THEM TO THE RUNNABLES
		 //Waiter waiter = new Waiter();
		ArrayList<Connection> conArray = new ArrayList<Connection>(5);
		 for (int i = 0; i < 5; i++){
			 conArray.add(connectionFactory.createConnection());
		     //conArray.get(i).start();
		 }
		 
		 Thread mainThread = Thread.currentThread();
		 ArrayList<Philosopher1> philArray = new ArrayList<Philosopher1>(5);
		 for (int i = 1; i < 6; i++){
			 philArray.add(new Philosopher1(i,  conArray.get(i-1)));
		 }
		 ArrayList<Thread> threadArray = new ArrayList<Thread>(5);
		 for (int i = 0; i < 5; i++){
			 threadArray.add(new Thread(philArray.get(i)));
		 }
		 for (int i = 0; i < 5; i++){
			 threadArray.get(i).start();
		 }
		 //Philosopher1 p1 = new Philosopher1(1, session, destination, destination2, connection, producer);
		 //Philosopher1 p2 = new Philosopher1(2, session, destination, destination2, connection, producer);
		 //Philosopher1 p3 = new Philosopher1(3, session, destination, destination2, connection, producer);
		 //Philosopher1 p4 = new Philosopher1(4, session, destination, destination2, connection, producer);
		 //Philosopher1 p5 = new Philosopher1(5, session, destination, destination2, connection, producer);
		 //Thread t1 = new Thread(p1);
		 //Thread t2 = new Thread(p2);
		 //Thread t3 = new Thread(p3);
		 //Thread t4 = new Thread(p4);
		 //Thread t5 = new Thread(p5);
		 //t1.start();
		 //t2.start();
		 //t3.start();
		 //t4.start();
		 //t5.start();
		 try{
			mainThread.sleep(waitTime);
		}
		catch(InterruptedException IO){
			System.out.println("MAIN THREAD INTERRUPTED?!");
		}
		for (int i = 0; i < 5; i++){
			 philArray.get(i).setStopFlag();
		 }
		//p1.setStopFlag();
		//p2.setStopFlag();
		//p3.setStopFlag();
		//p4.setStopFlag();
		//p5.setStopFlag();
		try{
			for (int i = 0; i < 5; i++){
			 threadArray.get(i).join();
			}
			//t1.join();
			//t2.join();
			//t3.join();
			//t4.join();
			//t5.join();
		}
		catch(InterruptedException E){
			System.out.println("Child thread interrupted");
		}
		
        //PRINT OUT THE STUFF	
		 /*for (int i = 0; i < 5; i++){
		     conArray.get(i).close();
		 }*/
		 //connection.close();
		System.out.println("Thread stopped!");
		for (int i = 0; i < 5; i++){
			System.out.println("Philosopher: " + philArray.get(i).getID());
			System.out.println("	Thoughts: " + philArray.get(i).getLoopTimes() + ", Meals: " + philArray.get(i).getEatTimes());
			System.out.println("	Waiting Time(MS): " + philArray.get(i).getTotalCritTime());
			System.out.println("	Total Time(MS): " + philArray.get(i).getTotalRunTime());
			System.out.println("	Time Ratio(MS): " + philArray.get(i).getTotalCritTime()/philArray.get(i).getTotalRunTime());
		}
        /*System.out.println("Thread stopped!");
		System.out.println("Philosopher: " + p1.getID());
		System.out.println("	Thoughts: " + p1.getLoopTimes() + ", Meals: " + p1.getEatTimes());
		System.out.println("	Waiting Time(MS): " + p1.getTotalCritTime());
		System.out.println("	Total Time(MS): " + p1.getTotalRunTime());
		System.out.println("	Time Ratio(MS): " + p1.getTotalCritTime()/p1.getTotalRunTime());
		System.out.println("Philosopher: " + p2.getID());
		System.out.println("	Thoughts: " + p2.getLoopTimes() + ", Meals: " + p2.getEatTimes());
		System.out.println("	Waiting Time(MS): " + p2.getTotalCritTime());
		System.out.println("	Total Time(MS): " + p2.getTotalRunTime());
		System.out.println("	Time Ratio(MS): " + p2.getTotalCritTime()/p2.getTotalRunTime());
		System.out.println("Philosopher: " + p3.getID());
		System.out.println("	Thoughts: " + p3.getLoopTimes() + ", Meals: " + p3.getEatTimes());
		System.out.println("	Waiting Time(MS): " + p3.getTotalCritTime());
		System.out.println("	Total Time(MS): " + p3.getTotalRunTime());
		System.out.println("	Time Ratio(MS): " + p3.getTotalCritTime()/p3.getTotalRunTime());
		System.out.println("Philosopher: " + p4.getID());
		System.out.println("	Thoughts: " + p4.getLoopTimes() + ", Meals: " + p4.getEatTimes());
		System.out.println("	Waiting Time(MS): " + p4.getTotalCritTime());
		System.out.println("	Total Time(MS): " + p4.getTotalRunTime());
		System.out.println("	Time Ratio(MS): " + p4.getTotalCritTime()/p4.getTotalRunTime());
		System.out.println("Philosopher: " + p5.getID());
		System.out.println("	Thoughts: " + p5.getLoopTimes() + ", Meals: " + p5.getEatTimes());
		System.out.println("	Waiting Time(MS): " + p5.getTotalCritTime());
		System.out.println("	Total Time(MS): " + p5.getTotalRunTime());
		System.out.println("	Time Ratio(MS): " + p5.getTotalCritTime()/p5.getTotalRunTime());
		*/
		System.exit(0);
	}
}