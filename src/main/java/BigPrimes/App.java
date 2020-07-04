package BigPrimes;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.AskPattern;

import java.math.BigInteger;
import java.time.Duration;
import java.util.SortedSet;
import java.util.concurrent.CompletionStage;


public class App {

    public static void main(String[] args) {
        ActorSystem<ManagerBehavior.Command> actorSystem = ActorSystem.create(ManagerBehavior.create(), "Manager");
        //   actorSystem.tell(new ManagerBehavior.InstructionCommand("create"));

        CompletionStage<SortedSet<BigInteger>> results =
                AskPattern.ask(actorSystem, (me) -> new ManagerBehavior.InstructionCommand("create", me),
                        Duration.ofSeconds(40),
                        actorSystem.scheduler());

        results.whenComplete((reply,failure)->{
            if(null != reply){
                reply.forEach(System.out::println);
            }else{
                System.out.println("System Error");
            }
            actorSystem.terminate();
        });
    }
}
