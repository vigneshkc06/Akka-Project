import BlockChain.blockchain.ManagerBehavior;
import BlockChain.blockchain.WorkerBehavior;
import BlockChain.model.Block;
import BlockChain.model.HashResult;
import BlockChain.utils.BlocksData;
import akka.actor.testkit.typed.CapturedLogEvent;
import akka.actor.testkit.typed.javadsl.BehaviorTestKit;
import akka.actor.testkit.typed.javadsl.TestInbox;
import org.junit.jupiter.api.Test;
import org.slf4j.event.Level;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MiningTests {

    @Test
    void testMiningNoMessage(){
       /// fail();
        BehaviorTestKit<WorkerBehavior.Command> testActor = BehaviorTestKit.create(WorkerBehavior.create());
        Block b = BlocksData.getNextBlock(0,"0");
        TestInbox<ManagerBehavior.Command> testInbox = TestInbox.create();
        WorkerBehavior.Command message = new WorkerBehavior.Command(b,0,5,testInbox.getRef());
        testActor.run(message);
        List<CapturedLogEvent> logEventList = testActor.getAllLogEntries();
        assertEquals(logEventList.size(),1);
        assertEquals(logEventList.get(0).message(),"Null");
        assertEquals(logEventList.get(0).level(), Level.DEBUG);
    }

    @Test
    void testMiningPass(){
        BehaviorTestKit<WorkerBehavior.Command> testActor = BehaviorTestKit.create(WorkerBehavior.create());
        Block b = BlocksData.getNextBlock(0,"0");
        TestInbox<ManagerBehavior.Command> testInbox = TestInbox.create();
        WorkerBehavior.Command message = new WorkerBehavior.Command(b,82300,1,testInbox.getRef());
        testActor.run(message);
        List<CapturedLogEvent> logEventList = testActor.getAllLogEntries();
        assertEquals(logEventList.size(),1);
        String expectedResult = "82309 : 051cd299dd4016ed1c1f30b59b100243149edb94bc97efb0598f2d4d872c036d";
        assertEquals(logEventList.get(0).message(),expectedResult);
        assertEquals(logEventList.get(0).level(), Level.DEBUG);
    }


    @Test
    void testMiningPassWithTestInbox(){
        BehaviorTestKit<WorkerBehavior.Command> testActor = BehaviorTestKit.create(WorkerBehavior.create());
        Block b = BlocksData.getNextBlock(0,"0");
        TestInbox<ManagerBehavior.Command> testInbox = TestInbox.create();
        WorkerBehavior.Command message = new WorkerBehavior.Command(b,82300,1,testInbox.getRef());
        testActor.run(message);
        HashResult expectedHash = new HashResult();
        expectedHash.foundAHash("051cd299dd4016ed1c1f30b59b100243149edb94bc97efb0598f2d4d872c036d",82309);
        ManagerBehavior.Command command = new ManagerBehavior.HashResultCommand(expectedHash);
        testInbox.expectMessage(command);
    }

    @Test
    void testMiningNoMessageWithInbox(){
        /// fail();
        BehaviorTestKit<WorkerBehavior.Command> testActor = BehaviorTestKit.create(WorkerBehavior.create());
        Block b = BlocksData.getNextBlock(0,"0");
        TestInbox<ManagerBehavior.Command> testInbox = TestInbox.create();
        WorkerBehavior.Command message = new WorkerBehavior.Command(b,0,5,testInbox.getRef());
        testActor.run(message);
        assertFalse(testInbox.hasMessages());
    }
}


