package org.apache.zookeeper.mytests;


import org.apache.zookeeper.data.Id;
import org.apache.zookeeper.server.quorum.Learner;
import org.apache.zookeeper.server.quorum.LearnerHandler;
import org.apache.zookeeper.server.quorum.ObserverMaster;
import org.apache.zookeeper.server.quorum.QuorumPacket;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

@RunWith(value = Parameterized.class)
public class ObserverMasterCacheCommittedPacketTest {

    private static int pktSizeLimit;

    //class to test
    private ObserverMaster obsM;

    //arguments
    private boolean expResult;
    private QuorumPacket qp;



    public ObserverMasterCacheCommittedPacketTest(boolean expResult, QuorumPacket qp) {
        //the arguments of the contructor are not used in the method we are testing
        //we can leave them as null
        obsM = new ObserverMaster(null, null, 80);
        //System.setProperty("zookeeper.observerMaster.sizeLimit", "2000");
        this.expResult = expResult;
        this.qp = qp;

    }

    @Before
    public void setUpQueue() {
        //add some dummy packets in the queue to check if they are removed correctly
        pktSizeLimit = Integer.getInteger("zookeeper.observerMaster.sizeLimit", 32 * 1024 * 1024);

        //System.out.println("askd: " + pktSizeLimit + " " + pktNumber);
        long pktSize = 0;
        while (pktSize < pktSizeLimit-300) {
            QuorumPacket qp = new QuorumPacket(0, 1234, "d".getBytes(), null);
            obsM.cacheCommittedPacket(qp);
            pktSize += LearnerHandler.packetSize(qp);
        }

        //System.out.println("askd: " + (pktSize - pktSizeLimit));

    }

    @Parameterized.Parameters
    public static Collection<?> getTestParameters() {
        //function signature
        //void cacheCommittedPacket(final QuorumPacket pkt)

        //some auth info - not relevant to the test
        Id dummyId = new Id("dummy", "dummy");
        List<Id> dummyAuthInfo = new ArrayList<Id>();
        dummyAuthInfo.add(dummyId);


        //filling the byte arrays with dummy data
        byte[] almostOverSizedArray = new byte[299-28];
        byte[] overSizedArray = new byte[350];
        byte[] reallyOverSizedArray = new byte[6713523];

        QuorumPacket undersizedPkt = new QuorumPacket(0, 1234, "dummyy".getBytes(), dummyAuthInfo);
        QuorumPacket almostOverSizedPkt = new QuorumPacket(0, 1234, almostOverSizedArray, dummyAuthInfo);
        QuorumPacket overSizedPkt = new QuorumPacket(0, 1234, overSizedArray, dummyAuthInfo);
        QuorumPacket reallyOverSizedPkt = new QuorumPacket(0, 1234, reallyOverSizedArray, dummyAuthInfo);

        return Arrays.asList(new Object[][]{

                //null packets are not allowed
                {false, null},

                //valid configurations
                {true, undersizedPkt},
                {true, almostOverSizedPkt},
                {true, overSizedPkt},
                {true, reallyOverSizedPkt}
        });

    }

    @Test
    public void cacheCommittedPacketTest() {


        //call the test method
        try {
            obsM.cacheCommittedPacket(qp);
        } catch (NullPointerException e) {
            Assert.assertNull(qp);
            return;
        }

        //get the committed packet queue
        ConcurrentLinkedQueue<QuorumPacket> queue = obsM.getCommittedPkts();

        //check that our packet has been added
        Assert.assertTrue(!expResult || queue.contains(qp));

        //check that queue has not exceeded maximum size
        int queueSize = 0;
        QuorumPacket pkt;
        while (true) {
            pkt = queue.poll();
            if (pkt == null) {
                break;
            }
            queueSize += LearnerHandler.packetSize(pkt);
        }

        Assert.assertTrue(queueSize < pktSizeLimit);



    }

}
