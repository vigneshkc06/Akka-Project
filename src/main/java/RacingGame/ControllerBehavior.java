package RacingGame;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.io.Serializable;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

public class ControllerBehavior extends AbstractBehavior<ControllerBehavior.Command> {

    public interface Command extends Serializable {}

    public static class InstructionCommand implements Command{
        private static final long serialVersionUID = 1L;
        private String message;
        private int noOfRacers;

        public InstructionCommand(String message, int noOfRacers) {
            this.message = message;
            this.noOfRacers = noOfRacers;
        }

        public String getMessage() {
            return message;
        }

        public int getNoOfRacers() {
            return noOfRacers;
        }
    }


    public static class ResultCommand implements Command{
        private static final long serialVersionUID = 1L;
        private ActorRef<RacerBehavior.Command> racer;
        private double currentPosition;

        public ResultCommand(ActorRef<RacerBehavior.Command> racer, double currentPosition) {
            this.racer = racer;
            this.currentPosition = currentPosition;
        }

        public double getCurrentPosition() {
            return currentPosition;
        }

        public ActorRef<RacerBehavior.Command> getRacer() {
            return racer;
        }
    }

    private class PositionCommand implements Command{
        private static final long serialVersionUID = 1L;
    }

    public static class FinishedCommand implements Command{
        private static final long serialVersionUID = 1L;
        private ActorRef<RacerBehavior.Command> racer;

        public FinishedCommand(ActorRef<RacerBehavior.Command> racer) {
            this.racer = racer;
        }

        public ActorRef<RacerBehavior.Command> getRacer() {
            return racer;
        }
    }


    private Map<ActorRef<RacerBehavior.Command> ,Double> currentPositions = new HashMap<>();
    private Map<ActorRef<RacerBehavior.Command> ,Long> finishedPosition = new HashMap<>();
    private long start;
    private int raceLength = 10;
    private Object TIMER_KEY;

    private void displayRace() {
        int displayLength = 160;
        for (int i = 0; i < 50; ++i) System.out.println();
        System.out.println("Race has been running for " + ((System.currentTimeMillis() - start) / 1000) + " seconds.");
        System.out.println("    " + new String (new char[displayLength]).replace('\0', '='));
        System.out.println(currentPositions);
        int i=0;
        for (ActorRef<RacerBehavior.Command> r: currentPositions.keySet()) {
            System.out.println(i + " : "  +
                    new String (new char[(int) (currentPositions.get(r) * displayLength / 100)])
                            .replace('\0', '*'));
            i++;
        }
    }

    private ControllerBehavior(ActorContext<Command> context) {
        super(context);
    }

    public static Behavior<Command> create(){
        return Behaviors.setup(ControllerBehavior::new);
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(InstructionCommand.class,i->{
                    if(i.getMessage().equals("start")){
                        start = System.currentTimeMillis();
                        IntStream.rangeClosed(1,i.getNoOfRacers()).forEach(r->{
                            ActorRef<RacerBehavior.Command> racer =
                                    getContext().spawn(RacerBehavior.create(),"Racer"+r);
                            currentPositions.put(racer,0.0);
                            racer.tell(new RacerBehavior.StartCommand(raceLength));
                        });
                    }
                    return Behaviors.withTimers(timer -> {
                        timer.startTimerAtFixedRate(TIMER_KEY,new PositionCommand(), Duration.ofSeconds(1));
                        return Behaviors.same();
                    });
                })
                .onMessage(PositionCommand.class,c->{
                    currentPositions.entrySet().forEach(r->{
                        r.getKey().tell(new RacerBehavior.PositionCommand(getContext().getSelf()));
                    });
                    displayRace();
                    return Behaviors.same();
                })
                .onMessage(ResultCommand.class,c->{
                    currentPositions.put(c.getRacer(),c.getCurrentPosition());
                    return Behaviors.same();
                })
                .onMessage(FinishedCommand.class,c->{
                    finishedPosition.put(c.getRacer(),System.currentTimeMillis());
                    if(finishedPosition.size() == 10)
                        return raceCompletedReceiver();
                    return Behaviors.same();
                })
                .build();
    }

    public Receive<Command> raceCompletedReceiver(){
        return newReceiveBuilder()
                .onMessage(PositionCommand.class,c->{
//                    for(ActorRef<RacerBehavior.Command> ref: currentPositions.keySet()){
//                        getContext().stop(ref);
//                    }
                    displayResults();
                    return Behaviors.withTimers(t->{
                        t.cancelAll();
                        return Behaviors.stopped();
                    });
                })
                .build();
    }

    private void displayResults(){
        System.out.println("Results");
        finishedPosition.values().stream().sorted().forEach(it -> {
            for (ActorRef<RacerBehavior.Command> key : finishedPosition.keySet()) {
                if (finishedPosition.get(key) == it) {
                    getContext().getLog().info("Racer " + key + " finished in " + ((double) it - start) / 1000 + " seconds.");
                }
            }
        });
    }
}
