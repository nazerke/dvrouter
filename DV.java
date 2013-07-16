import java.lang.Math;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;

public class DV implements RoutingAlgorithm {
    
    static int LOCAL = -1;
    static int UNKNOWN = -2;
    static int INFINITY = 60;
    private Router obj;
    private int updateInterval;
    private boolean preverseAllowed;
    private boolean expireAllowed;
    ArrayList<DVRoutingTableEntry>  routingTable = new ArrayList<DVRoutingTableEntry>();
	 DVRoutingTableEntry initialEntry;
	 boolean found;
    public DV()
    {

    }
    
    public void setRouterObject(Router obj)
    {
    	this.obj =obj; 
    }
    
    public void setUpdateInterval(int u)
    {
    	updateInterval = u;
    }
    
    public void setAllowPReverse(boolean flag)
    {
    	preverseAllowed = flag;
    }
    
    public void setAllowExpire(boolean flag)
    {
    	expireAllowed = flag;
    }

    public void initalise()
    {
    	
    	 initialEntry = new DVRoutingTableEntry(obj.getId(),LOCAL,0,obj.getCurrentTime());
    	 routingTable.add(initialEntry);
    }
    
    public int getNextHop(int destination)
    {
    	DVRoutingTableEntry dest = null;
    	
        for(DVRoutingTableEntry existingEntry: routingTable)
        {
        	if(existingEntry.getDestination()==destination)
        	{	
        		dest = existingEntry;
        	}
        }

        if (dest == null || dest.getMetric() == INFINITY)
        	return UNKNOWN;
        return dest.getInterface();
    }
    
    public void tidyTable()
    {
    	for(int i = 0; i<obj.getNumInterfaces();i++)
    	{
    		if(obj.getInterfaceState(i)==false)
    		{
    			for(DVRoutingTableEntry existingEntry:routingTable)
    			{
    				if(existingEntry.getInterface()==i)
    				{
    					existingEntry.setMetric(INFINITY);
    				}
    				
    			}
    		}
    	}
    }
    
    public Packet generateRoutingPacket(int iface)
    {   
       
    	Payload dataToPack = new Payload();
 
    	if(obj.getInterfaceState(iface)==true){
    		
    	int numberOfEntries = routingTable.size();
    	
    	for(int i = 0;i<numberOfEntries;i++){
    		DVRoutingTableEntry tempEntry = routingTable.get(i);
    		DVRoutingTableEntry copyOfEntry = new DVRoutingTableEntry(tempEntry.getDestination(),
    				tempEntry.getInterface(),tempEntry.getMetric(),tempEntry.getTime());
    		
    		if(preverseAllowed && getNextHop(copyOfEntry.getDestination())==iface)
    			{copyOfEntry.setMetric(INFINITY);}
    		dataToPack.addEntry(copyOfEntry);
    	}
    	Packet packetToSend = new RoutingPacket(obj.getId(),255);
    	packetToSend.setPayload(dataToPack);
        return packetToSend;
        }
    	return null;
    }
    
    public void processRoutingPacket(Packet p, int iface)
    {
    	//System.out.println("We are processing packet received from router "+p.src +" to router "+obj.getId());
    	
    	Payload payload = p.getPayload();
    	Vector<Object> receivedData = payload.getData();
    	DVRoutingTableEntry receivedEntry;
    	for(Object entry: receivedData)
    	{
    		receivedEntry = (DVRoutingTableEntry) entry;
    		int metric=0;
    		//System.out.println("Current received entry: "+receivedEntry.toString());
    		if(receivedEntry.getMetric()!=INFINITY){
    		metric = obj.getInterfaceWeight(iface)+receivedEntry.getMetric();
    		}
    		else 
    		{
    			metric =INFINITY;
    		}
    		boolean found=false;
    		for(DVRoutingTableEntry existingEntry: routingTable)
    		{
    			
    			if(receivedEntry.getDestination()==existingEntry.getDestination())
    			{
    				found = true;
    				//System.out.println("Entry with destination "+receivedEntry.getDestination()+" exists in a routing table: "+existingEntry.toString());
    				if(existingEntry.getInterface()==iface)
    				{
    					//System.out.println("Interfaces are the same");
    					existingEntry.setMetric(metric);
    					existingEntry.setInterface(iface);
    				}
    				else if(existingEntry.getMetric()>metric)
    				{
    					//System.out.println("metric is better");
    					existingEntry.setMetric(metric);
    					existingEntry.setInterface(iface);
    				}
    			
    			}
    		}
    		if(found==false)
    		{
    			DVRoutingTableEntry newEntry = new DVRoutingTableEntry(receivedEntry.getDestination(),iface,metric,receivedEntry.getTime());
    			routingTable.add(newEntry);
    		}
    	}
    }
//  * The format is :
//    * Router <id>
//    * d <destination> i <interface> m <metric>
//    * d <destination> i <interface> m <metric>
//    * d <destination> i <interface> m <metric>
//    */
    
    public void showRoutes()
    {
    	ArrayList <DVRoutingTableEntry> sortedTable = sortTable(routingTable);
    	System.out.println("Router "+obj.getId());
    	for(DVRoutingTableEntry existingEntry: sortedTable)
    	{
    		System.out.println(existingEntry.toString());
    		
    	}
    }

private ArrayList<DVRoutingTableEntry>sortTable(
		ArrayList<DVRoutingTableEntry> routingTable2) {
	ArrayList<DVRoutingTableEntry> sortTable=new ArrayList<DVRoutingTableEntry>();
	int array[] = new int[routingTable2.size()];
	int i = 0;
	for(DVRoutingTableEntry entry: routingTable2)  				//create array of  destinations  
	{
		 array[i] = entry.getDestination();
		 i++;	
	}
	Arrays.sort(array);		//sort destinations
	
	for(int i1=0;i1<array.length;i1++)						//sort entries using sorted destinations
	{
		for(DVRoutingTableEntry entry: routingTable2){
			if(entry.getDestination()==array[i1])
			{
				sortTable.add(entry);
			}
		}
	}

	return sortTable;
}
}

class DVRoutingTableEntry implements RoutingTableEntry
{
    int destination;
    int iface;
    int metric;
    int time;
    public DVRoutingTableEntry(int d, int i, int m, int t)
	{
    	destination = d;
    	iface = i;
    	metric = m;
    	time = t;
	}
    public int getDestination() { return destination; } 
    public void setDestination(int d) {destination =d; }
    public int getInterface() { return iface; }
    public void setInterface(int i) { iface = i;}
    public int getMetric() { return metric;}
    public void setMetric(int m) { metric = m;} 
    public int getTime() {return time;}
    public void setTime(int t) { time = t;}
    
    public String toString() 
	{
	    return "d "+getDestination() +" i "+getInterface() + " m "+getMetric();
	}
}

