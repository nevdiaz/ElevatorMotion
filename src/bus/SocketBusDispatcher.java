package bus;

import java.io.DataInputStream;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Talks to Team03's real Software Bus over a TCP socket, using the same BusMessageDispatcher methods as BusStandIn.
 *
 * The real bus never pushes messages, we have to ask for the messages with GET. So a small background thread asks for
 * every subscription over and over and puts what it finds in a local inbox. get() only reads that inbox and
 * never waits on the network, so the cabin's thread is not slowed down by the connection.
 *
 * Everything written to the socket goes through one lock, so a GET and its reply are never split by another request.
 * Use one SocketBusDispatcher per car; the bus does not give a connection its own messages back.
 *
 * If the connection breaks, the dispatcher stops (get returns nothing, publish does nothing) and getFailure()
 * says why. It never throws for network trouble, because the car's thread must keep simulating.
 */
public class SocketBusDispatcher implements BusMessageDispatcher, AutoCloseable {

    private static final int MAX_QUEUE_SIZE = 1024;
    private static final int READ_TIMEOUT_MS = 5000;     //Team03's README: never let a GET hang forever
    private static final long IDLE_PAUSE_MS = 2;         //pause when a pass over all subscriptions found nothing
    private static final long RETRY_PAUSE_MS = 100;

    private final Socket socket;
    private final DataInputStream in;
    private final OutputStream out;
    private final Object wire = new Object();     //one request on the socket at a time
    private final ConcurrentHashMap<Integer, ConcurrentLinkedDeque<BusMessage>> inboxes = new ConcurrentHashMap<>();
    private final Thread poller;

    private volatile boolean closed = false;
    private volatile IOException failure = null;
    private volatile int id = 0;

    /**
     * Connects to the bus. The bus may still be starting, so a refused connection is retried every 0.1 s,
     * up to attempts times (Team03's README suggests about 50).
     */
    public static SocketBusDispatcher connect(String host, int port, int attempts) throws IOException {
        if (attempts < 1) {
            throw new IllegalArgumentException("attempts must be at least 1");
        }
        ConnectException last = null;
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                return new SocketBusDispatcher(new Socket(host, port));
            } catch (ConnectException e) {
                last = e;
                try {
                    Thread.sleep(RETRY_PAUSE_MS);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }
        throw last;
    }

    private SocketBusDispatcher(Socket socket) throws IOException {
        this.socket = socket;
        socket.setSoTimeout(READ_TIMEOUT_MS);
        this.in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        this.out = socket.getOutputStream();
        this.poller = new Thread(this::pollLoop, "bus-poller");
        poller.setDaemon(true);
        poller.start();
    }

    /** The ID the bus gave us, or 0 before register() has finished. */
    public int getId() {
        return id;
    }

    public boolean isConnected() {
        return failure == null && !closed;
    }

    /** Why the connection stopped, or null if it has not. */
    public IOException getFailure() {
        return failure;
    }

    @Override
    public void register() {
        synchronized (wire) {
            if (id != 0 || !isConnected()) {
                return;
            }
            try {
                writeFrame(BusMessage.of(BusCodes.REGISTER, 0, 0));
                BusMessage reply = readFrame();     //REGISTER always gets exactly one reply, ID in the subtopic byte
                if (reply.getTopic() != BusCodes.REGISTER) {
                    throw new IOException("expected a REGISTER reply, got " + reply);
                }
                id = reply.getSubtopic();
            } catch (IOException e) {
                fail(e);
                return;
            }
        }
        if (id == 0) {
            throw new IllegalStateException("the bus refused REGISTER (no IDs left)");
        }
    }

    @Override
    public void subscribe(int topic, int subtopic) {
        checkTopicAndSubtopic(topic, subtopic);
        //Create the inbox first, so nothing the bus sends after SUBSCRIBE can be lost
        if (inboxes.putIfAbsent(key(topic, subtopic), new ConcurrentLinkedDeque<>()) == null) {
            send(BusMessage.of(BusCodes.SUBSCRIBE, topic, subtopic));
            //The bus does not reply to SUBSCRIBE and handles each connection on its own thread, so a message
            //published by someone else right after this call could be missed. One GET on this same connection
            //gets answered only after the SUBSCRIBE was processed, so when it returns the subscription is active.
            drain(key(topic, subtopic));
        }
    }

