import java.util.HashMap;
import java.util.Map;
import java.util.Random;

class Sim {

   // Class Sim variables
   public static double Clock, MeanInterArrivalTime, MeanServiceTime, SIGMA, LastEventTime,
      TotalBusy, MaxQueueLength, SumResponseTime, SumWaitingInQueue;
   public static int MinPatience, MaxPatience;
   public static long QueueLength, NumberInService,
      TotalCustomers, NumberOfDepartures, NumberOfReneged;

   public static int currentCall;

   public final static int arrival = 1;
   public final static int departure = 2;
   // new event type
   public final static int renege = 3;

   public static EventList FutureEventList;
   public static Queue Customers;
   public static Random stream;

   // assignment stats
   public static double[] a_RHO;
   public static double[] a_AVERAGEWAITINGTIMEINQUEUE;
   public static double[] a_RENEGED;

   // using a kind of BiMap
   public static Map<Event, Event> ArrivalRenegeMap;
   public static Map<Event, Event> RenegeArrivalMap;

   public static Map<Event, Double> ServiceTimeMap;

   public static void main(String argv[]) {

      MeanInterArrivalTime = 4.5;
      MeanServiceTime = 3.2;
      SIGMA = 0.6;
      TotalCustomers = 1000;
      MinPatience = 10;
      MaxPatience = 30;
      currentCall = -1;
      long seed = Long.parseLong(argv[0]);
      int numberOfCalls = Integer.parseInt(argv[1]);

      a_RHO = new double[numberOfCalls];
      a_AVERAGEWAITINGTIMEINQUEUE = new double[numberOfCalls];
      a_RENEGED = new double[numberOfCalls];

      stream = new Random(seed);           // initialize rng stream

      // iterate all simulations calls
      while (currentCall++ < numberOfCalls - 1) {

         FutureEventList = new EventList();
         Customers = new Queue();
         ArrivalRenegeMap = new HashMap<Event, Event>();
         RenegeArrivalMap = new HashMap<Event, Event>();
         ServiceTimeMap = new HashMap<Event, Double>();
         Initialization();

         // Loop until first "TotalCustomers" have departed
         while (NumberOfDepartures < TotalCustomers) {
            Event evt = FutureEventList.getMin();  // get imminent event
            FutureEventList.dequeue();                    // be rid of it
            Clock = evt.get_time();                       // advance simulation time
            if (evt.get_type() == arrival) ProcessArrival(evt);
            else if(evt.get_type() == departure) ProcessDeparture(evt);
            else ProcessRenege(evt);
         }
         ReportGeneration();
      }
      ReportAssignment(numberOfCalls);

   }

   // seed the event list with TotalCustomers arrivals
   public static void Initialization() {
      Clock = 0.0;
      QueueLength = 0;
      NumberInService = 0;
      LastEventTime = 0.0;
      TotalBusy = 0;
      MaxQueueLength = 0;
      SumResponseTime = 0;
      SumWaitingInQueue = 0;
      NumberOfDepartures = 0;
      NumberOfReneged = 0;

      // create first arrival event
      double a_time = exponential(stream, MeanInterArrivalTime);
      Event evt = new Event(arrival, a_time);
      FutureEventList.enqueue(evt);
      Event _renege = new Event(renege, a_time + uniform(stream, MinPatience, MaxPatience));
      FutureEventList.enqueue(_renege);

      ArrivalRenegeMap.put(_renege, evt);
      RenegeArrivalMap.put(evt, _renege);
   }

   public static void ProcessRenege(Event evt) {
      // get arrival of reneged customer
      Event arrival = ArrivalRenegeMap.get(evt);

      if(arrival != null) {
         boolean isDepartedAlready = !Customers.remove(arrival);
         if(isDepartedAlready) {
            // do nothing
         } else {
            QueueLength--;
            SumWaitingInQueue += evt.get_time() - arrival.get_time();
            NumberOfReneged++;
         }
      }
      TotalBusy += Clock - LastEventTime;
      LastEventTime = Clock;

   }

   public static void ProcessArrival(Event evt) {
      Customers.enqueue(evt);
      QueueLength++;
      // if the server is idle, fetch the event, do statistics
      // and put into service
      if (NumberInService == 0) {
         Event renege = RenegeArrivalMap.get(evt);
         ArrivalRenegeMap.remove(renege);
         // waiting time is zero
         ScheduleDeparture(evt);
      }
      else TotalBusy += (Clock - LastEventTime);  // server is busy

      // adjust max queue length statistics
      if (MaxQueueLength < QueueLength) MaxQueueLength = QueueLength;

      // schedule the next arrival
      double a_time = Clock + exponential(stream, MeanInterArrivalTime);
      Event next_arrival = new Event(arrival, a_time);
      FutureEventList.enqueue(next_arrival);
      Event next_renege = new Event(renege, a_time + uniform(stream, MinPatience, MaxPatience));
      FutureEventList.enqueue(next_renege);

      ArrivalRenegeMap.put(next_renege, next_arrival);
      RenegeArrivalMap.put(next_arrival, next_renege);

      LastEventTime = Clock;
   }

