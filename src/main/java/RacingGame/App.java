package RacingGame;

import akka.actor.typed.ActorSystem;

public class App {

    public static void main(String[] args) {
        ActorSystem<ControllerBehavior.Command> actorSystem =
                ActorSystem.create(ControllerBehavior.create(),"Controller");
        actorSystem.tell(new ControllerBehavior.InstructionCommand("start",10));
    }
}
