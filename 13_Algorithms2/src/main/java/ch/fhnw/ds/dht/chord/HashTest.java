package ch.fhnw.ds.dht.chord;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.pattern.Patterns;
import akka.util.Timeout;
import scala.concurrent.Await;
import scala.concurrent.Future;

public class HashTest {
	
	static class Put {
		public final Object key;
		public final Object value;
		public Put(Object key, Object value) { this.key = key; this.value = value; }
	}
	static class Put2 {
		public final Object key;
		public final Object value;
		public Put2(Object key, Object value) { this.key = key; this.value = value; }
	}
	static class Get {
		public final Object key;
		public final int counter;
		public Get(Object key) { this(key, 0); }
		public Get(Object key, int counter) { this.key = key; this.counter = counter; }
	}
	static class Get2 {
		public final Object key;
		public final int counter;
		public Get2(Object key, int counter) { this.key = key; this.counter = counter; }
	}
	static class Result {
		public final int id;
		public final Object key;
		public final Object value;
		public final int counter;
		public Result(int id, Object key, Object value, int counter) { this.id = id; this.key = key; this.value = value; this.counter = counter; }
		public String toString() { return String.format("Result(id = %d, value = %s, counter = %d)", id, value, counter); }
	}
	static class Partition {
		public final int id;
		public Partition(int id) { this.id = id; }
	}
	static class PartitionAnswer {
		public final Map<Object, Object> map;
		public PartitionAnswer(Map<Object, Object> map) { this.map = map; }
	}
	static class Print {}
	
	static final int N = 1000;	// size of the ring, i.e. number of nodes, >= 2
	static final int K = 32; // 1..32 (size of the finger table)

	public static void main(String[] args) throws Exception {
		ActorSystem as = ActorSystem.create();
		Random r = new Random();
		
		TreeMap<Integer, ActorRef> actors = new TreeMap<>();
		for(int i = 0; i < N; i++) { 
			int id = r.nextInt();
			actors.put(id, as.actorOf(Props.create(HashNode.class, id), "Node:"+id));
		}

		ActorRef a0 = actors.firstEntry().getValue();
		System.out.println("a0 = " + a0);
		
		var keySet = actors.navigableKeySet();
		for(int key : keySet) {
			var fingerTable = getFingerTable(actors, key);
			actors.get(key).tell(fingerTable, null);
		}
		
		Timeout  timeout = new Timeout(5, TimeUnit.SECONDS);
		BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
		boolean exit = false;
		while (!exit) {
			String input = stdin.readLine();
			String[] cmd = input.split(" ");
			switch (cmd[0]) {

			case "get":
				if(!enoughArguments(cmd, 1)) break;
				Future<Object> res = Patterns.ask(a0, new Get(cmd[1]), timeout);
				Object result = Await.result(res, timeout.duration());
				System.out.println(result);
				break;

			case "put":
				if(!enoughArguments(cmd, 2)) break;
				a0.tell(new Put(cmd[1], cmd[2]), null);
				break;

			case "node":  
				if(!enoughArguments(cmd, 1)) break;
				res = Patterns.ask(a0, new Get(new Object() {
					public int hashCode() { return Integer.parseInt(cmd[1]); }
				}), timeout);
				result = Await.result(res, timeout.duration());
				System.out.println(result);
				break;

			case "add":
				int id = r.nextInt();
				if(cmd.length > 1) {
					id = Integer.parseInt(cmd[1]);
				}
				while(actors.get(id) != null) id = r.nextInt();
				
				var keys = actors.navigableKeySet();
				var succ = keys.ceiling(id);
				if(succ == null) succ = keys.first();
				var prev = keys.floor(id);
				if(prev == null) prev = keys.last();

				ActorRef prevActor = actors.get(prev);
				ActorRef newActor = as.actorOf(Props.create(HashNode.class, id), "Node:"+id);
				actors.put(id, newActor);
				
				prevActor.tell(getFingerTable(actors, prev), null);
				newActor.tell(getFingerTable(actors, id), null);

				actors.get(succ).tell(new Partition(id), newActor);
				System.out.println("new node with id " + id);
				break;
			
			case "print":
				if(cmd.length == 1) {
					actors.values().forEach(a -> a.tell(new Print(), null));
				} else {
					id = Integer.parseInt(cmd[1]);
					ActorRef ref = actors.get(id);
					if(ref != null) ref.tell(new Print(), null);
					else System.out.printf("Node %d does not exist%n", id);
				}
				break;

			case "size":
				System.out.printf("%d nodes%n", actors.size());
				break;

			case "exit":
			case "quit": 
				exit = true;
				break;

			default: 
				System.out.println("Command not understood: " + input);
			}
		}

		as.terminate();
	}

	private static boolean enoughArguments(String[] cmd, int n) {
		if(cmd.length - 1 < n) {
			System.err.printf("Not enougth arguments, %s requires %d args%n", cmd[0], n);
		}
		return cmd.length - 1 >= n;
	}

	private static TreeMap<Integer, ActorRef> getFingerTable(TreeMap<Integer, ActorRef> actors, int key) {
		var keySet = actors.navigableKeySet();
		var fingerTable = new TreeMap<Integer, ActorRef>();
		int dx = 1;
		for(int k = 0; k < K; k++) {
			var succ = keySet.ceiling(key + dx);
			if(succ == null) succ = keySet.first();
			if(k == 0) { // first successor
				actors.get(key).tell(succ, null);
			}
			
			fingerTable.put(succ, actors.get(succ));
			dx = dx << 1;
		}
		return fingerTable;
	}

}