   public static void ScheduleDeparture(Event arrival) {
      double ServiceTime;
      // get the job at the head of the queue
      while ((ServiceTime = normal(stream, MeanServiceTime, SIGMA)) < 0) ;
      Event depart = new Event(departure, Clock + ServiceTime);
      ServiceTimeMap.put(arrival, ServiceTime);
      FutureEventList.enqueue(depart);
      NumberInService = 1;
      QueueLength--;
   }

   public static void ProcessDeparture(Event e) {
      // get the customer description
      Event finished = (Event) Customers.dequeue();
      // if there are customers in the queue then schedule
      // the departure of the next one
      Event nextCustomerArrival = (Event) Customers.peekFront();
      // remove renege of next customer since s/he is being serviced now
      Event nextCustomerRenege = RenegeArrivalMap.get(nextCustomerArrival);
      ArrivalRenegeMap.remove(nextCustomerRenege);

      if (QueueLength > 0) ScheduleDeparture(nextCustomerArrival);
      else NumberInService = 0;
      // measure the response time and add to the sum
      double response = (Clock - finished.get_time());
      SumResponseTime += response;
      double waitingTime = response - ServiceTimeMap.get(finished);
      SumWaitingInQueue += waitingTime;

      TotalBusy += (Clock - LastEventTime);
      NumberOfDepartures++;
      LastEventTime = Clock;
   }

   public static void ReportAssignment(int numberOfCalls) {
      double total_a_RHO = 0.0;
      double total_a_RENEGED = 0.0;
      double total_a_AVERAGEWAITINGTIMEINQUEUE = 0.0;

      for (double v : a_RHO) { total_a_RHO += v;}
      for (double v : a_RENEGED) { total_a_RENEGED += v;}
      for (double v : a_AVERAGEWAITINGTIMEINQUEUE) { total_a_AVERAGEWAITINGTIMEINQUEUE += v;}

      System.out.println("IE306 ASSIGNMENT#1 - RESULTS");
      System.out.println("\tNUMBER OF SIMULATIONS:\t" + numberOfCalls);
      System.out.println("\tSERVER UTILIZATION\t:" + total_a_RHO/numberOfCalls);
      System.out.println("\tAVERAGE WAITING TIME IN QUEUE:\t" + total_a_AVERAGEWAITINGTIMEINQUEUE/numberOfCalls);
      System.out.println("\tNUMBER OF CUSTOMERS RENEGED OVER ALL SIMULATIONS\t" + total_a_RENEGED);
      System.out.println("\tAVERAGE RENEGED CUSTOMER PER SIMULATION\t" + total_a_RENEGED/numberOfCalls);

   }

   public static void ReportGeneration() {
      double RHO = TotalBusy / Clock;
      double averageWaitingTimeInQueue = (SumWaitingInQueue / (NumberOfDepartures + NumberOfReneged));
      a_RHO[currentCall] = RHO;
      a_RENEGED[currentCall] = NumberOfReneged;
      a_AVERAGEWAITINGTIMEINQUEUE[currentCall] = averageWaitingTimeInQueue;

   }

   public static double exponential(Random rng, double mean) {
      return -mean * Math.log(rng.nextDouble());
   }

   public static double SaveNormal;
   public static int NumNormals = 0;
   public static final double PI = 3.1415927;

   public static double normal(Random rng, double mean, double sigma) {
      double ReturnNormal;
      // should we generate two normals?
      if (NumNormals == 0) {
         double r1 = rng.nextDouble();
         double r2 = rng.nextDouble();
         ReturnNormal = Math.sqrt(-2 * Math.log(r1)) * Math.cos(2 * PI * r2);
         SaveNormal = Math.sqrt(-2 * Math.log(r1)) * Math.sin(2 * PI * r2);
         NumNormals = 1;
      } else {
         NumNormals = 0;
         ReturnNormal = SaveNormal;
      }
      return ReturnNormal * sigma + mean;
   }

   public static double uniform(Random rng, int MinPatience, int MaxPatience) {

      return ((MaxPatience - MinPatience) * rng.nextDouble() + MinPatience);

   }
}

