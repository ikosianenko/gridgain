// @java.file.header

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.examples.messaging;

import org.gridgain.grid.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.util.lang.*;

import java.util.*;
import java.util.concurrent.*;

/**
 * Demonstrates various messaging APIs.
 * <p>
 * <h1 class="header">Starting Remote Nodes</h1>
 * To try this example you need to start at least one remote grid instance.
 * You can start as many as you like by executing the following script:
 * <pre class="snippet">{GRIDGAIN_HOME}/bin/ggstart.{bat|sh} examples/config/example-default.xml</pre>
 * Once remote instances are started, you can execute this example from
 * Eclipse, IntelliJ IDEA, or NetBeans (and any other Java IDE) by simply hitting run
 * button. You will see that all nodes discover each other and
 * some of the nodes will participate in task execution (check node
 * output).
 *
 * @author @java.author
 * @version @java.version
 */
public class GridMessagingPingPongExample {
    /**
     * This example demonstrates simple protocol-based exchange in playing a ping-pong between
     * two nodes.
     *
     * @param args Command line arguments (none required).
     * @throws GridException Thrown in case of any errors.
     */
    public static void main(String[] args) throws GridException {
        // Game is played over the default grid.
        try (Grid g = GridGain.start("examples/config/example-default.xml")) {
            if (g.forRemotes().nodes().size() < 1) {
                System.err.println("I need a partner to play a ping pong!");

                return;
            }

            // Pick random remote node as a partner.
            GridProjection nodeB = g.forRemotes().forRandom();

            // Note that both nodeA and nodeB will always point to
            // same nodes regardless of whether they were implicitly
            // serialized and deserialized on another node as part of
            // anonymous closure's state during its remote execution.

            // Set up remote player.
            nodeB.message().remoteListen(null, new GridBiPredicate<UUID, String>() {
                @Override public boolean apply(UUID nodeId, String rcvMsg) {
                    System.out.println(rcvMsg);

                    try {
                        if ("PING".equals(rcvMsg)) {
                            g.forNodeId(nodeId).message().send(null, "PONG");

                            return true; // Continue listening.
                        }

                        return false; // Unsubscribe.
                    }
                    catch (GridException e) {
                        throw new GridClosureException(e);
                    }
                }
            }).get();

            int MAX_PLAYS = 10;

            final CountDownLatch cnt = new CountDownLatch(MAX_PLAYS);

            // Set up local player.
            g.message().localListen(null, new GridBiPredicate<UUID, String>() {
                @Override public boolean apply(UUID nodeId, String rcvMsg) {
                    System.out.println(rcvMsg);

                    try {
                        if (cnt.getCount() == 1) {
                            g.forNodeId(nodeId).message().send(null, "STOP");

                            return false; // Stop listening.
                        }
                        else if ("PONG".equals(rcvMsg))
                            g.forNodeId(nodeId).message().send(null, "PING");

                        cnt.countDown();

                        return true; // Continue listening.
                    }
                    catch (GridException e) {
                        throw new GridClosureException(e);
                    }
                }
            });

            // Serve!
            nodeB.message().send(null, "PING");

            // Wait til the game is over.
            try {
                cnt.await();
            }
            catch (InterruptedException e) {
                System.err.println("Hm... let us finish the game!\n" + e);
            }
        }
    }
}