    @Override
    public void unsubscribe(int topic, int subtopic) {
        checkTopicAndSubtopic(topic, subtopic);
        inboxes.remove(key(topic, subtopic));
        send(BusMessage.of(BusCodes.UNSUBSCRIBE, topic, subtopic));
    }

    @Override
    public void publish(BusMessage message) {
        if (message == null || message.getTopic() < 1 || message.getTopic() > BusCodes.MAX_TOPIC) {
            throw new IllegalArgumentException("message must not be null and its topic must be 1 to " + BusCodes.MAX_TOPIC);
        }
        send(message);
    }

    @Override
    public Optional<BusMessage> get(int topic, int subtopic) {
        checkTopicAndSubtopic(topic, subtopic);
        ConcurrentLinkedDeque<BusMessage> inbox = inboxes.get(key(topic, subtopic));
        if (inbox == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(inbox.pollFirst());     //oldest first
    }

    @Override
    public void close() {
        closed = true;
        poller.interrupt();
        try {
            socket.close();
        } catch (IOException ignored) {
            //nothing useful to do
        }
    }

    //Background thread: ask the bus for the queued messages of every subscription, again and again
    private void pollLoop() {
        while (isConnected()) {
            boolean foundAny = false;
            for (Integer key : inboxes.keySet()) {
                if (drain(key)) {
                    foundAny = true;
                }
            }
            if (!foundAny) {
                try {
                    Thread.sleep(IDLE_PAUSE_MS);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
    }

    //GET until NO_MESSAGE for one subscription. Returns true if at least one message came.
    private boolean drain(int key) {
        boolean any = false;
        while (isConnected()) {
            BusMessage reply;
            synchronized (wire) {
                if (!inboxes.containsKey(key)) {
                    return any;       //unsubscribed in the meantime
                }
                try {
                    writeFrame(BusMessage.of(BusCodes.GET, key / 256, key % 256));
                    reply = readFrame();
                } catch (IOException e) {
                    fail(e);
                    return any;
                }
            }
            if (reply.getTopic() == BusCodes.NO_MESSAGE) {
                return any;
            }
            ConcurrentLinkedDeque<BusMessage> inbox = inboxes.get(key);
            if (inbox != null) {
                //Same as the real bus: when the queue is full the oldest message is dropped
                while (inbox.size() >= MAX_QUEUE_SIZE) {
                    inbox.pollFirst();
                }
                inbox.addLast(reply);
            }
            any = true;
        }
        return any;
    }

    private void send(BusMessage message) {
        synchronized (wire) {
            if (!isConnected()) {
                return;
            }
            try {
                writeFrame(message);
            } catch (IOException e) {
                fail(e);
            }
        }
    }

    private void writeFrame(BusMessage message) throws IOException {
        out.write(message.toBytes());
        out.flush();
    }

    private BusMessage readFrame() throws IOException {
        byte[] word = new byte[BusMessage.WORD_SIZE];
        in.readFully(word);        //throws EOFException if the bus closed the connection
        return BusMessage.fromBytes(word);
    }

    private void fail(IOException e) {
        if (failure == null && !closed) {
            failure = e;
        }
        try {
            socket.close();
        } catch (IOException ignored) {
            //nothing useful to do
        }
    }

    private static int key(int topic, int subtopic) {
        return topic * 256 + subtopic;
    }

    private static void checkTopicAndSubtopic(int topic, int subtopic) {
        if (topic < 1 || topic > BusCodes.MAX_TOPIC || subtopic < 0 || subtopic > 255) {
            throw new IllegalArgumentException("topic must be 1 to " + BusCodes.MAX_TOPIC + " and subtopic 0 to 255");
        }
    }
}