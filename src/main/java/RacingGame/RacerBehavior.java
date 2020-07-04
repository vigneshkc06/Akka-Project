package RacingGame;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.io.Serializable;
import java.util.Random;

public class RacerBehavior extends AbstractBehavior<RacerBehavior.Command> {

    public interface Command extends Serializable{}

    public static class StartCommand implements Command {
        private static final long serialVersionUID = 1L;
        private int raceLength;

        public StartCommand(int raceLength) {
            this.raceLength = raceLength;
        }

        public int getRaceLength() {
            return raceLength;
        }
    }

    public static class PositionCommand implements Command {
        private static final long serialVersionUID = 1L;
        private ActorRef<ControllerBehavior.Command> sender;

        public PositionCommand(ActorRef<ControllerBehavior.Command> sender) {
            this.sender = sender;
        }

        public ActorRef<ControllerBehavior.Command> getSender() {
            return sender;
        }
    }

    private final double defaultAverageSpeed = 48.2;
    private int averageSpeedAdjustmentFactor;
    private Random random;


    private double currentSpeed = 0;

    private RacerBehavior(ActorContext<Command> context) {
        super(context);
    }

    public static Behavior<Command> create(){
       return Behaviors.setup(RacerBehavior::new);
    }

    private double getMaxSpeed() {
        return defaultAverageSpeed * (1+((double)averageSpeedAdjustmentFactor / 100));
    }

    private double getDistanceMovedPerSecond() {
        return currentSpeed * 1000 / 3600;
    }

    private void determineNextSpeed(double currentPosition,int raceLength) {
        if (currentPosition < (raceLength / 4)) {
            currentSpeed = currentSpeed  + (((getMaxSpeed() - currentSpeed) / 10) * random.nextDouble());
        }
        else {
            currentSpeed = currentSpeed * (0.5 + random.nextDouble());
        }

        if (currentSpeed > getMaxSpeed())
            currentSpeed = getMaxSpeed();

        if (currentSpeed < 5)
            currentSpeed = 5;

        if (currentPosition > (raceLength / 2) && currentSpeed < getMaxSpeed() / 2) {
            currentSpeed = getMaxSpeed() / 2;
        }
    }

    @Override
    public Receive<Command> createReceive() {
        return notYetStarted();
    }

    public Receive<Command> notYetStarted() {
        return newReceiveBuilder()
                .onMessage(StartCommand.class,m->{
                    int raceLength = m.getRaceLength();
                    this.random = new Random();
                    this.averageSpeedAdjustmentFactor = random.nextInt(30)-10;
                    return running(0,raceLength);
                })
                .onMessage(PositionCommand.class,p->{
                    double currentPostion = 0;
                    p.getSender().tell(new ControllerBehavior.ResultCommand(getContext().getSelf(),0));
                    return Behaviors.same();
                })
                .build();
    }

    public Receive<Command> running(double currentPos,int raceLength) {
        return newReceiveBuilder()
                .onMessage(PositionCommand.class,p->{
                    double currentPosition = currentPos;
                    determineNextSpeed(currentPosition,raceLength);
                    currentPosition += getDistanceMovedPerSecond();
                    if (currentPosition > raceLength )
                        currentPosition  = raceLength;
                    p.getSender().tell(new ControllerBehavior.ResultCommand(getContext().getSelf(),currentPosition));
                    if( currentPosition < raceLength)
                        return running(currentPosition,raceLength);
                    else
                        return completed(currentPosition);
                })
                .build();
    }

    public Receive<Command> completed(double currentPos) {
        return newReceiveBuilder()
                .onMessage(PositionCommand.class,p->{
                    p.getSender().tell(new ControllerBehavior.ResultCommand(getContext().getSelf(),currentPos));
                    p.getSender().tell(new ControllerBehavior.FinishedCommand(getContext().getSelf()));
                    return beforeStop();
                })
                .build();
    }

    public Receive<Command> beforeStop() {
        return newReceiveBuilder()
                .onAnyMessage(c->{
                    return Behaviors.same();
                })
                .onSignal(PostStop.class,signal->{
                    System.out.println("I am about to stop");
                    return Behaviors.same();
                })
                .build();
    }
}
