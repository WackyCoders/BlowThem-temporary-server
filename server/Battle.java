package server;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
/**
 * Created by foban on 31.07.14.
 */
public class Battle extends Thread {
	
	private BlockingQueue<ConnectServer.UserProcessor> queue;
	private BlockingQueue<ConnectServer.UserProcessor> cloneQueue;
	private TreeMap<Integer, ConnectServer.UserProcessor> battleUsers;
	private Float X, Y;
    private int battleSize;
    private boolean closed = false;

    public Battle(TreeMap<Integer, ConnectServer.UserProcessor> battleUsers){
    	battleSize = 2;
    	this.battleUsers = battleUsers;
    	queue = new LinkedBlockingQueue<ConnectServer.UserProcessor>();
    	cloneQueue = new LinkedBlockingQueue<ConnectServer.UserProcessor>();
    	for(Map.Entry<Integer, ConnectServer.UserProcessor> entry : battleUsers.entrySet()){
    		queue.offer(entry.getValue());
    	}
    }
 
    public Battle(int battleSize){
    	this.battleSize = battleSize;
    }
    
    public void startChannel(ConnectServer.UserProcessor process){
    	if(!cloneQueue.contains(process)){
    		cloneQueue.offer(process);
    	}
    }
    public void removeChannel(ConnectServer.UserProcessor process){
    	if(cloneQueue.contains(process)){
    		cloneQueue.remove(process);
    	}
    }
    
    private void fillProcesses(){
    	for(Map.Entry<Integer, ConnectServer.UserProcessor> entry : battleUsers.entrySet()){
    		queue.offer(entry.getValue());
    	}
    }

	public void setX(Float x) {
		X = x;
	}

	public void setY(Float y) {
		Y = y;
	}
	
	boolean full(){
        return battleSize <= battleUsers.size();
    }

	synchronized boolean userExist(Integer id_user){
        return battleUsers.containsKey(id_user);
    }

    synchronized void deleteUser(Integer id_user){
        if(userExist(id_user)){
            battleUsers.remove(id_user);
            if(battleUsers.size()==0)
                closed = true;
        }
    }
    
    void addUser(Integer id_user, ConnectServer.UserProcessor processor) throws Exception {
        if(!userExist(id_user) && !full()){
            battleUsers.put(id_user,processor);
            if(full()){
                final Thread thread = new Thread(this);
                thread.start();
            }
        }
        else
            throw new Exception("Such user already in Battle");
    }

	public void run() {
    	
		while(true){
			try{
				ConnectServer.UserProcessor currentProcess = cloneQueue.take();
				for(int i = 0; i < queue.size(); ++i){
					ConnectServer.UserProcessor innerProcess = queue.take();
					if(currentProcess.userId != innerProcess.userId){
						//System.out.println("X = " + currentProcess.X + " ; Y = " + currentProcess.Y);
						innerProcess.send("$motion$");
						innerProcess.send(String.valueOf(currentProcess.X));
						innerProcess.send(String.valueOf(currentProcess.Y)); 
						innerProcess.send(String.valueOf(currentProcess.bitmapAngle));
						innerProcess.send(String.valueOf(currentProcess.targetX));
						innerProcess.send(String.valueOf(currentProcess.targetY));
						
						if(currentProcess.fired){
							//System.out.println("FIRE!!!");
							innerProcess.send("$fire$");
							innerProcess.send(String.valueOf(currentProcess.xFire));
							innerProcess.send(String.valueOf(currentProcess.yFire));
							currentProcess.fired = false;
						}
						
						if(currentProcess.lost){
							System.out.println("Surrender accepted :)");
							innerProcess.send("$victory$");
							currentProcess.lost = false;
						}
					}
				}
				fillProcesses();
			} catch(InterruptedException e){
				e.printStackTrace();
			}
		}
		
	}
}
