package ch.fhnw.ds.dht.complete;

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
		public Get(Object key) { this.key = key; }
	}
	static class Get2 {
		public final Object key;
		public Get2(Object key) { this.key = key; }
	}
	static class Result {
		public final int id;
		public Object value;
		public Result(int id, Object value) { this.id = id; this.value = value; }
		public String toString() { return String.format("Result(id = %d, value = %s)", id, value); }
	}
	static class AddNode {
		public final int id;
		public final ActorRef actor;
		public AddNode(int id, ActorRef actor) { this.id = id; this.actor = actor; }
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
	
	static final int N = 100;	// size of the ring, i.e. number of nodes

	public static void main(String[] args) throws Exception {
		ActorSystem as = ActorSystem.create();
		Random r = new Random();
		
		TreeMap<Integer, ActorRef> actors = new TreeMap<>();
		for(int i = 0; i < N; i++) { 
			int id = r.nextInt();
			actors.put(id, as.actorOf(Props.create(HashNode.class, id), "Node:"+id));
		}
		
		actors.values().forEach(a -> a.tell(actors.clone(), null));
		
		ActorRef a0 = actors.firstEntry().getValue();
		System.out.println("a0 = " + a0);
		
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

			case "add":
				int id = r.nextInt();
				if(cmd.length > 1) {
					id = Integer.parseInt(cmd[1]);
				}
				while(actors.get(id) != null) id = r.nextInt();
				final int idfinal = id;
				
				var keys = actors.navigableKeySet();
				var succ = keys.ceiling(id);
				if(succ == null) succ = keys.first();
				ActorRef newActor = as.actorOf(Props.create(HashNode.class, id), "Node:"+id);
				newActor.tell(actors.clone(), null);
				actors.put(id, newActor);
				actors.values().forEach(a -> a.tell(new AddNode(idfinal, actors.get(idfinal)), null));
				actors.get(succ).tell(new Partition(id), actors.get(id));
				System.out.println("new node with id " + id);
				break;

			case "print":
				actors.values().forEach(a -> a.tell(new Print(), null));
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

}
