### IE306 Assignment-1

#### (Nurettin YILMAZ - 2006101594)

#### Definition
> In this assignment we are expected to modify and extend the code *Sim.java* which is given by the textbook in order to simulate **Single Channel Queue with reneging**. Reneging process is leaving of customers after waiting some patience time while they are in queue. We are also required to implement some code to collect the statistics of average waiting time in queue which is not did in *Sim.java* yet.

#### Preperation
> Since reneging is a state changing event ie removal of entity from queue, we must additionally define a reneging event and treat that event as arrival and departure events previously did in *Sim.java*.

#### Code
> Firstly we must create outer loop for calling the simulation multiple times which is read from console explicitly. Outer loop continues until the desired calls have been made.

	 while (currentCall++ < numberOfCalls - 1) {
	 	// simulation
	 }

> The important thing is that we must re-initialize the simulation variables to avoid overwriting statistics between seperate simulation calls. Thus:

	FutureEventList = new EventList();
	Customers = new Queue();
	ArrivalRenegeMap = new HashMap<Event, Event>();
	RenegeArrivalMap = new HashMap<Event, Event>();
	ServiceTimeMap = new HashMap<Event, Double>();
	Initialization();

is called inside outer while loop.

> To mark reneging event we define new integer as 3:

	public final static int renege = 3;

> We acquire the imminent event as usual but controlling further the event type with additional else block:

	if (evt.get_type() == arrival) ProcessArrival(evt);
	else if(evt.get_type() == departure) ProcessDeparture(evt);
	else ProcessRenege(evt);

> Every arrival event comes with an (potential) reneging event. That is to say when we create an arrival we must also create its reneging event. Without binding arrival event to its corresponding reneging event, we cannot track which customer reneged actually before s/he will be serviced. Using `BiMap` we are able to overcome this issue:

	public static Map<Event, Event> ArrivalRenegeMap = new HashMap<Event, Event>();
	public static Map<Event, Event> RenegeArrivalMap = new HashMap<Event, Event>();

Reneged customer must be pulled out of the queue, by storing arrivals and reneging events as above we can remove customer as such (`Queue` implementation of Java allows that.):

	Customers.remove(arrival);

If that expression returns false, customer is no longer in queue namely departed already. We take no action in this situation. But, if s/he is in queue:

	QueueLength--;
	SumWaitingInQueue += evt.get_time() - arrival.get_time();
	NumberOfReneged++;

Operations are done. By storing which reneging belongs to which arrival we can calculate the waiting time in queue of that particular customer. `evt` is the reneging event in that function scope.

> In `ProcessDeparture` method we are given the *response time* of a specific customer. Response time is essentially the time between customer arrives and departs. Substracting his/her service time from response time gives us the time that customer has waited in queue. Therefore we need an another storage of structure:

	public static Map<Event, Double> ServiceTimeMap;

After calculation of service time in `ScheduleDeparture` method of a customer we record that service time:

	double ServiceTime;
	// get the job at the head of the queue
	while ((ServiceTime = normal(stream, MeanServiceTime, SIGMA)) < 0) ;
	Event depart = new Event(departure, Clock + ServiceTime);
	ServiceTimeMap.put(arrival, ServiceTime);

#### Statistics
> To keep track of statistics we define arrays of `double`s:

	public static double[] a_RHO;
	public static double[] a_AVERAGEWAITINGTIMEINQUEUE;
	public static double[] a_RENEGED;

Those arrays are initialized before outer while loop with size of call number and populated respectively in each simulation call. After all calls completed they are simply summed up:

	for (double v : a_RHO) { total_a_RHO += v;}
	for (double v : a_RENEGED) { total_a_RENEGED += v;}
	for (double v : a_AVERAGEWAITINGTIMEINQUEUE) { total_a_AVERAGEWAITINGTIMEINQUEUE += v;}

#### Example Outputs
> With seed: 23456784, number of calls: 1000

IE306 ASSIGNMENT#1 - RESULTS
NUMBER OF SIMULATIONS:	1000
SERVER UTILIZATION	:0.8374511484771064
AVERAGE WAITING TIME IN QUEUE:	3.4907424136391016
NUMBER OF CUSTOMERS RENEGED OVER ALL SIMULATIONS	13262.0
AVERAGE RENEGED CUSTOMER PER SIMULATION	13.262

> With seed: 4566745643462, number of calls: 3000

IE306 ASSIGNMENT#1 - RESULTS
NUMBER OF SIMULATIONS:	3000
SERVER UTILIZATION	:0.8377798585606803
AVERAGE WAITING TIME IN QUEUE:	3.4865624897261163
NUMBER OF CUSTOMERS RENEGED OVER ALL SIMULATIONS	39587.0
AVERAGE RENEGED CUSTOMER PER SIMULATION	13.195666666666666

> With seed: 8903478394, number of calls: 8000

IE306 ASSIGNMENT#1 - RESULTS
NUMBER OF SIMULATIONS:	8000
SERVER UTILIZATION	:0.8378287327139426
AVERAGE WAITING TIME IN QUEUE:	3.4871282389401093
NUMBER OF CUSTOMERS RENEGED OVER ALL SIMULATIONS	105340.0
AVERAGE RENEGED CUSTOMER PER SIMULATION	13.1675

#### Conclusion
> We are getting average of 13 customers reneged over 1000 customers, this corresponds to 1,3% reneging. I would not take any precautions like increasing servers unless that percetage is over 5% (taking confidence interval %95).

#### References
> The whole project can be seen [here](https://github.com/nyilmaz/single_channel_queue)
> Modifications to the original code can be seen [here](https://github.com/nyilmaz/single_channel_queue/commit/a1a17cdcb460205a4241f1e2786c9721de8bdc75)
